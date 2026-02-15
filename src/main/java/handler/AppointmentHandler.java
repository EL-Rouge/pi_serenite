package handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.AppointmentRequest;
import service.AppointmentRequestService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class AppointmentHandler implements HttpHandler {

    private final AppointmentRequestService service = new AppointmentRequestService();
    private final ObjectMapper mapper;

    public AppointmentHandler() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET"    -> handleGet(exchange, path);
                case "POST"   -> handlePost(exchange);
                case "PUT"    -> handlePut(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default       -> sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    // ─── GET /api/appointments        → all
    // ─── GET /api/appointments/{id}   → one
    private void handleGet(HttpExchange exchange, String path) throws SQLException, IOException {
        if (isCollectionPath(path)) {
            sendResponse(exchange, 200, mapper.writeValueAsString(service.getAllAppointments()));
        } else {
            long id = extractId(path);
            AppointmentRequest app = service.getById(id);
            sendResponse(exchange, 200, mapper.writeValueAsString(app));
        }
    }

    // ─── POST /api/appointments
    private void handlePost(HttpExchange exchange) throws SQLException, IOException {
        AppointmentRequest app = mapper.readValue(exchange.getRequestBody(), AppointmentRequest.class);
        AppointmentRequest created = service.createAppointment(app);
        sendResponse(exchange, 201, mapper.writeValueAsString(created));
    }

    // ─── PUT /api/appointments/{id}
    private void handlePut(HttpExchange exchange, String path) throws SQLException, IOException {
        long id = extractId(path);
        AppointmentRequest app = mapper.readValue(exchange.getRequestBody(), AppointmentRequest.class);
        app.setId(id);
        service.updateAppointment(app);
        sendResponse(exchange, 200, mapper.writeValueAsString(app));
    }

    // ─── DELETE /api/appointments/{id}
    private void handleDelete(HttpExchange exchange, String path) throws SQLException, IOException {
        long id = extractId(path);
        service.cancelAppointment(id);
        sendResponse(exchange, 200, "{\"message\":\"Appointment deleted\"}");
    }

    // ─── Utilities ───────────────────────────────────────────────

    private boolean isCollectionPath(String path) {
        // true if path is exactly /api/appointments (no ID at the end)
        return path.replaceAll("/$", "").equals("/api/appointments");
    }

    private long extractId(String path) {
        String[] parts = path.split("/");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID in path: " + parts[parts.length - 1]);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
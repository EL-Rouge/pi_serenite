package handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Consultation;
import service.ConsultationService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Routes handled:
 *
 *  GET    /api/consultations                             → all consultations
 *  GET    /api/consultations/{id}                        → one consultation
 *  GET    /api/consultations/client/{clientId}           → by client
 *  GET    /api/consultations/doctor/{doctorId}           → by doctor
 *  GET    /api/consultations/appointment/{appointmentId} → by appointment
 *
 *  POST   /api/consultations                             → create (appointment must be CONFIRMED)
 *
 *  PUT    /api/consultations/{id}                        → full update
 *
 *  DELETE /api/consultations/{id}                        → delete
 */
public class ConsultationHandler implements HttpHandler {

    private final ConsultationService service = new ConsultationService();
    private final ObjectMapper        mapper;

    public ConsultationHandler() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ─── Entry point ─────────────────────────────────────────────

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath().replaceAll("/$", "");

        try {
            switch (method) {
                case "GET"    -> handleGet(exchange, path);
                case "POST"   -> handlePost(exchange, path);
                case "PUT"    -> handlePut(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default       -> sendResponse(exchange, 405, error("Method not allowed"));
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, error("Database error: " + e.getMessage()));
        } catch (IllegalStateException e) {
            // appointment not CONFIRMED → 409 Conflict
            sendResponse(exchange, 409, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, error(e.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, error("Unexpected error: " + e.getMessage()));
        }
    }

    // ─── GET ─────────────────────────────────────────────────────

    private void handleGet(HttpExchange exchange, String path) throws SQLException, IOException {

        // GET /api/consultations
        if (isExactCollection(path)) {
            sendResponse(exchange, 200, mapper.writeValueAsString(service.getAllConsultations()));
            return;
        }

        // GET /api/consultations/client/{clientId}
        if (path.matches(".*/consultations/client/\\d+")) {
            long clientId = extractTrailingId(path);
            sendResponse(exchange, 200, mapper.writeValueAsString(service.getByClientId(clientId)));
            return;
        }

        // GET /api/consultations/doctor/{doctorId}
        if (path.matches(".*/consultations/doctor/\\d+")) {
            long doctorId = extractTrailingId(path);
            sendResponse(exchange, 200, mapper.writeValueAsString(service.getByDoctorId(doctorId)));
            return;
        }

        // GET /api/consultations/appointment/{appointmentId}
        if (path.matches(".*/consultations/appointment/\\d+")) {
            long appointmentId = extractTrailingId(path);
            sendResponse(exchange, 200,
                    mapper.writeValueAsString(service.getByAppointmentRequestId(appointmentId)));
            return;
        }

        // GET /api/consultations/{id}
        if (path.matches(".*/consultations/\\d+")) {
            long id = extractTrailingId(path);
            sendResponse(exchange, 200, mapper.writeValueAsString(service.getById(id)));
            return;
        }

        sendResponse(exchange, 404, error("Unknown path: " + path));
    }

    // ─── POST ────────────────────────────────────────────────────

    private void handlePost(HttpExchange exchange, String path) throws SQLException, IOException {

        // POST /api/consultations
        if (isExactCollection(path)) {
            Consultation consultation = mapper.readValue(exchange.getRequestBody(), Consultation.class);
            Consultation created      = service.createConsultation(consultation);
            sendResponse(exchange, 201, mapper.writeValueAsString(created));
            return;
        }

        sendResponse(exchange, 404, error("Unknown path: " + path));
    }

    // ─── PUT ─────────────────────────────────────────────────────

    private void handlePut(HttpExchange exchange, String path) throws SQLException, IOException {

        // PUT /api/consultations/{id}
        if (path.matches(".*/consultations/\\d+")) {
            long         id           = extractTrailingId(path);
            Consultation consultation = mapper.readValue(exchange.getRequestBody(), Consultation.class);
            consultation.setId(id);
            Consultation updated = service.updateConsultation(consultation);
            sendResponse(exchange, 200, mapper.writeValueAsString(updated));
            return;
        }

        sendResponse(exchange, 404, error("Unknown path: " + path));
    }

    // ─── DELETE ──────────────────────────────────────────────────

    private void handleDelete(HttpExchange exchange, String path) throws SQLException, IOException {

        // DELETE /api/consultations/{id}
        if (path.matches(".*/consultations/\\d+")) {
            long id = extractTrailingId(path);
            service.deleteConsultation(id);
            sendResponse(exchange, 200, "{\"message\":\"Consultation deleted\"}");
            return;
        }

        sendResponse(exchange, 404, error("Unknown path: " + path));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private boolean isExactCollection(String path) {
        return path.equals("/api/consultations");
    }

    private long extractTrailingId(String path) {
        String[] parts = path.split("/");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID: " + parts[parts.length - 1]);
        }
    }

    private String error(String message) {
        return "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
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
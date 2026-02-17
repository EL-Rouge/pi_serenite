package handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Consultation;
import service.ConsultationService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ConsultationHandler implements HttpHandler {

    private final ConsultationService service = new ConsultationService();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET"    -> handleGet(exchange, path);
                case "POST"   -> handlePost(exchange);
                case "PUT"    -> handlePut(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default       -> send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            send(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGet(HttpExchange ex, String path) throws Exception {
        send(ex, 200, mapper.writeValueAsString(service.getAllConsultations()));
    }

    private void handlePost(HttpExchange ex) throws Exception {
        Consultation c = mapper.readValue(ex.getRequestBody(), Consultation.class);
        Consultation created = service.createConsultation(c);
        send(ex, 201, mapper.writeValueAsString(created));
    }

    private void handlePut(HttpExchange ex, String path) throws Exception {
        long id = Long.parseLong(path.substring(path.lastIndexOf("/") + 1));
        Consultation c = mapper.readValue(ex.getRequestBody(), Consultation.class);
        c.setId(id);
        service.updateConsultation(c);
        send(ex, 200, mapper.writeValueAsString(c));
    }

    private void handleDelete(HttpExchange ex, String path) throws Exception {
        long id = Long.parseLong(path.substring(path.lastIndexOf("/") + 1));
        service.deleteConsultation(id);
        send(ex, 200, "{\"message\":\"Deleted\"}");
    }

    private void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
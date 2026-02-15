package pi_serenite;

import com.sun.net.httpserver.HttpServer;
import handler.AppointmentHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main extends Application {

    private HttpServer server;

    @Override
    public void start(Stage stage) {
        try {
            // start the HTTP server in background
            startHttpServer();

            // load the FXML and show the window
            // Note: Corrected path casing to match file system if needed,
            // usually resources are case-sensitive in JARs but loose on Windows.
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/appointmentview/ClientAppointments.fxml"));
            Parent root = loader.load();

            stage.setTitle("Create Appointment");
            stage.setScene(new Scene(root, 900, 700)); // Increased size for better view
            stage.show();
        } catch (Exception e) {
            System.err.println("CRASH DURING STARTUP:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void startHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/appointments", new AppointmentHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP Server running → http://localhost:8080");
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped.");
        }
        super.stop();
        System.exit(0); // Ensure all threads are killed
    }

    public static void main(String[] args) {
        launch(args);
    }
}
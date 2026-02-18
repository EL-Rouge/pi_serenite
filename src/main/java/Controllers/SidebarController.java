package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class SidebarController {

    @FXML private Button btnDashboard;
    @FXML private Button btnAppointments;
    @FXML private Button btnConsultations;
    @FXML private Button btnProfile;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    // ── Navigation helpers ───────────────────────────────────────

    @FXML private void handleDashboard()     { navigateTo("/view/Dashboard.fxml"); }
    @FXML private void handleAppointments()  { navigateTo("/fxml/appointmentview/ClientAppointments.fxml"); }
    @FXML private void handleConsultations() {
        navigateTo("/fxml/consultationview/DoctorConsultationView.fxml");
    }

    @FXML private void handleProfile()  { navigateTo("/fxml/appointmentview/DoctorDashboard.fxml"); }
    @FXML private void handleSettings() { navigateTo("/fxml/consultationview/ConsultationListView.fxml"); }

    @FXML
    private void handleLogout() {
        // TODO: clear session then navigate to login
        navigateTo("/view/Login.fxml");
    }

    // ── Highlight active button ──────────────────────────────────

    private void setActive(Button active) {
        for (Button b : new Button[]{btnDashboard, btnAppointments, btnConsultations,
                btnProfile, btnSettings}) {
            b.getStyleClass().remove("sidebar-btn-active");
        }
        if (active != null) active.getStyleClass().add("sidebar-btn-active");
    }

    // ── Generic navigation ───────────────────────────────────────

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("[SidebarController] Navigation failed: " + fxmlPath);
            e.printStackTrace(); // ← ADD THIS
        }
    }
}


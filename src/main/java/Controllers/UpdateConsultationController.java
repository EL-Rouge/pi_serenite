package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.Consultation;
import service.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateConsultationController {

    @FXML private TextField consultationIdField;
    @FXML private TextField appointmentIdField;
    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea diagnosisField;
    @FXML private TextArea prescriptionField;
    @FXML private TextArea notesField;
    @FXML private VBox mainContainer;
    @FXML private Label errorLabel;

    private final ConsultationService service = new ConsultationService();
    private Consultation consultation;

    // ── Called by parent controller to pre-fill the form ─────────
    public void initData(Consultation c) {
        this.consultation = c;

        consultationIdField.setText(String.valueOf(c.getId()));
        appointmentIdField.setText(String.valueOf(c.getAppointmentRequestId()));

        if (c.getConsultationDate() != null) {
            consultationDatePicker.setValue(c.getConsultationDate().toLocalDate());
        }

        diagnosisField.setText(c.getDiagnosis() != null ? c.getDiagnosis() : "");
        prescriptionField.setText(c.getPrescription() != null ? c.getPrescription() : "");
        notesField.setText(c.getNotes() != null ? c.getNotes() : "");

        hideError();
    }

    // ── Save updated consultation ────────────────────────────────
    @FXML
    private void handleUpdate() {
        hideError();

        try {
            if (consultationDatePicker.getValue() == null) {
                showError("Consultation date is required.");
                return;
            }

            String diagnosis = diagnosisField.getText() != null ? diagnosisField.getText().trim() : "";
            if (diagnosis.isBlank()) {
                showError("Diagnosis is required.");
                return;
            }

            consultation.setConsultationDate(LocalDateTime.of(
                    consultationDatePicker.getValue(), LocalTime.now()));
            consultation.setDiagnosis(diagnosis);
            consultation.setPrescription(prescriptionField.getText() != null ? prescriptionField.getText().trim() : "");
            consultation.setNotes(notesField.getText() != null ? notesField.getText().trim() : "");

            service.updateConsultation(consultation);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Consultation updated successfully.");
            closeWindow();

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    // ── Cancel editing ──────────────────────────────────────────
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    // ── Helpers ────────────────────────────────────────────────
    private void closeWindow() {
        mainContainer.getScene().getWindow().hide();
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠ " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}

package Controllers.Consultation;


import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.Consultation;
import service.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EditConsultationController {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label      headerSubtitle;
    @FXML private Label      metaConsultId;
    @FXML private Label      metaApptId;
    @FXML private Label      metaDate;

    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea   notesArea;
    @FXML private TextArea   diagnosisArea;
    @FXML private TextArea   prescriptionArea;
    @FXML private Label      errorLabel;

    // ── State ─────────────────────────────────────────────────────
    private Consultation consultation;

    private final ConsultationService consultationService = new ConsultationService();

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    // ── Init (called by ConsultationListController) ───────────────

    public void initData(Consultation c) {
        this.consultation = c;

        // ── Header ──
        headerSubtitle.setText("Consultation #" + c.getId()
                + "  ·  Appointment #" + c.getAppointmentRequestId());

        // ── Meta chips ──
        metaConsultId.setText("#" + c.getId());
        metaApptId.setText("#" + c.getAppointmentRequestId());
        metaDate.setText(c.getConsultationDate() != null
                ? c.getConsultationDate().format(DATETIME_FMT) : "—");

        // ── Pre-fill fields ──
        if (c.getConsultationDate() != null) {
            consultationDatePicker.setValue(c.getConsultationDate().toLocalDate());
        } else {
            consultationDatePicker.setValue(LocalDate.now());
        }

        notesArea.setText(c.getNotes() != null ? c.getNotes() : "");
        diagnosisArea.setText(c.getDiagnosis() != null ? c.getDiagnosis() : "");
        prescriptionArea.setText(c.getPrescription() != null ? c.getPrescription() : "");
    }

    // ── Actions ───────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        LocalDate selectedDate = consultationDatePicker.getValue();
        if (selectedDate == null) {
            showError("Please select a consultation date.");
            return;
        }

        String notes        = trim(notesArea.getText());
        String diagnosis    = trim(diagnosisArea.getText());
        String prescription = trim(prescriptionArea.getText());

        if (diagnosis.isEmpty()) {
            showError("Diagnosis is required.");
            return;
        }

        // ── Apply changes to the existing consultation object ──
        consultation.setNotes(notes.isEmpty() ? null : notes);
        consultation.setDiagnosis(diagnosis);
        consultation.setPrescription(prescription.isEmpty() ? null : prescription);

        // Preserve original time, only update the date part
        LocalDateTime updatedDateTime;
        if (consultation.getConsultationDate() != null) {
            updatedDateTime = selectedDate.atTime(
                    consultation.getConsultationDate().toLocalTime());
        } else {
            updatedDateTime = selectedDate.atTime(LocalDateTime.now().toLocalTime());
        }
        consultation.setConsultationDate(updatedDateTime);

        // ── Persist ──
        try {
            consultationService.updateConsultation(consultation);
            closeDialog();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void closeDialog() {
        Stage stage = (Stage) notesArea.getScene().getWindow();
        stage.close();
    }
}
package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.AppointmentRequest;
import models.Client;
import models.Consultation;
import service.ClientService;
import service.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddConsultationDoctorController {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label      headerSubtitle;
    @FXML private Label      metaApptId;
    @FXML private Label      metaClientName;
    @FXML private Label      metaConfirmedDate;

    @FXML private DatePicker consultationDatePicker;
    @FXML private TextArea   notesArea;
    @FXML private TextArea   diagnosisArea;
    @FXML private TextArea   prescriptionArea;
    @FXML private Label      errorLabel;

    // ── State ─────────────────────────────────────────────────────
    private AppointmentRequest appointment;
    private long               doctorId;

    private final ConsultationService consultationService = new ConsultationService();
    private final ClientService       clientService       = new ClientService();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    // ── Init (called by DoctorConsultationsController) ────────────

    public void initData(AppointmentRequest appt, long doctorId) {
        this.appointment = appt;
        this.doctorId    = doctorId;

        // ── Header subtitle ──
        headerSubtitle.setText(
                "Appointment #" + appt.getId() + "  ·  " + appt.getType());

        // ── Meta chips ──
        metaApptId.setText("#" + appt.getId());

        // Resolve client name
        try {
            Client client = clientService.getById(appt.getClientId());
            metaClientName.setText(client.getFullname().trim());
        } catch (Exception e) {
            metaClientName.setText("Client #" + appt.getClientId());
        }

        // Confirmed date
        if (appt.getConfirmedDate() != null) {
            metaConfirmedDate.setText(appt.getConfirmedDate().format(DATETIME_FMT));
            // Pre-fill the date picker with the confirmed date
            consultationDatePicker.setValue(appt.getConfirmedDate().toLocalDate());
        } else {
            metaConfirmedDate.setText("Not set");
            consultationDatePicker.setValue(LocalDate.now());
        }
    }

    // ── Actions ───────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        // ── Validation ──
        LocalDate selectedDate = consultationDatePicker.getValue();
        if (selectedDate == null) {
            showError("Please select a consultation date.");
            return;
        }

        String notes        = notesArea.getText()        == null ? "" : notesArea.getText().trim();
        String diagnosis    = diagnosisArea.getText()    == null ? "" : diagnosisArea.getText().trim();
        String prescription = prescriptionArea.getText() == null ? "" : prescriptionArea.getText().trim();

        if (diagnosis.isEmpty()) {
            showError("Diagnosis is required.");
            return;
        }

        // ── Build consultation ──
        Consultation consultation = new Consultation();
        consultation.setAppointmentRequestId(appointment.getId());
        consultation.setClientId(appointment.getClientId());
        consultation.setDoctorId(doctorId);
        consultation.setNotes(notes.isEmpty() ? null : notes);
        consultation.setDiagnosis(diagnosis);
        consultation.setPrescription(prescription.isEmpty() ? null : prescription);

// Always use the date the doctor selected, preserving the time from confirmedDate if available
        LocalDateTime consultationDateTime;
        if (appointment.getConfirmedDate() != null) {
            // Keep the confirmed time, but use the picker's date
            consultationDateTime = selectedDate.atTime(appointment.getConfirmedDate().toLocalTime());
        } else {
            consultationDateTime = selectedDate.atTime(LocalDateTime.now().toLocalTime());
        }
        consultation.setConsultationDate(consultationDateTime);

        // ── Persist ──
        try {
            consultationService.createConsultation(consultation);
            closeDialog();
        } catch (IllegalStateException | IllegalArgumentException e) {
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
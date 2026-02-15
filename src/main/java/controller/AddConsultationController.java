package controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.AppointmentRequest;
import models.Consultation;
import service.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AddConsultationController {

    // ── FXML bindings ───────────────────────────────────────────
    @FXML private Label      appointmentRefLabel;
    @FXML private DatePicker consultationDate;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private TextField  diagnosisField;
    @FXML private TextArea   prescriptionArea;
    @FXML private TextArea   notesArea;
    @FXML private Label      errorLabel;

    // ── State ───────────────────────────────────────────────────
    private final ConsultationService service = new ConsultationService();
    private AppointmentRequest linkedAppointment;

    // ── Init (called by parent controller before showAndWait) ───

    /**
     * Pass the confirmed appointment this consultation is linked to.
     * Must be called AFTER FXMLLoader.load() but BEFORE stage.showAndWait().
     */
    public void initData(AppointmentRequest appointment) {
        this.linkedAppointment = appointment;

        appointmentRefLabel.setText(
                "Appointment #" + appointment.getId() +
                        " · Doctor #" + appointment.getDoctorId() +
                        (appointment.getConfirmedDate() != null
                                ? " · " + appointment.getConfirmedDate()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                : "")
        );

        // Pre-fill date from confirmed appointment date
        if (appointment.getConfirmedDate() != null) {
            consultationDate.setValue(appointment.getConfirmedDate().toLocalDate());
            hourSpinner  .getValueFactory().setValue(appointment.getConfirmedDate().getHour());
            minuteSpinner.getValueFactory().setValue(appointment.getConfirmedDate().getMinute());
        }
    }

    // ── FXML initialize (spinner factories) ─────────────────────

    @FXML
    public void initialize() {
        hourSpinner  .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Allow user to type directly into spinner
        hourSpinner  .setEditable(true);
        minuteSpinner.setEditable(true);
    }

    // ── Save ────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        // ── Validation ─────────────────────────────────────────
        if (consultationDate.getValue() == null) {
            showError("Please select a consultation date.");
            return;
        }
        if (diagnosisField.getText() == null || diagnosisField.getText().isBlank()) {
            showError("Diagnosis is required.");
            return;
        }

        // ── Build model ────────────────────────────────────────
        LocalDateTime dateTime = LocalDateTime.of(
                consultationDate.getValue(),
                java.time.LocalTime.of(
                        hourSpinner  .getValue(),
                        minuteSpinner.getValue()
                )
        );

        Consultation consultation = new Consultation();
        consultation.setAppointmentRequestId(linkedAppointment.getId());
        consultation.setClientId            (linkedAppointment.getClientId());
        consultation.setDoctorId            (linkedAppointment.getDoctorId());
        consultation.setConsultationDate    (dateTime);
        consultation.setDiagnosis           (diagnosisField.getText().trim());
        consultation.setPrescription        (prescriptionArea.getText() == null ? ""
                : prescriptionArea.getText().trim());
        consultation.setNotes               (notesArea.getText() == null ? ""
                : notesArea.getText().trim());

        // ── Persist ────────────────────────────────────────────
        try {
            service.createConsultation(consultation);
            closeDialog();
        } catch (IllegalStateException e) {
            // appointment not CONFIRMED (shouldn't happen from this flow, but guard anyway)
            showError("Cannot create consultation: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // ── Close ───────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) diagnosisField.getScene().getWindow();
        stage.close();
    }

    // ── Error helpers ───────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
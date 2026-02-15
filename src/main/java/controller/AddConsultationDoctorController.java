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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AddConsultationDoctorController {

    // ── FXML ─────────────────────────────────────────────────────
    @FXML private Label     appointmentRefLabel;
    @FXML private Label     doctorIdDisplay;
    @FXML private Label     clientIdDisplay;
    @FXML private Label     apptIdDisplay;

    @FXML private DatePicker      consultationDate;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;

    @FXML private TextField diagnosisField;
    @FXML private TextArea  prescriptionArea;
    @FXML private TextArea  notesArea;
    @FXML private Label     errorLabel;

    // ── State ─────────────────────────────────────────────────────
    private final ConsultationService service = new ConsultationService();
    private AppointmentRequest linkedAppointment;
    private long               doctorId;

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    // ── Spinner init (called by FXML loader) ──────────────────────

    @FXML
    public void initialize() {
        hourSpinner  .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        hourSpinner  .setEditable(true);
        minuteSpinner.setEditable(true);
    }

    // ── Called by DoctorConsultationsController BEFORE showAndWait ─

    /**
     * Injects the linked appointment and the current doctor's id.
     * Both are used to pre-fill UI labels and to build the Consultation object on save.
     */
    public void initData(AppointmentRequest appt, long doctorId) {
        this.linkedAppointment = appt;
        this.doctorId          = doctorId;

        // ── Populate banner labels ────────────────────────────
        doctorIdDisplay.setText("Dr. #" + doctorId);
        clientIdDisplay.setText("Client #" + appt.getClientId());
        apptIdDisplay  .setText("Appt #"   + appt.getId());

        appointmentRefLabel.setText(
                "Appointment #" + appt.getId() + "  ·  " +
                        appt.getType() +
                        (appt.getConfirmedDate() != null
                                ? "  ·  " + appt.getConfirmedDate().format(LABEL_FMT)
                                : "")
        );

        // ── Pre-fill date/time from confirmed appointment ─────
        if (appt.getConfirmedDate() != null) {
            consultationDate.setValue(appt.getConfirmedDate().toLocalDate());
            hourSpinner  .getValueFactory().setValue(appt.getConfirmedDate().getHour());
            minuteSpinner.getValueFactory().setValue(appt.getConfirmedDate().getMinute());
        }
    }

    // ── Save ──────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        // ── Client-side validation ────────────────────────────
        if (consultationDate.getValue() == null) {
            showError("Please select a consultation date.");
            return;
        }

        String diagnosis = diagnosisField.getText() == null ? "" : diagnosisField.getText().trim();
        if (diagnosis.isBlank()) {
            showError("Diagnosis is required.");
            return;
        }

        // ── Build LocalDateTime ───────────────────────────────
        LocalDateTime dateTime = LocalDateTime.of(
                consultationDate.getValue(),
                LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue())
        );

        // ── Build Consultation model ───────────────────────────
        // doctorId and clientId come from the appointment — not from any user input
        Consultation consultation = new Consultation();
        consultation.setAppointmentRequestId(linkedAppointment.getId());
        consultation.setDoctorId            (doctorId);                          // auto from session/arg
        consultation.setClientId            (linkedAppointment.getClientId());   // auto from appointment
        consultation.setConsultationDate    (dateTime);
        consultation.setDiagnosis           (diagnosis);
        consultation.setPrescription(prescriptionArea.getText() != null
                ? prescriptionArea.getText().trim() : "");
        consultation.setNotes(notesArea.getText() != null
                ? notesArea.getText().trim() : "");

        // ── Persist ───────────────────────────────────────────
        try {
            service.createConsultation(consultation);
            closeDialog();
        } catch (IllegalStateException e) {
            showError("Cannot save: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // ── Close ─────────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) diagnosisField.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────────

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
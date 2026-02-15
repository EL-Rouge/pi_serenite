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

public class CreateConsultationController {

    @FXML private TextField appointmentIdField;
    @FXML private TextArea diagnosisField;
    @FXML private TextArea prescriptionField;
    @FXML private TextArea notesField;
    @FXML private DatePicker consultationDatePicker;
    @FXML private VBox mainContainer;

    private final ConsultationService service = new ConsultationService();


    @FXML
    private void handleCancel() {
        // Option 1: Go back to previous page
        mainContainer.getScene().getWindow().hide();

        // OR Option 2: Clear form instead
        // handleClear();
    }

    @FXML
    private void handleSave() {

        try {
            long appointmentId = Long.parseLong(appointmentIdField.getText());

            LocalDate date = consultationDatePicker.getValue();
            if (date == null)
                throw new IllegalArgumentException("Consultation date is required.");

            Consultation c = new Consultation();
            c.setAppointmentRequestId(appointmentId);
            c.setClientId(13);  // replace with logged user
            c.setDoctorId(16);  // replace with logged doctor
            c.setNotes(notesField.getText());
            c.setDiagnosis(diagnosisField.getText());
            c.setPrescription(prescriptionField.getText());
            c.setConsultationDate(LocalDateTime.of(date, LocalTime.now()));

            service.createConsultation(c);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Consultation created successfully.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

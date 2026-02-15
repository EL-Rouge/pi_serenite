package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import models.AppointmentRequest;
import models.ProposedDate;
import service.AppointmentRequestService;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class AppointmentController implements Initializable {

    // ── these fx:id names must match EXACTLY what's in the FXML ──
    @FXML private TextField clientIdField;
    @FXML private TextField doctorIdField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Label statusLabel;

    private final AppointmentRequestService service = new AppointmentRequestService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // populate the dropdown when the window opens
        typeComboBox.setItems(FXCollections.observableArrayList(
                "IN_PERSON", "ONLINE", "HOME_VISIT"
        ));
    }

    @FXML
    private void handleCreate() {
        // 1. read values from the form
        String clientIdText = clientIdField.getText().trim();
        String doctorIdText = doctorIdField.getText().trim();
        String type         = typeComboBox.getValue();
        var    date         = datePicker.getValue();

        // 2. basic UI validation before even calling the service
        if (clientIdText.isEmpty() || doctorIdText.isEmpty() || type == null || date == null) {
            setStatus("Please fill in all fields.", "red");
            return;
        }

        try {
            // 3. build the model from the form data
            AppointmentRequest appointment = new AppointmentRequest();
            appointment.setClientId(Long.parseLong(clientIdText));
            appointment.setDoctorId(Long.parseLong(doctorIdText));
            appointment.setType(type);

            ProposedDate proposed = new ProposedDate();
            proposed.setProposedDateTime(date.atTime(9, 0)); // default 9:00 AM
            appointment.setProposedDates(List.of(proposed));

            // 4. call the service (same service your Postman was calling!)
            AppointmentRequest created = service.createAppointment(appointment);

            // 5. show success
            setStatus("✔ Appointment created! ID: " + created.getId(), "green");
            handleClear();

        } catch (NumberFormatException e) {
            setStatus("Client ID and Doctor ID must be numbers.", "red");
        } catch (IllegalArgumentException e) {
            setStatus("Validation error: " + e.getMessage(), "red");
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage(), "red");
        }
    }

    @FXML
    private void handleClear() {
        clientIdField.clear();
        doctorIdField.clear();
        typeComboBox.setValue(null);
        datePicker.setValue(null);
        statusLabel.setText("");
    }

    private void setStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }
}
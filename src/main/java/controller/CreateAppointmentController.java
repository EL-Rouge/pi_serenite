package controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import models.AppointmentRequest;
import models.ProposedDate;
import service.AppointmentRequestService;


import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateAppointmentController implements Initializable {

    private static final String DOCTOR_NAME_PLACEHOLDER = "Strange";
    private static final long MOCK_CLIENT_ID = 13L;
    private static final long MOCK_DOCTOR_ID = 16L;

    @FXML
    private Label doctorNameLabel;
    @FXML
    private DatePicker datePicker1;
    @FXML
    private DatePicker datePicker2;
    @FXML
    private DatePicker datePicker3;

    // Updated to Buttons for custom picker
    @FXML
    private Button timeBtn1;
    @FXML
    private Button timeBtn2;
    @FXML
    private Button timeBtn3;

    @FXML
    private Button onlineButton;
    @FXML
    private Button offlineButton;
    @FXML
    private VBox mainContainer;

    private String selectedType = "ONLINE";
    private final AppointmentRequestService service = new AppointmentRequestService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (doctorNameLabel != null) {
            doctorNameLabel.setText("Welcome to Dr. " + DOCTOR_NAME_PLACEHOLDER);
        }

        handleOnlineType(null);
    }

    @FXML
    private void handleTimeSelect(ActionEvent event) {
        Button sourceBtn = (Button) event.getSource();
        showTimePopup(sourceBtn);
    }

    private void showTimePopup(Button targetBtn) {
        ContextMenu popup = new ContextMenu();
        popup.getStyleClass().add("time-picker-popup"); // we will style this in CSS

        FlowPane flow = new FlowPane();
        flow.setHgap(10);
        flow.setVgap(10);
        flow.setPrefWrapLength(250);
        flow.setAlignment(Pos.CENTER);
        flow.setStyle("-fx-padding: 10; -fx-background-color: transparent;");

        // Generate 30 min slots from 08:00 to 17:30
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(16, 0);

        while (start.isBefore(end)) {
            final String timeStr = start.toString();
            Button slotBtn = new Button(timeStr);
            slotBtn.getStyleClass().add("time-slot-pill");

            slotBtn.setOnAction(e -> {
                targetBtn.setText(timeStr);
                targetBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: white;"); // Highlight selected
                popup.hide();
            });

            flow.getChildren().add(slotBtn);
            start = start.plusMinutes(30);
        }

        MenuItem item = new MenuItem();
        item.setGraphic(flow);
        item.setStyle("-fx-padding: 0;");

        popup.getItems().add(item);
        popup.show(targetBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleOnlineType(ActionEvent event) {
        selectedType = "ONLINE";
        setActiveButton(onlineButton);
        setInactiveButton(offlineButton);
    }

    @FXML
    private void handleOfflineType(ActionEvent event) {
        selectedType = "IN_PERSON";
        setActiveButton(offlineButton);
        setInactiveButton(onlineButton);
    }

    private void setActiveButton(Button btn) {
        if (btn == null)
            return;
        if (!btn.getStyleClass().contains("btn-primary")) {
            btn.getStyleClass().add("btn-primary");
            btn.getStyleClass().remove("btn-secondary");
        }
    }

    private void setInactiveButton(Button btn) {
        if (btn == null)
            return;
        if (!btn.getStyleClass().contains("btn-secondary")) {
            btn.getStyleClass().add("btn-secondary");
            btn.getStyleClass().remove("btn-primary");
        }
    }

    @FXML
    private void handleSave() {
        if (selectedType == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select an appointment type.");
            return;
        }

        LocalDate d1 = datePicker1.getValue();
        LocalDate d2 = datePicker2.getValue();
        LocalDate d3 = datePicker3.getValue();

        String t1 = parseTime(timeBtn1);
        String t2 = parseTime(timeBtn2);
        String t3 = parseTime(timeBtn3);

        if (d1 == null && d2 == null && d3 == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please propose at least one date.");
            return;
        }

        if ((d1 != null && t1 == null) || (d2 != null && t2 == null) || (d3 != null && t3 == null)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select a time for every date you picked.");
            return;
        }

        if ((d1 != null && d1.isBefore(LocalDate.now())) ||
                (d2 != null && d2.isBefore(LocalDate.now())) ||
                (d3 != null && d3.isBefore(LocalDate.now()))) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Dates cannot be in the past.");
            return;
        }

        try {
            AppointmentRequest request = new AppointmentRequest();
            request.setClientId(MOCK_CLIENT_ID);
            request.setDoctorId(MOCK_DOCTOR_ID);
            request.setType(selectedType);
            request.setStatus("PENDING");
            request.setCreationDate(LocalDateTime.now());

            List<ProposedDate> proposedDates = new ArrayList<>();
            if (d1 != null && t1 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d1, LocalTime.parse(t1))));
            if (d2 != null && t2 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d2, LocalTime.parse(t2))));
            if (d3 != null && t3 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d3, LocalTime.parse(t3))));

            // Duplicate check
            for (int i = 0; i < proposedDates.size(); i++) {
                for (int j = i + 1; j < proposedDates.size(); j++) {
                    if (proposedDates.get(i).getProposedDateTime().equals(proposedDates.get(j).getProposedDateTime())) {
                        showAlert(Alert.AlertType.WARNING, "Validation Warning",
                                "Please select distinct date and time slots.");
                        return;
                    }
                }
            }

            request.setProposedDates(proposedDates);
            service.createAppointment(request);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Appointment request successfully created!");
            clearForm();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save appointment: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String parseTime(Button btn) {
        String text = btn.getText();
        if (text == null || text.contains("--") || text.isEmpty()) {
            return null;
        }
        // If length is 5 e.g. 08:00, return it. If 8 (seconds), chop.
        return text.length() > 5 ? text.substring(0, 5) : text;
    }

    @FXML
    private void handleCancel() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/fxml/appointmentview/ClientAppointments.fxml"));
            mainContainer.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        datePicker1.setValue(null);
        datePicker2.setValue(null);
        datePicker3.setValue(null);

        resetTimeBtn(timeBtn1);
        resetTimeBtn(timeBtn2);
        resetTimeBtn(timeBtn3);

        handleOnlineType(null);
    }

    private void resetTimeBtn(Button btn) {
        btn.setText("-- : --");
        btn.setStyle(""); // remove custom border
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

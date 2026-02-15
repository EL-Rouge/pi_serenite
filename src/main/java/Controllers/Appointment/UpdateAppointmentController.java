package Controllers.Appointment;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import models.AppointmentRequest;
import models.ProposedDate;
import service.AppointmentRequestService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class UpdateAppointmentController implements Initializable {

    @FXML private Label doctorNameLabel;
    @FXML private DatePicker datePicker1, datePicker2, datePicker3;
    @FXML private Button timeBtn1, timeBtn2, timeBtn3;
    @FXML private Button onlineButton, offlineButton;
    @FXML private VBox mainContainer;

    private String selectedType = "ONLINE";
    private AppointmentRequest currentAppointment;
    private final AppointmentRequestService service = new AppointmentRequestService();

    // Called by ClientAppointmentsController before loading this view
    public void setAppointment(AppointmentRequest app) {
        this.currentAppointment = app;
        prefillForm();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // prefillForm() is called after setAppointment(), not here
    }

    private void prefillForm() {
        if (currentAppointment == null) return;

        // Doctor name in header
        String docName = currentAppointment.getDoctor() != null
                && currentAppointment.getDoctor().getFullname() != null
                ? currentAppointment.getDoctor().getFullname() : "Unknown";
        doctorNameLabel.setText("Editing appointment with Dr. " + docName);

        // Pre-fill type buttons
        if ("ONLINE".equals(currentAppointment.getType())) {
            handleOnlineType(null);
        } else {
            handleOfflineType(null);
        }

        // Pre-fill up to 3 proposed dates
        List<ProposedDate> dates = currentAppointment.getProposedDates();
        DatePicker[] pickers = {datePicker1, datePicker2, datePicker3};
        Button[] timeBtns = {timeBtn1, timeBtn2, timeBtn3};

        for (int i = 0; i < Math.min(dates.size(), 3); i++) {
            LocalDateTime dt = dates.get(i).getProposedDateTime();
            pickers[i].setValue(dt.toLocalDate());
            timeBtns[i].setText(dt.toLocalTime().toString().substring(0, 5));
            timeBtns[i].setStyle("-fx-border-color: #6366f1; -fx-text-fill: white;");
        }
    }

    @FXML
    private void handleTimeSelect(ActionEvent event) {
        showTimePopup((Button) event.getSource());
    }

    private void showTimePopup(Button targetBtn) {
        ContextMenu popup = new ContextMenu();

        FlowPane flow = new FlowPane();
        flow.setHgap(10);
        flow.setVgap(10);
        flow.setPrefWrapLength(250);
        flow.setAlignment(Pos.CENTER);
        flow.setStyle("-fx-padding: 10; -fx-background-color: transparent;");

        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(16, 0);

        while (start.isBefore(end)) {
            final String timeStr = start.toString();
            Button slotBtn = new Button(timeStr);
            slotBtn.getStyleClass().add("time-slot-pill");
            slotBtn.setOnAction(e -> {
                targetBtn.setText(timeStr);
                targetBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: white;");
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
        setActive(onlineButton);
        setInactive(offlineButton);
    }

    @FXML
    private void handleOfflineType(ActionEvent event) {
        selectedType = "CABINE";
        setActive(offlineButton);
        setInactive(onlineButton);
    }

    private void setActive(Button btn) {
        if (btn == null) return;
        btn.getStyleClass().remove("btn-secondary");
        if (!btn.getStyleClass().contains("btn-primary")) btn.getStyleClass().add("btn-primary");
    }

    private void setInactive(Button btn) {
        if (btn == null) return;
        btn.getStyleClass().remove("btn-primary");
        if (!btn.getStyleClass().contains("btn-secondary")) btn.getStyleClass().add("btn-secondary");
    }

    @FXML
    private void handleSave() {
        LocalDate d1 = datePicker1.getValue();
        LocalDate d2 = datePicker2.getValue();
        LocalDate d3 = datePicker3.getValue();

        String t1 = parseTime(timeBtn1);
        String t2 = parseTime(timeBtn2);
        String t3 = parseTime(timeBtn3);

        if (d1 == null && d2 == null && d3 == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Please propose at least one date.");
            return;
        }

        if ((d1 != null && t1 == null) || (d2 != null && t2 == null) || (d3 != null && t3 == null)) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Please select a time for every date you picked.");
            return;
        }

        if ((d1 != null && d1.isBefore(LocalDate.now())) ||
                (d2 != null && d2.isBefore(LocalDate.now())) ||
                (d3 != null && d3.isBefore(LocalDate.now()))) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Dates cannot be in the past.");
            return;
        }

        try {
            List<ProposedDate> proposedDates = new ArrayList<>();
            if (d1 != null && t1 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d1, LocalTime.parse(t1))));
            if (d2 != null && t2 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d2, LocalTime.parse(t2))));
            if (d3 != null && t3 != null)
                proposedDates.add(new ProposedDate(0, LocalDateTime.of(d3, LocalTime.parse(t3))));

            // Duplicate check
            for (int i = 0; i < proposedDates.size(); i++)
                for (int j = i + 1; j < proposedDates.size(); j++)
                    if (proposedDates.get(i).getProposedDateTime()
                            .equals(proposedDates.get(j).getProposedDateTime())) {
                        showAlert(Alert.AlertType.WARNING, "Validation", "Please select distinct slots.");
                        return;
                    }

            currentAppointment.setType(selectedType);
            currentAppointment.setStatus("PENDING"); // reset to pending on reschedule
            currentAppointment.setProposedDates(proposedDates);

            service.updateAppointment(currentAppointment);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Appointment updated successfully!");

            // Go back to appointments list
            navigateBack();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        navigateBack();
    }

    private void navigateBack() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/fxml/appointmentview/ClientAppointments.fxml"));
            mainContainer.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseTime(Button btn) {
        String text = btn.getText();
        if (text == null || text.contains("--") || text.isEmpty()) return null;
        return text.length() > 5 ? text.substring(0, 5) : text;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
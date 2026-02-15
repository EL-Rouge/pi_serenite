package Controllers.Appointment;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.AppointmentRequest;
import models.Doctor;
import models.ProposedDate;
import service.AppointmentRequestService;
import service.DoctorService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientAppointmentsController implements Initializable {

    private static final long CLIENT_ID = 13L;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterType;
    @FXML private VBox appointmentsContainer;

    private final AppointmentRequestService service = new AppointmentRequestService();
    private final DoctorService doctorService = new DoctorService();

    private ObservableList<AppointmentRequest> allAppointments = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        filterStatus.setItems(FXCollections.observableArrayList("ALL", "PENDING", "CONFIRMED", "REFUSED"));
        filterType.setItems(FXCollections.observableArrayList("ALL", "ONLINE", "IN_PERSON"));
        filterStatus.getSelectionModel().selectFirst();
        filterType.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, o, n) -> filterList());
        filterStatus.valueProperty().addListener((obs, o, n) -> filterList());
        filterType.valueProperty().addListener((obs, o, n) -> filterList());

        loadAppointments();
    }

    private void loadAppointments() {
        // Show a loading indicator while fetching
        appointmentsContainer.getChildren().clear();
        Label loading = new Label("Loading appointments...");
        loading.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        appointmentsContainer.getChildren().add(loading);

        // Run DB work on a background thread
        Thread thread = new Thread(() -> {
            try {
                List<AppointmentRequest> list = service.getAppointmentsByClientId(CLIENT_ID);

                // Pre-fetch all doctors in the background too
                for (AppointmentRequest app : list) {
                    Doctor doc = doctorService.getDoctorById(app.getDoctorId());
                    app.setDoctor(doc); // Store doctor on the appointment (see note below)
                }

                // Back to UI thread to render
                Platform.runLater(() -> {
                    allAppointments.setAll(list);
                    renderAppointments(list);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Error", "Could not load appointments: " + e.getMessage())
                );
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void filterList() {
        String query = searchField.getText().toLowerCase();
        String status = filterStatus.getValue();
        String type = filterType.getValue();


        List<AppointmentRequest> filtered = allAppointments.stream()
                .filter(app -> {
                    String doctorName = app.getDoctor() != null && app.getDoctor().getFullname() != null
                            ? app.getDoctor().getFullname().toLowerCase() : "";
                    boolean matchesSearch = query.isEmpty() || doctorName.contains(query);
                    boolean matchesStatus = status == null || status.equals("ALL")
                            || app.getStatus().equalsIgnoreCase(status);
                    boolean matchesType = type == null || type.equals("ALL")
                            || app.getType().equalsIgnoreCase(type);
                    return matchesSearch && matchesStatus && matchesType;
                })
                .collect(Collectors.toList());

        renderAppointments(filtered);
    }

    private void renderAppointments(List<AppointmentRequest> list) {
        appointmentsContainer.getChildren().clear();

        if (list.isEmpty()) {
            Label placeholder = new Label("No appointments found.");
            placeholder.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            appointmentsContainer.getChildren().add(placeholder);
            return;
        }

        for (AppointmentRequest app : list) {
            appointmentsContainer.getChildren().add(createAppointmentCard(app));
        }
    }

    private HBox createAppointmentCard(AppointmentRequest app) {
        // Doctor was pre-fetched in loadAppointments(), no extra DB call needed here
        Doctor doc = app.getDoctor();
        String docName    = (doc != null && doc.getFullname() != null)    ? doc.getFullname()      : "Unknown Doctor";
        String speciality = (doc != null && doc.getSpeciality() != null)  ? doc.getSpeciality()   : "Generalist";
        String address    = (doc != null && doc.getAddressCabine() != null)? doc.getAddressCabine(): "No Address";
        String email      = (doc != null && doc.getEmail() != null)        ? doc.getEmail()        : "No Email";
        String phone      = (doc != null && doc.getPhone() != null)        ? doc.getPhone()        : "No Phone";

        HBox card = new HBox();
        card.getStyleClass().add("card-compact");
        card.setSpacing(20);
        card.setAlignment(Pos.CENTER_LEFT);

        // ── Left: Info ──────────────────────────────────────────
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Header row: doctor name + badges
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label doctorLabel = new Label("👨‍⚕️ Dr. " + docName);
        doctorLabel.getStyleClass().add("doctor-name-large");

        Label statusBadge = new Label(app.getStatus());
        statusBadge.getStyleClass().addAll("status-badge", "status-" + app.getStatus().toLowerCase());

        Label typeBadge = new Label(app.getType().replace("_", " "));
        typeBadge.getStyleClass().add("type-badge");

        header.getChildren().addAll(doctorLabel, statusBadge, typeBadge);

        // Doctor detail labels
        Label specLabel  = new Label("🩺 " + speciality);
        Label addrLabel  = new Label("🏥 " + address);
        Label emailLabel = new Label("📧 " + email);
        Label phoneLabel = new Label("📞 " + phone);

        String detailStyle = "-fx-text-fill: #94a3b8; -fx-font-size: 13px;";
        String subStyle    = "-fx-text-fill: #64748b;  -fx-font-size: 12px;";
        specLabel.setStyle(detailStyle);
        addrLabel.setStyle(detailStyle);
        emailLabel.setStyle(subStyle);
        phoneLabel.setStyle(subStyle);

        // Creation date
        Label dateLabel = new Label("📅 Created: " + app.getCreationDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("date-label");

        // Proposed slots
        VBox datesBox = new VBox(2);
        datesBox.setStyle("-fx-padding: 8 0 0 0;");
        Label datesTitle = new Label("Proposed Slots:");
        datesTitle.getStyleClass().add("sub-label");
        datesBox.getChildren().add(datesTitle);

        if (app.getProposedDates() != null) {
            for (ProposedDate pd : app.getProposedDates()) {
                Label dateItem = new Label("⏰ " + pd.getProposedDateTime().format(SLOT_FORMATTER));
                dateItem.getStyleClass().add("date-item");
                dateItem.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
                datesBox.getChildren().add(dateItem);
            }
        }

        infoBox.getChildren().addAll(header, specLabel, addrLabel, emailLabel, phoneLabel, dateLabel, datesBox);

        // ── Right: Actions ───────────────────────────────────────
        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().addAll("btn", "btn-secondary");
        editBtn.setStyle("-fx-min-width: 90;");
        editBtn.setOnAction(e -> handleEdit(app));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setStyle("-fx-min-width: 90;");
        deleteBtn.setOnAction(e -> handleDelete(app));

        actionBox.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(infoBox, actionBox);
        return card;
    }

    private void handleEdit(AppointmentRequest app) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/appointmentview/UpdateAppointment.fxml"));
            Parent view = loader.load();

            // Pass the appointment to the update controller BEFORE showing the view
            UpdateAppointmentController controller = loader.getController();
            controller.setAppointment(app);

            appointmentsContainer.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open edit view: " + e.getMessage());
        }
    }

    private void handleDelete(AppointmentRequest app) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Appointment");
        alert.setHeaderText("Are you sure you want to delete this appointment?");
        alert.setContentText("This action cannot be undone.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.cancelAppointment(app.getId());
                    loadAppointments();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not delete: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleNewAppointment() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/fxml/appointmentview/createappointment.fxml"));
            appointmentsContainer.getScene().setRoot(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open create appointment view: " + e.getMessage());
        }
    }
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterStatus.getSelectionModel().select("ALL");
        filterType.getSelectionModel().select("ALL");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
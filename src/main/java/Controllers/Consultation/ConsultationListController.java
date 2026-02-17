package Controllers.Consultation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Consultation;
import models.Client;
import models.AppointmentRequest;
import service.ConsultationService;
import service.ClientService;
import service.AppointmentRequestService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ConsultationListController implements Initializable {

    // ── SESSION — replace with real session later ──────────────────
    private static final long DOCTOR_ID = 16L; // TODO: replace with session doctor ID

    @FXML private VBox      cardsContainer;
    @FXML private VBox      emptyState;
    @FXML private TextField searchField;
    @FXML private Label     subtitleLabel;
    @FXML private Label     statsCount;

    private final ConsultationService       consultationService = new ConsultationService();
    private final ClientService             clientService       = new ClientService();
    private final AppointmentRequestService appointmentService  = new AppointmentRequestService();

    private List<Consultation> consultations;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadConsultations();
    }

    // ─── DATA LOADING ───────────────────────────────────────────────

    private void loadConsultations() {
        try {
            consultations = consultationService.getConsultationsByDoctorId(DOCTOR_ID);
        } catch (SQLException e) {
            consultations = List.of();
            System.err.println("[ConsultationListController] Load error: " + e.getMessage());
        }
        renderCards(consultations);
    }

    // ─── SEARCH ─────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        List<Consultation> filtered = consultations.stream()
                .filter(c -> q.isEmpty()
                        || c.getDiagnosis().toLowerCase().contains(q)
                        || (c.getNotes() != null && c.getNotes().toLowerCase().contains(q))
                        || (c.getPrescription() != null && c.getPrescription().toLowerCase().contains(q))
                        || String.valueOf(c.getClientId()).contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        renderCards(consultations);
    }

    // ─── RENDERING ──────────────────────────────────────────────────

    private void renderCards(List<Consultation> list) {
        cardsContainer.getChildren().removeIf(n -> n instanceof HBox);

        statsCount.setText(String.valueOf(list.size()));
        subtitleLabel.setText(list.size() + " consultation"
                + (list.size() == 1 ? "" : "s") + " recorded");

        if (list.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (Consultation c : list) {
            Client client = fetchClient(c.getClientId());
            String apptType = fetchAppointmentType(c.getAppointmentRequestId());
            cardsContainer.getChildren().add(buildCard(c, client, apptType));
        }
    }

    private HBox buildCard(Consultation c, Client client, String apptType) {

        VBox strip = createStrip();
        VBox infoBox = createInfoBox(c, client, apptType);
        VBox actionBox = createActionBox(c);

        HBox card = new HBox(0, strip, infoBox, actionBox);
        card.getStyleClass().add("doc-appt-card");
        card.setAlignment(Pos.CENTER_LEFT);

        return card;
    }

    // ─── CARD COMPONENTS ────────────────────────────────────────────

    private VBox createStrip() {
        VBox strip = new VBox();
        strip.getStyleClass().addAll("doc-card-strip", "strip-consulted");
        strip.setPrefWidth(6);
        strip.setMinHeight(140);
        return strip;
    }

    private VBox createInfoBox(Consultation c, Client client, String apptType) {
        String fullName = client != null ? client.getFullname().trim() : "Client #" + c.getClientId();
        String email = client != null && client.getEmail() != null ? client.getEmail() : "—";
        String phone = client != null && client.getPhone() != null ? client.getPhone() : "—";

        HBox nameRow = createNameRow(fullName, apptType);
        HBox contactRow = createContactRow(email, phone);
        HBox dateRow = createDateRow(c);
        HBox diagRow = createDiagnosisRow(c);
        HBox metaRow = createMetaRow(c);

        VBox infoBox = new VBox(8, nameRow, contactRow, dateRow, diagRow, metaRow);
        infoBox.setStyle("-fx-padding: 18 16 18 16;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        return infoBox;
    }

    private HBox createNameRow(String fullName, String apptType) {
        Label clientNameLabel = new Label(fullName);
        clientNameLabel.getStyleClass().add("client-name");

        Label statusBadge = new Label("CONSULTED");
        statusBadge.getStyleClass().addAll("status-badge", "status-consulted");

        Label typeBadge = new Label(apptType);
        typeBadge.getStyleClass().add("type-badge");

        HBox nameRow = new HBox(10, clientNameLabel, statusBadge, typeBadge);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        return nameRow;
    }

    private HBox createContactRow(String email, String phone) {
        HBox contactRow = new HBox(20, metaChip("✉", email), metaChip("📞", phone));
        contactRow.setAlignment(Pos.CENTER_LEFT);
        return contactRow;
    }

    private HBox createDateRow(Consultation c) {
        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 12;");
        Label dateLabel = new Label(
                c.getConsultationDate() != null
                        ? "Consultation: " + c.getConsultationDate().format(DATETIME_FMT)
                        : "Date not set");
        dateLabel.getStyleClass().add("confirmed-date-label");

        HBox dateRow = new HBox(8, calIcon, dateLabel);
        dateRow.getStyleClass().add("confirmed-date-box");
        dateRow.setAlignment(Pos.CENTER_LEFT);
        return dateRow;
    }

    private HBox createDiagnosisRow(Consultation c) {
        Label diagIcon = new Label("🔬");
        diagIcon.setStyle("-fx-font-size: 11;");
        String diagText = c.getDiagnosis().length() > 80
                ? c.getDiagnosis().substring(0, 80) + "…"
                : c.getDiagnosis();
        Label diagLabel = new Label(diagText);
        diagLabel.getStyleClass().add("meta-label");

        HBox diagRow = new HBox(6, diagIcon, diagLabel);
        diagRow.setAlignment(Pos.CENTER_LEFT);
        return diagRow;
    }

    private HBox createMetaRow(Consultation c) {
        HBox metaRow = new HBox(20, metaChip("🗓", "Created " + c.getCreationDate().format(DATE_FMT)));
        metaRow.setAlignment(Pos.CENTER_LEFT);
        return metaRow;
    }

    private VBox createActionBox(Consultation c) {
        Button editBtn = new Button("✏  Edit");
        editBtn.getStyleClass().addAll("btn", "btn-accent");
        editBtn.setMinWidth(100);
        editBtn.setOnAction(e -> openEditForm(c));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setMinWidth(100);
        deleteBtn.setOnAction(e -> handleDelete(c));

        VBox actionBox = new VBox(10, editBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setStyle("-fx-padding: 0 24 0 0;");
        return actionBox;
    }

    // ─── ACTIONS ────────────────────────────────────────────────────

    private void handleDelete(Consultation c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Consultation");
        confirm.setHeaderText("Delete this consultation?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    consultationService.deleteConsultation(c.getId());
                    loadConsultations();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not delete: " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    private void openEditForm(Consultation c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/consultationview/EditConsultation.fxml"));
            Parent root = loader.load();

            EditConsultationController ctrl = loader.getController();
            ctrl.initData(c);

            Stage stage = new Stage();
            stage.setTitle("Edit Consultation #" + c.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadConsultations();

        } catch (IOException e) {
            System.err.println("[ConsultationListController] Edit dialog error: " + e.getMessage());
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────

    private Client fetchClient(long clientId) {
        try {
            return clientService.getById(clientId);
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchAppointmentType(long appointmentId) {
        try {
            AppointmentRequest appt = appointmentService.getById(appointmentId);
            return appt.getType();
        } catch (Exception e) {
            return "—";
        }
    }

    private HBox metaChip(String icon, String text) {
        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 11;");
        Label t = new Label(text);
        t.getStyleClass().add("meta-label");
        HBox box = new HBox(5, i, t);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
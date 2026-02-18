package Controllers.Consultation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AppointmentRequest;
import models.Client;
import service.AppointmentRequestService;
import service.ClientService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DoctorConsultationsController implements Initializable {

    // ── CHANGE 1: will come from session later, kept here for now ──
    private static final long DOCTOR_ID = 16L;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    @FXML private VBox      cardsContainer;
    @FXML private VBox      emptyState;
    @FXML private TextField searchField;
    @FXML private Label     subtitleLabel;
    @FXML private Label     statsCount;

    private final AppointmentRequestService service       = new AppointmentRequestService();
    private final ClientService             clientService = new ClientService();

    private List<AppointmentRequest> appointments;

    // ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAppointments();
    }

    // ── CHANGE 2: extracted filter constants so the stream is readable ──
    private void loadAppointments() {
        try {
            appointments = service.getAllAppointments().stream()
                    .filter(a -> a.getDoctorId() == DOCTOR_ID)
                    .filter(a -> "CONFIRMED".equals(a.getStatus()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            appointments = List.of();
            System.err.println("[DoctorConsultations] Load error: " + e.getMessage());
        }
        renderCards(appointments);
    }

    // ─────────────────────────────────────────────────────────────

    @FXML private void handleSearch() {
        String q = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        // ── CHANGE 3: extracted predicate to a named variable ──
        List<AppointmentRequest> filtered = q.isEmpty() ? appointments
                : appointments.stream()
                .filter(a -> a.getType().toLowerCase().contains(q)
                        || String.valueOf(a.getClientId()).contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    @FXML private void handleClear() {
        searchField.clear();
        renderCards(appointments);
    }

    // ─────────────────────────────────────────────────────────────

    private void renderCards(List<AppointmentRequest> list) {
        cardsContainer.getChildren().removeIf(n -> n instanceof HBox);

        statsCount.setText(String.valueOf(list.size()));
        subtitleLabel.setText(list.size() + " confirmed appointment"
                + (list.size() == 1 ? "" : "s") + " awaiting consultation");

        boolean empty = list.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);   // ── CHANGE 4: collapsed 4 lines into 2

        if (!empty) {
            list.forEach(appt -> {
                Client client = fetchClient(appt.getClientId()); // ── CHANGE 5: extracted to method
                cardsContainer.getChildren().add(buildCard(appt, client));
            });
        }
    }

    // ── CHANGE 5: client fetch extracted — keeps renderCards() clean ──
    private Client fetchClient(long clientId) {
        try {
            return clientService.getById(clientId);
        } catch (Exception e) {
            System.err.println("[DoctorConsultations] Client fetch error: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────

    private HBox buildCard(AppointmentRequest appt, Client client) {

        // ── CHANGE 6: resolve display values up-front in one block ──
        String fullName = client != null ? client.getFullname().trim() : "Client #" + appt.getClientId();
        String email    = client != null && client.getEmail() != null ? client.getEmail() : "—";
        String phone    = client != null && client.getPhone() != null ? client.getPhone() : "—";

        // Left strip
        VBox strip = new VBox();
        strip.getStyleClass().add("doc-card-strip");
        strip.setPrefWidth(6);
        strip.setMinHeight(140);

        // Row 1 — name + badges
        Label clientNameLabel = new Label(fullName);
        clientNameLabel.getStyleClass().add("client-name");

        Label statusBadge = new Label("CONFIRMED");
        statusBadge.getStyleClass().addAll("status-badge", "status-confirmed");

        Label typeBadge = new Label(appt.getType());
        typeBadge.getStyleClass().add("type-badge");

        HBox nameRow = new HBox(10, clientNameLabel, statusBadge, typeBadge);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2 — contact
        HBox contactRow = new HBox(20, metaChip("✉", email), metaChip("📞", phone));
        contactRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3 — confirmed date
        // ── CHANGE 7: removed redundant calIcon label, date text is self-explanatory ──
        String dateText = appt.getConfirmedDate() != null
                ? "📅  Confirmed for: " + appt.getConfirmedDate().format(DATETIME_FMT)
                : "📅  Date not set";
        Label confirmedDateLabel = new Label(dateText);
        confirmedDateLabel.getStyleClass().add("confirmed-date-label");
        HBox confirmedRow = new HBox(confirmedDateLabel);
        confirmedRow.getStyleClass().add("confirmed-date-box");
        confirmedRow.setAlignment(Pos.CENTER_LEFT);

        // Row 4 — created date
        HBox metaRow = new HBox(20, metaChip("🗓", "Created " + appt.getCreationDate().format(DATE_FMT)));
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Info box — ── CHANGE 8: padding moved to CSS (.card-info-box) ──
        VBox infoBox = new VBox(8, nameRow, contactRow, confirmedRow, metaRow);
        infoBox.getStyleClass().add("card-info-box");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Action button — ── CHANGE 8: font-size moved to CSS ──
        Button consultBtn = new Button("+ Consultation");
        consultBtn.getStyleClass().addAll("btn", "btn-accent");
        consultBtn.setMinWidth(140);
        consultBtn.setOnAction(e -> openConsultationForm(appt));

        VBox actionBox = new VBox(consultBtn);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.getStyleClass().add("card-action-box"); // ── CHANGE 8: padding moved to CSS

        // Assemble
        HBox card = new HBox(0, strip, infoBox, actionBox);
        card.getStyleClass().add("doc-appt-card");
        card.setAlignment(Pos.CENTER_LEFT);

        return card;
    }

    // ── CHANGE 9: removed getInitials() — it was defined but never called ──

    private HBox metaChip(String icon, String text) {
        Label i = new Label(icon);
        i.getStyleClass().add("meta-icon");   // ── CHANGE 8: font-size moved to CSS
        Label t = new Label(text);
        t.getStyleClass().add("meta-label");
        HBox box = new HBox(5, i, t);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ─────────────────────────────────────────────────────────────

    private void openConsultationForm(AppointmentRequest appt) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/consultationview/Addconsultationdoctor.fxml"));
            Parent root = loader.load();

            AddConsultationDoctorController ctrl = loader.getController();
            ctrl.initData(appt, DOCTOR_ID);

            Stage stage = new Stage();
            stage.setTitle("New Consultation — Appt #" + appt.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadAppointments();

        } catch (IOException e) {
            System.err.println("[DoctorConsultations] Dialog error: " + e.getMessage());
        }
    }
}
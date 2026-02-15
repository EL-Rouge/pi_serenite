package controller;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AppointmentRequest;
import service.AppointmentRequestService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DoctorConsultationsController implements Initializable {

    // ── Hardcoded until session exists ───────────────────────────
    private static final long DOCTOR_ID = 16L;

    // ── FXML ─────────────────────────────────────────────────────
    @FXML private VBox      cardsContainer;
    @FXML private VBox      emptyState;
    @FXML private TextField searchField;
    @FXML private Label     subtitleLabel;
    @FXML private Label     statsCount;

    // ── State ─────────────────────────────────────────────────────
    private final AppointmentRequestService service = new AppointmentRequestService();
    private List<AppointmentRequest> appointments;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  'at'  HH:mm");

    // ── Init ──────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAppointments();
    }

    private void loadAppointments() {
        try {
            // Fetch ALL appointments for this doctor, keep only CONFIRMED
            appointments = service.getAllAppointments().stream()
                    .filter(a -> a.getDoctorId() == DOCTOR_ID)
                    .filter(a -> "CONFIRMED".equals(a.getStatus()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            appointments = List.of();
            System.err.println("[DoctorConsultationsController] Load error: " + e.getMessage());
        }
        renderCards(appointments);
    }

    // ── Search ────────────────────────────────────────────────────

    @FXML private void handleSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        List<AppointmentRequest> filtered = appointments.stream()
                .filter(a -> q.isEmpty()
                        || a.getType().toLowerCase().contains(q)
                        || String.valueOf(a.getClientId()).contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    @FXML private void handleClear() {
        searchField.clear();
        renderCards(appointments);
    }

    // ── Card rendering ────────────────────────────────────────────

    private void renderCards(List<AppointmentRequest> list) {
        // Remove all real cards (HBox nodes), keep emptyState VBox
        cardsContainer.getChildren().removeIf(n -> n instanceof HBox);

        // Update stats pill
        statsCount.setText(String.valueOf(list.size()));
        subtitleLabel.setText(list.size() + " confirmed appointment"
                + (list.size() == 1 ? "" : "s") + " awaiting consultation");

        if (list.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (AppointmentRequest appt : list) {
            cardsContainer.getChildren().add(buildCard(appt));
        }
    }

    private HBox buildCard(AppointmentRequest appt) {

        // ── Left colour strip ──────────────────────────────────
        VBox strip = new VBox();
        strip.getStyleClass().add("doc-card-strip");
        strip.setPrefWidth(6);
        strip.setMinHeight(140);

        // ── Client avatar (initials from clientId for now) ─────
        // TODO: replace with real client name once you add a ClientService
        String initials = "C" + appt.getClientId(); // e.g. "C42"
        Label avatarText = new Label(initials.length() > 2
                ? initials.substring(0, 2).toUpperCase()
                : initials.toUpperCase());
        avatarText.getStyleClass().add("client-avatar-text");
        StackPane avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("client-avatar");
        avatar.setStyle("-fx-margin: 0 0 0 20;");

        // ── Info VBox ──────────────────────────────────────────
        // Row 1: client name + badges
        Label clientName = new Label("Client #" + appt.getClientId());
        clientName.getStyleClass().add("client-name");

        Label statusBadge = new Label("CONFIRMED");
        statusBadge.getStyleClass().addAll("status-badge", "status-confirmed");

        Label typeBadge = new Label(appt.getType());
        typeBadge.getStyleClass().add("type-badge");

        HBox nameRow = new HBox(10, clientName, statusBadge, typeBadge);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: confirmed date
        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 12;");
        Label confirmedDateLabel = new Label(
                appt.getConfirmedDate() != null
                        ? "Confirmed for: " + appt.getConfirmedDate().format(DATETIME_FMT)
                        : "Date not set");
        confirmedDateLabel.getStyleClass().add("confirmed-date-label");
        HBox confirmedRow = new HBox(8, calIcon, confirmedDateLabel);
        confirmedRow.getStyleClass().add("confirmed-date-box");
        confirmedRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3: meta
        HBox clientMeta = metaChip("👤", "Client #" + appt.getClientId());
        HBox apptMeta   = metaChip("🆔", "Appt #" + appt.getId());
        HBox dateMeta   = metaChip("🗓", "Created " + appt.getCreationDate().format(DATE_FMT));

        HBox metaRow = new HBox(20, clientMeta, apptMeta, dateMeta);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(8, nameRow, confirmedRow, metaRow);
        infoBox.setStyle("-fx-padding: 18 16 18 16;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // ── + Consultation button ──────────────────────────────
        Button consultBtn = new Button("+ Consultation");
        consultBtn.getStyleClass().addAll("btn", "btn-accent");
        consultBtn.setMinWidth(140);
        consultBtn.setStyle("-fx-font-size: 13px;");
        consultBtn.setOnAction(e -> openConsultationForm(appt));

        VBox actionBox = new VBox(consultBtn);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setStyle("-fx-padding: 0 24 0 0;");

        // ── Assemble ───────────────────────────────────────────
        HBox card = new HBox(0, strip, avatar, infoBox, actionBox);
        card.getStyleClass().add("doc-appt-card");
        card.setAlignment(Pos.CENTER_LEFT);

        return card;
    }

    /** Small icon + text label used for meta row */
    private HBox metaChip(String icon, String text) {
        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 11;");
        Label t = new Label(text);
        t.getStyleClass().add("meta-label");
        HBox box = new HBox(5, i, t);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ── Open consultation form ────────────────────────────────────

    private void openConsultationForm(AppointmentRequest appt) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/doctor/AddConsultationDoctor.fxml"));
            Parent root = loader.load();

            AddConsultationDoctorController ctrl = loader.getController();
            ctrl.initData(appt, DOCTOR_ID);   // ← pass appointment + doctorId

            Stage stage = new Stage();
            stage.setTitle("New Consultation — Appt #" + appt.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh after save
            loadAppointments();

        } catch (IOException e) {
            System.err.println("[DoctorConsultationsController] Dialog error: " + e.getMessage());
        }
    }
}
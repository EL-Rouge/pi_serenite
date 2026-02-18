package Controllers.Appointment;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import models.AppointmentRequest;
import models.Client;
import models.ProposedDate;
import service.AppointmentRequestService;
import service.ClientService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DoctorDashboardController implements Initializable {

    private static final long DOCTOR_ID = 16L;
    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("EEE dd MMM · HH:mm");
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_FMT    = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter CREATED_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm");

    // ── FXML ─────────────────────────────────────────────────────
    @FXML private Label  totalBadge;
    @FXML private Button tabAll, tabPending, tabConfirmed, tabRefused;
    @FXML private VBox   appointmentList;
    @FXML private Label  countPending, countConfirmed, countRefused, countConsulted;
    @FXML private Label  calMonthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Label  detailDateLabel;
    @FXML private VBox   detailList;

    // ── STATE ─────────────────────────────────────────────────────
    private final AppointmentRequestService apptService   = new AppointmentRequestService();
    private final ClientService             clientService = new ClientService();

    private List<AppointmentRequest> allAppointments = new ArrayList<>();
    private String    currentFilter   = "ALL";
    private YearMonth currentMonth    = YearMonth.now();
    private StackPane selectedDayCell = null;
    private AppointmentRequest expandedCard = null;

    // ── INIT ──────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    private void loadData() {
        try {
            allAppointments = apptService.getAppointmentsByDoctorId(DOCTOR_ID);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load appointments: " + e.getMessage());
            allAppointments = new ArrayList<>();
        }
        refreshUI();
    }

    private void refreshUI() {
        updateStats();
        renderAppointmentList();
        renderCalendar();
    }

    // ── STATS ─────────────────────────────────────────────────────
    private void updateStats() {
        countPending.setText(String.valueOf(count("PENDING")));
        countConfirmed.setText(String.valueOf(count("CONFIRMED")));
        countRefused.setText(String.valueOf(count("REFUSED")));
        countConsulted.setText(String.valueOf(count("CONSULTED")));
        totalBadge.setText(allAppointments.size() + " total");
    }

    private long count(String status) {
        return allAppointments.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    // ── FILTER TABS ───────────────────────────────────────────────
    @FXML private void filterAll()       { setFilter("ALL");       }
    @FXML private void filterPending()   { setFilter("PENDING");   }
    @FXML private void filterConfirmed() { setFilter("CONFIRMED"); }
    @FXML private void filterRefused()   { setFilter("REFUSED");   }

    private void setFilter(String filter) {
        currentFilter = filter;
        expandedCard  = null;
        List<Button> tabs    = List.of(tabAll, tabPending, tabConfirmed, tabRefused);
        List<String> filters = List.of("ALL", "PENDING", "CONFIRMED", "REFUSED");
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).getStyleClass().removeAll("tab-active");
            if (filters.get(i).equals(filter))
                tabs.get(i).getStyleClass().add("tab-active");
        }
        renderAppointmentList();
    }

    // ── APPOINTMENT LIST ──────────────────────────────────────────
    private void renderAppointmentList() {
        appointmentList.getChildren().clear();

        List<AppointmentRequest> filtered = allAppointments.stream()
                .filter(a -> "ALL".equals(currentFilter) || currentFilter.equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("No appointments found.");
            empty.getStyleClass().add("detail-empty-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            appointmentList.getChildren().add(empty);
            return;
        }

        for (AppointmentRequest appt : filtered)
            appointmentList.getChildren().add(buildCard(appt));
    }

    private VBox buildCard(AppointmentRequest appt) {
        // Client name
        String clientName = "Client #" + appt.getClientId();
        try {
            Client c = clientService.getById(appt.getClientId());
            if (c != null && c.getFullname() != null) clientName = c.getFullname();
        } catch (Exception ignored) {}

        // Status color
        String barColor = switch (appt.getStatus().toUpperCase()) {
            case "CONFIRMED" -> "#10b981";
            case "REFUSED"   -> "#ef4444";
            case "CONSULTED" -> "#8b5cf6";
            default          -> "#f59e0b";
        };

        // Colored left bar
        Rectangle bar = new Rectangle(4, 90);
        bar.setFill(Color.web(barColor));
        bar.setArcWidth(4);
        bar.setArcHeight(4);

        // ── Card content ──
        VBox content = new VBox(8);
        content.setPadding(new Insets(14));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: avatar + client info + status chip
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = makeAvatar(getInitials(clientName), barColor);

        VBox clientInfo = new VBox(2);
        Label nameLabel = new Label(clientName);
        nameLabel.getStyleClass().add("client-name-label");
        Label typeLabel = new Label(getTypeIcon(appt.getType()) + "  " + appt.getType());
        typeLabel.getStyleClass().add("client-type-label");
        clientInfo.getChildren().addAll(nameLabel, typeLabel);
        HBox.setHgrow(clientInfo, Priority.ALWAYS);

        Label statusChip = new Label(appt.getStatus().toUpperCase());
        statusChip.getStyleClass().add("chip-" + appt.getStatus().toLowerCase());

        topRow.getChildren().addAll(avatar, clientInfo, statusChip);

        // Row 2: meta info (date confirmed / proposed count)
        Label metaLabel = new Label(buildMetaText(appt));
        metaLabel.getStyleClass().add("client-meta-label");

        // Row 3: ── CREATION DATE ──
        String createdText = "📝 Submitted: ";
        if (appt.getCreationDate() != null) {
            createdText += appt.getCreationDate().format(CREATED_FMT);
        } else {
            createdText += "Unknown";
        }
        Label createdLabel = new Label(createdText);
        createdLabel.getStyleClass().add("created-date-label");

        content.getChildren().addAll(topRow, metaLabel, createdLabel);

        // Expandable section (PENDING only)
        VBox expandSection = new VBox(10);
        expandSection.setVisible(false);
        expandSection.setManaged(false);

        if ("PENDING".equalsIgnoreCase(appt.getStatus())) {
            buildExpandSection(appt, expandSection);
            content.getChildren().add(expandSection);
        }

        // Assemble
        HBox cardInner = new HBox(0);
        cardInner.getChildren().addAll(bar, content);
        cardInner.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(0);
        card.getStyleClass().add("appt-card");
        card.getChildren().add(cardInner);

        // Click to expand (PENDING only)
        if ("PENDING".equalsIgnoreCase(appt.getStatus())) {
            card.setOnMouseClicked(e -> {
                boolean wasExpanded = expandSection.isVisible();
                collapseAllCards();
                if (!wasExpanded) {
                    expandSection.setVisible(true);
                    expandSection.setManaged(true);
                    card.getStyleClass().add("appt-card-selected");
                    expandedCard = appt;
                }
            });
        }

        return card;
    }

    private void buildExpandSection(AppointmentRequest appt, VBox expandSection) {
        Region sep = new Region();
        sep.getStyleClass().add("card-separator");
        sep.setMaxWidth(Double.MAX_VALUE);

        Label datesLabel = new Label("Choose a date:");
        datesLabel.getStyleClass().add("client-meta-label");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        final Button[] confirmBtn = {null};
        final ProposedDate[] selectedDate = {null};
        List<Button> chipButtons = new ArrayList<>();

        for (ProposedDate pd : appt.getProposedDates()) {
            Button chip = new Button(pd.getProposedDateTime().format(DATE_FMT));
            chip.getStyleClass().add("date-chip");
            chip.setOnAction(e -> {
                chipButtons.forEach(b -> {
                    b.getStyleClass().removeAll("date-chip-selected");
                    b.getStyleClass().add("date-chip");
                });
                chip.getStyleClass().removeAll("date-chip");
                chip.getStyleClass().add("date-chip-selected");
                selectedDate[0] = pd;
                if (confirmBtn[0] != null) confirmBtn[0].setDisable(false);
            });
            chipButtons.add(chip);
            chipsPane.getChildren().add(chip);
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button confirm = new Button("✓  Confirm");
        confirm.getStyleClass().add("btn-confirm");
        confirm.setDisable(true);
        confirmBtn[0] = confirm;
        confirm.setOnAction(e -> {
            if (selectedDate[0] != null)
                handleConfirm(appt, selectedDate[0].getProposedDateTime());
        });

        Button refuse = new Button("✕  Refuse");
        refuse.getStyleClass().add("btn-refuse");
        refuse.setOnAction(e -> handleRefuse(appt));

        actions.getChildren().addAll(confirm, refuse);
        expandSection.getChildren().addAll(sep, datesLabel, chipsPane, actions);
    }

    private void collapseAllCards() {
        appointmentList.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                card.getStyleClass().removeAll("appt-card-selected");
                card.getChildren().stream()
                        .filter(n -> n instanceof HBox).map(n -> (HBox) n)
                        .forEach(hbox -> hbox.getChildren().stream()
                                .filter(n -> n instanceof VBox).map(n -> (VBox) n)
                                .forEach(vbox -> vbox.getChildren().stream()
                                        .filter(n -> n instanceof VBox).map(n -> (VBox) n)
                                        .forEach(inner -> {
                                            inner.setVisible(false);
                                            inner.setManaged(false);
                                        })));
            }
        });
        expandedCard = null;
    }

    // ── ACTIONS ───────────────────────────────────────────────────
    private void handleConfirm(AppointmentRequest appt, LocalDateTime chosenDate) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Confirm Appointment");
        dlg.setHeaderText(null);
        dlg.setContentText("Confirm this appointment for:\n" + chosenDate.format(DATE_FMT) + "?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    apptService.confirmAppointment(appt.getId(), chosenDate);
                    showAlert(Alert.AlertType.INFORMATION, "Done",
                            "Appointment confirmed for " + chosenDate.format(DATE_FMT));
                    loadData();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                }
            }
        });
    }

    private void handleRefuse(AppointmentRequest appt) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Refuse Appointment");
        dlg.setHeaderText(null);
        dlg.setContentText("Are you sure you want to refuse this appointment?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    apptService.refuseAppointment(appt.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Done", "Appointment refused.");
                    loadData();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                }
            }
        });
    }

    // ── CALENDAR ──────────────────────────────────────────────────
    @FXML private void prevMonth() { currentMonth = currentMonth.minusMonths(1); renderCalendar(); }
    @FXML private void nextMonth() { currentMonth = currentMonth.plusMonths(1);  renderCalendar(); }
    @FXML private void goToDashboard() {}

    private void renderCalendar() {
        calendarGrid.getChildren().clear();
        calMonthLabel.setText(currentMonth.atDay(1).format(MONTH_FMT));

        Map<LocalDate, List<AppointmentRequest>> confirmedByDay = allAppointments.stream()
                .filter(a -> "CONFIRMED".equalsIgnoreCase(a.getStatus())
                        && a.getConfirmedDate() != null
                        && YearMonth.from(a.getConfirmedDate()).equals(currentMonth))
                .collect(Collectors.groupingBy(a -> a.getConfirmedDate().toLocalDate()));

        String[] headers = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(headers[i]);
            h.getStyleClass().add("cal-day-header");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            calendarGrid.add(h, i, 0);
        }

        LocalDate first    = currentMonth.atDay(1);
        int       startDow = first.getDayOfWeek().getValue() - 1;
        LocalDate today    = LocalDate.now();
        int row = 1, col = startDow;

        for (int i = 0; i < startDow; i++)
            calendarGrid.add(makeDayCell(first.minusDays(startDow - i), true, false, null), i, row);

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            calendarGrid.add(makeDayCell(date, false, date.equals(today), confirmedByDay.get(date)), col, row);
            if (++col == 7) { col = 0; row++; }
        }

        int next = 1;
        while (col > 0 && col < 7)
            calendarGrid.add(makeDayCell(currentMonth.atEndOfMonth().plusDays(next++), true, false, null), col++, row);
    }

    private StackPane makeDayCell(LocalDate date, boolean otherMonth, boolean isToday,
                                  List<AppointmentRequest> appts) {
        StackPane cell = new StackPane();
        cell.setMinSize(44, 40);
        cell.setMaxSize(44, 40);
        cell.setAlignment(Pos.CENTER);

        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        cell.getChildren().add(dayNum);

        if (otherMonth) {
            dayNum.getStyleClass().add("cal-day-other");
            cell.getStyleClass().add("cal-day");
        } else if (isToday) {
            cell.getStyleClass().addAll("cal-day", "cal-day-today");
            dayNum.getStyleClass().add("cal-day-today");
        } else if (appts != null && !appts.isEmpty()) {
            cell.getStyleClass().add("cal-day");
            dayNum.getStyleClass().add("cal-day-has-appt");
            Circle dot = new Circle(3, Color.web("#10b981"));
            StackPane.setAlignment(dot, Pos.BOTTOM_CENTER);
            dot.setTranslateY(-3);
            cell.getChildren().add(dot);
        } else {
            cell.getStyleClass().add("cal-day");
            dayNum.getStyleClass().add("cal-day");
        }

        if (!otherMonth) {
            final List<AppointmentRequest> finalAppts = appts;
            cell.setOnMouseClicked(e -> selectDay(cell, date, finalAppts));
        }
        return cell;
    }

    private void selectDay(StackPane cell, LocalDate date, List<AppointmentRequest> appts) {
        if (selectedDayCell != null)
            selectedDayCell.getStyleClass().removeAll("cal-day-selected");
        cell.getStyleClass().add("cal-day-selected");
        selectedDayCell = cell;

        detailDateLabel.setText("Appointments — " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        detailList.getChildren().clear();

        if (appts == null || appts.isEmpty()) {
            Label empty = new Label("✨  No appointments on this day.");
            empty.getStyleClass().add("detail-empty-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            detailList.getChildren().add(empty);
        } else {
            for (AppointmentRequest a : appts)
                detailList.getChildren().add(buildDetailRow(a));
        }
    }

    private HBox buildDetailRow(AppointmentRequest a) {
        String clientName = "Client #" + a.getClientId();
        try {
            Client c = clientService.getById(a.getClientId());
            if (c != null && c.getFullname() != null) clientName = c.getFullname();
        } catch (Exception ignored) {}

        HBox row = new HBox(12);
        row.getStyleClass().add("detail-appt-row");
        row.setAlignment(Pos.CENTER_LEFT);

        String timeStr = a.getConfirmedDate() != null ? a.getConfirmedDate().format(TIME_FMT) : "--:--";
        Label time = new Label(timeStr);
        time.getStyleClass().add("detail-time");

        VBox info = new VBox(2);
        Label name = new Label(clientName);
        name.getStyleClass().add("detail-client");
        Label type = new Label(getTypeIcon(a.getType()) + "  " + a.getType());
        type.getStyleClass().add("detail-type");
        info.getChildren().addAll(name, type);

        row.getChildren().addAll(time, info);
        return row;
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private StackPane makeAvatar(String initials, String color) {
        StackPane av = new StackPane();
        av.setMinSize(36, 36);
        av.setMaxSize(36, 36);
        av.setStyle("-fx-background-color: " + color + "33; -fx-background-radius: 50%;");
        Label lbl = new Label(initials);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: 700;");
        av.getChildren().add(lbl);
        return av;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String getTypeIcon(String type) {
        if (type == null) return "🩺";
        return switch (type.toUpperCase()) {
            case "ONLINE" -> "💻";
            case "CABINE" -> "🏥";
            default       -> "🩺";
        };
    }

    private String buildMetaText(AppointmentRequest appt) {
        return switch (appt.getStatus().toUpperCase()) {
            case "PENDING"   -> "🗓  " + appt.getProposedDates().size() + " proposed date(s)";
            case "CONFIRMED" -> appt.getConfirmedDate() != null
                    ? "📍  " + appt.getConfirmedDate().format(DATE_FMT)
                    : "📍  Date not set";
            case "REFUSED"   -> "✕  Refused by doctor";
            case "CONSULTED" -> "✅  Consultation done";
            default          -> "";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
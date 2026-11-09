package com.nextfit.ui.fx;

import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.model.Role;
import com.nextfit.service.audit.AuditService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit module in JavaFX, only for the administrator: shows who did what and when (sign-ins, blocked
 * users, account changes and backups). It only reads: nobody can edit or delete a line from here.
 * The numbers and the list come from {@link AuditService}; this class only shows them.
 */
public final class AuditModule implements FxModule {

    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String ALL = "Todos los eventos";
    private static final int MAX_ROWS = 500;

    private final AuditService service;
    private final TableView<AuditEntry> table = new TableView<>();
    private final Label countLabel = new Label();
    private final ComboBox<String> filter = new ComboBox<>();
    private final KpiCard today = new KpiCard("Eventos de hoy", "0", "Todo lo que se registró hoy",
            FxIcons.AUDIT, KpiCard.Kind.FEATURE);
    private final KpiCard failed = new KpiCard("Inicios fallidos hoy", "0",
            "Usuario o contraseña incorrectos", FxIcons.USERS, KpiCard.Kind.VIOLET);
    private final KpiCard locked = new KpiCard("Bloqueos hoy", "0",
            "Intentos con el usuario bloqueado", FxIcons.ACCESS, KpiCard.Kind.LIME);
    private final VBox view = new VBox(16);
    private List<AuditEntry> entries = new ArrayList<>();

    /**
     * @param service the service that reads the audit log
     */
    public AuditModule(AuditService service) {
        this.service = service;
        filter.getItems().add(ALL);
        for (AuditAction action : AuditAction.values()) {
            filter.getItems().add(action.getLabel());
        }
        filter.getSelectionModel().selectFirst();
        filter.setPrefHeight(44);
        filter.setOnAction(e -> showRows());
        view.getChildren().addAll(buildHeader(), buildCards(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        FxMotion.enter(view.getChildren(), 0, 90);
    }

    @Override
    public String id() {
        return "audit";
    }

    @Override
    public String title() {
        return "Auditoría";
    }

    @Override
    public String icon() {
        return FxIcons.AUDIT;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** Only the administrator can read the audit log. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN;
    }

    private Node buildHeader() {
        Label title = new Label("Auditoría");
        title.getStyleClass().add("nf-card-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(10, titles, filter);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildCards() {
        HBox cards = new HBox(18, today, failed, locked);
        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
        }
        return cards;
    }

    private Node buildTable() {
        TableColumn<AuditEntry, String> moment = FxTables.column("Fecha y hora", 190,
                e -> e.timestamp().format(MOMENT));
        TableColumn<AuditEntry, String> user = FxTables.column("Usuario", 160, AuditEntry::username);
        TableColumn<AuditEntry, String> action = FxTables.column("Evento", 260, e -> e.action().getLabel());
        FxTables.colorize(action, label -> styleOf(label));
        TableColumn<AuditEntry, String> detail = FxTables.column("Detalle", 420, AuditEntry::detail);
        table.getColumns().addAll(List.of(moment, user, action, detail));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Aún no hay eventos"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(280);
        return table;
    }

    // Reads the log again and updates the numbers and the list.
    private void refresh() {
        entries = service.findAll();
        today.value().setText(String.valueOf(service.countToday(null)));
        failed.value().setText(String.valueOf(service.countToday(AuditAction.LOGIN_FAILED)));
        locked.value().setText(String.valueOf(service.countToday(AuditAction.LOCKOUT)));
        showRows();
    }

    // Applies the chosen filter to the list that is already loaded.
    private void showRows() {
        int index = filter.getSelectionModel().getSelectedIndex();
        AuditAction chosen = index <= 0 ? null : AuditAction.values()[index - 1];
        List<AuditEntry> rows = entries.stream()
                .filter(e -> chosen == null || e.action() == chosen)
                .toList();
        table.getItems().setAll(rows.size() > MAX_ROWS ? rows.subList(0, MAX_ROWS) : rows);
        countLabel.setText(rows.size() > MAX_ROWS
                ? "Mostrando los últimos " + MAX_ROWS + " de " + rows.size() + " eventos"
                : rows.size() == 1 ? "1 evento" : rows.size() + " eventos");
    }

    // Red for what looks like an attack, amber for what changes access, green for normal events.
    private static String styleOf(String label) {
        for (AuditAction action : AuditAction.values()) {
            if (action.getLabel().equals(label)) {
                return switch (action) {
                    case LOGIN_FAILED, LOCKOUT -> "nf-state-bad";
                    case SESSION_EXPIRED, PASSWORD_RESET, USER_DEACTIVATED, BACKUP_RESTORED -> "nf-state-warn";
                    case LOGIN, USER_CREATED, USER_REACTIVATED, BACKUP_CREATED -> "nf-state-on";
                    default -> "nf-state-off";
                };
            }
        }
        return "nf-state-off";
    }
}
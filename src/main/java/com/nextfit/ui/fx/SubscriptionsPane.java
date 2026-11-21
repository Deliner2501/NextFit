package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Subscription;
import com.nextfit.model.SubscriptionStatus;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.Formats;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen with every membership sold: who has it, until when and in what state.
 * The staff can filter by state, search by member and cancel a membership. The
 * rules live in {@link SubscriptionService}; this class only shows data.
 */
final class SubscriptionsPane extends VBox {

    private static final int SOON_DAYS = 7;
    private static final String SOON = "Por vencer";

    private enum Filter {
        ALL("Todas"), ACTIVE("Activas"), EXPIRING("Por vencer (7 días)"), UPCOMING("Por iniciar"),
        EXPIRED("Vencidas"), CANCELLED("Canceladas");

        private final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    /** One table row: the subscription with the names already looked up. */
    private record Row(Subscription subscription, String member, String plan, String state, String daysLeft) {
    }

    private final SubscriptionService subscriptions;
    private final MembershipPlanService plans;
    private final MemberService members;
    private final TableView<Row> table = new TableView<>();
    private final Label countLabel = new Label();
    private final TextField searchField = new TextField();
    private final ComboBox<Filter> filterBox = new ComboBox<>();

    /**
     * @param subscriptions the service that knows the memberships
     * @param plans         the service used to show plan names
     * @param members       the service used to show member names
     */
    SubscriptionsPane(SubscriptionService subscriptions, MembershipPlanService plans, MemberService members) {
        super(16);
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.members = members;
        getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Suscripciones");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        searchField.setPromptText("Buscar por nombre o documento");
        searchField.getStyleClass().add("nf-input");
        searchField.setPrefSize(280, 44);
        searchField.textProperty().addListener((obs, old, now) -> refresh());
        filterBox.getItems().addAll(Filter.values());
        filterBox.setValue(Filter.ALL);
        filterBox.setPrefHeight(44);
        filterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Filter filter) {
                return filter == null ? "" : filter.label;
            }

            @Override
            public Filter fromString(String text) {
                return null;
            }
        });
        filterBox.valueProperty().addListener((obs, old, now) -> refresh());
        Button cancel = new Button("Cancelar membresía");
        cancel.getStyleClass().add("nf-secondary");
        cancel.setOnAction(e -> cancelSelected());

        HBox header = new HBox(10, titles, searchField, filterBox, cancel);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<Row, String> member = FxTables.column("Socio", 220, Row::member);
        TableColumn<Row, String> plan = FxTables.column("Plan", 160, Row::plan);
        TableColumn<Row, String> start = FxTables.column("Inicio", 120,
                r -> Formats.date(r.subscription().getStartDate()));
        TableColumn<Row, String> end = FxTables.column("Fin", 120,
                r -> Formats.date(r.subscription().getEndDate()));
        TableColumn<Row, String> state = FxTables.column("Estado", 130, Row::state);
        FxTables.colorize(state, SubscriptionsPane::styleOf);
        TableColumn<Row, String> days = FxTables.column("Días restantes", 140, Row::daysLeft);
        table.getColumns().addAll(List.of(member, plan, start, end, state, days));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No hay suscripciones para mostrar"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(300);
        return table;
    }

    private static String styleOf(String state) {
        if (state.equals(SubscriptionStatus.ACTIVE.getLabel())) {
            return "nf-state-on";
        }
        if (state.equals(SOON)) {
            return "nf-state-warn";
        }
        if (state.equals(SubscriptionStatus.CANCELLED.getLabel())) {
            return "nf-state-bad";
        }
        return "nf-state-off";
    }

    /** Reloads the memberships from the service, applying the search text and the filter. */
    void refresh() {
        Map<Long, Member> memberById = new HashMap<>();
        for (Member member : members.findAll()) {
            memberById.put(member.getId(), member);
        }
        Map<Long, MembershipPlan> planById = new HashMap<>();
        for (MembershipPlan plan : plans.findAll()) {
            planById.put(plan.getId(), plan);
        }
        String wanted = searchField.getText().trim().toLowerCase();
        Filter filter = filterBox.getValue();

        List<Row> rows = new java.util.ArrayList<>();
        for (Subscription s : subscriptions.findAll()) {
            Member member = memberById.get(s.getMemberId());
            String name = member == null ? "Socio #" + s.getMemberId() : member.getFullName();
            String document = member == null ? "" : member.getDocumentNumber();
            if (!wanted.isEmpty() && !name.toLowerCase().contains(wanted) && !document.contains(wanted)) {
                continue;
            }
            if (!matches(s, filter)) {
                continue;
            }
            MembershipPlan plan = planById.get(s.getPlanId());
            SubscriptionStatus status = subscriptions.statusOf(s);
            long daysLeft = subscriptions.daysLeftOf(s);
            rows.add(new Row(s, name, plan == null ? "Plan #" + s.getPlanId() : plan.getName(), stateText(s),
                    status == SubscriptionStatus.ACTIVE ? String.valueOf(daysLeft) : "-"));
        }
        table.getItems().setAll(rows);
        countLabel.setText(rows.size() == 1 ? "1 suscripción" : rows.size() + " suscripciones");
    }

    private boolean matches(Subscription s, Filter filter) {
        SubscriptionStatus status = subscriptions.statusOf(s);
        return switch (filter == null ? Filter.ALL : filter) {
            case ALL -> true;
            case ACTIVE -> status == SubscriptionStatus.ACTIVE;
            case EXPIRING -> status == SubscriptionStatus.ACTIVE && subscriptions.daysLeftOf(s) <= SOON_DAYS;
            case UPCOMING -> status == SubscriptionStatus.UPCOMING;
            case EXPIRED -> status == SubscriptionStatus.EXPIRED;
            case CANCELLED -> status == SubscriptionStatus.CANCELLED;
        };
    }

    private String stateText(Subscription s) {
        SubscriptionStatus status = subscriptions.statusOf(s);
        if (status == SubscriptionStatus.ACTIVE && subscriptions.daysLeftOf(s) <= SOON_DAYS) {
            return SOON;
        }
        return status.getLabel();
    }

    private void cancelSelected() {
        Row selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona una suscripción", "Elige una suscripción de la tabla primero.");
            return;
        }
        if (!FxDialogs.confirm(this, "Cancelar membresía",
                "¿Cancelar la membresía de " + selected.member() + "? El socio perderá el acceso.",
                "Cancelar membresía")) {
            return;
        }
        try {
            subscriptions.cancel(selected.subscription().getId());
            refresh();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
    }
}

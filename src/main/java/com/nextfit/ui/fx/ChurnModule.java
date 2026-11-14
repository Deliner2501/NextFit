package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Role;
import com.nextfit.service.churn.ChurnRisk;
import com.nextfit.service.churn.ChurnService;
import com.nextfit.service.churn.RiskLevel;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Churn module in JavaFX: it shows which members with a membership may stop coming, why, and what
 * the staff should do. The administrator and the reception can open it. The risk, the level, the
 * reasons and the action come from {@link ChurnService}; this class only shows them.
 */
public final class ChurnModule implements FxModule {

    private static final String ALL = "Todos los niveles";

    private final ChurnService service;
    private final TableView<ChurnRisk> table = new TableView<>();
    private final Label countLabel = new Label();
    private final ComboBox<String> filter = new ComboBox<>();
    private final KpiCard high = new KpiCard("Riesgo alto", "0", RiskLevel.HIGH.getAction(),
            FxIcons.MEMBERS, KpiCard.Kind.VIOLET);
    private final KpiCard medium = new KpiCard("Riesgo medio", "0", RiskLevel.MEDIUM.getAction(),
            FxIcons.ANALYTICS, KpiCard.Kind.FEATURE);
    private final KpiCard low = new KpiCard("Riesgo bajo", "0", "Siguen viniendo con normalidad",
            FxIcons.PANEL, KpiCard.Kind.LIME);
    private final VBox view = new VBox(16);
    private List<ChurnRisk> all = new ArrayList<>();

    /**
     * @param service the service that calculates the risk of each member
     */
    public ChurnModule(ChurnService service) {
        this.service = service;
        filter.getItems().add(ALL);
        for (RiskLevel level : RiskLevel.values()) {
            filter.getItems().add(level.getLabel());
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
        return "churn";
    }

    @Override
    public String title() {
        return "Abandono";
    }

    @Override
    public String icon() {
        return FxIcons.ANALYTICS;
    }

    @Override
    public List<String> keywords() {
        return List.of("riesgo", "retención", "predicción", "inactivos", "se van");
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** The administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private Node buildHeader() {
        Label title = new Label("Riesgo de abandono");
        title.getStyleClass().add("nf-card-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(10, titles, filter);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildCards() {
        HBox cards = new HBox(18, high, medium, low);
        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
        }
        return cards;
    }

    private Node buildTable() {
        TableColumn<ChurnRisk, String> member = FxTables.column("Socio", 200, r -> r.member().getFullName());
        TableColumn<ChurnRisk, String> score = FxTables.column("Puntaje", 90, r -> String.valueOf(r.score()));
        TableColumn<ChurnRisk, String> level = FxTables.column("Nivel", 100, r -> r.level().getLabel());
        FxTables.colorize(level, label -> styleOf(label));
        TableColumn<ChurnRisk, String> reasons = FxTables.column("Motivos", 420,
                r -> r.reasons().isEmpty() ? "Sin señales de abandono" : String.join(". ", r.reasons()));
        TableColumn<ChurnRisk, String> action = FxTables.column("Qué hacer", 240, ChurnRisk::action);
        table.getColumns().addAll(List.of(member, score, level, reasons, action));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No hay socios con membresía vigente"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(280);
        return table;
    }

    // Asks the service again and updates the cards and the list.
    private void refresh() {
        Map<RiskLevel, Long> counts;
        try {
            all = service.findAll();
            counts = service.countByLevel();
        } catch (NextFitException e) {
            FxDialogs.error(view, e);
            return;
        }
        high.value().setText(String.valueOf(counts.get(RiskLevel.HIGH)));
        medium.value().setText(String.valueOf(counts.get(RiskLevel.MEDIUM)));
        low.value().setText(String.valueOf(counts.get(RiskLevel.LOW)));
        showRows();
    }

    // Applies the chosen level to the list that is already loaded.
    private void showRows() {
        int index = filter.getSelectionModel().getSelectedIndex();
        RiskLevel chosen = index <= 0 ? null : RiskLevel.values()[index - 1];
        List<ChurnRisk> rows = all.stream()
                .filter(r -> chosen == null || r.level() == chosen)
                .toList();
        table.getItems().setAll(rows);
        countLabel.setText(rows.size() == 1 ? "1 socio" : rows.size() + " socios");
    }

    // Green for low risk, amber for medium and red for high.
    private static String styleOf(String label) {
        for (RiskLevel level : RiskLevel.values()) {
            if (level.getLabel().equals(label)) {
                return switch (level) {
                    case HIGH -> "nf-state-bad";
                    case MEDIUM -> "nf-state-warn";
                    case LOW -> "nf-state-on";
                };
            }
        }
        return "nf-state-off";
    }
}
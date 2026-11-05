package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.AnalyticsService;
import com.nextfit.service.AnalyticsService.AtRisk;
import com.nextfit.service.AnalyticsService.Report;
import com.nextfit.service.MemberService;
import com.nextfit.service.OccupancyHeatmap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;

/**
 * Analytics screen: four insight cards, the busy-hours heatmap and the list of
 * members who stopped coming. All numbers come from {@link AnalyticsService};
 * this class only decides how to draw them.
 */
final class AnalyticsView extends VBox {

    private static final String[] DAYS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final int DEFAULT_FIRST_HOUR = 5;
    private static final int DEFAULT_LAST_HOUR = 22;
    private static final int LISTED_AT_RISK = 8;

    private final AnalyticsService service;
    private final MemberService members;

    AnalyticsView(AnalyticsService service, MemberService members) {
        super(20);
        this.service = service;
        this.members = members;
        setPadding(new Insets(0, 4, 24, 0));
    }

    /** Works out the report again and redraws the whole screen. */
    void refresh() {
        Report report = service.report();
        Label title = new Label("Analítica");
        title.getStyleClass().add("nf-title");
        Label subtitle = new Label("Cuándo se llena el gimnasio y a quién hay que recuperar");
        subtitle.getStyleClass().add("nf-muted");
        getChildren().setAll(new VBox(4, title, subtitle),
                row(peakCard(report.heatmap()), stayCard(report), renewalCard(report), riskCard(report)),
                heatmapCard(report.heatmap()),
                atRiskCard(report));
    }

    private static HBox row(Region... cards) {
        HBox row = new HBox(16, cards);
        for (Region card : cards) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setPrefWidth(10);
            card.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    private static Region kpi(String caption, String value, String hint, String icon) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("nf-muted");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("nf-kpi-value");
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("nf-muted-small");
        hintLabel.setWrapText(true);
        VBox texts = new VBox(6, captionLabel, valueLabel, hintLabel);
        HBox.setHgrow(texts, Priority.ALWAYS);
        StackPane badge = new StackPane(FxIcons.of(icon, 20));
        badge.getStyleClass().add("nf-badge");
        badge.setMinSize(44, 44);
        badge.setMaxSize(44, 44);
        HBox box = new HBox(texts, badge);
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(20));
        return box;
    }

    private static Region peakCard(OccupancyHeatmap map) {
        OccupancyHeatmap.Slot peak = map.peak();
        if (peak.count() == 0) {
            return kpi("Hora pico", "—", "aún no hay entradas registradas", FxIcons.ANALYTICS);
        }
        return kpi("Hora pico", DAYS[peak.day().getValue() - 1].substring(0, 3) + " " + hour(peak.hour()),
                peak.count() + " entradas en las últimas " + AnalyticsService.HEATMAP_WEEKS + " semanas",
                FxIcons.ANALYTICS);
    }

    private static Region stayCard(Report report) {
        String value = report.averageStayMin() == 0 ? "—" : report.averageStayMin() + " min";
        return kpi("Estancia promedio", value, "entre la entrada y la salida", FxIcons.TRAINING);
    }

    private static Region renewalCard(Report report) {
        if (report.renewalRate() < 0) {
            return kpi("Renovación", "—", "ninguna membresía venció en los últimos "
                    + AnalyticsService.RENEWAL_WINDOW_DAYS + " días", FxIcons.BILLING);
        }
        return kpi("Renovación", Math.round(report.renewalRate() * 100) + " %",
                "de " + report.renewalBase() + " membresías vencidas en " + AnalyticsService.RENEWAL_WINDOW_DAYS
                        + " días", FxIcons.BILLING);
    }

    private static Region riskCard(Report report) {
        return kpi("En riesgo", Integer.toString(report.atRisk().size()),
                "con membresía vigente y sin venir hace " + AnalyticsService.AT_RISK_DAYS + " días o más",
                FxIcons.USERS);
    }

    private Region heatmapCard(OccupancyHeatmap map) {
        int first = DEFAULT_FIRST_HOUR;
        int last = DEFAULT_LAST_HOUR;
        for (DayOfWeek day : DayOfWeek.values()) {
            for (int h = 0; h < 24; h++) {
                if (map.count(day, h) > 0) {
                    first = Math.min(first, h);
                    last = Math.max(last, h);
                }
            }
        }
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        for (int h = first; h <= last; h++) {
            Label label = new Label(Integer.toString(h));
            label.getStyleClass().add("nf-muted-small");
            GridPane.setHalignment(label, javafx.geometry.HPos.CENTER);
            grid.add(label, h - first + 1, 0);
        }
        long max = map.max();
        for (DayOfWeek day : DayOfWeek.values()) {
            int row = day.getValue();
            Label dayLabel = new Label(DAYS[row - 1].substring(0, 3));
            dayLabel.getStyleClass().add("nf-muted-small");
            grid.add(dayLabel, 0, row);
            for (int h = first; h <= last; h++) {
                grid.add(cell(map.count(day, h), max, day, h), h - first + 1, row);
            }
        }
        Label caption = new Label(map.total() == 0
                ? "Todavía no hay entradas para dibujar. Registra visitas en Control de acceso."
                : "Entradas por día y hora (últimas " + AnalyticsService.HEATMAP_WEEKS
                        + " semanas). Más color significa más gente.");
        caption.getStyleClass().add("nf-muted-small");
        return card("Horas de mayor afluencia", new VBox(12, caption, grid));
    }

    private static Region cell(long count, long max, DayOfWeek day, int hour) {
        Region cell = new Region();
        cell.setMinSize(28, 28);
        cell.setPrefSize(34, 30);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        if (count == 0) {
            cell.getStyleClass().add("nf-heat-empty");
        } else {
            cell.getStyleClass().add("nf-heat-cell");
            cell.setOpacity(0.22 + 0.78 * count / max);
        }
        Tooltip.install(cell, new Tooltip(DAYS[day.getValue() - 1] + " " + hour(hour) + " · "
                + (count == 1 ? "1 entrada" : count + " entradas")));
        return cell;
    }

    private Region atRiskCard(Report report) {
        VBox list = new VBox(10);
        if (report.atRisk().isEmpty()) {
            Label none = new Label("Todos los socios con membresía vigente han venido en los últimos "
                    + AnalyticsService.AT_RISK_DAYS + " días.");
            none.getStyleClass().add("nf-muted-small");
            list.getChildren().add(none);
        }
        int shown = 0;
        for (AtRisk risk : report.atRisk()) {
            if (shown++ == LISTED_AT_RISK) {
                Label more = new Label("y " + (report.atRisk().size() - LISTED_AT_RISK) + " más");
                more.getStyleClass().add("nf-muted-small");
                list.getChildren().add(more);
                break;
            }
            Member member = members.findById(risk.memberId());
            Label name = new Label(member.getFullName());
            name.getStyleClass().add("nf-list-name");
            Label since = new Label(sinceText(risk));
            since.getStyleClass().add("nf-muted-small");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label phone = new Label(member.getPhone());
            phone.getStyleClass().add("nf-muted-small");
            HBox line = new HBox(14, MemberAvatar.of(member, 38), new VBox(2, name, since), spacer, phone);
            line.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(line);
        }
        return card("Socios en riesgo de abandono", list);
    }

    private static String sinceText(AtRisk risk) {
        if (risk.lastVisit() == null) {
            return "Nunca ha venido";
        }
        return "Última visita hace " + risk.daysSince() + " días";
    }

    private static String hour(int hour) {
        return String.format("%02d:00", hour);
    }

    private static Region card(String title, Region content) {
        Label label = new Label(title);
        label.getStyleClass().add("nf-card-title");
        VBox box = new VBox(12, label, content);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(20));
        return box;
    }
}

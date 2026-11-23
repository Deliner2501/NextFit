package com.nextfit.ui.fx;

import com.nextfit.service.AttendanceService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.occupancy.GymCapacity;
import com.nextfit.service.DashboardService.Summary;
import com.nextfit.ui.Formats;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.LongFunction;

/** Arranges the daily indicators and charts calculated by {@link DashboardService}. */
final class DashboardView extends VBox {

    private static final String LAST_3 = "Últimos 3 meses";
    private static final String LAST_6 = "Últimos 6 meses";
    private static final double WIDE_LAYOUT = 1010;
    private static final double FIVE_CARD_LAYOUT = 1200;
    private final DashboardService service;
    private final AttendanceService attendanceService;
    private final GymCapacity capacity;

    private record DashboardData(Summary summary, long inside, int limit) {}
    private final GridPane indicators = new GridPane();
    private final GridPane charts = new GridPane();
    private final Label loadingStatus = new Label();
    private List<KpiCard> keyFigures = List.of();
    private Region income;
    private Region memberships;
    private boolean loading;

    /** @param service the source of the dashboard summary */
    DashboardView(DashboardService service) {
        this(service, null, null);
    }

    /**
     * @param service the source of the dashboard summary
     * @param attendanceService the source of current visits
     * @param capacity the configured admission limit
     */
    DashboardView(DashboardService service, AttendanceService attendanceService, GymCapacity capacity) {
        super(16);
        this.attendanceService = attendanceService;
        this.capacity = capacity;
        this.service = service;
        getStyleClass().add("nf-dashboard");
        loadingStatus.getStyleClass().add("nf-muted");
        setPadding(new Insets(0, 0, 16, 0));
        for (GridPane grid : List.of(indicators, charts)) {
            grid.setHgap(16);
            grid.setVgap(16);
        }
        widthProperty().addListener((obs, old, width) -> {
            double before = old.doubleValue();
            double after = width.doubleValue();
            if (income != null && ((before >= WIDE_LAYOUT) != (after >= WIDE_LAYOUT)
                    || (before >= FIVE_CARD_LAYOUT) != (after >= FIVE_CARD_LAYOUT))) {
                arrange();
            }
        });
    }

    /** Reloads the business data and displays the same figures in either layout. */
    void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        loadingStatus.setText("Cargando datos del panel…");
        getChildren().remove(loadingStatus);
        getChildren().add(0, loadingStatus);
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                Summary summary = service.summary();
                return new DashboardData(summary,
                        attendanceService == null ? 0 : attendanceService.countInside(),
                        capacity == null ? 0 : capacity.limit());
            }
        };
        task.setOnSucceeded(event -> {
            loading = false;
            display(task.getValue());
        });
        task.setOnFailed(event -> {
            loading = false;
            loadingStatus.setText("No se pudieron cargar los datos del panel. Vuelve a entrar en Panel para reintentar.");
        });
        Thread worker = new Thread(task, "dashboard-load");
        worker.setDaemon(true);
        worker.start();
    }

    private void display(DashboardData data) {
        Summary summary = data.summary();
        long valid = Math.max(0, summary.activeSubscriptions() - summary.expiringSoon());
        KpiCard revenue = figure("Ingresos del mes", "", FxIcons.ANALYTICS,
                summary.incomeThisMonth(), Formats::money);
        summary.incomeChangePercent().ifPresent(percent -> revenue.trendBelow(
                Formats.signedPercent(percent), percent >= 0,
                "Frente a los mismos días del mes anterior (" + Formats.money(summary.incomeLastMonthToDate()) + ")"));
        KpiCard active = figure("Membresías activas", "", FxIcons.MEMBERS,
                summary.activeSubscriptions(), Long::toString);
        active.getStyleClass().add("nf-dashboard-members");
        KpiCard expiring = figure("Por vencer", "Próximos 7 días", FxIcons.BILLING,
                summary.expiringSoon(), Long::toString);
        KpiCard today = figure("Asistencia de hoy", "Entradas registradas", FxIcons.ACCESS,
                summary.checkInsToday(), Long::toString);
        today.getStyleClass().add("nf-dashboard-attendance");
        long change = summary.checkInsChange();
        if (change != 0) {
            today.trendBelow((change > 0 ? "+" : "") + change, change > 0,
                    "Frente a ayer (" + summary.checkInsYesterday() + ")");
        }

        if (data.limit() > 0) {
            long spots = Math.max(0, data.limit() - data.inside());
            String availability = spots == 0 ? "Aforo completo"
                    : spots == 1 ? "1 cupo libre" : spots + " cupos libres";
            KpiCard occupancy = figure("Aforo actual", availability, FxIcons.ACCESS,
                    data.inside(), count -> count + " de " + data.limit());
            keyFigures = List.of(revenue, active, expiring, today, occupancy);
        } else {
            keyFigures = List.of(revenue, active, expiring, today);
        }
        income = incomeCard(summary);
        memberships = card("Estado de las membresías", null, DashboardCharts.statusDonut(valid, summary));
        Region attendance = card("Asistencia por día de la semana", null,
                DashboardCharts.weekdayBars(summary.checkInsByWeekday()));
        arrange();
        getChildren().setAll(indicators, charts, attendance);
        FxMotion.enter(getChildren(), 0, 65);
    }

    /** Keeps figures and legends readable when the sidebar is open in a narrow window. */
    private void arrange() {
        boolean wide = getWidth() >= WIDE_LAYOUT;
        int columns = getWidth() >= FIVE_CARD_LAYOUT ? keyFigures.size() : 2;
        indicators.getChildren().clear();
        indicators.getColumnConstraints().clear();
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            indicators.getColumnConstraints().add(column(100.0 / columns));
        }
        for (int index = 0; index < keyFigures.size(); index++) {
            indicators.add(keyFigures.get(index), index % columns, index / columns);
        }

        charts.getChildren().clear();
        charts.getColumnConstraints().setAll(wide ? List.of(column(57), column(43)) : List.of(column(100)));
        charts.add(income, 0, 0);
        charts.add(memberships, wide ? 1 : 0, wide ? 0 : 1);
    }

    private static ColumnConstraints column(double percent) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percent);
        return column;
    }

    private static KpiCard figure(String title, String hint, String icon, long amount, LongFunction<String> format) {
        KpiCard card = new KpiCard(title, format.apply(0), hint, icon, KpiCard.Kind.LIME);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        FxMotion.countUp(card.value(), amount, format);
        return card;
    }

    private static Region incomeCard(Summary summary) {
        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll(LAST_3, LAST_6);
        range.setValue(LAST_6);
        range.setAccessibleText("Período de ingresos");
        StackPane chart = new StackPane(DashboardCharts.incomeChart(summary.incomeByMonth(), 6));
        range.valueProperty().addListener((obs, old, value) -> chart.getChildren()
                .setAll(DashboardCharts.incomeChart(summary.incomeByMonth(), LAST_3.equals(value) ? 3 : 6)));
        return card("Evolución de ingresos", range, chart);
    }

    private static Region card(String title, Region action, Region content) {
        Label label = new Label(title);
        label.getStyleClass().add("nf-card-title");
        label.setWrapText(true);
        HBox heading = new HBox(12, label);
        heading.setAlignment(Pos.CENTER_LEFT);
        if (action != null) {
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
            heading.getChildren().add(action);
        }
        VBox box = new VBox(8, heading, content);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(14, 16, 12, 16));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }
}

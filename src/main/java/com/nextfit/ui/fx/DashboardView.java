package com.nextfit.ui.fx;

import com.nextfit.service.DashboardService;
import com.nextfit.service.DashboardService.Summary;
import com.nextfit.service.Session;
import com.nextfit.ui.Formats;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongFunction;

/**
 * Home screen: a greeting, four key numbers and three charts. Every number is
 * worked out by {@link DashboardService}; this class only decides how to draw it.
 */
final class DashboardView extends VBox {

    private static final String[] DAY_NAMES = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final DashboardService service;
    private final Session session;

    /**
     * @param service the source of the summary
     * @param session the signed-in user, for the greeting
     */
    DashboardView(DashboardService service, Session session) {
        super(20);
        this.service = service;
        this.session = session;
        setPadding(new Insets(0, 4, 24, 0));
    }

    /** Reads the summary again and redraws the whole screen. */
    void refresh() {
        Summary summary = service.summary();
        long vigentes = Math.max(0, summary.activeSubscriptions() - summary.expiringSoon());
        getChildren().setAll(
                hero(summary),
                row(kpi("Membresías activas", summary.activeSubscriptions(), "socios con acceso hoy", FxIcons.MEMBERS, n -> Long.toString(n)),
                    kpi("Ingresos del mes", summary.incomeThisMonth(), "desde el primer día del mes", FxIcons.BILLING, Formats::money),
                    kpi("Por vencer", summary.expiringSoon(), "terminan en 7 días o menos", FxIcons.USERS, n -> Long.toString(n)),
                    kpi("Asistencia de hoy", summary.checkInsToday(), "entradas registradas", FxIcons.PANEL, n -> Long.toString(n))),
                row(statusCard(vigentes, summary), weekdayCard(summary.checkInsByWeekday())),
                incomeCard(summary.incomeByMonth()));
    }

    private Region hero(Summary summary) {
        Region photo = FxImages.cover("/images/banners/dashboard.jpg", 0.5, 0.5);
        Region shade = new Region();
        shade.getStyleClass().add("nf-hero-shade");
        Label greeting = new Label("Hola, " + session.getCurrentUser().map(u -> u.getFullName()).orElse("bienvenido"));
        greeting.getStyleClass().add("nf-hero-title");
        Label date = new Label("Así va el gimnasio hoy");
        date.getStyleClass().add("nf-hero-text");
        VBox text = new VBox(4, greeting, date);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setPadding(new Insets(0, 0, 0, 36));
        StackPane hero = new StackPane(photo, shade, text);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMinHeight(140);
        hero.setPrefHeight(140);
        hero.setMaxHeight(140);
        hero.getStyleClass().add("nf-hero");
        roundCorners(hero, 16);
        return hero;
    }

    private static void roundCorners(Region region, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
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

    private static Region kpi(String caption, long value, String hint, String icon,
                              LongFunction<String> format) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("nf-muted");
        Label valueLabel = new Label(format.apply(0));
        valueLabel.getStyleClass().add("nf-kpi-value");
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("nf-muted-small");
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
        countUp(valueLabel, value, format);
        return box;
    }

    // El número sube desde cero hasta su valor en unos instantes.
    private static void countUp(Label label, long target, LongFunction<String> format) {
        if (target == 0) {
            return;
        }
        Transition animation = new Transition() {
            {
                setCycleDuration(Duration.millis(750));
            }

            @Override
            protected void interpolate(double fraction) {
                double eased = 1 - Math.pow(1 - fraction, 3);
                label.setText(format.apply(Math.round(target * eased)));
            }
        };
        animation.play();
    }

    private Region statusCard(long vigentes, Summary s) {
        PieChart chart = new PieChart();
        chart.getData().addAll(
                new PieChart.Data("Vigentes", vigentes),
                new PieChart.Data("Por vencer", s.expiringSoon()),
                new PieChart.Data("Por iniciar", s.upcoming()),
                new PieChart.Data("Vencidas", s.expired()),
                new PieChart.Data("Canceladas", s.cancelled()));
        chart.setLabelsVisible(false);
        chart.setLegendSide(javafx.geometry.Side.BOTTOM);
        chart.setStartAngle(90);
        chart.setMinHeight(280);
        chart.setPrefHeight(300);
        chart.setMaxHeight(320);
        return card("Estado de las membresías", chart);
    }

    private Region weekdayCard(Map<DayOfWeek, Long> byDay) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = countAxis(byDay.values().stream().mapToLong(Long::longValue).max().orElse(0));
        BarChart<String, Number> chart = new BarChart<>(x, y);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < days.length; i++) {
            series.getData().add(new XYChart.Data<>(DAY_NAMES[i], byDay.getOrDefault(days[i], 0L)));
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setBarGap(6);
        chart.setCategoryGap(18);
        chart.setMinHeight(280);
        return card("Asistencia por día de la semana", chart);
    }

    // Eje de números enteros con 5 divisiones, para que no salgan marcas como 2,5 o 12,5.
    private static NumberAxis countAxis(long max) {
        long top = Math.max(5, ((max + 4) / 5) * 5);
        NumberAxis axis = new NumberAxis(0, top, top / 5.0);
        axis.setMinorTickVisible(false);
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return String.valueOf(value.longValue());
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });
        return axis;
    }

    private Region incomeCard(Map<YearMonth, Long> byMonth) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(x, y);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<YearMonth, Long> entry : new TreeMap<>(byMonth).entrySet()) {
            series.getData().add(new XYChart.Data<>(Formats.monthShort(entry.getKey()), entry.getValue() / 1_000_000.0));
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setMinHeight(260);
        return card("Ingresos de los últimos meses (millones de pesos)", chart);
    }

    private static Region card(String title, Region content) {
        Label label = new Label(title);
        label.getStyleClass().add("nf-card-title");
        VBox box = new VBox(10, label, content);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }
}

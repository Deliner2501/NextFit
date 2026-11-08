package com.nextfit.ui.fx;

import com.nextfit.service.DashboardService.Summary;
import com.nextfit.ui.Formats;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.transform.Scale;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The charts of the home screen. Each method receives plain numbers and returns
 * a node, so {@link DashboardView} only decides where they go.
 */
final class DashboardCharts {

    private static final String[] DAY_NAMES = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private DashboardCharts() {
    }

    /**
     * Area chart of the income of the last months, with a green line and violet fade.
     *
     * @param byMonth the income of each month
     * @param months  how many of the latest months to draw
     * @return the chart
     */
    static Region incomeChart(Map<YearMonth, Long> byMonth, int months) {
        CategoryAxis x = new CategoryAxis();
        List<Map.Entry<YearMonth, Long>> entries = List.copyOf(new TreeMap<>(byMonth).entrySet());
        List<Map.Entry<YearMonth, Long>> shown = entries.subList(Math.max(0, entries.size() - months), entries.size());
        // A fixed scale: with automatic scaling the axis would jump around while the line grows.
        double max = shown.stream().mapToDouble(e -> e.getValue() / 1_000_000.0).max().orElse(0);
        NumberAxis y = incomeAxis(max);
        AreaChart<String, Number> chart = new AreaChart<>(x, y);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<XYChart.Data<String, Number>> points = new ArrayList<>();
        List<Double> targets = new ArrayList<>();
        for (Map.Entry<YearMonth, Long> entry : shown) {
            // The points start at zero and rise to their value.
            XYChart.Data<String, Number> point = new XYChart.Data<>(Formats.monthShort(entry.getKey()), 0.0);
            series.getData().add(point);
            // The mark of each month says the full amount when the mouse rests on it.
            Tooltip tip = new Tooltip(Formats.money(entry.getValue()));
            point.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    Tooltip.install(node, tip);
                }
            });
            if (point.getNode() != null) {
                Tooltip.install(point.getNode(), tip);
            }
            points.add(point);
            targets.add(entry.getValue() / 1_000_000.0);
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(230);
        rise(points, targets, 250, 70);
        return chart;
    }

    /**
     * Donut of the state of the memberships, with a legend that shows the percentages.
     *
     * @param vigentes memberships that are valid and not about to end
     * @param s        the summary with the other counts
     * @return the card content
     */
    static Region statusDonut(long vigentes, Summary s) {
        String[] names = {"Vigentes", "Por vencer", "Por iniciar", "Vencidas", "Canceladas"};
        long[] values = {vigentes, s.expiringSoon(), s.upcoming(), s.expired(), s.cancelled()};
        long total = 0;
        PieChart pie = new PieChart();
        for (int i = 0; i < names.length; i++) {
            pie.getData().add(new PieChart.Data(names[i], values[i]));
            total += values[i];
        }
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setStartAngle(90);
        pie.setPadding(Insets.EMPTY);
        pie.setMinSize(190, 190);
        pie.setAnimated(false);

        Circle hole = new Circle();
        hole.getStyleClass().add("nf-donut-hole");
        Label count = new Label("0");
        count.getStyleClass().add("nf-donut-total");
        Label caption = new Label("membresías");
        caption.getStyleClass().add("nf-muted-small");
        VBox center = new VBox(0, count, caption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);
        StackPane donut = new StackPane(pie, hole, center);
        hole.radiusProperty().bind(Bindings.min(donut.widthProperty(), donut.heightProperty()).multiply(0.5 * 0.58));
        hole.setMouseTransparent(true);
        donut.setMinSize(190, 190);
        HBox.setHgrow(donut, Priority.ALWAYS);
        FxMotion.spinIn(pie, 450);
        FxMotion.countUp(count, total, n -> Long.toString(n));

        VBox legend = new VBox(12);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setMinWidth(170);
        for (int i = 0; i < names.length; i++) {
            Region dot = new Region();
            dot.getStyleClass().add("nf-dot-" + i);
            dot.setMinSize(12, 12);
            dot.setMaxSize(12, 12);
            Label name = new Label(names[i]);
            name.getStyleClass().add("nf-legend-name");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            long percent = total == 0 ? 0 : Math.round(values[i] * 100.0 / total);
            Label share = new Label(percent + " %");
            share.getStyleClass().add("nf-legend-value");
            HBox row = new HBox(10, dot, name, spacer, share);
            row.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(row);
        }
        HBox box = new HBox(20, donut, legend);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /**
     * Bars of the entries by weekday. Every bar shows its number, and today's bar is green with a bubble.
     *
     * @param byDay the entries of each weekday
     * @return the chart
     */
    static Region weekdayBars(Map<DayOfWeek, Long> byDay) {
        long max = byDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
        CategoryAxis x = new CategoryAxis();
        // Room above the tallest bar for its number, so the plot area never cuts it.
        NumberAxis y = countAxis((long) Math.ceil(max * 1.25));
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DayOfWeek[] days = DayOfWeek.values();
        int today = LocalDate.now().getDayOfWeek().getValue() - 1;
        List<Scale> grows = new ArrayList<>();
        for (int i = 0; i < days.length; i++) {
            long entries = byDay.getOrDefault(days[i], 0L);
            // The data already holds its final value, so the chart always lays the bars out right; the rise
            // is only a scale of each bar (see decorate).
            XYChart.Data<String, Number> data = new XYChart.Data<>(DAY_NAMES[i], (double) entries);
            decorate(data, i == today, entries, grows);
            series.getData().add(data);
        }
        chart.getData().add(series);
        Platform.runLater(() -> {
            Timeline timeline = new Timeline();
            for (int i = 0; i < grows.size(); i++) {
                double start = 500 + i * 80;
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(start + 650),
                        new KeyValue(grows.get(i).yProperty(), 1.0, Interpolator.EASE_OUT)));
            }
            timeline.play();
        });
        chart.setLegendVisible(false);
        chart.setBarGap(6);
        chart.setCategoryGap(22);
        chart.setMinHeight(250);
        return chart;
    }

    private static void decorate(XYChart.Data<String, Number> data, boolean today, long entries, List<Scale> grows) {
        java.util.function.Consumer<Node> apply = node -> {
            if (node instanceof StackPane bar) {
                // The color goes on the bar itself, so it does not depend on which style rule wins.
                bar.setStyle("-fx-background-color: " + (today
                        ? "linear-gradient(to bottom, #C4FF5A, #6DB21A)"
                        : "linear-gradient(to bottom, #8F55FF, #5A22B8)")
                        + "; -fx-background-radius: 8 8 0 0;");
                Scale grow = new Scale(1, 0);
                grow.pivotYProperty().bind(bar.heightProperty());
                bar.getTransforms().add(grow);
                grows.add(grow);
                Label label = new Label(Long.toString(entries));
                label.getStyleClass().add(today ? "nf-bar-bubble" : "nf-bar-value");
                label.setMinWidth(Region.USE_PREF_SIZE);
                label.setMouseTransparent(true);
                StackPane.setAlignment(label, Pos.TOP_CENTER);
                label.setTranslateY(today ? -34 : -24);
                bar.getChildren().add(label);
                if (today) {
                    bar.getStyleClass().add("nf-bar-today");
                }
            }
        };
        if (data.getNode() != null) {
            apply.accept(data.getNode());
        }
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) {
                apply.accept(node);
            }
        });
    }

    /**
     * Makes the values of a chart climb from zero to their targets, one after another. It starts once the
     * screen has been laid out, so the first frames are not lost.
     *
     * @param points    the data points, all starting at zero
     * @param targets   the final value of each point
     * @param delay     milliseconds before the first one starts
     * @param stagger   extra milliseconds between one point and the next
     */
    private static void rise(List<XYChart.Data<String, Number>> points, List<Double> targets, double delay,
                             double stagger) {
        Timeline timeline = new Timeline();
        for (int i = 0; i < points.size(); i++) {
            double start = delay + i * stagger;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(start), new KeyValue(points.get(i).YValueProperty(), 0.0)),
                    new KeyFrame(Duration.millis(start + 650),
                            new KeyValue(points.get(i).YValueProperty(), targets.get(i), Interpolator.EASE_OUT)));
        }
        Platform.runLater(timeline::play);
    }

    // Fixed scale for the income, in millions: a round step and a ceiling a little above the highest value.
    private static NumberAxis incomeAxis(double max) {
        double step = 0.25;
        double top = 1;
        if (max > 0) {
            double raw = max * 1.1 / 5;
            double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
            double n = raw / magnitude;
            step = (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * magnitude;
            top = Math.ceil(max * 1.05 / step) * step;
        }
        NumberAxis axis = new NumberAxis(0, top, step);
        axis.setMinorTickVisible(false);
        // The values are in millions of pesos, so each mark says it: "$ 0,5 M".
        java.text.DecimalFormat shown = new java.text.DecimalFormat("0.##",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("es-CO")));
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value.doubleValue() == 0 ? "0" : "$ " + shown.format(value.doubleValue()) + " M";
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });
        return axis;
    }

    // Integer axis with 5 divisions, so no tick like 2.5 or 12.5 appears.
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
}

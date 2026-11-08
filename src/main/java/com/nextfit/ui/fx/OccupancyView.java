package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.occupancy.OccupancyService;
import com.nextfit.service.occupancy.OccupancySnapshot;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Live picture of the gym for the TV: how many people are inside, how full it is, who is
 * inside, the entries of today by hour and the best hour to come. It refreshes itself every
 * few seconds while it is on screen. It only shows what {@link OccupancyService} calculates.
 */
final class OccupancyView extends VBox {

    private static final int REFRESH_SECONDS = 5;

    private final OccupancyService service;
    private final KpiCard people = new KpiCard("Personas dentro", "0", "Aforo del gimnasio",
            FxIcons.MEMBERS, KpiCard.Kind.FEATURE);
    private final KpiCard level = new KpiCard("Nivel de ocupación", "-", "Se actualiza solo cada 5 segundos",
            FxIcons.ANALYTICS, KpiCard.Kind.LIME);
    private final KpiCard bestHour = new KpiCard("Mejor hora para venir hoy", "-",
            "Según las últimas 8 semanas", FxIcons.CALENDAR, KpiCard.Kind.VIOLET);
    private final Label percentLabel = new Label();
    private final ProgressBar bar = new ProgressBar(0);
    private final Label insideTitle = new Label("Quiénes están dentro");
    private final VBox visitorRows = new VBox(10);
    private final NumberAxis yAxis = new NumberAxis();
    private final BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), yAxis);
    private final Timeline timeline;
    private String levelClass = "";

    /** @param service the service that calculates the occupancy */
    OccupancyView(OccupancyService service) {
        super(18);
        this.service = service;
        timeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS), e -> tick()));
        timeline.setCycleCount(Animation.INDEFINITE);

        HBox top = new HBox(18, people, level, bestHour);
        for (Node card : top.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
        }
        HBox bottom = new HBox(18, buildInsideCard(), buildChartCard());
        VBox.setVgrow(bottom, Priority.ALWAYS);
        getChildren().addAll(top, buildPercentCard(), bottom);
        refresh();
    }

    /** Refreshes now and starts refreshing by itself. */
    void start() {
        refresh();
        timeline.play();
    }

    /** Stops the automatic refresh. */
    void stop() {
        timeline.stop();
    }

    // Si la vista ya no está en ninguna ventana (se cambió de módulo), deja de refrescar.
    private void tick() {
        if (getScene() == null) {
            timeline.stop();
        } else {
            refresh();
        }
    }

    private Node buildPercentCard() {
        percentLabel.getStyleClass().add("nf-tv-percent");
        bar.getStyleClass().add("nf-occupancy-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(22);
        VBox card = new VBox(10, percentLabel, bar);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private Node buildInsideCard() {
        insideTitle.getStyleClass().add("nf-card-title");
        ScrollPane scroll = new ScrollPane(visitorRows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("nf-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox card = new VBox(14, insideTitle, scroll);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(360);
        card.setMinWidth(300);
        card.setMaxWidth(360);
        return card;
    }

    private Node buildChartCard() {
        Label title = new Label("Entradas de hoy por hora");
        title.getStyleClass().add("nf-card-title");
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return String.valueOf(value.longValue());
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(10);
        chart.setMinHeight(240);
        VBox.setVgrow(chart, Priority.ALWAYS);
        VBox card = new VBox(14, title, chart);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // Pide la foto al servicio y la dibuja; si falla la lectura de datos, se deja lo que ya se veía.
    private void refresh() {
        OccupancySnapshot snapshot;
        try {
            snapshot = service.snapshot();
        } catch (NextFitException e) {
            return;
        }
        people.value().setText(snapshot.inside() + " / " + snapshot.capacity());
        level.value().setText(snapshot.level().getLabel());
        bestHour.value().setText(snapshot.quietestHour() < 0
                ? "Sin datos" : String.format("%02d:00", snapshot.quietestHour()));
        percentLabel.setText(snapshot.percent() + " % del aforo");
        bar.setProgress(snapshot.percent() / 100.0);
        showLevel(snapshot.level());
        showVisitors(snapshot);
        showChart(snapshot);
    }

    // El color de la barra lo pone la hoja de estilos según el nivel; aquí solo se cambia la clase.
    private void showLevel(OccupancyLevel current) {
        bar.getStyleClass().remove(levelClass);
        levelClass = "nf-level-" + current.name().toLowerCase();
        bar.getStyleClass().add(levelClass);
    }

    private void showVisitors(OccupancySnapshot snapshot) {
        insideTitle.setText("Quiénes están dentro (" + snapshot.inside() + ")");
        visitorRows.getChildren().clear();
        if (snapshot.visitors().isEmpty()) {
            Label none = new Label("Nadie en el gimnasio por ahora.");
            none.getStyleClass().add("nf-muted");
            visitorRows.getChildren().add(none);
            return;
        }
        for (OccupancySnapshot.Visitor visitor : snapshot.visitors()) {
            Label name = new Label(visitor.firstName());
            name.getStyleClass().add("nf-list-name");
            Label minutes = new Label(visitor.minutesInside() + " min");
            minutes.getStyleClass().add("nf-muted");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            visitorRows.getChildren().add(new HBox(8, name, spacer, minutes));
        }
    }

    private void showChart(OccupancySnapshot snapshot) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int current = snapshot.updatedAt().getHour();
        int max = 0;
        for (int hour = OccupancyService.OPEN_HOUR; hour < OccupancyService.CLOSE_HOUR; hour++) {
            int entries = snapshot.entriesByHour().get(hour);
            max = Math.max(max, entries);
            XYChart.Data<String, Number> data = new XYChart.Data<>(hour + "h", entries);
            highlight(data, hour == current);
            series.getData().add(data);
        }
        // Eje de números enteros con 5 divisiones, para que no salgan marcas como 2.5.
        int top = Math.max(5, ((max + 4) / 5) * 5);
        yAxis.setUpperBound(top);
        yAxis.setTickUnit(top / 5.0);
        chart.getData().setAll(List.of(series));
    }

    // La barra de la hora actual se pinta distinta (clase de la hoja de estilos).
    private static void highlight(XYChart.Data<String, Number> data, boolean current) {
        if (!current) {
            return;
        }
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) {
                node.getStyleClass().add("nf-bar-today");
            }
        });
    }
}
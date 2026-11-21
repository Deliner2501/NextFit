package com.nextfit.ui.fx;

import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.service.HealthService;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The measurements tab of the member file: a line chart with the weight over time, the
 * history in a table and a form to record a new measurement.
 */
final class MeasurementsPane extends VBox {

    private final Member member;
    private final HealthService health;
    private final Runnable onChange;
    private final XYChart.Series<String, Number> weightSeries = new XYChart.Series<>();
    private final TableView<Measurement> table = new TableView<>();

    /**
     * @param member   the member whose measurements are shown
     * @param health   the service that stores the measurements
     * @param onChange called after a measurement is saved, so other tabs can update
     */
    MeasurementsPane(Member member, HealthService health, Runnable onChange) {
        super(16);
        this.member = member;
        this.health = health;
        this.onChange = onChange;
        setPadding(new Insets(16, 4, 0, 0));

        Label chartTitle = new Label("Evolución del peso");
        chartTitle.getStyleClass().add("nf-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button add = new Button("+ Nueva medición");
        add.getStyleClass().add("nf-primary-small");
        add.setMinWidth(Region.USE_PREF_SIZE);
        add.setOnAction(e -> openForm());
        HBox header = new HBox(10, chartTitle, spacer, add);
        header.setAlignment(Pos.CENTER_LEFT);

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(false);
        y.setLabel("kg");
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.getData().add(weightSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(240);
        chart.setPrefHeight(260);
        VBox chartCard = new VBox(12, header, chart);
        chartCard.getStyleClass().add("nf-card");
        chartCard.setPadding(new Insets(20));

        getChildren().addAll(chartCard, buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private TableView<Measurement> buildTable() {
        TableColumn<Measurement, String> date = FxTables.column("Fecha", 150, m -> Formats.date(m.getDate()));
        TableColumn<Measurement, String> weight = FxTables.column("Peso (kg)", 120,
                m -> Decimals.text(m.getWeightKg()));
        TableColumn<Measurement, String> waist = FxTables.column("Cintura (cm)", 120,
                m -> Decimals.text(m.getWaistCm()));
        TableColumn<Measurement, String> chest = FxTables.column("Pecho (cm)", 120,
                m -> Decimals.text(m.getChestCm()));
        TableColumn<Measurement, String> hips = FxTables.column("Cadera (cm)", 120,
                m -> Decimals.text(m.getHipsCm()));
        table.getColumns().addAll(List.of(date, weight, waist, chest, hips));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(44);
        table.setPlaceholder(new Label("Todavía no hay mediciones. Registra la primera con «+ Nueva medición»."));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(200);
        return table;
    }

    /** Reads the measurements again and redraws the chart and the table. */
    void refresh() {
        List<Measurement> history = health.measurementHistory(member.getId());
        List<XYChart.Data<String, Number>> points = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (Measurement m : history) {
            String label = Formats.date(m.getDate());
            // Two measurements on the same day would share a label on the chart, so the second one gets a space.
            while (!used.add(label)) {
                label = label + " ";
            }
            points.add(new XYChart.Data<>(label, m.getWeightKg()));
        }
        weightSeries.getData().setAll(points);
        List<Measurement> newestFirst = new ArrayList<>(history);
        newestFirst.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        table.getItems().setAll(newestFirst);
    }

    // Opens the form to record a new measurement of the member.
    private void openForm() {
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        TextField weight = field("Ej. 72.5");
        TextField waist = field("Ej. 80");
        TextField chest = field("Ej. 98");
        TextField hips = field("Ej. 95");
        HBox firstRow = new HBox(12, FxDialogs.labeled("Fecha", datePicker), FxDialogs.labeled("Peso (kg)", weight));
        HBox secondRow = new HBox(12, FxDialogs.labeled("Cintura (cm)", waist),
                FxDialogs.labeled("Pecho (cm)", chest), FxDialogs.labeled("Cadera (cm)", hips));
        for (HBox row : List.of(firstRow, secondRow)) {
            row.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));
        }
        VBox fields = new VBox(12, firstRow, secondRow);
        fields.setPrefWidth(480);
        FxDialogs.form(this, "Nueva medición de " + member.getFullName(), "Guardar", fields, () -> {
            double weightKg = Decimals.parse("peso", weight.getText());
            double waistCm = Decimals.parse("cintura", waist.getText());
            double chestCm = Decimals.parse("pecho", chest.getText());
            double hipsCm = Decimals.parse("cadera", hips.getText());
            health.addMeasurement(new Measurement(0, member.getId(), datePicker.getValue(),
                    weightKg, waistCm, chestCm, hipsCm));
            refresh();
            onChange.run();
        });
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().add("nf-input");
        field.setPromptText(prompt);
        field.setPrefHeight(44);
        return field;
    }
}

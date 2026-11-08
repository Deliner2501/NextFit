package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.export.PdfExporter;
import com.nextfit.export.XlsxWriter;
import com.nextfit.service.ColumnType;
import com.nextfit.service.ReportService;
import com.nextfit.service.ReportTable;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Reports screen: the user picks a report (payments, attendance or members) and a range of days, sees the
 * table and exports it as PDF or Excel. The tables come from {@link ReportService}; this class only shows
 * them and starts the export.
 */
final class ReportsPane extends VBox {

    private static final String PAYMENTS = "Pagos";
    private static final String ATTENDANCE = "Asistencia";
    private static final String MEMBERS = "Socios";
    private static final DateTimeFormatter PICKER_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportService reports;
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final DatePicker fromPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker toPicker = new DatePicker(LocalDate.now());
    private final HBox range = new HBox(10);
    private final Label countLabel = new Label();
    private final TableView<List<Object>> table = new TableView<>();

    /**
     * @param reports the service that builds the tables
     */
    ReportsPane(ReportService reports) {
        super(16);
        this.reports = reports;
        setPadding(new Insets(0, 4, 24, 0));
        typeBox.getItems().addAll(PAYMENTS, ATTENDANCE, MEMBERS);
        typeBox.setValue(PAYMENTS);
        typeBox.setPrefHeight(44);
        for (DatePicker picker : List.of(fromPicker, toPicker)) {
            picker.setPrefHeight(44);
            picker.setPrefWidth(150);
            picker.setEditable(false);
            picker.setConverter(converter());
            picker.valueProperty().addListener((obs, old, now) -> refresh());
        }
        typeBox.valueProperty().addListener((obs, old, now) -> {
            range.setVisible(!MEMBERS.equals(now));
            range.setManaged(!MEMBERS.equals(now));
            refresh();
        });
        getChildren().addAll(buildHeader(), buildFilters(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private static StringConverter<LocalDate> converter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate day) {
                return day == null ? "" : PICKER_FORMAT.format(day);
            }

            @Override
            public LocalDate fromString(String text) {
                try {
                    return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), PICKER_FORMAT);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }

    private Node buildHeader() {
        Label title = new Label("Reportes");
        title.getStyleClass().add("nf-section-title");
        Label subtitle = new Label("Elige un reporte y descárgalo en PDF o Excel");
        subtitle.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button all = new Button("Excel con todo");
        all.getStyleClass().add("nf-secondary");
        all.setOnAction(e -> exportAll());
        Button excel = new Button("Excel");
        excel.getStyleClass().add("nf-secondary");
        excel.setOnAction(e -> exportExcel());
        Button pdf = new Button("Exportar PDF");
        pdf.getStyleClass().add("nf-primary-small");
        pdf.setOnAction(e -> exportPdf());
        HBox header = new HBox(10, titles, all, excel, pdf);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildFilters() {
        Label from = new Label("Desde");
        from.getStyleClass().add("nf-muted");
        Label to = new Label("Hasta");
        to.getStyleClass().add("nf-muted");
        range.getChildren().addAll(from, fromPicker, to, toPicker);
        range.setAlignment(Pos.CENTER_LEFT);
        countLabel.getStyleClass().add("nf-muted");
        Label spacerLabel = new Label();
        HBox.setHgrow(spacerLabel, Priority.ALWAYS);
        HBox filters = new HBox(14, typeBox, range, spacerLabel, countLabel);
        filters.setAlignment(Pos.CENTER_LEFT);
        return filters;
    }

    private Node buildTable() {
        // Every column shares the width in proportion to its own, so a report with many columns still shows them all.
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setFixedCellSize(46);
        table.setPlaceholder(new Label("No hay registros para mostrar"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(360);
        return table;
    }

    /** The table of the report that is chosen now. */
    private ReportTable currentTable() {
        String type = typeBox.getValue();
        if (MEMBERS.equals(type)) {
            return reports.members();
        }
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null) {
            throw new InvalidDataException("Fechas", "elige el día inicial y el final");
        }
        if (from.isAfter(to)) {
            throw new InvalidDataException("Fechas", "el día inicial no puede ser posterior al final");
        }
        return ATTENDANCE.equals(type) ? reports.attendance(from, to) : reports.payments(from, to);
    }

    /** Builds the report again and shows it. */
    void refresh() {
        table.getColumns().clear();
        table.getItems().clear();
        try {
            ReportTable data = currentTable();
            for (int c = 0; c < data.headers().size(); c++) {
                table.getColumns().add(column(data, c));
            }
            table.getItems().setAll(data.rows());
            countLabel.setText(data.rows().size() == 1 ? "1 registro" : data.rows().size() + " registros");
            countLabel.getStyleClass().remove("nf-result-bad");
        } catch (NextFitException e) {
            countLabel.setText(e.getMessage());
            if (!countLabel.getStyleClass().contains("nf-result-bad")) {
                countLabel.getStyleClass().add("nf-result-bad");
            }
        }
    }

    private static TableColumn<List<Object>, String> column(ReportTable data, int index) {
        ColumnType type = data.types().get(index);
        String header = data.headers().get(index);
        double width = Math.max(110, header.length() * 10 + 60);
        TableColumn<List<Object>, String> column = FxTables.column(header, width, row -> format(row.get(index), type));
        // Without a minimum, the table may squeeze the columns to nothing and hide their title and values.
        column.setMinWidth(Math.min(width, 120));
        if (type != ColumnType.TEXT) {
            column.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
        return column;
    }

    private static String format(Object value, ColumnType type) {
        if (value instanceof Long number && type == ColumnType.MONEY) {
            return Formats.money(number);
        }
        return value == null ? "" : value.toString();
    }

    private String fileName(String extension) {
        String type = typeBox.getValue().toLowerCase();
        if (MEMBERS.equals(typeBox.getValue()) || fromPicker.getValue() == null || toPicker.getValue() == null) {
            return "reporte-" + type + "." + extension;
        }
        return "reporte-" + type + "-" + FILE_DAY.format(fromPicker.getValue()) + "_"
                + FILE_DAY.format(toPicker.getValue()) + "." + extension;
    }

    private String subtitle() {
        if (MEMBERS.equals(typeBox.getValue())) {
            return "Todos los socios";
        }
        return "Del " + PICKER_FORMAT.format(fromPicker.getValue()) + " al " + PICKER_FORMAT.format(toPicker.getValue());
    }

    private void exportPdf() {
        ReportTable data;
        try {
            data = currentTable();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
            return;
        }
        String subtitle = subtitle();
        FileExport.save(this, "Guardar el reporte", fileName("pdf"), "Documento PDF", "pdf",
                path -> PdfExporter.table(path, data, subtitle, LocalDateTime.now()));
    }

    private void exportExcel() {
        ReportTable data;
        try {
            data = currentTable();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
            return;
        }
        FileExport.save(this, "Guardar el reporte", fileName("xlsx"), "Libro de Excel", "xlsx",
                path -> XlsxWriter.write(path, List.of(data)));
    }

    /** One Excel file with the three reports, each in its own sheet. */
    private void exportAll() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            FxDialogs.error(this, new InvalidDataException("Fechas", "elige un rango válido para los pagos y la asistencia"));
            return;
        }
        FileExport.save(this, "Guardar todos los reportes",
                "reportes-" + FILE_DAY.format(from) + "_" + FILE_DAY.format(to) + ".xlsx", "Libro de Excel", "xlsx",
                path -> XlsxWriter.write(path, List.of(reports.payments(from, to), reports.attendance(from, to),
                        reports.members())));
    }
}

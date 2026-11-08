package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.PaymentMethod;
import com.nextfit.export.PdfExporter;
import com.nextfit.export.XlsxWriter;
import com.nextfit.service.CashClose;
import com.nextfit.service.CashCloseService;
import com.nextfit.service.ReportService;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cash close of one day: totals, the money of each payment method, the cash count of the drawer and the
 * payments. It can be exported as PDF and Excel. The numbers come from {@link CashCloseService}; the cash
 * counted by the user lives only on this screen and travels to the exported file.
 */
final class CashClosePane extends VBox {

    private static final DateTimeFormatter PICKER_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",
            Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CashCloseService service;
    private final ReportService reports;
    private final DatePicker picker = new DatePicker(LocalDate.now());
    private final Label dayLabel = new Label();
    private final KpiCard totalCard = new KpiCard("Total recaudado", "$ 0", "", FxIcons.BILLING, KpiCard.Kind.FEATURE);
    private final KpiCard countCard = new KpiCard("Pagos", "0", "", FxIcons.MEMBERS, KpiCard.Kind.VIOLET);
    private final KpiCard discountCard = new KpiCard("Descuentos", "$ 0", "", FxIcons.ANALYTICS, KpiCard.Kind.LIME);
    private final KpiCard cashCard = new KpiCard("Efectivo esperado", "$ 0", "Lo que debe haber en caja",
            FxIcons.BILLING, KpiCard.Kind.VIOLET);
    private final List<MethodRow> methodRows = new ArrayList<>();
    private final Label expectedLabel = new Label();
    private final TextField countedField = new TextField();
    private final Label resultLabel = new Label();
    private final TableView<CashClose.Line> table = new TableView<>();
    private CashClose current;
    private Long counted;

    /**
     * @param service the service that builds the close of a day
     * @param reports the service that builds the sheets of the Excel file
     */
    CashClosePane(CashCloseService service, ReportService reports) {
        super(18);
        this.service = service;
        this.reports = reports;
        configurePicker();
        getChildren().addAll(buildHeader(), buildKpis(), buildMiddle(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        setPadding(new Insets(0, 4, 24, 0));
        refresh();
    }

    private void configurePicker() {
        picker.setPrefHeight(44);
        picker.setEditable(false);
        picker.setConverter(new StringConverter<>() {
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
        });
        picker.valueProperty().addListener((obs, old, now) -> {
            countedField.clear();
            refresh();
        });
    }

    private Node buildHeader() {
        Label title = new Label("Cierre de caja");
        title.getStyleClass().add("nf-section-title");
        dayLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, dayLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button today = new Button("Hoy");
        today.getStyleClass().add("nf-secondary");
        today.setOnAction(e -> picker.setValue(LocalDate.now()));
        Button excel = new Button("Excel");
        excel.getStyleClass().add("nf-secondary");
        excel.setOnAction(e -> exportExcel());
        Button pdf = new Button("Exportar PDF");
        pdf.getStyleClass().add("nf-primary-small");
        pdf.setOnAction(e -> exportPdf());

        HBox header = new HBox(10, titles, picker, today, excel, pdf);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildKpis() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        double[] percents = {31, 23, 23, 23};
        Region[] cards = {totalCard, countCard, discountCard, cashCard};
        for (int i = 0; i < cards.length; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percents[i]);
            grid.getColumnConstraints().add(column);
            grid.add(cards[i], i, 0);
            cards[i].setMaxWidth(Double.MAX_VALUE);
        }
        return grid;
    }

    private Node buildMiddle() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(56);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(44);
        grid.getColumnConstraints().addAll(left, right);
        Region methods = methodsCard();
        Region count = countCard();
        methods.setMaxWidth(Double.MAX_VALUE);
        count.setMaxWidth(Double.MAX_VALUE);
        grid.add(methods, 0, 0);
        grid.add(count, 1, 0);
        GridPane.setFillHeight(methods, true);
        GridPane.setFillHeight(count, true);
        return grid;
    }

    private Region methodsCard() {
        Label title = new Label("Por método de pago");
        title.getStyleClass().add("nf-card-title");
        VBox box = new VBox(16, title);
        int[] dots = {0, 2, 1};
        PaymentMethod[] methods = PaymentMethod.values();
        for (int i = 0; i < methods.length; i++) {
            MethodRow row = new MethodRow(methods[i], dots[i % dots.length]);
            methodRows.add(row);
            box.getChildren().add(row);
        }
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(22));
        return box;
    }

    private Region countCard() {
        Label title = new Label("Conteo de efectivo");
        title.getStyleClass().add("nf-card-title");
        Label expectedCaption = new Label("Esperado en caja");
        expectedCaption.getStyleClass().add("nf-muted");
        expectedLabel.getStyleClass().add("nf-kpi-value-small");
        countedField.setPromptText("Efectivo contado, por ejemplo 150.000");
        countedField.getStyleClass().add("nf-input");
        countedField.setPrefHeight(44);
        countedField.textProperty().addListener((obs, old, now) -> readCounted());
        resultLabel.getStyleClass().add("nf-muted");
        resultLabel.setWrapText(true);
        VBox box = new VBox(10, title, new VBox(2, expectedCaption, expectedLabel),
                FxDialogs.labeled("Efectivo contado", countedField), resultLabel);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(22));
        return box;
    }

    private Node buildTable() {
        TableColumn<CashClose.Line, String> receipt = FxTables.column("Recibo", 120, CashClose.Line::receipt);
        TableColumn<CashClose.Line, String> time = FxTables.column("Hora", 80,
                l -> l.paidAt().toLocalTime().toString());
        TableColumn<CashClose.Line, String> member = FxTables.column("Socio", 220, CashClose.Line::member);
        TableColumn<CashClose.Line, String> plan = FxTables.column("Plan", 170, CashClose.Line::plan);
        TableColumn<CashClose.Line, String> method = FxTables.column("Método", 120, l -> l.method().getLabel());
        TableColumn<CashClose.Line, String> discount = FxTables.column("Descuento", 120,
                l -> l.discount() == 0 ? "—" : Formats.money(l.discount()));
        TableColumn<CashClose.Line, String> total = FxTables.column("Total", 130, l -> Formats.money(l.total()));
        TableColumn<CashClose.Line, String> cashier = FxTables.column("Atendido por", 130,
                l -> l.cashier() == null ? "" : l.cashier());
        discount.setStyle("-fx-alignment: CENTER-RIGHT;");
        total.setStyle("-fx-alignment: CENTER-RIGHT;");
        table.getColumns().addAll(List.of(receipt, time, member, plan, method, discount, total, cashier));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.setPlaceholder(new Label("No hubo pagos este día"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(260);
        Label title = new Label("Detalle de pagos");
        title.getStyleClass().add("nf-card-title");
        VBox box = new VBox(10, title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    /** Loads the close of the chosen day again. */
    void refresh() {
        LocalDate day = picker.getValue() == null ? LocalDate.now() : picker.getValue();
        current = service.closeOf(day);
        dayLabel.setText(capital(LONG_DAY.format(day)));
        totalCard.value().setText(Formats.money(current.total()));
        totalCard.hint().setText(current.count() == 0 ? "Sin pagos este día" : "Todos los métodos");
        countCard.value().setText(String.valueOf(current.count()));
        countCard.hint().setText(current.count() == 1 ? "Pago registrado" : "Pagos registrados");
        discountCard.value().setText(Formats.money(current.discounts()));
        discountCard.hint().setText("Descontado en las ventas");
        cashCard.value().setText(Formats.money(current.expectedCash()));
        expectedLabel.setText(Formats.money(current.expectedCash()));
        long max = Math.max(1, current.total());
        for (MethodRow row : methodRows) {
            long amount = current.byMethod().getOrDefault(row.method, 0L);
            long count = current.lines().stream().filter(l -> l.method() == row.method).count();
            row.update(amount, count, (double) amount / max);
        }
        table.getItems().setAll(current.lines());
        readCounted();
    }

    private void readCounted() {
        String text = countedField.getText();
        counted = null;
        setResult("Cuenta el efectivo de la caja y escríbelo para ver si cuadra.", "nf-muted");
        if (current == null || text == null || text.isBlank()) {
            return;
        }
        try {
            long value = Formats.parseMoney("Efectivo contado", text);
            if (value < 0) {
                setResult("El efectivo contado no puede ser negativo.", "nf-result-bad");
                return;
            }
            counted = value;
            long difference = current.withCountedCash(value).difference();
            if (difference == 0) {
                setResult("Caja cuadrada: el efectivo coincide con lo esperado.", "nf-result-ok");
            } else if (difference > 0) {
                setResult("Sobrante de " + Formats.money(difference) + ".", "nf-result-warn");
            } else {
                setResult("Faltante de " + Formats.money(-difference) + ".", "nf-result-bad");
            }
        } catch (NextFitException e) {
            setResult("Escribe solo números, por ejemplo 150000.", "nf-result-bad");
        }
    }

    private void setResult(String text, String style) {
        resultLabel.setText(text);
        resultLabel.getStyleClass().removeAll("nf-muted", "nf-result-ok", "nf-result-warn", "nf-result-bad");
        resultLabel.getStyleClass().add(style);
    }

    private CashClose closeToExport() {
        return counted == null ? current : current.withCountedCash(counted);
    }

    private void exportPdf() {
        CashClose close = closeToExport();
        FileExport.save(this, "Guardar el cierre de caja", "cierre-de-caja-" + FILE_DAY.format(close.day()) + ".pdf",
                "Documento PDF", "pdf", path -> PdfExporter.cashClose(path, close, LocalDateTime.now()));
    }

    private void exportExcel() {
        CashClose close = closeToExport();
        FileExport.save(this, "Guardar el cierre de caja", "cierre-de-caja-" + FILE_DAY.format(close.day()) + ".xlsx",
                "Libro de Excel", "xlsx", path -> XlsxWriter.write(path, reports.cashCloseSheets(close)));
    }

    private static String capital(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** One payment method: a colored dot, its name, how many payments, the amount and a bar with its share. */
    private static final class MethodRow extends VBox {
        private final PaymentMethod method;
        private final Label count = new Label();
        private final Label amount = new Label();
        private final Region fill = new Region();
        private final StackPane track = new StackPane(fill);
        private double share;

        MethodRow(PaymentMethod method, int dotStyle) {
            super(8);
            this.method = method;
            Region dot = new Region();
            dot.getStyleClass().add("nf-dot-" + dotStyle);
            dot.setMinSize(10, 10);
            dot.setMaxSize(10, 10);
            Label name = new Label(method.getLabel());
            name.getStyleClass().add("nf-legend-name");
            count.getStyleClass().add("nf-muted-small");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            amount.getStyleClass().add("nf-legend-value");
            HBox line = new HBox(10, dot, name, count, spacer, amount);
            line.setAlignment(Pos.CENTER_LEFT);

            fill.getStyleClass().addAll("nf-dot-" + dotStyle, "nf-bar-fill");
            fill.setMinHeight(8);
            fill.setPrefHeight(8);
            fill.setMaxHeight(8);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getStyleClass().add("nf-bar-track");
            track.setMinHeight(8);
            track.setPrefHeight(8);
            track.setMaxHeight(8);
            track.widthProperty().addListener((obs, old, now) -> resize());
            getChildren().addAll(line, track);
        }

        void update(long pesos, long payments, double fraction) {
            amount.setText(Formats.money(pesos));
            count.setText(payments == 1 ? "· 1 pago" : "· " + payments + " pagos");
            share = Math.max(0, Math.min(1, fraction));
            resize();
        }

        private void resize() {
            double width = track.getWidth() * share;
            fill.setMinWidth(share == 0 ? 0 : Math.max(8, width));
            fill.setPrefWidth(share == 0 ? 0 : Math.max(8, width));
            fill.setMaxWidth(share == 0 ? 0 : Math.max(8, width));
        }
    }
}

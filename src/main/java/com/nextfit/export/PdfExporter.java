package com.nextfit.export;

import com.nextfit.model.PaymentMethod;
import com.nextfit.service.CashClose;
import com.nextfit.service.ColumnType;
import com.nextfit.service.ReportTable;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws reports as PDF files with the NextFit colors: a violet header band with a lime mark, zebra tables
 * that repeat their header on every page, and a footer with the page number. It only draws; the data comes
 * from {@link ReportTable} and {@link CashClose}.
 */
public final class PdfExporter {

    private static final int VIOLET = 0x5A22B8;
    private static final int LIME = 0xA7F626;
    private static final int INK = 0x1A1330;
    private static final int MUTED = 0x5B5575;
    private static final int LINE = 0xDDD5F0;
    private static final int ZEBRA = 0xF5F1FD;
    private static final int WHITE = 0xFFFFFF;
    private static final int GOOD = 0x2C7A0B;
    private static final int BAD = 0xC62828;
    private static final double MARGIN = 40;
    private static final double BOTTOM = 52;
    private static final double ROW = 21;
    private static final double TEXT = 9;
    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfExporter() {
    }

    /**
     * Writes one table as a PDF. The page is turned sideways when the table has many columns.
     *
     * @param file        where to write it
     * @param table       the table
     * @param subtitle    a line shown in the header, for example the period; may be empty
     * @param generatedAt the moment printed in the footer
     */
    public static void table(Path file, ReportTable table, String subtitle, LocalDateTime generatedAt) {
        Flow flow = new Flow(new PdfDocument(table.title()), table.headers().size() > 6, table.title());
        flow.firstHeader(table.title(), subtitle);
        drawTable(flow, table, MARGIN, flow.contentWidth());
        flow.finish(generatedAt);
        flow.doc.save(file);
    }

    /**
     * Writes the cash close of a day as a PDF: totals, payment methods, cash count, payments and signatures.
     *
     * @param file        where to write it
     * @param close       the close
     * @param generatedAt the moment printed in the footer
     */
    public static void cashClose(Path file, CashClose close, LocalDateTime generatedAt) {
        Flow flow = new Flow(new PdfDocument("Cierre de caja"), false, "Cierre de caja");
        flow.firstHeader("Cierre de caja", capital(LONG_DAY.format(close.day())));
        drawKpis(flow, close);
        flow.gap(20);
        flow.sectionTitle("Por método de pago");
        drawTable(flow, byMethod(close), MARGIN, flow.contentWidth() * 0.62);
        flow.gap(18);
        drawCashCount(flow, close);
        flow.gap(22);
        flow.sectionTitle("Detalle de pagos");
        drawTable(flow, detail(close), MARGIN, flow.contentWidth());
        drawSignatures(flow);
        flow.finish(generatedAt);
        flow.doc.save(file);
    }

    // ---------------------------------------------------------------- cash close pieces

    private static ReportTable byMethod(CashClose close) {
        List<List<Object>> rows = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            long count = close.lines().stream().filter(l -> l.method() == method).count();
            rows.add(List.of(method.getLabel(), count, close.byMethod().getOrDefault(method, 0L)));
        }
        return new ReportTable("Por método de pago", List.of("Método", "Pagos", "Total"),
                List.of(ColumnType.TEXT, ColumnType.NUMBER, ColumnType.MONEY), rows);
    }

    private static ReportTable detail(CashClose close) {
        List<List<Object>> rows = new ArrayList<>();
        for (CashClose.Line line : close.lines()) {
            rows.add(List.of(line.receipt(), line.paidAt().toLocalTime().toString(), line.member(), line.plan(),
                    line.method().getLabel(), line.total()));
        }
        return new ReportTable("Detalle de pagos", List.of("Recibo", "Hora", "Socio", "Plan", "Método", "Total"),
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT,
                        ColumnType.MONEY), rows);
    }

    private static void drawKpis(Flow flow, CashClose close) {
        String[] labels = {"Total recaudado", "Pagos", "Descuentos", "Efectivo esperado"};
        String[] values = {peso(close.total()), String.valueOf(close.count()), peso(close.discounts()),
                peso(close.expectedCash())};
        double gap = 10;
        double width = (flow.contentWidth() - gap * 3) / 4;
        double height = 62;
        flow.ensure(height);
        PdfDocument.Page page = flow.page;
        for (int i = 0; i < 4; i++) {
            double x = MARGIN + i * (width + gap);
            boolean feature = i == 0;
            page.fillRoundRect(x, flow.y, width, height, 10, feature ? VIOLET : ZEBRA);
            if (!feature) {
                page.strokeRoundRect(x, flow.y, width, height, 10, LINE, 0.8);
            }
            page.text(x + 12, flow.y + 20, labels[i], 8.5, false, feature ? 0xE4DDF5 : MUTED);
            double size = 17;
            while (size > 10 && PdfFonts.width(values[i], size, true) > width - 24) {
                size -= 1;
            }
            page.text(x + 12, flow.y + 46, values[i], size, true, feature ? LIME : INK);
        }
        flow.y += height;
    }

    private static void drawCashCount(Flow flow, CashClose close) {
        double height = 62;
        flow.ensure(height);
        PdfDocument.Page page = flow.page;
        page.fillRoundRect(MARGIN, flow.y, flow.contentWidth(), height, 10, ZEBRA);
        page.strokeRoundRect(MARGIN, flow.y, flow.contentWidth(), height, 10, LINE, 0.8);
        double column = flow.contentWidth() / 3;
        String counted = close.isCounted() ? peso(close.countedCash()) : "Sin contar";
        String verdict;
        int color;
        if (!close.isCounted()) {
            verdict = "Pendiente";
            color = MUTED;
        } else if (close.difference() == 0) {
            verdict = "Caja cuadrada";
            color = GOOD;
        } else if (close.difference() > 0) {
            verdict = "Sobrante " + peso(close.difference());
            color = GOOD;
        } else {
            verdict = "Faltante " + peso(-close.difference());
            color = BAD;
        }
        String[] labels = {"Efectivo esperado", "Efectivo contado", "Resultado"};
        String[] values = {peso(close.expectedCash()), counted, verdict};
        int[] colors = {INK, INK, color};
        for (int i = 0; i < 3; i++) {
            double x = MARGIN + 14 + i * column;
            page.text(x, flow.y + 22, labels[i], 8.5, false, MUTED);
            page.text(x, flow.y + 44, PdfFonts.fit(values[i], 13, true, column - 20), 13, true, colors[i]);
        }
        flow.y += height;
    }

    private static void drawSignatures(Flow flow) {
        flow.gap(46);
        flow.ensure(46);
        PdfDocument.Page page = flow.page;
        double width = 190;
        double second = flow.page.width() - MARGIN - width;
        page.hLine(MARGIN, MARGIN + width, flow.y, MUTED, 0.8);
        page.hLine(second, second + width, flow.y, MUTED, 0.8);
        page.text(MARGIN, flow.y + 14, "Cajero", 9, false, MUTED);
        page.text(second, flow.y + 14, "Administrador", 9, false, MUTED);
        flow.y += 20;
    }

    // ---------------------------------------------------------------- table

    private static void drawTable(Flow flow, ReportTable table, double x, double width) {
        double[] widths = columnWidths(table, width);
        List<Object> totals = totalsRow(table);
        drawHeaderRow(flow, table, widths, x, width);
        if (table.rows().isEmpty()) {
            flow.ensure(ROW * 2);
            flow.page.text(x + 8, flow.y + 22, "Sin registros en este periodo", TEXT + 1, false, MUTED);
            flow.y += ROW * 2;
            return;
        }
        int index = 0;
        for (List<Object> row : table.rows()) {
            if (flow.needs(ROW)) {
                flow.newPage();
                drawHeaderRow(flow, table, widths, x, width);
            }
            if (index % 2 == 1) {
                flow.page.fillRect(x, flow.y, width, ROW, ZEBRA);
            }
            drawCells(flow, table, widths, x, row, false);
            flow.y += ROW;
            index++;
        }
        if (totals != null) {
            flow.ensure(ROW + 2);
            flow.page.hLine(x, x + width, flow.y, VIOLET, 1.2);
            drawCells(flow, table, widths, x, totals, true);
            flow.y += ROW;
        }
        flow.page.hLine(x, x + width, flow.y, LINE, 0.8);
    }

    private static void drawHeaderRow(Flow flow, ReportTable table, double[] widths, double x, double width) {
        flow.ensure(ROW * 3);
        flow.page.fillRoundRect(x, flow.y, width, ROW + 2, 5, VIOLET);
        double cursor = x;
        for (int i = 0; i < widths.length; i++) {
            String label = PdfFonts.fit(table.headers().get(i), TEXT, true, widths[i] - 12);
            if (table.types().get(i) == ColumnType.TEXT) {
                flow.page.text(cursor + 8, flow.y + 15, label, TEXT, true, WHITE);
            } else {
                flow.page.textRight(cursor + widths[i] - 8, flow.y + 15, label, TEXT, true, WHITE);
            }
            cursor += widths[i];
        }
        flow.y += ROW + 2;
    }

    private static void drawCells(Flow flow, ReportTable table, double[] widths, double x, List<Object> row, boolean bold) {
        double cursor = x;
        for (int i = 0; i < widths.length; i++) {
            String value = format(row.get(i), table.types().get(i));
            String shown = PdfFonts.fit(value, TEXT, bold, widths[i] - 12);
            if (table.types().get(i) == ColumnType.TEXT) {
                flow.page.text(cursor + 8, flow.y + 14, shown, TEXT, bold, INK);
            } else {
                flow.page.textRight(cursor + widths[i] - 8, flow.y + 14, shown, TEXT, bold, INK);
            }
            cursor += widths[i];
        }
    }

    /** The sum of the money columns, or null when the table has none or has no rows. */
    private static List<Object> totalsRow(ReportTable table) {
        if (table.rows().isEmpty() || !table.types().contains(ColumnType.MONEY)) {
            return null;
        }
        List<Object> totals = new ArrayList<>();
        for (int c = 0; c < table.headers().size(); c++) {
            if (table.types().get(c) == ColumnType.MONEY) {
                long sum = 0;
                for (List<Object> row : table.rows()) {
                    sum += (Long) row.get(c);
                }
                totals.add(sum);
            } else {
                totals.add(c == 0 ? "Total" : "");
            }
        }
        return totals;
    }

    /** Gives each column the width of its content; when the page is narrower, the widest columns shrink first. */
    static double[] columnWidths(ReportTable table, double available) {
        int n = table.headers().size();
        double[] natural = new double[n];
        for (int c = 0; c < n; c++) {
            double max = PdfFonts.width(table.headers().get(c), TEXT, true);
            for (List<Object> row : table.rows()) {
                max = Math.max(max, PdfFonts.width(format(row.get(c), table.types().get(c)), TEXT, false));
            }
            natural[c] = max + 20;
        }
        double sum = 0;
        for (double w : natural) {
            sum += w;
        }
        double[] widths = natural.clone();
        if (sum <= available) {
            double extra = (available - sum) / n;
            for (int c = 0; c < n; c++) {
                widths[c] += extra;
            }
            return widths;
        }
        boolean[] fixed = new boolean[n];
        double remaining = available;
        int free = n;
        boolean changed = true;
        while (changed && free > 0) {
            changed = false;
            double share = remaining / free;
            for (int c = 0; c < n; c++) {
                if (!fixed[c] && natural[c] <= share) {
                    fixed[c] = true;
                    remaining -= natural[c];
                    free--;
                    changed = true;
                }
            }
        }
        double share = free == 0 ? 0 : remaining / free;
        for (int c = 0; c < n; c++) {
            widths[c] = fixed[c] ? natural[c] : share;
        }
        return widths;
    }

    private static String format(Object value, ColumnType type) {
        if (value instanceof Long number) {
            return type == ColumnType.MONEY ? peso(number) : grouped(number);
        }
        return value == null ? "" : value.toString();
    }

    private static String grouped(long number) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(SPANISH));
        return format.format(number);
    }

    /** Formats pesos as {@code $ 1.234.000}. */
    static String peso(long amount) {
        return (amount < 0 ? "-$ " : "$ ") + grouped(Math.abs(amount));
    }

    private static String capital(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---------------------------------------------------------------- page flow

    /** The cursor that moves down the pages, adds a page when one is full and draws headers and footers. */
    private static final class Flow {
        private final PdfDocument doc;
        private final boolean landscape;
        private final String running;
        private PdfDocument.Page page;
        private double y;

        Flow(PdfDocument doc, boolean landscape, String running) {
            this.doc = doc;
            this.landscape = landscape;
            this.running = running;
            this.page = doc.addPage(landscape);
        }

        double contentWidth() {
            return page.width() - MARGIN * 2;
        }

        boolean needs(double height) {
            return y + height > page.height() - BOTTOM;
        }

        void ensure(double height) {
            if (needs(height)) {
                newPage();
            }
        }

        void gap(double height) {
            y += height;
        }

        void newPage() {
            page = doc.addPage(landscape);
            page.fillRect(0, 0, page.width(), 30, VIOLET);
            page.fillRect(MARGIN, 9, 4, 12, LIME);
            page.text(MARGIN + 11, 19.5, "NextFit  ·  " + running, 9, true, WHITE);
            y = 30 + 22;
        }

        void firstHeader(String title, String subtitle) {
            page.fillRect(0, 0, page.width(), 82, VIOLET);
            page.fillRect(MARGIN, 20, 5, 44, LIME);
            page.text(MARGIN + 14, 32, "NEXTFIT", 9.5, true, LIME);
            page.text(MARGIN + 14, 58, PdfFonts.fit(title, 24, true, contentWidth() * 0.62), 24, true, WHITE);
            if (!subtitle.isEmpty()) {
                page.textRight(page.width() - MARGIN, 58, PdfFonts.fit(subtitle, 10.5, false, contentWidth() * 0.4),
                        10.5, false, 0xE4DDF5);
            }
            y = 82 + 24;
        }

        void sectionTitle(String text) {
            ensure(ROW * 4);
            page.text(MARGIN, y + 11, text, 12, true, INK);
            y += 20;
        }

        void finish(LocalDateTime generatedAt) {
            List<PdfDocument.Page> all = doc.pages();
            for (int i = 0; i < all.size(); i++) {
                PdfDocument.Page p = all.get(i);
                p.hLine(MARGIN, p.width() - MARGIN, p.height() - 34, LINE, 0.8);
                p.text(MARGIN, p.height() - 20, "NextFit  ·  Generado el " + STAMP.format(generatedAt), 8, false, MUTED);
                p.textRight(p.width() - MARGIN, p.height() - 20, "Página " + (i + 1) + " de " + all.size(), 8, false, MUTED);
            }
        }
    }
}

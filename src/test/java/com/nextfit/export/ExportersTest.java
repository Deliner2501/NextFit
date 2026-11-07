package com.nextfit.export;

import com.nextfit.model.PaymentMethod;
import com.nextfit.service.CashClose;
import com.nextfit.service.ColumnType;
import com.nextfit.service.ReportTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportersTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 21, 0);

    private static ReportTable table(int rows) {
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            data.add(List.of("R-" + i, "Ñandú & <Cía> " + i, (long) (50_000 + i)));
        }
        return new ReportTable("Pagos de prueba", List.of("Recibo", "Socio", "Total"),
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.MONEY), data);
    }

    private static CashClose close(int payments) {
        List<CashClose.Line> lines = new ArrayList<>();
        Map<PaymentMethod, Long> by = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            by.put(m, 0L);
        }
        long total = 0;
        for (int i = 0; i < payments; i++) {
            lines.add(new CashClose.Line("R-" + i, LocalDateTime.of(2026, 11, 6, 9, i % 60), "Socio " + i, "Mensual",
                    PaymentMethod.CASH, 80_000, 0, 80_000, "admin"));
            total += 80_000;
        }
        by.put(PaymentMethod.CASH, total);
        return new CashClose(LocalDate.of(2026, 11, 6), lines, by, total, 0, -1);
    }

    private static int pages(byte[] pdf) {
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        int count = 0;
        for (int i = text.indexOf("/Type /Page /"); i >= 0; i = text.indexOf("/Type /Page /", i + 1)) {
            count++;
        }
        return count;
    }

    @Test
    void pdfIsAValidFileWithOnePageForAShortTable() throws IOException {
        Path file = Files.createTempFile("table", ".pdf");
        PdfExporter.table(file, table(5), "Periodo", NOW);
        byte[] bytes = Files.readAllBytes(file);
        Files.delete(file);

        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("%PDF-1.4"));
        assertTrue(text.trim().endsWith("%%EOF"));
        assertEquals(1, pages(bytes));
    }

    @Test
    void aLongTableFlowsToMorePagesAndKeepsAllRows() throws IOException {
        Path small = Files.createTempFile("small", ".pdf");
        Path big = Files.createTempFile("big", ".pdf");
        PdfExporter.table(small, table(5), "", NOW);
        PdfExporter.table(big, table(200), "", NOW);
        int smallPages = pages(Files.readAllBytes(small));
        int bigPages = pages(Files.readAllBytes(big));
        Files.delete(small);
        Files.delete(big);

        assertTrue(bigPages > smallPages);
        assertTrue(bigPages >= 4);
    }

    @Test
    void cashClosePdfIsWrittenForAnEmptyAndAFullDay() throws IOException {
        Path empty = Files.createTempFile("empty", ".pdf");
        Path full = Files.createTempFile("full", ".pdf");
        PdfExporter.cashClose(empty, close(0), NOW);
        PdfExporter.cashClose(full, close(60).withCountedCash(4_000_000), NOW);
        byte[] emptyBytes = Files.readAllBytes(empty);
        byte[] fullBytes = Files.readAllBytes(full);
        Files.delete(empty);
        Files.delete(full);

        assertEquals(1, pages(emptyBytes));
        assertTrue(pages(fullBytes) >= 2);
    }

    @Test
    void pdfFailsClearlyWhenTheFolderDoesNotExist() {
        assertThrows(com.nextfit.exceptions.PersistenceException.class,
                () -> PdfExporter.table(Path.of("no-such-folder", "x.pdf"), table(1), "", NOW));
    }

    @Test
    void columnWidthsFitThePageAndNeverGoNegative() {
        double[] widths = PdfExporter.columnWidths(table(30), 300);

        double sum = 0;
        for (double w : widths) {
            assertTrue(w > 0);
            sum += w;
        }
        assertEquals(300.0, sum, 0.001);
    }

    @Test
    void pesoUsesDotsAsThousandsSeparator() {
        assertEquals("$ 1.234.000", PdfExporter.peso(1_234_000));
        assertEquals("-$ 5.000", PdfExporter.peso(-5_000));
        assertEquals("$ 0", PdfExporter.peso(0));
    }

    @Test
    void pdfWidthsKnowAccentsAndTruncateWithAnEllipsis() {
        assertEquals(PdfFonts.width("e", 10, false), PdfFonts.width("é", 10, false), 0.001);
        assertTrue(PdfFonts.width("Hola", 10, true) > PdfFonts.width("Hola", 10, false));
        String cut = PdfFonts.fit("Un texto demasiado largo para caber", 9, false, 60);
        assertTrue(cut.endsWith("…"));
        assertTrue(PdfFonts.width(cut, 9, false) <= 60);
        assertEquals("corto", PdfFonts.fit("corto", 9, false, 200));
    }

    @Test
    void xlsxHasTheSheetWithEscapedTextAndNumbers() throws IOException {
        Path file = Files.createTempFile("report", ".xlsx");
        XlsxWriter.write(file, List.of(table(3)));
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            assertTrue(sheet != null);
            String xml = new String(zip.getInputStream(sheet).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("Ñandú &amp; &lt;Cía&gt; 0"));
            assertTrue(xml.contains("<v>50000</v>"));
            assertTrue(xml.contains("NEXTFIT"));
            assertTrue(xml.contains("Generado el "));
            assertTrue(xml.contains("<mergeCell ref=\"A1:"));
            assertTrue(zip.getEntry("xl/styles.xml") != null);
            assertTrue(zip.getEntry("[Content_Types].xml") != null);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void xlsxNeedsAtLeastOneTable() {
        assertThrows(IllegalArgumentException.class, () -> XlsxWriter.write(Path.of("x.xlsx"), List.of()));
    }
}

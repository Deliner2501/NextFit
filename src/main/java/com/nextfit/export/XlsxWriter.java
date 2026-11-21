package com.nextfit.export;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.service.ColumnType;
import com.nextfit.service.ReportTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes Excel files (.xlsx) with the JDK only. An .xlsx is a zip of small XML
 * files, so no library is needed. Each {@link ReportTable} becomes a sheet with
 * the NextFit look of the PDF reports: a violet banner with the lime brand name,
 * the report title, a lime rule, the date it was generated, a violet header row
 * (frozen and filterable), zebra rows, money shown as pesos, columns sized to
 * their content and landscape printing that repeats the header on every page.
 */
public final class XlsxWriter {

    private static final int MAX_COLUMN_WIDTH = 50;
    private static final int MIN_COLUMN_WIDTH = 10;
    /** Rows above the data: brand, title, lime rule, generated stamp, column headers. */
    private static final int HEADER_ROWS = 5;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Style indexes in styles().
    private static final int S_HEADER = 1;
    private static final int S_HEADER_NUM = 2;
    private static final int S_TEXT = 3;
    private static final int S_MONEY = 5;
    private static final int S_NUMBER = 7;
    private static final int S_BRAND = 9;
    private static final int S_TITLE = 10;
    private static final int S_RULE = 11;
    private static final int S_STAMP = 12;

    private XlsxWriter() {
    }

    /**
     * Writes a workbook with one sheet per table, stamped with the current time.
     *
     * @param file   where to write it
     * @param tables the tables, at least one
     * @throws PersistenceException when the file cannot be written
     */
    public static void write(Path file, List<ReportTable> tables) {
        write(file, tables, LocalDateTime.now());
    }

    /**
     * Writes a workbook with one sheet per table.
     *
     * @param file        where to write it
     * @param tables      the tables, at least one
     * @param generatedAt the moment printed under the title of every sheet
     * @throws PersistenceException when the file cannot be written
     */
    public static void write(Path file, List<ReportTable> tables, LocalDateTime generatedAt) {
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos una tabla");
        }
        List<String> names = sheetNames(tables);
        byte[] logo = logo();
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            entry(zip, "[Content_Types].xml", contentTypes(tables.size(), logo != null));
            entry(zip, "_rels/.rels", rootRels());
            entry(zip, "docProps/core.xml", core(tables.get(0).title(), generatedAt));
            entry(zip, "xl/workbook.xml", workbook(names));
            entry(zip, "xl/_rels/workbook.xml.rels", workbookRels(tables.size()));
            entry(zip, "xl/styles.xml", styles());
            if (logo != null) {
                zip.putNextEntry(new ZipEntry("xl/media/logo.png"));
                zip.write(logo);
                zip.closeEntry();
            }
            for (int i = 0; i < tables.size(); i++) {
                entry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(tables.get(i), generatedAt, logo != null));
                if (logo != null) {
                    int n = i + 1;
                    entry(zip, "xl/worksheets/_rels/sheet" + n + ".xml.rels", relationships("rId1",
                            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing",
                            "../drawings/drawing" + n + ".xml"));
                    entry(zip, "xl/drawings/drawing" + n + ".xml", drawing(tables.get(i)));
                    entry(zip, "xl/drawings/_rels/drawing" + n + ".xml.rels", relationships("rId1",
                            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image",
                            "../media/logo.png"));
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar el archivo de Excel", e);
        }
    }

    /** The NextFit logo for the banner, or {@code null} when the picture is not available (the sheet is then plain). */
    private static byte[] logo() {
        try (InputStream in = XlsxWriter.class.getResourceAsStream("/images/logo/nextfit-logo-128.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static String relationships(String id, String type, String target) {
        return header() + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"" + id + "\" Type=\"" + type + "\" Target=\"" + target + "\"/></Relationships>";
    }

    /** Places the logo at the right end of the banner, so it never covers the brand name or the title. */
    private static String drawing(ReportTable table) {
        int last = table.headers().size() - 1;
        long size = 54L * 9525;
        long columnPixels = Math.round(width(table, last) * 7);
        long offset = Math.max(0, columnPixels - 54 - 12) * 9525;
        return header() + "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" "
                + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                + "<xdr:oneCellAnchor><xdr:from><xdr:col>" + last + "</xdr:col><xdr:colOff>" + offset + "</xdr:colOff>"
                + "<xdr:row>0</xdr:row><xdr:rowOff>142875</xdr:rowOff></xdr:from><xdr:ext cx=\"" + size + "\" cy=\"" + size + "\"/>"
                + "<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"2\" name=\"NextFit\" descr=\"Logo de NextFit\"/>"
                + "<xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr>"
                + "<xdr:blipFill><a:blip xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" r:embed=\"rId1\"/>"
                + "<a:stretch><a:fillRect/></a:stretch></xdr:blipFill>"
                + "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + size + "\" cy=\"" + size + "\"/></a:xfrm>"
                + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor></xdr:wsDr>";
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String header() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    }

    private static String contentTypes(int sheets, boolean logo) {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        if (logo) {
            sb.append("<Default Extension=\"png\" ContentType=\"image/png\"/>");
        }
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        sb.append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>");
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= sheets; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            if (logo) {
                sb.append("<Override PartName=\"/xl/drawings/drawing").append(i)
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");
            }
        }
        return sb.append("</Types>").toString();
    }

    private static String rootRels() {
        return header() + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
                + "</Relationships>";
    }

    private static String core(String title, LocalDateTime at) {
        String iso = at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "Z";
        return header() + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:title>NextFit - " + escape(title) + "</dc:title><dc:creator>NextFit</dc:creator>"
                + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + iso + "</dcterms:created></cp:coreProperties>";
    }

    private static String workbook(List<String> names) {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < names.size(); i++) {
            sb.append("<sheet name=\"").append(escape(names.get(i))).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets><definedNames>");
        for (int i = 0; i < names.size(); i++) {
            // The banner and the column names repeat at the top of every printed page.
            sb.append("<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"").append(i).append("\">'")
                    .append(escape(names.get(i).replace("'", "''"))).append("'!$1:$").append(HEADER_ROWS)
                    .append("</definedName>");
        }
        return sb.append("</definedNames></workbook>").toString();
    }

    private static String workbookRels(int sheets) {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= sheets; i++) {
            sb.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(sheets + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    /**
     * Cell formats: 0 default, 1-2 header (text, number), 3-4 text (plain, zebra), 5-6 money, 7-8 whole number,
     * 9 brand name, 10 report title, 11 lime rule, 12 generated stamp.
     */
    private static String styles() {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;#,##0\"/></numFmts>");
        sb.append("<fonts count=\"6\">")
                .append("<font><sz val=\"11\"/><color rgb=\"FF1E1838\"/><name val=\"Calibri\"/></font>")
                .append("<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>")
                .append("<font><b/><sz val=\"22\"/><color rgb=\"FFA7F626\"/><name val=\"Calibri\"/></font>")
                .append("<font><b/><sz val=\"14\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>")
                .append("<font><i/><sz val=\"9\"/><color rgb=\"FF483F68\"/><name val=\"Calibri\"/></font>")
                .append("<font><sz val=\"11\"/><name val=\"Calibri\"/></font>")
                .append("</fonts>");
        sb.append("<fills count=\"5\"><fill><patternFill patternType=\"none\"/></fill>")
                .append("<fill><patternFill patternType=\"gray125\"/></fill>")
                .append(solid("FF5A22B8")).append(solid("FFA7F626")).append(solid("FFF3EEFC"))
                .append("</fills>");
        sb.append("<borders count=\"3\"><border><left/><right/><top/><bottom/><diagonal/></border>")
                .append("<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FFD9D0F0\"/></bottom><diagonal/></border>")
                .append("<border><left/><right/><top/><bottom style=\"medium\"><color rgb=\"FFA7F626\"/></bottom><diagonal/></border>")
                .append("</borders>");
        sb.append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"5\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>");
        sb.append("<cellXfs count=\"13\">")
                .append("<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>")
                .append(xf(0, 1, 2, 2, "left", 1)).append(xf(0, 1, 2, 2, "right", 2))
                .append(xf(0, 0, 0, 1, "left", 0)).append(xf(0, 0, 4, 1, "left", 0))
                .append(xf(164, 0, 0, 1, "right", 0)).append(xf(164, 0, 4, 1, "right", 0))
                .append(xf(3, 0, 0, 1, "right", 0)).append(xf(3, 0, 4, 1, "right", 0))
                .append(xf(0, 2, 2, 0, "left", 1)).append(xf(0, 3, 2, 0, "left", 1))
                .append(xf(0, 5, 3, 0, "left", 0)).append(xf(0, 4, 0, 0, "left", 1))
                .append("</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
        return sb.toString();
    }

    private static String solid(String rgb) {
        return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"" + rgb + "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
    }

    private static String xf(int numFmt, int font, int fill, int border, String align, int indent) {
        return "<xf numFmtId=\"" + numFmt + "\" fontId=\"" + font + "\" fillId=\"" + fill + "\" borderId=\"" + border
                + "\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"" + align + "\" vertical=\"center\"" + (indent > 0 ? " indent=\"" + indent + "\"" : "") + "/></xf>";
    }

    private static String sheet(ReportTable table, LocalDateTime generatedAt, boolean logo) {
        int columns = table.headers().size();
        int firstData = HEADER_ROWS + 1;
        int lastRow = Math.max(HEADER_ROWS, table.rows().size() + HEADER_ROWS);
        String last = ref(columns - 1, 1).replaceAll("\\d", "");
        StringBuilder sb = new StringBuilder(header());
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        sb.append("<sheetPr><tabColor rgb=\"FF5A22B8\"/><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
        sb.append("<sheetViews><sheetView showGridLines=\"0\" workbookViewId=\"0\"><pane ySplit=\"").append(HEADER_ROWS)
                .append("\" topLeftCell=\"A").append(firstData).append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"18\"/>");
        sb.append("<cols>");
        for (int c = 0; c < columns; c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"")
                    .append(width(table, c)).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        banner(sb, 1, 38, columns, "NEXTFIT", S_BRAND);
        banner(sb, 2, 26, columns, table.title(), S_TITLE);
        sb.append("<row r=\"3\" ht=\"5\" customHeight=\"1\">");
        for (int c = 0; c < columns; c++) {
            sb.append("<c r=\"").append(ref(c, 3)).append("\" s=\"").append(S_RULE).append("\"/>");
        }
        sb.append("</row>");
        banner(sb, 4, 20, columns, "Generado el " + STAMP.format(generatedAt) + "  ·  " + table.rows().size() + " registros", S_STAMP);
        sb.append("<row r=\"").append(HEADER_ROWS).append("\" ht=\"26\" customHeight=\"1\">");
        for (int c = 0; c < columns; c++) {
            boolean text = table.types().get(c) == ColumnType.TEXT;
            sb.append(textCell(ref(c, HEADER_ROWS), table.headers().get(c), text ? S_HEADER : S_HEADER_NUM));
        }
        sb.append("</row>");
        for (int r = 0; r < table.rows().size(); r++) {
            int rowNumber = firstData + r;
            boolean zebra = r % 2 == 1;
            sb.append("<row r=\"").append(rowNumber).append("\" ht=\"20\" customHeight=\"1\">");
            List<Object> row = table.rows().get(r);
            for (int c = 0; c < columns; c++) {
                sb.append(cell(ref(c, rowNumber), row.get(c), table.types().get(c), zebra));
            }
            sb.append("</row>");
        }
        sb.append("</sheetData>");
        sb.append("<autoFilter ref=\"A").append(HEADER_ROWS).append(':').append(ref(columns - 1, lastRow)).append("\"/>");
        if (columns > 1) {
            sb.append("<mergeCells count=\"3\">");
            for (int row : new int[] {1, 2, 4}) {
                sb.append("<mergeCell ref=\"A").append(row).append(":").append(last).append(row).append("\"/>");
            }
            sb.append("</mergeCells>");
        }
        sb.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>");
        sb.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        sb.append("<headerFooter><oddFooter>&amp;L&amp;8NextFit&amp;R&amp;8Página &amp;P de &amp;N</oddFooter></headerFooter>");
        if (logo) {
            sb.append("<drawing r:id=\"rId1\"/>");
        }
        return sb.append("</worksheet>").toString();
    }

    /** A full-width band: the text goes in the first cell and the rest only carry the color. */
    private static void banner(StringBuilder sb, int row, int height, int columns, String text, int style) {
        sb.append("<row r=\"").append(row).append("\" ht=\"").append(height).append("\" customHeight=\"1\">");
        sb.append(textCell(ref(0, row), text, style));
        for (int c = 1; c < columns; c++) {
            sb.append("<c r=\"").append(ref(c, row)).append("\" s=\"").append(style).append("\"/>");
        }
        sb.append("</row>");
    }

    private static String cell(String ref, Object value, ColumnType type, boolean zebra) {
        int shift = zebra ? 1 : 0;
        if (type == ColumnType.TEXT) {
            return textCell(ref, String.valueOf(value), S_TEXT + shift);
        }
        long number = value instanceof Number n ? n.longValue() : 0;
        return "<c r=\"" + ref + "\" s=\"" + ((type == ColumnType.MONEY ? S_MONEY : S_NUMBER) + shift) + "\"><v>" + number + "</v></c>";
    }

    private static String textCell(String ref, String text, int style) {
        return "<c r=\"" + ref + "\" t=\"inlineStr\"" + (style == 0 ? "" : " s=\"" + style + "\"")
                + "><is><t xml:space=\"preserve\">" + escape(text) + "</t></is></c>";
    }

    private static int width(ReportTable table, int column) {
        // The header also holds the filter button, so it needs room for it.
        int longest = table.headers().get(column).length() + 4;
        for (List<Object> row : table.rows()) {
            Object value = row.get(column);
            int length = value instanceof Number n ? ("$" + String.format("%,d", n.longValue())).length()
                    : String.valueOf(value).length();
            longest = Math.max(longest, length);
        }
        return Math.min(MAX_COLUMN_WIDTH, Math.max(MIN_COLUMN_WIDTH, longest + 3));
    }

    /** Column letters and row number, for example (0, 1) is A1 and (26, 3) is AA3. */
    static String ref(int column, int row) {
        StringBuilder letters = new StringBuilder();
        int c = column;
        do {
            letters.insert(0, (char) ('A' + c % 26));
            c = c / 26 - 1;
        } while (c >= 0);
        return letters.toString() + row;
    }

    private static List<String> sheetNames(List<ReportTable> tables) {
        List<String> names = new ArrayList<>();
        for (ReportTable table : tables) {
            String base = table.title().replaceAll("[\\[\\]:*?/\\\\]", "-").trim();
            if (base.isEmpty()) {
                base = "Hoja";
            }
            base = base.length() > 31 ? base.substring(0, 31) : base;
            String name = base;
            int n = 2;
            while (containsIgnoreCase(names, name)) {
                String suffix = " (" + n++ + ")";
                name = base.substring(0, Math.min(base.length(), 31 - suffix.length())) + suffix;
            }
            names.add(name);
        }
        return names;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        return list.stream().anyMatch(s -> s.equalsIgnoreCase(value));
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    // XML 1.0 does not allow most control characters: drop them.
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}

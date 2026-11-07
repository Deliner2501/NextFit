package com.nextfit.export;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.service.ColumnType;
import com.nextfit.service.ReportTable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes Excel files (.xlsx) with the JDK only. An .xlsx is a zip of small XML
 * files, so no library is needed: each {@link ReportTable} becomes a sheet with
 * a violet header row, frozen and filterable, money shown as pesos and columns
 * sized to their content.
 */
public final class XlsxWriter {

    private static final int MAX_COLUMN_WIDTH = 50;
    private static final int MIN_COLUMN_WIDTH = 8;

    private XlsxWriter() {
    }

    /**
     * Writes a workbook with one sheet per table.
     *
     * @param file   where to write it
     * @param tables the tables, at least one
     * @throws PersistenceException when the file cannot be written
     */
    public static void write(Path file, List<ReportTable> tables) {
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos una tabla");
        }
        List<String> names = sheetNames(tables);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            entry(zip, "[Content_Types].xml", contentTypes(tables.size()));
            entry(zip, "_rels/.rels", rootRels());
            entry(zip, "xl/workbook.xml", workbook(names));
            entry(zip, "xl/_rels/workbook.xml.rels", workbookRels(tables.size()));
            entry(zip, "xl/styles.xml", styles());
            for (int i = 0; i < tables.size(); i++) {
                entry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(tables.get(i)));
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar el archivo de Excel", e);
        }
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String header() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    }

    private static String contentTypes(int sheets) {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= sheets; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private static String rootRels() {
        return header() + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(List<String> names) {
        StringBuilder sb = new StringBuilder(header());
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < names.size(); i++) {
            sb.append("<sheet name=\"").append(escape(names.get(i))).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
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

    /** Style 0 default, 1 header, 2 money, 3 whole number. */
    private static String styles() {
        return header() + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;#,##0\"/></numFmts>"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF5A22B8\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"4\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
    }

    private static String sheet(ReportTable table) {
        int columns = table.headers().size();
        int lastRow = table.rows().size() + 1;
        StringBuilder sb = new StringBuilder(header());
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        sb.append("<cols>");
        for (int c = 0; c < columns; c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"")
                    .append(width(table, c)).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        for (int c = 0; c < columns; c++) {
            sb.append(textCell(ref(c, 1), table.headers().get(c), 1));
        }
        sb.append("</row>");
        for (int r = 0; r < table.rows().size(); r++) {
            sb.append("<row r=\"").append(r + 2).append("\">");
            List<Object> row = table.rows().get(r);
            for (int c = 0; c < columns; c++) {
                sb.append(cell(ref(c, r + 2), row.get(c), table.types().get(c)));
            }
            sb.append("</row>");
        }
        sb.append("</sheetData>");
        sb.append("<autoFilter ref=\"A1:").append(ref(columns - 1, Math.max(lastRow, 1))).append("\"/>");
        return sb.append("</worksheet>").toString();
    }

    private static String cell(String ref, Object value, ColumnType type) {
        if (type == ColumnType.TEXT) {
            return textCell(ref, String.valueOf(value), 0);
        }
        long number = value instanceof Number n ? n.longValue() : 0;
        return "<c r=\"" + ref + "\" s=\"" + (type == ColumnType.MONEY ? 2 : 3) + "\"><v>" + number + "</v></c>";
    }

    private static String textCell(String ref, String text, int style) {
        return "<c r=\"" + ref + "\" t=\"inlineStr\"" + (style == 0 ? "" : " s=\"" + style + "\"")
                + "><is><t xml:space=\"preserve\">" + escape(text) + "</t></is></c>";
    }

    private static int width(ReportTable table, int column) {
        int longest = table.headers().get(column).length();
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

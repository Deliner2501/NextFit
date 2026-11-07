package com.nextfit.export;

import com.nextfit.exceptions.PersistenceException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.DeflaterOutputStream;

/**
 * A minimal PDF writer that uses the JDK only. It draws rectangles, lines and text with the two standard
 * fonts (Helvetica and Helvetica-Bold), so it needs no library and embeds no font. Coordinates are in
 * points and measured from the top-left corner of the page, as in a screen.
 */
final class PdfDocument {

    static final double A4_SHORT = 595.28;
    static final double A4_LONG = 841.89;
    private static final Charset WINDOWS = Charset.forName("windows-1252");
    private static final double CURVE = 0.5523;

    private final String title;
    private final List<Page> pages = new ArrayList<>();

    PdfDocument(String title) {
        this.title = title;
    }

    /** Adds a blank A4 page at the end. */
    Page addPage(boolean landscape) {
        Page page = new Page(landscape ? A4_LONG : A4_SHORT, landscape ? A4_SHORT : A4_LONG);
        pages.add(page);
        return page;
    }

    List<Page> pages() {
        return pages;
    }

    /** Writes the document to a file. */
    void save(Path file) {
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(toBytes());
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar el archivo PDF", e);
        }
    }

    /** Returns the bytes of the whole file. */
    byte[] toBytes() {
        try {
            return build();
        } catch (IOException e) {
            throw new PersistenceException("No se pudo generar el PDF", e);
        }
    }

    private byte[] build() throws IOException {
        List<Integer> offsets = new ArrayList<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n%âãÏÓ\n");
        int objects = 5 + pages.size() * 2;

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(6 + i * 2).append(" 0 R ");
        }
        object(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        object(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        object(out, offsets, "<< /Title (" + escape(title) + ") /Producer (NextFit) >>");
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            int contentId = 7 + i * 2;
            object(out, offsets, String.format(Locale.ROOT,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> "
                            + "/Contents %d 0 R >>", page.width, page.height, contentId));
            byte[] packed = deflate(page.ops.toString().getBytes(StandardCharsets.ISO_8859_1));
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + packed.length + " /Filter /FlateDecode >>\nstream\n");
            out.write(packed);
            write(out, "\nendstream\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (objects + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (objects + 1) + " /Root 1 0 R /Info 5 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void object(ByteArrayOutputStream out, List<Integer> offsets, String body) throws IOException {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(packed)) {
            deflater.write(data);
        }
        return packed.toByteArray();
    }

    /** Turns a text into the bytes of a PDF string (Windows-1252), one char per byte, with the marks escaped. */
    static String escape(String text) {
        String bytes = new String(text.getBytes(WINDOWS), StandardCharsets.ISO_8859_1);
        return bytes.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /** A page: its drawing commands and its size. */
    static final class Page {
        private final double width;
        private final double height;
        private final StringBuilder ops = new StringBuilder();

        private Page(double width, double height) {
            this.width = width;
            this.height = height;
        }

        double width() {
            return width;
        }

        double height() {
            return height;
        }

        private static String rgb(int color) {
            return String.format(Locale.ROOT, "%.3f %.3f %.3f", ((color >> 16) & 0xFF) / 255.0,
                    ((color >> 8) & 0xFF) / 255.0, (color & 0xFF) / 255.0);
        }

        private static String n(double value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        /** Fills a rectangle; (x, y) is the top-left corner. */
        void fillRect(double x, double y, double w, double h, int color) {
            ops.append(rgb(color)).append(" rg ").append(n(x)).append(' ').append(n(height - y - h)).append(' ')
                    .append(n(w)).append(' ').append(n(h)).append(" re f\n");
        }

        /** Fills a rectangle with rounded corners. */
        void fillRoundRect(double x, double y, double w, double h, double r, int color) {
            ops.append(rgb(color)).append(" rg\n");
            roundPath(x, y, w, h, r);
            ops.append("f\n");
        }

        /** Draws the border of a rectangle with rounded corners. */
        void strokeRoundRect(double x, double y, double w, double h, double r, int color, double lineWidth) {
            ops.append(rgb(color)).append(" RG ").append(n(lineWidth)).append(" w\n");
            roundPath(x + lineWidth / 2, y + lineWidth / 2, w - lineWidth, h - lineWidth, r);
            ops.append("S\n");
        }

        private void roundPath(double x, double y, double w, double h, double r) {
            double top = height - y;
            double bottom = height - y - h;
            double k = r * CURVE;
            ops.append(n(x + r)).append(' ').append(n(bottom)).append(" m\n");
            ops.append(n(x + w - r)).append(' ').append(n(bottom)).append(" l\n");
            curve(x + w - r + k, bottom, x + w, bottom + r - k, x + w, bottom + r);
            ops.append(n(x + w)).append(' ').append(n(top - r)).append(" l\n");
            curve(x + w, top - r + k, x + w - r + k, top, x + w - r, top);
            ops.append(n(x + r)).append(' ').append(n(top)).append(" l\n");
            curve(x + r - k, top, x, top - r + k, x, top - r);
            ops.append(n(x)).append(' ').append(n(bottom + r)).append(" l\n");
            curve(x, bottom + r - k, x + r - k, bottom, x + r, bottom);
            ops.append("h\n");
        }

        private void curve(double x1, double y1, double x2, double y2, double x3, double y3) {
            ops.append(n(x1)).append(' ').append(n(y1)).append(' ').append(n(x2)).append(' ').append(n(y2)).append(' ')
                    .append(n(x3)).append(' ').append(n(y3)).append(" c\n");
        }

        /** Draws a horizontal line. */
        void hLine(double x1, double x2, double y, int color, double lineWidth) {
            ops.append(rgb(color)).append(" RG ").append(n(lineWidth)).append(" w ").append(n(x1)).append(' ')
                    .append(n(height - y)).append(" m ").append(n(x2)).append(' ').append(n(height - y)).append(" l S\n");
        }

        /** Draws a text whose baseline starts at (x, y). */
        void text(double x, double y, String text, double size, boolean bold, int color) {
            if (text.isEmpty()) {
                return;
            }
            ops.append("BT /").append(bold ? "F2 " : "F1 ").append(n(size)).append(" Tf ").append(rgb(color)).append(" rg ")
                    .append(n(x)).append(' ').append(n(height - y)).append(" Td (").append(escape(text)).append(") Tj ET\n");
        }

        /** Draws a text that ends at x. */
        void textRight(double x, double y, String text, double size, boolean bold, int color) {
            text(x - PdfFonts.width(text, size, bold), y, text, size, bold, color);
        }

        /** Draws a text centered on x. */
        void textCenter(double x, double y, String text, double size, boolean bold, int color) {
            text(x - PdfFonts.width(text, size, bold) / 2, y, text, size, bold, color);
        }
    }
}

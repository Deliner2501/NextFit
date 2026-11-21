package com.nextfit.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws member carnets as a PDF ready to print and cut: each carnet is a credit-card sized pair (front on the
 * left, back with the QR on the right), four pairs to an A4 page. Everything is vector except the photo, so
 * the QR scans well at any size.
 */
public final class CarnetPdf {

    /** What one carnet shows. */
    public record Card(String fullName, String document, String validity, boolean[][] qr, Path photo) {
    }

    private static final int VIOLET = 0x5A22B8;
    private static final int LIME = 0xA7F626;
    private static final int INK = 0x1A1330;
    private static final int MUTED = 0x5B5575;
    private static final int LINE = 0xDDD5F0;
    private static final int SOFT = 0xE4DDF5;
    private static final int WHITE = 0xFFFFFF;

    /** Credit card size, ISO/IEC 7810 ID-1: 85.6 x 54 mm, in points. */
    private static final double W = 85.6 * 72 / 25.4;
    private static final double H = 54.0 * 72 / 25.4;
    private static final double GAP = 20;
    private static final double ROW_GAP = 22;
    private static final double TOP = 56;
    private static final int PER_PAGE = 4;
    private static final double PHOTO_W = 56;
    private static final double PHOTO_H = 70;
    private static final double QR_SIZE = 98;

    private CarnetPdf() {
    }

    /**
     * Writes the carnets to a file.
     *
     * @param file  where to save the PDF
     * @param cards the carnets, at least one
     */
    public static void save(Path file, List<Card> cards) {
        build(cards).save(file);
    }

    /**
     * Builds the PDF in memory.
     *
     * @param cards the carnets, at least one
     * @return the bytes of the file
     */
    public static byte[] toBytes(List<Card> cards) {
        return build(cards).toBytes();
    }

    private static PdfDocument build(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos un carnet");
        }
        PdfDocument doc = new PdfDocument("Carnets NextFit");
        PdfImage mark = PdfImage.load("/images/logo/nextfit-logo-256.png");
        int logo = mark == null ? -1 : doc.addImage(mark);
        double left = (PdfDocument.A4_SHORT - (2 * W + GAP)) / 2;
        PdfDocument.Page page = null;
        for (int i = 0; i < cards.size(); i++) {
            int slot = i % PER_PAGE;
            if (slot == 0) {
                page = doc.addPage(false);
                page.text(left, 34, "Carnets NextFit", 11, true, VIOLET);
                page.text(left, 46, "Frente a la izquierda, reverso a la derecha. Recorta por el borde.", 7.5, false, MUTED);
            }
            double y = TOP + slot * (H + ROW_GAP);
            Card card = cards.get(i);
            int photo = -1;
            if (card.photo() != null) {
                PdfImage image = PdfImage.fromFile(card.photo(), 400, PHOTO_W / PHOTO_H);
                photo = image == null ? -1 : doc.addImage(image);
            }
            front(page, left, y, card, photo, logo);
            back(page, left + W + GAP, y, card);
        }
        return doc;
    }

    private static void front(PdfDocument.Page page, double x, double y, Card card, int photo, int logo) {
        page.fillRoundRect(x, y, W, H, 9, VIOLET);
        double textX = x + 14;
        if (logo >= 0) {
            page.image(logo, x + 12, y + 10, 22, 22);
            textX = x + 40;
        }
        page.text(textX, y + 23, "NEXTFIT", 14, true, WHITE);
        page.text(textX, y + 33, "CARNET DE SOCIO", 6.5, true, LIME);

        double photoX = x + 14;
        double photoY = y + 46;
        page.fillRoundRect(photoX - 2, photoY - 2, PHOTO_W + 4, PHOTO_H + 4, 5, WHITE);
        if (photo >= 0) {
            page.image(photo, photoX, photoY, PHOTO_W, PHOTO_H);
        } else {
            page.fillRoundRect(photoX, photoY, PHOTO_W, PHOTO_H, 4, LIME);
            page.textCenter(photoX + PHOTO_W / 2, photoY + PHOTO_H / 2 + 8, initials(card.fullName()), 24, true, INK);
        }

        double infoX = photoX + PHOTO_W + 14;
        double infoW = x + W - 14 - infoX;
        double ty = y + 62;
        for (String line : wrap(card.fullName(), 12, true, infoW, 2)) {
            page.text(infoX, ty, line, 12, true, WHITE);
            ty += 14;
        }
        ty += 4;
        page.text(infoX, ty, "Documento", 6.5, false, SOFT);
        page.text(infoX, ty + 10, PdfFonts.fit(card.document(), 9.5, true, infoW), 9.5, true, WHITE);
        page.text(infoX, ty + 24, PdfFonts.fit(card.validity(), 7, false, infoW), 7, false, SOFT);
        page.fillRoundRect(x + 14, y + H - 11, W - 28, 2.5, 1.2, LIME);
    }

    private static void back(PdfDocument.Page page, double x, double y, Card card) {
        page.fillRoundRect(x, y, W, H, 9, WHITE);
        page.strokeRoundRect(x, y, W, H, 9, LINE, 0.8);
        double qrX = x + W - QR_SIZE - 14;
        double qrY = y + (H - QR_SIZE) / 2;
        drawQr(page, card.qr(), qrX, qrY, QR_SIZE);

        double textW = qrX - (x + 14) - 8;
        page.text(x + 14, y + 26, "NEXTFIT", 12, true, VIOLET);
        double ty = y + 42;
        for (String line : wrap("Escanea el código con la cámara del celular para registrar tu entrada o salida.",
                7, false, textW, 5)) {
            page.text(x + 14, ty, line, 7, false, MUTED);
            ty += 9;
        }
        page.text(x + 14, y + H - 14, "Personal e intransferible", 6.5, true, VIOLET);
    }

    /** Draws the dark modules as rectangles, joining the neighbors of a row into a single bar. */
    private static void drawQr(PdfDocument.Page page, boolean[][] grid, double x, double y, double size) {
        int n = grid.length;
        double module = size / n;
        for (int row = 0; row < n; row++) {
            int col = 0;
            while (col < n) {
                if (!grid[row][col]) {
                    col++;
                    continue;
                }
                int start = col;
                while (col < n && grid[row][col]) {
                    col++;
                }
                // A hair of overlap between rows keeps thin white lines from showing in some viewers.
                page.fillRect(x + start * module, y + row * module, (col - start) * module, module + 0.15, INK);
            }
        }
    }

    private static List<String> wrap(String text, double size, boolean bold, double maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] words = text.trim().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String candidate = current.isEmpty() ? words[i] : current + " " + words[i];
            if (PdfFonts.width(candidate, size, bold) <= maxWidth || current.isEmpty()) {
                current = new StringBuilder(candidate);
                continue;
            }
            if (lines.size() == maxLines - 1) {
                // No room for another line: the rest of the text goes in the last one, cut with an ellipsis.
                current = new StringBuilder(candidate);
                for (int j = i + 1; j < words.length; j++) {
                    current.append(' ').append(words[j]);
                }
                break;
            }
            lines.add(current.toString());
            current = new StringBuilder(words[i]);
        }
        lines.add(PdfFonts.fit(current.toString(), size, bold, maxWidth));
        return lines;
    }

    private static String initials(String fullName) {
        StringBuilder result = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && result.length() < 2) {
                result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            }
        }
        return result.toString();
    }
}

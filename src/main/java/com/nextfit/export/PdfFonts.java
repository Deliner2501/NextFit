package com.nextfit.export;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Widths of the Helvetica and Helvetica-Bold letters, in thousandths of the font size (Adobe font metrics).
 * PDF readers know these two fonts, so the file does not embed them; the writer only needs the widths to
 * align, wrap and truncate text. The tables cover the Windows-1252 characters from 32 to 255, which
 * include the Spanish accents, the inverted marks and the ñ.
 */
final class PdfFonts {

    private static final int FIRST = 32;
    private static final int[] REGULAR = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 761,
        556, 0, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0,
        0, 222, 222, 333, 333, 350, 556, 1000, 333, 1000, 500, 333, 944, 0, 500, 667,
        278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 556, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611,
        667, 667, 667, 667, 667, 667, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278,
        722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 584, 611, 556, 556, 556, 556, 500, 556, 500
    };
    private static final int[] BOLD = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584, 761,
        556, 0, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0,
        0, 278, 278, 500, 500, 350, 556, 1000, 333, 1000, 556, 333, 944, 0, 500, 667,
        278, 333, 556, 556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 611, 556, 278, 333, 333, 365, 556, 834, 834, 834, 611,
        722, 722, 722, 722, 722, 722, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278,
        722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 556, 556, 556, 556, 556, 278, 278, 278, 278,
        611, 611, 611, 611, 611, 611, 611, 584, 611, 611, 611, 611, 611, 556, 611, 556
    };
    private static final Map<Character, Integer> CODE_OF = new HashMap<>();

    static {
        Charset windows = Charset.forName("windows-1252");
        for (int code = FIRST; code < 256; code++) {
            String letter = new String(new byte[] {(byte) code}, windows);
            if (!letter.equals("\uFFFD")) {
                CODE_OF.put(letter.charAt(0), code);
            }
        }
    }

    private PdfFonts() {
    }

    /**
     * Returns the width of a text.
     *
     * @param text the text
     * @param size the font size in points
     * @param bold true for Helvetica-Bold
     * @return the width in points
     */
    static double width(String text, double size, boolean bold) {
        int[] table = bold ? BOLD : REGULAR;
        long sum = 0;
        for (int i = 0; i < text.length(); i++) {
            Integer code = CODE_OF.get(text.charAt(i));
            sum += code == null ? table['?' - FIRST] : table[code - FIRST];
        }
        return sum * size / 1000.0;
    }

    /**
     * Cuts a text with an ellipsis so that it fits a width.
     *
     * @param text     the text
     * @param size     the font size in points
     * @param bold     true for Helvetica-Bold
     * @param maxWidth the available width in points
     * @return the same text when it fits, otherwise a shorter one that ends in an ellipsis
     */
    static String fit(String text, double size, boolean bold, double maxWidth) {
        if (width(text, size, bold) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && width(cut + "\u2026", size, bold) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "\u2026";
    }
}

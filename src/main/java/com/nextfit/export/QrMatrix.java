package com.nextfit.export;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

/**
 * The modules of a QR code as a grid of dark and light squares, so a PDF can draw them as vectors (sharp at
 * any print size) instead of as a picture.
 */
public final class QrMatrix {

    private QrMatrix() {
    }

    /**
     * Encodes a text.
     *
     * @param text the text to put in the QR
     * @return the grid, {@code true} for a dark module, with a quiet zone of one module around it
     */
    public static boolean[][] of(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            boolean[][] grid = new boolean[matrix.getHeight()][matrix.getWidth()];
            for (int y = 0; y < grid.length; y++) {
                for (int x = 0; x < grid[y].length; x++) {
                    grid[y][x] = matrix.get(x, y);
                }
            }
            return grid;
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }
}

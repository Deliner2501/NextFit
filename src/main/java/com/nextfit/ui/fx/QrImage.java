package com.nextfit.ui.fx;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * Draws a QR code as a JavaFX image. Every QR module becomes a square of whole
 * pixels, so the code stays sharp (no smoothing) and any scanner reads it.
 */
public final class QrImage {

    private QrImage() {
    }

    /**
     * Creates the QR image of a text.
     *
     * @param text   the text to encode
     * @param scale  pixels per QR module (an integer of 4 or more is recommended)
     * @return the black on white image, with a quiet zone of one module
     */
    public static Image of(String text, int scale) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            int size = matrix.getWidth() * scale;
            WritableImage image = new WritableImage(size, size);
            var writer = image.getPixelWriter();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    writer.setColor(x, y, matrix.get(x / scale, y / scale) ? Color.BLACK : Color.WHITE);
                }
            }
            return image;
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }
}

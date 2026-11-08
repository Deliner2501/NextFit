package com.nextfit.export;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * A picture ready to be placed in a PDF: its colors and its transparency, each compressed, so rounded or
 * faded edges keep their shape on the page.
 */
final class PdfImage {

    final int width;
    final int height;
    final byte[] rgb;
    final byte[] alpha;

    private PdfImage(int width, int height, byte[] rgb, byte[] alpha) {
        this.width = width;
        this.height = height;
        this.rgb = rgb;
        this.alpha = alpha;
    }

    /**
     * Reads a picture from the class path.
     *
     * @param resource the path, for example {@code /images/logo/nextfit-logo-256.png}
     * @return the picture, or {@code null} when it is missing or cannot be read, so a report is still
     *         produced without it
     */
    static PdfImage load(String resource) {
        try (InputStream in = PdfImage.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return null;
            }
            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
            byte[] colors = new byte[w * h * 3];
            byte[] transparency = new byte[w * h];
            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                transparency[i] = (byte) (p >>> 24);
                colors[3 * i] = (byte) (p >> 16);
                colors[3 * i + 1] = (byte) (p >> 8);
                colors[3 * i + 2] = (byte) p;
            }
            return new PdfImage(w, h, PdfDocument.deflate(colors), PdfDocument.deflate(transparency));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}

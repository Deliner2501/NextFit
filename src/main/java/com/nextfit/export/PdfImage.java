package com.nextfit.export;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
            return of(ImageIO.read(in));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Reads a picture from a file and shrinks it when it is bigger than needed, so a photo taken with a phone
     * does not make the PDF heavy.
     *
     * @param file    the image file (PNG, JPG, GIF)
     * @param maxSide the longest side to keep, in pixels
     * @param aspect  width divided by height of the area the picture will fill; the picture is cropped from the
     *                center to that shape so it is never stretched
     * @return the picture, or {@code null} when the file is missing or is not an image
     */
    static PdfImage fromFile(Path file, int maxSide, double aspect) {
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return null;
            }
            image = crop(image, aspect);
            int longest = Math.max(image.getWidth(), image.getHeight());
            if (longest > maxSide) {
                double ratio = (double) maxSide / longest;
                int w = Math.max(1, (int) Math.round(image.getWidth() * ratio));
                int h = Math.max(1, (int) Math.round(image.getHeight() * ratio));
                BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = small.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(image, 0, 0, w, h, null);
                g.dispose();
                image = small;
            }
            return of(image);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static BufferedImage crop(BufferedImage image, double aspect) {
        int w = image.getWidth();
        int h = image.getHeight();
        int cropW = Math.min(w, (int) Math.round(h * aspect));
        int cropH = Math.min(h, (int) Math.round(w / aspect));
        return image.getSubimage((w - cropW) / 2, (h - cropH) / 2, cropW, cropH);
    }

    /** Converts a decoded picture into its compressed colors and transparency. */
    static PdfImage of(BufferedImage image) throws IOException {
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
    }
}

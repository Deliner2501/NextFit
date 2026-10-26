package com.nextfit.ui.components;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * A photo that fills an area completely, like the CSS "cover" rule: it keeps
 * its proportions and the part that does not fit is cut. The point of interest
 * (for example a face) decides which part is kept. The scaled copy is stored,
 * so animated screens can draw it many times a second without resizing again.
 */
public final class CoverPhoto {

    private final BufferedImage source;
    private final double focusX;
    private final double focusY;
    private BufferedImage cached;

    private CoverPhoto(BufferedImage source, double focusX, double focusY) {
        this.source = source;
        this.focusX = focusX;
        this.focusY = focusY;
    }

    /**
     * Loads a photo from a stored path (a file or an application resource).
     *
     * @param path   the path, as {@link PictureSource#load} understands it
     * @param focusX horizontal point to keep, from 0 (left) to 1 (right)
     * @param focusY vertical point to keep, from 0 (top) to 1 (bottom)
     * @return the photo; when the file is missing {@link #isAvailable()} is false and nothing is drawn
     */
    public static CoverPhoto load(String path, double focusX, double focusY) {
        Optional<BufferedImage> image = PictureSource.load(path);
        return new CoverPhoto(image.orElse(null), clamp(focusX), clamp(focusY));
    }

    /**
     * Tells whether the picture could be loaded.
     *
     * @return true when {@link #paint} will draw something
     */
    public boolean isAvailable() {
        return source != null;
    }

    /**
     * Draws the photo so it covers an area whose corner is the origin.
     *
     * @param g2     the surface to draw on
     * @param width  width of the area
     * @param height height of the area
     */
    public void paint(Graphics2D g2, int width, int height) {
        if (source == null || width <= 0 || height <= 0) {
            return;
        }
        if (cached == null || cached.getWidth() != width || cached.getHeight() != height) {
            cached = render(width, height);
        }
        g2.drawImage(cached, 0, 0, null);
    }

    private BufferedImage render(int width, int height) {
        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
        int scaledWidth = Math.max(width, (int) Math.ceil(source.getWidth() * scale));
        int scaledHeight = Math.max(height, (int) Math.ceil(source.getHeight() * scale));
        BufferedImage scaled = Images.scale(source, scaledWidth, scaledHeight);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();
        try {
            int x = (int) Math.round(-(scaledWidth - width) * focusX);
            int y = (int) Math.round(-(scaledHeight - height) * focusY);
            g2.drawImage(scaled, x, y, null);
        } finally {
            g2.dispose();
        }
        return result;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

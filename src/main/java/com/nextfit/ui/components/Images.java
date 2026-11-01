package com.nextfit.ui.components;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Loads pictures from the application resources and resizes them with good
 * quality. A missing or unreadable picture gives an empty result instead of an
 * exception, so screens can show a substitute and keep working.
 */
public final class Images {

    private Images() {
    }

    /**
     * Loads a picture from the classpath.
     *
     * @param resourcePath the path starting with a slash, e.g. "/images/logo/nextfit-logo-512.png"
     * @return the picture, or empty when it does not exist or cannot be read
     */
    public static Optional<BufferedImage> load(String resourcePath) {
        try (InputStream in = Images.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(ImageIO.read(in));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Resizes a picture. Big reductions are done in steps of half the size,
     * which keeps edges smooth.
     *
     * @param source the original picture
     * @param width  the wanted width in pixels
     * @param height the wanted height in pixels
     * @return a new picture of the wanted size
     */
    public static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int currentWidth = source.getWidth();
        int currentHeight = source.getHeight();
        while (currentWidth / 2 >= width && currentHeight / 2 >= height) {
            currentWidth /= 2;
            currentHeight /= 2;
            current = resample(current, currentWidth, currentHeight);
        }
        if (currentWidth == width && currentHeight == height) {
            return current;
        }
        return resample(current, width, height);
    }

    /**
     * Tells how many real screen pixels there are for each logical pixel of a
     * surface. It is 1 on a normal screen and bigger (1.25, 1.5, 2...) when the
     * operating system scales the interface. Pictures are prepared at this
     * density so they stay sharp instead of being stretched by the system.
     *
     * @param g2 the surface being painted
     * @return the density, never less than 1
     */
    public static double deviceScale(Graphics2D g2) {
        return Math.max(1.0, g2.getTransform().getScaleX());
    }

    private static BufferedImage resample(BufferedImage source, int width, int height) {
        // Las fotos no tienen transparencia: RGB es más rápido y ocupa menos memoria.
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g2 = result.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(source, 0, 0, width, height, null);
        } finally {
            g2.dispose();
        }
        return result;
    }
}
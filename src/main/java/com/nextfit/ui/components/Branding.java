package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gives every screen the NextFit logo in the size it needs. The logo comes
 * from picture files in the resources; when they are missing, a simple drawn
 * version is used so the application always looks complete.
 */
public final class Branding {

    private static final int[] SOURCE_SIZES = {32, 64, 128, 256, 512};
    private static final Map<Integer, Optional<BufferedImage>> CACHE = new HashMap<>();

    private Branding() {
    }

    /**
     * Returns the logo scaled to a square size.
     *
     * @param size the side in pixels
     * @return the logo, or empty when the picture files are not available
     */
    public static synchronized Optional<BufferedImage> logo(int size) {
        return CACHE.computeIfAbsent(size, Branding::build);
    }

    /**
     * Returns the logo in every available size, for the icon of a window and of
     * the task bar.
     *
     * @return the pictures, possibly empty
     */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : SOURCE_SIZES) {
            Images.load(path(size)).ifPresent(icons::add);
        }
        return icons;
    }

    /**
     * Paints the logo. If the picture is not available it paints a rounded
     * accent square with a dumbbell instead.
     *
     * @param g2       where to paint
     * @param x        left edge
     * @param y        top edge
     * @param size     side in pixels
     * @param observer the component that is painting
     */
    public static void paintLogo(Graphics2D g2, int x, int y, int size, Component observer) {
        Optional<BufferedImage> logo = logo(size);
        if (logo.isPresent()) {
            g2.drawImage(logo.get(), x, y, null);
            return;
        }
        g2.setColor(Theme.ACCENT);
        g2.fill(new RoundRectangle2D.Double(x, y, size, size, size * 0.3, size * 0.3));
        int icon = (int) Math.round(size * 0.56);
        Icons.of("training", icon, Theme.BACKGROUND)
                .paintIcon(observer, g2, x + (size - icon) / 2, y + (size - icon) / 2);
    }

    private static Optional<BufferedImage> build(int size) {
        // Prefer the smallest picture that is at least as big as needed, so it is only reduced.
        for (int source : SOURCE_SIZES) {
            if (source >= size) {
                Optional<BufferedImage> image = Images.load(path(source));
                if (image.isPresent()) {
                    return Optional.of(Images.scale(image.get(), size, size));
                }
            }
        }
        // Otherwise enlarge the biggest one available.
        for (int i = SOURCE_SIZES.length - 1; i >= 0; i--) {
            Optional<BufferedImage> image = Images.load(path(SOURCE_SIZES[i]));
            if (image.isPresent()) {
                return Optional.of(Images.scale(image.get(), size, size));
            }
        }
        return Optional.empty();
    }

    private static String path(int size) {
        return "/images/logo/nextfit-logo-" + size + ".png";
    }
}
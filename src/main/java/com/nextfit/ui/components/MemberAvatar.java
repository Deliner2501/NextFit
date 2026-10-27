package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Round picture of a member: the photo cut in a circle or, when there is no
 * photo, the initials of the name over a color that depends on the name.
 */
public class MemberAvatar extends JComponent {

    private static final Color[] PALETTE = {
            Theme.ACCENT_ALT, Theme.WARNING, Theme.SUCCESS,
            Color.decode("#3DA5FF"), Color.decode("#A06BFF"), Color.decode("#FF6BA9")};
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private final int size;
    private String name = "";
    private BufferedImage photo;

    /**
     * Creates an avatar.
     *
     * @param size the width and height in pixels
     */
    public MemberAvatar(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
    }

    /**
     * Forgets the photos already loaded; call it after a photo was replaced.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Sets what the avatar shows.
     *
     * @param fullName  the name, used for the initials and the color
     * @param photoPath the path of the photo, or an empty text when there is none
     */
    public void setMember(String fullName, String photoPath) {
        this.name = fullName == null ? "" : fullName;
        this.photo = loadPhoto(photoPath);
        repaint();
    }

    private BufferedImage loadPhoto(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }
        String key = photoPath + "@" + size;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        BufferedImage result = null;
        File file = new File(photoPath);
        if (file.isFile()) {
            try {
                BufferedImage original = ImageIO.read(file);
                if (original != null) {
                    result = coverSquare(original);
                }
            } catch (IOException e) {
                result = null;
            }
        }
        CACHE.put(key, result);
        return result;
    }

    /** Crops the center of the picture to a square and resizes it to twice the avatar size. */
    private BufferedImage coverSquare(BufferedImage original) {
        int side = Math.min(original.getWidth(), original.getHeight());
        BufferedImage square = original.getSubimage(
                (original.getWidth() - side) / 2, (original.getHeight() - side) / 2, side, side);
        return Images.scale(square, size * 2, size * 2);
    }

    private String initials() {
        String[] words = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(2, words.length); i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }
        return result.toString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setClip(new Ellipse2D.Double(x, y, size, size));
        if (photo != null) {
            g2.drawImage(photo, x, y, size, size, null);
        } else {
            g2.setColor(PALETTE[Math.abs(name.hashCode()) % PALETTE.length]);
            g2.fillOval(x, y, size, size);
            g2.setColor(Theme.BACKGROUND);
            g2.setFont(getFont().deriveFont(Font.BOLD, size * 0.4f));
            FontMetrics metrics = g2.getFontMetrics();
            String text = initials();
            g2.drawString(text, x + (size - metrics.stringWidth(text)) / 2,
                    y + (size + metrics.getAscent() - metrics.getDescent()) / 2);
        }
        g2.dispose();
    }
}
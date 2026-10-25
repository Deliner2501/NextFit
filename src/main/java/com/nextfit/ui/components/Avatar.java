package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * A round picture for people (members, trainers). When there is no picture,
 * it shows the initials of the name on a colored circle.
 */
public class Avatar extends JComponent {

    private final int size;
    private String name;
    private BufferedImage picture;   // ya recortada al tamaño del círculo; null si no hay foto

    /**
     * @param name      full name, used for the initials
     * @param imagePath stored path of the photo, may be null or blank
     * @param size      diameter in pixels
     */
    public Avatar(String name, String imagePath, int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        update(name, imagePath);
    }

    /** Changes the shown person, so the same component can be reused. */
    public void update(String newName, String imagePath) {
        this.name = newName == null ? "" : newName.trim();
        this.picture = PictureSource.load(imagePath)
                .map(image -> PictureSource.cover(image, size, size))
                .orElse(null);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Ellipse2D circle = new Ellipse2D.Float(0, 0, size - 1, size - 1);
            g2.clip(circle);   // clip() intersecta con el recorte actual: todo queda dentro del círculo
            if (picture != null) {
                g2.drawImage(picture, 0, 0, null);
            } else {
                g2.setColor(Theme.SURFACE_ALT);
                g2.fill(circle);
                g2.setColor(Theme.ACCENT);
                g2.setFont(getFont().deriveFont(Font.BOLD, size * 0.38f));
                FontMetrics fm = g2.getFontMetrics();
                String text = initials();
                g2.drawString(text, (size - fm.stringWidth(text)) / 2,
                        (size - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g2.dispose();
        }
    }

    // Iniciales: primera letra de las dos primeras palabras del nombre.
    private String initials() {
        String[] words = name.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length && result.length() < 2; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }
        return result.length() == 0 ? "?" : result.toString();
    }
}
package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.Shape;

/**
 * A reusable card with a picture on top and a title and subtitle below.
 * Used for the exercise catalog and wherever an item with an image is listed.
 * It highlights on hover and can be marked as selected.
 */
public class ImageCard extends JComponent {

    private static final int ARC = 16;
    private static final int TEXT_AREA = 64;

    private final int width;
    private final int height;
    private final String title;
    private final String subtitle;
    private final BufferedImage picture;   // recortada al área de la imagen; null si no hay
    private boolean hovered;
    private boolean selected;

    /**
     * @param title     main text, for example the exercise name
     * @param subtitle  secondary text, for example the muscle group
     * @param imagePath stored path of the picture, may be null or blank
     * @param width     card width in pixels
     * @param height    card height in pixels (the picture takes what the text leaves)
     * @param onClick   action to run when the card is clicked, may be null
     */
    public ImageCard(String title, String subtitle, String imagePath,
                     int width, int height, Runnable onClick) {
        this.width = width;
        this.height = height;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.picture = PictureSource.load(imagePath)
                .map(image -> PictureSource.cover(image, width, height - TEXT_AREA))
                .orElse(null);
        setPreferredSize(new Dimension(width, height));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    /** Marks the card as selected (shows an accent border). */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, width - 1, height - 1, ARC, ARC);
            // Se guarda el recorte que Swing ya puso (el área visible del scroll) para restaurarlo después.
            Shape visibleArea = g2.getClip();
            g2.setColor(hovered ? Theme.SURFACE_ALT : Theme.SURFACE);
            g2.fill(shape);
            paintPicture(g2, shape);
            paintTexts(g2);
            // Borde: de acento si está seleccionada o con el mouse encima.
            g2.setClip(visibleArea);
            g2.setColor(selected || hovered ? Theme.ACCENT : Theme.SURFACE_ALT);
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
    }

    // Dibuja la imagen en la parte superior, recortada a las esquinas redondeadas.
    private void paintPicture(Graphics2D g2, RoundRectangle2D shape) {
        int pictureHeight = height - TEXT_AREA;
        g2.clip(shape);   // clip() intersecta con el recorte actual; setClip() lo reemplazaría
        if (picture != null) {
            g2.drawImage(picture, 0, 0, null);
        } else {
            // Sin imagen: bloque de color con la inicial del título.
            g2.setColor(Theme.BACKGROUND);
            g2.fillRect(0, 0, width, pictureHeight);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(getFont().deriveFont(Font.BOLD, pictureHeight * 0.4f));
            FontMetrics fm = g2.getFontMetrics();
            String letter = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase();
            g2.drawString(letter, (width - fm.stringWidth(letter)) / 2,
                    (pictureHeight - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // Dibuja el título y el subtítulo debajo de la imagen.
    private void paintTexts(Graphics2D g2) {
        int x = 14;
        int maxWidth = width - 2 * x;
        int top = height - TEXT_AREA;
        g2.setColor(Theme.TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(fit(g2, title, maxWidth), x, top + 26);
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString(fit(g2, subtitle, maxWidth), x, top + 46);
    }

    // Recorta el texto con "..." si no cabe en el ancho disponible.
    private String fit(Graphics2D g2, String text, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && fm.stringWidth(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }
}
package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Wide banner for the top of a screen. It paints a gym-style background
 * (gradient, soft light, barbell plates and a dumbbell) and shows the
 * component it receives, usually a greeting, on the left.
 */
public class HeroBanner extends JPanel {

    private static final int ARC = 24;
    private static final int DEFAULT_HEIGHT = 120;

    private final CoverPhoto photo;

    /**
     * Creates the banner.
     *
     * @param content what to show over the drawing, placed on the left
     */
    public HeroBanner(JComponent content) {
        this(content, null, 0.5, 0.5, DEFAULT_HEIGHT);
    }

    /**
     * Creates the banner over a photo. When the photo is missing it falls back
     * to the drawn background, so the screen still looks right.
     *
     * @param content   what to show over the picture, placed on the left
     * @param photoPath stored path of the photo, or null for the drawn background
     * @param focusX    horizontal point of the photo to keep, from 0 to 1
     * @param focusY    vertical point of the photo to keep, from 0 to 1
     * @param height    height of the banner in pixels
     */
    public HeroBanner(JComponent content, String photoPath, double focusX, double focusY, int height) {
        super(new BorderLayout());
        this.photo = photoPath == null ? null : CoverPhoto.load(photoPath, focusX, focusY);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 32));
        setPreferredSize(new Dimension(0, height));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        content.setOpaque(false);
        // Centered vertically and kept on the left, so the text sits in the middle of the banner.
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        holder.add(content, c);
        add(holder, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, ARC, ARC);
            g2.setPaint(new GradientPaint(0, 0, Theme.SURFACE, w, h, new Color(0x1B2A10)));
            g2.fill(shape);
            g2.clip(shape);
            if (photo != null && photo.isAvailable()) {
                photo.paint(g2, w, h);
                // The left side is darkened so the text stays readable over any photo.
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 16, 20, 235), w * 0.85f, 0,
                        new Color(14, 16, 20, 70)));
                g2.fillRect(0, 0, w, h);
                g2.setPaint(new GradientPaint(0, h - 4, new Color(182, 255, 59, 0), 0, h, Theme.ACCENT));
                g2.fillRect(0, h - 4, w, 4);
                g2.setClip(null);
                g2.setColor(Theme.SURFACE_ALT);
                g2.draw(shape);
                return;
            }
            g2.setPaint(new RadialGradientPaint(w * 0.82f, h * 0.5f, h * 1.6f, new float[]{0f, 1f},
                    new Color[]{new Color(182, 255, 59, 60), new Color(182, 255, 59, 0)}));
            g2.fillRect(0, 0, w, h);
            GymArt.plate(g2, w - 70, h * 0.85, h * 0.95, 5, Theme.ACCENT, 46);
            GymArt.plate(g2, w * 0.58, -h * 0.2, h * 0.7, 4, Theme.ACCENT_ALT, 28);
            GymArt.dumbbell(g2, w - 190, h * 0.52, Math.min(300, w * 0.32), Math.toRadians(-14),
                    Theme.ACCENT, 150);
            g2.setClip(null);
            g2.setColor(Theme.SURFACE_ALT);
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
    }
}

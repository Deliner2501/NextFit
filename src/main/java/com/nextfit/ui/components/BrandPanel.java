package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;


/**
 * Decorative panel of the sign-in screen: a dark gradient with two soft lights
 * that drift slowly, the NextFit mark and its tagline. The animation only runs
 * while the panel is on screen.
 */
public class BrandPanel extends JPanel {

    private static final int WIDTH = 480;
    private static final int FRAME_MS = 33;

    private float phase;
    private final Timer timer = new Timer(FRAME_MS, e -> {
        phase += 0.02f;
        repaint();
    });

    /** Creates the panel. */
    public BrandPanel() {
        setPreferredSize(new Dimension(WIDTH, 0));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(0x0B0D11), w, h, new Color(0x16200C)));
            g2.fillRect(0, 0, w, h);
            glow(g2, w * 0.25f + (float) Math.sin(phase) * 50, h * 0.28f + (float) Math.cos(phase * 0.8) * 40,
                    320, Theme.ACCENT, 0.20f);
            glow(g2, w * 0.85f + (float) Math.cos(phase * 0.7) * 40, h * 0.82f + (float) Math.sin(phase * 0.9) * 50,
                    360, Theme.ACCENT_ALT, 0.14f);

            int x = 56;
            int y = Math.max(60, h / 2 - 150);
            Branding.paintLogo(g2, x, y, 68, this);

            Font base = getFont();
            g2.setColor(Theme.TEXT);
            g2.setFont(base.deriveFont(Font.BOLD, 50f));
            g2.drawString("NextFit", x, y + 140);

            g2.setFont(base.deriveFont(Font.PLAIN, 21f));
            g2.setColor(Theme.ACCENT);
            g2.drawString("Entrena. Mide. Mejora.", x, y + 182);

            g2.setFont(base.deriveFont(Font.PLAIN, 15f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("Gestión de socios, membresías, salud", x, y + 226);
            g2.drawString("y rutinas en un solo lugar.", x, y + 250);

            g2.setFont(base.deriveFont(Font.PLAIN, 12f));
            g2.drawString("Programación III · Universidad Popular del Cesar", x, h - 36);
        } finally {
            g2.dispose();
        }
    }

    private static void glow(Graphics2D g2, float cx, float cy, float radius, Color color, float alpha) {
        Color center = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha));
        Color edge = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, new float[]{0f, 1f}, new Color[]{center, edge}));
        g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }
}
package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.LongFunction;

/**
 * An indicator card for the dashboard: an icon, a title, a big number and a
 * small caption. The number counts up from its previous value to the new one
 * when it changes. The card knows nothing about members or payments; the
 * screen decides what number to show and how to format it.
 */
public class StatCard extends JPanel {

    private static final int ARC = 22;
    private static final int ANIMATION_MS = 900;
    private static final int FRAME_MS = 15;

    private final String title;
    private final String iconName;
    private final Color accent;
    private final LongFunction<String> formatter;
    private String caption = "";
    private long target;
    private double shown;
    private double from;
    private boolean animated = true;
    private Timer timer;
    private long animationStart;

    /**
     * Creates a card showing zero.
     *
     * @param title     the text above the number, e.g. "Socios activos"
     * @param iconName  the icon name (see {@link Icons})
     * @param accent    the color of the icon and its background
     * @param formatter how the number is turned into text, e.g. as pesos
     */
    public StatCard(String title, String iconName, Color accent, LongFunction<String> formatter) {
        this.title = title;
        this.iconName = iconName;
        this.accent = accent;
        this.formatter = formatter;
        setOpaque(false);
        setPreferredSize(new Dimension(240, 124));
    }

    /**
     * Sets the number to show. The shown number counts from the current one
     * to the new one.
     *
     * @param value the new number
     */
    public void setValue(long value) {
        if (timer != null) {
            timer.stop();
        }
        from = shown;
        target = value;
        if (!animated || from == target) {
            shown = target;
            repaint();
            return;
        }
        animationStart = System.nanoTime();
        timer = new Timer(FRAME_MS, e -> {
            double elapsed = (System.nanoTime() - animationStart) / 1_000_000.0 / ANIMATION_MS;
            if (elapsed >= 1.0) {
                shown = target;
                timer.stop();
            } else {
                double eased = 1 - Math.pow(1 - elapsed, 3);
                shown = from + (target - from) * eased;
            }
            repaint();
        });
        timer.start();
    }

    /**
     * Sets the small text under the number.
     *
     * @param text the caption, e.g. "próximos 7 días"
     */
    public void setCaption(String text) {
        this.caption = text == null ? "" : text;
        repaint();
    }

    /**
     * Turns the counting animation on or off.
     *
     * @param enabled true to animate the number
     */
    public void setAnimated(boolean enabled) {
        this.animated = enabled;
    }

    /**
     * Returns the number the card is showing or counting towards.
     *
     * @return the target value
     */
    public long getValue() {
        return target;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);

        int badge = 40;
        int badgeX = w - badge - 18;
        g2.setColor(withAlpha(accent, 40));
        g2.fillRoundRect(badgeX, 18, badge, badge, 14, 14);
        Icons.of(iconName, 22, accent).paintIcon(this, g2, badgeX + 9, 27);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(title, 20, 34);

        String text = formatter.apply(Math.round(shown));
        Font valueFont = getFont().deriveFont(Font.BOLD, 30f);
        g2.setFont(fit(g2, text, valueFont, w - 40));
        g2.setColor(Theme.TEXT);
        g2.drawString(text, 20, 82);

        if (!caption.isEmpty()) {
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(caption, 20, h - 16);
        }
        g2.dispose();
    }

    /** Shrinks the font until the text fits the available width. */
    private static Font fit(Graphics2D g2, String text, Font font, int maxWidth) {
        Font current = font;
        FontMetrics metrics = g2.getFontMetrics(current);
        while (metrics.stringWidth(text) > maxWidth && current.getSize2D() > 14f) {
            current = current.deriveFont(current.getSize2D() - 1f);
            metrics = g2.getFontMetrics(current);
        }
        return current;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}

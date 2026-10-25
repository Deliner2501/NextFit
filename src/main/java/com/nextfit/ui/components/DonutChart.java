package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.List;

/**
 * A donut chart drawn by hand with Java 2D. It receives a list of
 * {@link Slice} and knows nothing about memberships or payments, so any
 * screen can reuse it. The ring sweeps in when the data arrives, the slice
 * under the mouse grows, and the center shows the total or the hovered value.
 * The legend sits on the right.
 */
public class DonutChart extends JPanel {

    /** One slice: its name, its value and its color. */
    public record Slice(String label, double value, Color color) {
    }

    private static final int PAD = 20;
    private static final int TITLE_HEIGHT = 34;
    private static final double GAP_DEGREES = 2.5;
    private static final int ANIMATION_MS = 900;
    private static final int FRAME_MS = 15;

    private final String title;
    private List<Slice> slices = List.of();
    private String centerLabel = "total";
    private String emptyMessage = "Sin datos todavía";
    private boolean animated = true;
    private double progress = 1.0;
    private int hoverIndex = -1;
    private Timer timer;
    private long animationStart;

    /**
     * Creates an empty chart.
     *
     * @param title the title shown at the top left
     */
    public DonutChart(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(460, 280));
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverIndex != -1) {
                    hoverIndex = -1;
                    repaint();
                }
            }
        };
        addMouseMotionListener(hover);
        addMouseListener(hover);
    }

    /**
     * Replaces the data and plays the entry animation.
     *
     * @param newSlices the slices; values must not be negative
     */
    public void setData(List<Slice> newSlices) {
        this.slices = List.copyOf(newSlices);
        this.hoverIndex = -1;
        startAnimation();
    }

    /**
     * Sets the small text under the total in the center of the ring.
     *
     * @param label the text, e.g. "membresías"
     */
    public void setCenterLabel(String label) {
        this.centerLabel = label;
        repaint();
    }

    /**
     * Changes the text shown when there is nothing to draw.
     *
     * @param message the text
     */
    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
        repaint();
    }

    /**
     * Turns the entry animation on or off.
     *
     * @param enabled true to animate the ring when the data arrives
     */
    public void setAnimated(boolean enabled) {
        this.animated = enabled;
    }

    private double total() {
        return slices.stream().mapToDouble(Slice::value).sum();
    }

    private void startAnimation() {
        if (timer != null) {
            timer.stop();
        }
        if (!animated || total() <= 0) {
            progress = 1.0;
            repaint();
            return;
        }
        progress = 0.0;
        animationStart = System.nanoTime();
        timer = new Timer(FRAME_MS, e -> {
            double elapsed = (System.nanoTime() - animationStart) / 1_000_000.0 / ANIMATION_MS;
            if (elapsed >= 1.0) {
                progress = 1.0;
                timer.stop();
            } else {
                progress = 1 - Math.pow(1 - elapsed, 3);
            }
            repaint();
        });
        timer.start();
    }

    private int diameter() {
        return Math.max(60, Math.min(getHeight() - TITLE_HEIGHT - PAD, getWidth() / 2 - PAD));
    }

    private int centerX() {
        return PAD + diameter() / 2;
    }

    private int centerY() {
        return TITLE_HEIGHT + (getHeight() - TITLE_HEIGHT) / 2;
    }

    private double thickness() {
        return Math.max(18, diameter() * 0.17);
    }

    private void updateHover(int mouseX, int mouseY) {
        int found = -1;
        double total = total();
        if (total > 0) {
            double dx = mouseX - centerX();
            double dy = mouseY - centerY();
            double distance = Math.hypot(dx, dy);
            double outer = diameter() / 2.0 + 6;
            double inner = diameter() / 2.0 - thickness() - 6;
            if (distance >= inner && distance <= outer) {
                double degrees = ((90 - Math.toDegrees(Math.atan2(-dy, dx))) % 360 + 360) % 360;
                double accumulated = 0;
                for (int i = 0; i < slices.size(); i++) {
                    accumulated += slices.get(i).value() / total * 360;
                    if (degrees < accumulated) {
                        found = slices.get(i).value() > 0 ? i : -1;
                        break;
                    }
                }
            }
        }
        if (found != hoverIndex) {
            hoverIndex = found;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, PAD, 24);
        if (total() <= 0) {
            drawEmptyMessage(g2);
        } else {
            drawRing(g2);
            drawCenter(g2);
            drawLegend(g2);
        }
        g2.dispose();
    }

    private void drawEmptyMessage(Graphics2D g2) {
        g2.setFont(getFont().deriveFont(13f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(emptyMessage, (getWidth() - metrics.stringWidth(emptyMessage)) / 2,
                TITLE_HEIGHT + (getHeight() - TITLE_HEIGHT) / 2);
    }

    private void drawRing(Graphics2D g2) {
        double total = total();
        long visible = slices.stream().filter(s -> s.value() > 0).count();
        double gap = visible > 1 ? GAP_DEGREES : 0;
        double start = 90;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            if (slice.value() <= 0) {
                continue;
            }
            double sweep = slice.value() / total * 360;
            double drawn = Math.max(0.5, sweep - gap) * progress;
            float width = (float) (thickness() + (i == hoverIndex ? 8 : 0));
            double radius = diameter() / 2.0 - thickness() / 2 + (i == hoverIndex ? 4 : 0);
            g2.setColor(slice.color());
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(centerX() - radius, centerY() - radius, radius * 2, radius * 2,
                    start - gap / 2, -drawn, Arc2D.OPEN));
            start -= sweep;
        }
    }

    private void drawCenter(Graphics2D g2) {
        String big;
        String small;
        if (hoverIndex >= 0) {
            big = formatValue(slices.get(hoverIndex).value());
            small = slices.get(hoverIndex).label();
        } else {
            big = formatValue(total());
            small = centerLabel;
        }
        g2.setFont(getFont().deriveFont(Font.BOLD, 26f));
        g2.setColor(Theme.TEXT);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(big, centerX() - metrics.stringWidth(big) / 2, centerY() + 6);
        g2.setFont(getFont().deriveFont(12f));
        g2.setColor(Theme.TEXT_MUTED);
        metrics = g2.getFontMetrics();
        g2.drawString(small, centerX() - metrics.stringWidth(small) / 2, centerY() + 26);
    }

    private void drawLegend(Graphics2D g2) {
        int x = PAD * 2 + diameter() + 10;
        int rowHeight = 28;
        int y = centerY() - slices.size() * rowHeight / 2 + 8;
        double total = total();
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            int rowY = y + i * rowHeight;
            g2.setColor(slice.color());
            g2.fillRoundRect(x, rowY - 10, 12, 12, 4, 4);
            g2.setFont(getFont().deriveFont(i == hoverIndex ? Font.BOLD : Font.PLAIN, 13f));
            g2.setColor(i == hoverIndex ? Theme.TEXT : Theme.TEXT_MUTED);
            g2.drawString(slice.label(), x + 22, rowY);
            String value = formatValue(slice.value()) + "  (" + Math.round(slice.value() / total * 100) + "%)";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(value, Math.max(x + 22 + metrics.stringWidth(slice.label()) + 16,
                    getWidth() - PAD - metrics.stringWidth(value)), rowY);
        }
    }

    private static String formatValue(double value) {
        return value == Math.rint(value) ? String.format("%,d", (long) value) : String.format("%,.1f", value);
    }
}

package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Locale;

/**
 * A line chart drawn by hand with Java 2D. It knows nothing about members or
 * payments: it receives a list of {@link Point} and draws them, so any screen
 * can reuse it (weight, body mass index, income...).
 *
 * <p>The line draws itself when the data arrives, the area below it fades out,
 * and passing the mouse over a point shows its label and value.
 */
public class LineChart extends JPanel {

    /** One point of the chart: the text shown on the x axis and its value. */
    public record Point(String label, double value) {
    }

    private static final int MIN_PAD_LEFT = 52;
    private static final int PAD_RIGHT = 24;
    private static final int PAD_TOP = 44;
    private static final int PAD_BOTTOM = 36;
    private static final int GRID_LINES = 4;
    private static final int MAX_X_LABELS = 6;
    private static final int HOVER_DISTANCE = 40;
    private static final int ANIMATION_MS = 800;
    private static final int FRAME_MS = 15;

    private final String title;
    private final String unit;
    private List<Point> points = List.of();
    private Color lineColor = Theme.ACCENT;
    private String emptyMessage = "Sin datos todavía";
    private int decimals = 1;
    private boolean animated = true;
    private double progress = 1.0;
    private int hoverIndex = -1;
    private Timer animationTimer;
    private long animationStart;

    /**
     * Creates an empty chart.
     *
     * @param title the title shown at the top left
     * @param unit  the unit of the values, e.g. "kg"; it can be empty
     */
    public LineChart(String title, String unit) {
        this.title = title;
        this.unit = unit;
        setOpaque(false);
        setPreferredSize(new Dimension(560, 300));
        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        };
        addMouseMotionListener(hoverListener);
        addMouseListener(hoverListener);
    }

    /**
     * Replaces the data and plays the entry animation.
     *
     * @param newPoints the points, from the oldest to the newest
     */
    public void setData(List<Point> newPoints) {
        this.points = List.copyOf(newPoints);
        this.hoverIndex = -1;
        startAnimation();
    }

    /**
     * Changes the color of the line, the points and the area.
     *
     * @param color the new color
     */
    public void setLineColor(Color color) {
        this.lineColor = color;
        repaint();
    }

    /**
     * Changes how many decimals the values show.
     *
     * @param newDecimals the number of decimals
     */
    public void setDecimals(int newDecimals) {
        this.decimals = newDecimals;
        repaint();
    }

    /**
     * Changes the text shown when there are no points.
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
     * @param enabled true to animate the line when the data arrives
     */
    public void setAnimated(boolean enabled) {
        this.animated = enabled;
    }

    private void startAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (!animated || points.size() < 2) {
            progress = 1.0;
            repaint();
            return;
        }
        progress = 0.0;
        animationStart = System.nanoTime();
        animationTimer = new Timer(FRAME_MS, e -> {
            double elapsed = (System.nanoTime() - animationStart) / 1_000_000.0 / ANIMATION_MS;
            if (elapsed >= 1.0) {
                progress = 1.0;
                animationTimer.stop();
            } else {
                progress = 1 - Math.pow(1 - elapsed, 3);
            }
            repaint();
        });
        animationTimer.start();
    }

    private void updateHover(int mouseX) {
        int nearest = -1;
        double nearestDistance = HOVER_DISTANCE;
        for (int i = 0; i < points.size(); i++) {
            double distance = Math.abs(xOf(i) - mouseX);
            if (distance <= nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        if (nearest != hoverIndex) {
            hoverIndex = nearest;
            repaint();
        }
    }

    private int leftPadding() {
        if (points.isEmpty()) {
            return MIN_PAD_LEFT;
        }
        double[] range = valueRange();
        FontMetrics metrics = getFontMetrics(getFont().deriveFont(11f));
        int widest = Math.max(metrics.stringWidth(format(range[0])), metrics.stringWidth(format(range[1])));
        return Math.max(MIN_PAD_LEFT, widest + 16);
    }

    private Rectangle plotArea() {
        int padLeft = leftPadding();
        return new Rectangle(padLeft, PAD_TOP,
                getWidth() - padLeft - PAD_RIGHT, getHeight() - PAD_TOP - PAD_BOTTOM);
    }

    private double[] valueRange() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Point p : points) {
            min = Math.min(min, p.value());
            max = Math.max(max, p.value());
        }
        double margin = (max == min) ? Math.max(1, Math.abs(max) * 0.05) : (max - min) * 0.2;
        return new double[]{min - margin, max + margin};
    }

    private double xOf(int index) {
        Rectangle plot = plotArea();
        if (points.size() == 1) {
            return plot.x + plot.width / 2.0;
        }
        return plot.x + plot.width * index / (points.size() - 1.0);
    }

    private double yOf(double value, double[] range) {
        Rectangle plot = plotArea();
        return plot.y + plot.height * (1 - (value - range[0]) / (range[1] - range[0]));
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%,." + decimals + "f", value);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawTitle(g2);
        if (points.isEmpty()) {
            drawEmptyMessage(g2);
        } else {
            double[] range = valueRange();
            drawGrid(g2, range);
            drawXLabels(g2);
            drawLineAndArea(g2, range);
            drawPoints(g2, range);
            drawTooltip(g2, range);
        }
        g2.dispose();
    }

    private void drawTitle(Graphics2D g2) {
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, MIN_PAD_LEFT, 24);
        if (!unit.isEmpty()) {
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            g2.setFont(getFont().deriveFont(12f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("(" + unit + ")", MIN_PAD_LEFT + titleWidth + 8, 24);
        }
    }

    private void drawEmptyMessage(Graphics2D g2) {
        Rectangle plot = plotArea();
        g2.setFont(getFont().deriveFont(13f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        int x = plot.x + (plot.width - metrics.stringWidth(emptyMessage)) / 2;
        int y = plot.y + plot.height / 2;
        g2.drawString(emptyMessage, x, y);
    }

    private void drawGrid(Graphics2D g2, double[] range) {
        Rectangle plot = plotArea();
        g2.setFont(getFont().deriveFont(11f));
        FontMetrics metrics = g2.getFontMetrics();
        for (int i = 0; i <= GRID_LINES; i++) {
            int y = plot.y + plot.height * i / GRID_LINES;
            g2.setColor(Theme.SURFACE_ALT);
            g2.drawLine(plot.x, y, plot.x + plot.width, y);
            double value = range[1] - (range[1] - range[0]) * i / GRID_LINES;
            String text = format(value);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(text, plot.x - 8 - metrics.stringWidth(text), y + 4);
        }
    }

    private void drawXLabels(Graphics2D g2) {
        Rectangle plot = plotArea();
        g2.setFont(getFont().deriveFont(11f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        int shown = Math.min(points.size(), MAX_X_LABELS);
        for (int k = 0; k < shown; k++) {
            int index = (shown == 1) ? 0 : (int) Math.round(k * (points.size() - 1.0) / (shown - 1));
            String text = points.get(index).label();
            int x = (int) xOf(index) - metrics.stringWidth(text) / 2;
            g2.drawString(text, x, plot.y + plot.height + 22);
        }
    }

    private void drawLineAndArea(Graphics2D g2, double[] range) {
        if (points.size() < 2) {
            return;
        }
        Rectangle plot = plotArea();
        Path2D line = new Path2D.Double();
        for (int i = 0; i < points.size(); i++) {
            double x = xOf(i);
            double y = yOf(points.get(i).value(), range);
            if (i == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
        }
        Path2D area = new Path2D.Double(line);
        area.lineTo(xOf(points.size() - 1), plot.y + plot.height);
        area.lineTo(xOf(0), plot.y + plot.height);
        area.closePath();

        Graphics2D clipped = (Graphics2D) g2.create();
        clipped.clipRect(0, 0, (int) (plot.x + (plot.width + PAD_RIGHT) * progress), getHeight());
        clipped.setPaint(new GradientPaint(0, plot.y, withAlpha(lineColor, 110),
                0, plot.y + plot.height, withAlpha(lineColor, 0)));
        clipped.fill(area);
        clipped.setColor(lineColor);
        clipped.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        clipped.draw(line);
        clipped.dispose();
    }

    private void drawPoints(Graphics2D g2, double[] range) {
        Rectangle plot = plotArea();
        double reachedX = plot.x + (plot.width + PAD_RIGHT) * progress;
        for (int i = 0; i < points.size(); i++) {
            double x = xOf(i);
            if (x > reachedX) {
                break;
            }
            double y = yOf(points.get(i).value(), range);
            int radius = (i == hoverIndex) ? 7 : 5;
            g2.setColor(Theme.BACKGROUND);
            g2.fillOval((int) (x - radius - 2), (int) (y - radius - 2), (radius + 2) * 2, (radius + 2) * 2);
            g2.setColor(lineColor);
            g2.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        }
    }

    private void drawTooltip(Graphics2D g2, double[] range) {
        if (hoverIndex < 0) {
            return;
        }
        Point point = points.get(hoverIndex);
        String valueText = format(point.value()) + (unit.isEmpty() ? "" : " " + unit);
        Font labelFont = getFont().deriveFont(11f);
        Font valueFont = getFont().deriveFont(Font.BOLD, 14f);
        int width = Math.max(g2.getFontMetrics(labelFont).stringWidth(point.label()),
                g2.getFontMetrics(valueFont).stringWidth(valueText)) + 24;
        int height = 46;
        int x = (int) xOf(hoverIndex) - width / 2;
        x = Math.max(4, Math.min(x, getWidth() - width - 4));
        int y = (int) yOf(point.value(), range) - height - 14;
        if (y < 4) {
            y = (int) yOf(point.value(), range) + 16;
        }
        g2.setColor(Theme.SURFACE_ALT);
        g2.fillRoundRect(x, y, width, height, 12, 12);
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, width, height, 12, 12);
        g2.setFont(labelFont);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(point.label(), x + 12, y + 18);
        g2.setFont(valueFont);
        g2.setColor(Theme.TEXT);
        g2.drawString(valueText, x + 12, y + 37);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
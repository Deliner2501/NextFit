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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * A bar chart drawn by hand with Java 2D. It receives a list of {@link Bar}
 * and knows nothing about attendance or income, so any screen can reuse it.
 * The bars grow one after another when the data arrives, and the bar under
 * the mouse is highlighted with its value.
 */
public class BarChart extends JPanel {

    /** One bar: the text under it and its value. */
    public record Bar(String label, double value) {
    }

    private static final int PAD_LEFT = 24;
    private static final int PAD_RIGHT = 20;
    private static final int PAD_TOP = 48;
    private static final int PAD_BOTTOM = 34;
    private static final int GRID_LINES = 4;
    private static final int ANIMATION_MS = 800;
    private static final int STAGGER_MS = 60;
    private static final int FRAME_MS = 15;

    private final String title;
    private List<Bar> bars = List.of();
    private Color barColor = Theme.ACCENT;
    private String emptyMessage = "Sin datos todavía";
    private DoubleFunction<String> formatter = v -> String.format("%,.0f", v);
    private boolean animated = true;
    private long elapsedMs = ANIMATION_MS * 10L;
    private int hoverIndex = -1;
    private Timer timer;
    private long animationStart;

    /**
     * Creates an empty chart.
     *
     * @param title the title shown at the top left
     */
    public BarChart(String title) {
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
     * @param newBars the bars from left to right; values must not be negative
     */
    public void setData(List<Bar> newBars) {
        this.bars = List.copyOf(newBars);
        this.hoverIndex = -1;
        startAnimation();
    }

    /**
     * Changes the color of the bars.
     *
     * @param color the new color
     */
    public void setBarColor(Color color) {
        this.barColor = color;
        repaint();
    }

    /**
     * Changes how values are written on the axis and in the tooltip.
     *
     * @param newFormatter the function that turns a value into text
     */
    public void setValueFormatter(DoubleFunction<String> newFormatter) {
        this.formatter = newFormatter;
        repaint();
    }

    /**
     * Changes the text shown when there are no bars.
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
     * @param enabled true to animate the bars when the data arrives
     */
    public void setAnimated(boolean enabled) {
        this.animated = enabled;
    }

    private void startAnimation() {
        if (timer != null) {
            timer.stop();
        }
        long totalMs = ANIMATION_MS + (long) STAGGER_MS * bars.size();
        if (!animated || bars.isEmpty()) {
            elapsedMs = totalMs;
            repaint();
            return;
        }
        elapsedMs = 0;
        animationStart = System.nanoTime();
        timer = new Timer(FRAME_MS, e -> {
            elapsedMs = (System.nanoTime() - animationStart) / 1_000_000;
            if (elapsedMs >= totalMs) {
                elapsedMs = totalMs;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    private double growth(int index) {
        double t = Math.max(0, Math.min(1, (elapsedMs - (double) STAGGER_MS * index) / ANIMATION_MS));
        return 1 - Math.pow(1 - t, 3);
    }

    /** Rounds the biggest value up to a "nice" number so the grid labels are clean. */
    private double axisMax() {
        double max = bars.stream().mapToDouble(Bar::value).max().orElse(0);
        if (max <= 0) {
            return 1;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        double[] steps = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        for (double step : steps) {
            if (step * magnitude >= max) {
                return step * magnitude;
            }
        }
        return 10 * magnitude;
    }

    private int leftPadding() {
        FontMetrics metrics = getFontMetrics(getFont().deriveFont(11f));
        return Math.max(PAD_LEFT, metrics.stringWidth(formatter.apply(axisMax())) + 20);
    }

    private Rectangle plotArea() {
        int left = leftPadding();
        return new Rectangle(left, PAD_TOP, getWidth() - left - PAD_RIGHT, getHeight() - PAD_TOP - PAD_BOTTOM);
    }

    private double slotWidth() {
        return plotArea().width / (double) Math.max(1, bars.size());
    }

    private double barWidth() {
        return Math.min(48, slotWidth() * 0.55);
    }

    private double barX(int index) {
        return plotArea().x + slotWidth() * index + (slotWidth() - barWidth()) / 2;
    }

    private void updateHover(int mouseX, int mouseY) {
        int found = -1;
        Rectangle plot = plotArea();
        if (!bars.isEmpty() && mouseY >= plot.y - 20 && mouseY <= plot.y + plot.height + PAD_BOTTOM
                && mouseX >= plot.x && mouseX <= plot.x + plot.width) {
            found = Math.min(bars.size() - 1, (int) ((mouseX - plot.x) / slotWidth()));
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
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, PAD_LEFT, 24);
        if (bars.isEmpty()) {
            drawEmptyMessage(g2);
        } else {
            drawGrid(g2);
            drawBars(g2);
            drawLabels(g2);
            drawTooltip(g2);
        }
        g2.dispose();
    }

    private void drawEmptyMessage(Graphics2D g2) {
        Rectangle plot = plotArea();
        g2.setFont(getFont().deriveFont(13f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(emptyMessage, plot.x + (plot.width - metrics.stringWidth(emptyMessage)) / 2,
                plot.y + plot.height / 2);
    }

    private void drawGrid(Graphics2D g2) {
        Rectangle plot = plotArea();
        double max = axisMax();
        g2.setFont(getFont().deriveFont(11f));
        FontMetrics metrics = g2.getFontMetrics();
        for (int i = 0; i <= GRID_LINES; i++) {
            int y = plot.y + plot.height * i / GRID_LINES;
            g2.setColor(Theme.SURFACE_ALT);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(plot.x, y, plot.x + plot.width, y);
            String text = formatter.apply(max - max * i / GRID_LINES);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(text, plot.x - 8 - metrics.stringWidth(text), y + 4);
        }
    }

    private void drawBars(Graphics2D g2) {
        Rectangle plot = plotArea();
        double max = axisMax();
        for (int i = 0; i < bars.size(); i++) {
            double height = plot.height * bars.get(i).value() / max * growth(i);
            if (height < 1) {
                continue;
            }
            Color color = (hoverIndex == -1 || hoverIndex == i) ? barColor : dim(barColor);
            g2.setColor(color);
            double radius = Math.min(10, barWidth() / 2);
            double y = plot.y + plot.height - height;
            // The rounded rectangle is drawn taller and clipped, so only the top corners are round.
            Graphics2D clipped = (Graphics2D) g2.create();
            clipped.clipRect((int) barX(i) - 1, plot.y, (int) barWidth() + 2, plot.height);
            clipped.fill(new RoundRectangle2D.Double(barX(i), y, barWidth(), height + radius, radius * 2, radius * 2));
            clipped.dispose();
        }
    }

    private void drawLabels(Graphics2D g2) {
        Rectangle plot = plotArea();
        g2.setFont(getFont().deriveFont(11f));
        FontMetrics metrics = g2.getFontMetrics();
        for (int i = 0; i < bars.size(); i++) {
            String text = bars.get(i).label();
            g2.setColor(i == hoverIndex ? Theme.TEXT : Theme.TEXT_MUTED);
            double center = barX(i) + barWidth() / 2;
            g2.drawString(text, (int) (center - metrics.stringWidth(text) / 2.0), plot.y + plot.height + 22);
        }
    }

    private void drawTooltip(Graphics2D g2) {
        if (hoverIndex < 0) {
            return;
        }
        Rectangle plot = plotArea();
        Bar bar = bars.get(hoverIndex);
        String valueText = formatter.apply(bar.value());
        Font labelFont = getFont().deriveFont(11f);
        Font valueFont = getFont().deriveFont(Font.BOLD, 14f);
        int width = Math.max(g2.getFontMetrics(labelFont).stringWidth(bar.label()),
                g2.getFontMetrics(valueFont).stringWidth(valueText)) + 24;
        int height = 46;
        double barTop = plot.y + plot.height - plot.height * bar.value() / axisMax() * growth(hoverIndex);
        int x = (int) (barX(hoverIndex) + barWidth() / 2 - width / 2.0);
        x = Math.max(4, Math.min(x, getWidth() - width - 4));
        int y = (int) barTop - height - 10;
        if (y < 4) {
            y = 4;
        }
        g2.setColor(Theme.SURFACE_ALT);
        g2.fillRoundRect(x, y, width, height, 12, 12);
        g2.setColor(barColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, width, height, 12, 12);
        g2.setFont(labelFont);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(bar.label(), x + 12, y + 18);
        g2.setFont(valueFont);
        g2.setColor(Theme.TEXT);
        g2.drawString(valueText, x + 12, y + 37);
    }

    private static Color dim(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
    }
}

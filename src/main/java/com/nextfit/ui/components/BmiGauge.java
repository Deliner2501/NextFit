package com.nextfit.ui.components;

import com.nextfit.model.BmiCategory;
import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * Color bar that shows where a body mass index falls: blue for low weight,
 * green for normal weight, orange for overweight and red for obesity. A
 * marker points at the position of the member.
 */
public class BmiGauge extends JComponent {

    private static final double SCALE_MIN = 15.0;
    private static final double SCALE_MAX = 40.0;
    private static final double[] LIMITS = {18.5, 25.0, 30.0};
    private static final Color[] SEGMENT_COLORS = {
            Color.decode("#3DA5FF"), Theme.SUCCESS, Theme.WARNING, Theme.DANGER};
    private static final int BAR_Y = 46;
    private static final int BAR_HEIGHT = 14;
    private static final int SIDE_MARGIN = 10;

    private Double bmi;

    /** Creates a gauge without data. */
    public BmiGauge() {
        setPreferredSize(new Dimension(420, 96));
        setOpaque(false);
    }

    /**
     * Sets the index to show.
     *
     * @param value the body mass index, or null when it cannot be calculated yet
     */
    public void setBmi(Double value) {
        this.bmi = value;
        repaint();
    }

    private int xOf(double value) {
        double clamped = Math.max(SCALE_MIN, Math.min(SCALE_MAX, value));
        int barWidth = getWidth() - 2 * SIDE_MARGIN;
        return SIDE_MARGIN + (int) (barWidth * (clamped - SCALE_MIN) / (SCALE_MAX - SCALE_MIN));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawCaption(g2);
        drawBar(g2);
        drawLimits(g2);
        if (bmi != null) {
            drawMarker(g2);
        }
        g2.dispose();
    }

    private void drawCaption(Graphics2D g2) {
        if (bmi == null) {
            g2.setFont(getFont().deriveFont(13f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("Guarda la estatura y registra el peso para ver el IMC", SIDE_MARGIN, 18);
            return;
        }
        BmiCategory category = BmiCategory.fromBmi(bmi);
        String value = "IMC " + String.format(Locale.ROOT, "%.1f", bmi);
        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(Theme.TEXT);
        g2.drawString(value, SIDE_MARGIN, 22);
        int valueWidth = g2.getFontMetrics().stringWidth(value);
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.setColor(SEGMENT_COLORS[category.ordinal()]);
        g2.drawString(category.getLabel(), SIDE_MARGIN + valueWidth + 12, 22);
    }

    private void drawBar(Graphics2D g2) {
        int barWidth = getWidth() - 2 * SIDE_MARGIN;
        Graphics2D clipped = (Graphics2D) g2.create();
        clipped.setClip(new RoundRectangle2D.Double(SIDE_MARGIN, BAR_Y, barWidth, BAR_HEIGHT, 14, 14));
        double[] edges = {SCALE_MIN, LIMITS[0], LIMITS[1], LIMITS[2], SCALE_MAX};
        for (int i = 0; i < SEGMENT_COLORS.length; i++) {
            clipped.setColor(SEGMENT_COLORS[i]);
            clipped.fillRect(xOf(edges[i]), BAR_Y, xOf(edges[i + 1]) - xOf(edges[i]), BAR_HEIGHT);
        }
        clipped.dispose();
    }

    private void drawLimits(Graphics2D g2) {
        g2.setFont(getFont().deriveFont(11f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        for (double limit : LIMITS) {
            String text = String.format(Locale.ROOT, "%.1f", limit).replace(".0", "");
            g2.drawString(text, xOf(limit) - metrics.stringWidth(text) / 2, BAR_Y + BAR_HEIGHT + 18);
        }
    }

    private void drawMarker(Graphics2D g2) {
        int x = xOf(bmi);
        g2.setColor(Theme.TEXT);
        g2.fill(new Polygon(new int[]{x - 7, x + 7, x}, new int[]{BAR_Y - 12, BAR_Y - 12, BAR_Y - 2}, 3));
        g2.fillRect(x - 1, BAR_Y - 2, 3, BAR_HEIGHT + 4);
    }
}
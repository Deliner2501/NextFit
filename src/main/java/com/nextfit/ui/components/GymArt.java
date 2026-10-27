package com.nextfit.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small vector drawings of gym equipment (a dumbbell and barbell plates) that
 * screens use as decoration. They only draw; each screen decides the place,
 * size and color so the same shapes can be reused on the sign-in window and on
 * the home banner.
 */
public final class GymArt {

    private GymArt() {
    }

    /**
     * Draws a dumbbell seen from the side.
     *
     * @param g2     the surface to draw on
     * @param cx     horizontal center
     * @param cy     vertical center
     * @param length total length from one end to the other
     * @param angle  rotation in radians around the center
     * @param color  main color; the edge uses a brighter shade
     * @param alpha  opacity from 0 to 255
     */
    public static void dumbbell(Graphics2D g2, double cx, double cy, double length, double angle,
                                Color color, int alpha) {
        AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(angle);
        double barHeight = length * 0.045;
        double bigHeight = length * 0.36;
        double smallHeight = length * 0.26;
        double plateWidth = length * 0.075;
        double gap = length * 0.012;

        Color fill = withAlpha(color, alpha);
        Color edge = withAlpha(brighter(color), Math.min(255, alpha + 60));
        g2.setStroke(new BasicStroke(Math.max(1.5f, (float) (length * 0.006))));

        // Handle between the two weights.
        part(g2, new RoundRectangle2D.Double(-length * 0.30, -barHeight / 2, length * 0.60, barHeight,
                barHeight, barHeight), fill, edge, barHeight);
        for (int side = -1; side <= 1; side += 2) {
            double inner = side * length * 0.30;
            // Small plate next to the handle, big plate outside it, then the tip.
            double x1 = side < 0 ? inner - plateWidth : inner;
            part(g2, new RoundRectangle2D.Double(x1, -smallHeight / 2, plateWidth, smallHeight,
                    plateWidth * 0.6, plateWidth * 0.6), fill, edge, smallHeight);
            double outer = inner + side * (plateWidth + gap);
            double x2 = side < 0 ? outer - plateWidth * 1.3 : outer;
            part(g2, new RoundRectangle2D.Double(x2, -bigHeight / 2, plateWidth * 1.3, bigHeight,
                    plateWidth * 0.7, plateWidth * 0.7), fill, edge, bigHeight);
            double tip = outer + side * (plateWidth * 1.3 + gap);
            double x3 = side < 0 ? tip - length * 0.05 : tip;
            part(g2, new RoundRectangle2D.Double(x3, -barHeight * 0.7, length * 0.05, barHeight * 1.4,
                    barHeight, barHeight), fill, edge, barHeight);
        }
        g2.setTransform(saved);
    }

    /**
     * Draws concentric rings like the side of a barbell plate.
     *
     * @param g2     the surface to draw on
     * @param cx     horizontal center
     * @param cy     vertical center
     * @param radius radius of the outer ring
     * @param rings  how many rings to draw
     * @param color  ring color
     * @param alpha  opacity of the outer ring; inner rings fade
     */
    public static void plate(Graphics2D g2, double cx, double cy, double radius, int rings,
                             Color color, int alpha) {
        g2.setStroke(new BasicStroke(Math.max(2f, (float) (radius * 0.035))));
        for (int i = 0; i < rings; i++) {
            double r = radius * (1 - i * 0.7 / Math.max(1, rings));
            g2.setColor(withAlpha(color, Math.max(4, alpha - i * alpha / (rings + 1))));
            g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
        double hub = radius * 0.1;
        g2.setColor(withAlpha(color, alpha));
        g2.fill(new Ellipse2D.Double(cx - hub, cy - hub, hub * 2, hub * 2));
    }

    private static void part(Graphics2D g2, RoundRectangle2D shape, Color fill, Color edge, double extent) {
        g2.setPaint(new GradientPaint(0, (float) -extent / 2, brighterAlpha(fill), 0, (float) extent / 2, fill));
        g2.fill(shape);
        g2.setColor(edge);
        g2.draw(shape);
    }

    private static Color brighterAlpha(Color color) {
        Color lighter = brighter(color);
        return new Color(lighter.getRed(), lighter.getGreen(), lighter.getBlue(), color.getAlpha());
    }

    private static Color brighter(Color color) {
        return new Color(Math.min(255, color.getRed() + 60), Math.min(255, color.getGreen() + 60),
                Math.min(255, color.getBlue() + 60));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}

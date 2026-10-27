package com.nextfit.ui.components;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Vector icons drawn with Java2D on a 24 x 24 grid. They scale to any size,
 * take any color and need no image files.
 */
public final class Icons {

    private Icons() {
    }

    /**
     * Creates an icon.
     *
     * @param name  one of dashboard, members, payments, training, shield, logout, menu, chart
     * @param size  width and height in pixels
     * @param color the color of the strokes
     * @return the icon; an unknown name gives a plain circle
     */
    public static Icon of(String name, int size, Color color) {
        return new VectorIcon(name, size, color);
    }

    private static final class VectorIcon implements Icon {

        private final String name;
        private final int size;
        private final Color color;

        VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.scale(size / 24.0, size / 24.0);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g2);
            } finally {
                g2.dispose();
            }
        }

        private void draw(Graphics2D g) {
            switch (name) {
                case "dashboard" -> {
                    double[][] cells = {{3, 3}, {14, 3}, {3, 14}, {14, 14}};
                    for (double[] cell : cells) {
                        g.draw(new RoundRectangle2D.Double(cell[0], cell[1], 7, 7, 3, 3));
                    }
                }
                case "members" -> {
                    g.draw(new Ellipse2D.Double(8, 3, 8, 8));
                    g.draw(new Arc2D.Double(4, 14, 16, 16, 0, 180, Arc2D.OPEN));
                }
                case "payments" -> {
                    g.draw(new RoundRectangle2D.Double(2, 5, 20, 14, 3, 3));
                    g.draw(new Line2D.Double(2, 10, 22, 10));
                    g.draw(new Line2D.Double(6, 15, 10, 15));
                }
                case "training" -> {
                    g.fill(new RoundRectangle2D.Double(2, 9, 3, 6, 1.5, 1.5));
                    g.fill(new RoundRectangle2D.Double(5, 6, 3, 12, 1.5, 1.5));
                    g.fill(new RoundRectangle2D.Double(16, 6, 3, 12, 1.5, 1.5));
                    g.fill(new RoundRectangle2D.Double(19, 9, 3, 6, 1.5, 1.5));
                    g.draw(new Line2D.Double(8, 12, 16, 12));
                }
                case "shield" -> {
                    Path2D shield = new Path2D.Double();
                    shield.moveTo(12, 2);
                    shield.lineTo(20, 5);
                    shield.lineTo(20, 12);
                    shield.quadTo(20, 18, 12, 22);
                    shield.quadTo(4, 18, 4, 12);
                    shield.lineTo(4, 5);
                    shield.closePath();
                    g.draw(shield);
                    Path2D check = new Path2D.Double();
                    check.moveTo(8.5, 12);
                    check.lineTo(11, 14.5);
                    check.lineTo(15.5, 9.5);
                    g.draw(check);
                }
                case "logout" -> {
                    Path2D door = new Path2D.Double();
                    door.moveTo(10, 4);
                    door.lineTo(5, 4);
                    door.lineTo(5, 20);
                    door.lineTo(10, 20);
                    g.draw(door);
                    g.draw(new Line2D.Double(10, 12, 20, 12));
                    Path2D head = new Path2D.Double();
                    head.moveTo(16, 8);
                    head.lineTo(20, 12);
                    head.lineTo(16, 16);
                    g.draw(head);
                }
                case "menu" -> {
                    g.draw(new Line2D.Double(4, 7, 20, 7));
                    g.draw(new Line2D.Double(4, 12, 20, 12));
                    g.draw(new Line2D.Double(4, 17, 20, 17));
                }
                case "chart" -> {
                    g.draw(new Line2D.Double(4, 20, 20, 20));
                    g.draw(new Line2D.Double(7, 20, 7, 12));
                    g.draw(new Line2D.Double(12, 20, 12, 6));
                    g.draw(new Line2D.Double(17, 20, 17, 15));
                }
                default -> g.draw(new Ellipse2D.Double(4, 4, 16, 16));
            }
        }
    }
}
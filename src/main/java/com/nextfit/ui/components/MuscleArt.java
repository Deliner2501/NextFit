package com.nextfit.ui.components;

import com.nextfit.model.MuscleGroup;
import com.nextfit.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Original illustrations of the muscle groups, drawn with vector shapes: a
 * simple figure with the worked area lit in a color of its own. They are used
 * when an exercise has no photo, so the catalog never shows empty gray blocks.
 *
 * <p>An illustration is requested with a stored path such as
 * {@code art:CHEST}, the same way a photo path is stored, so the screens that
 * show pictures do not need to know the difference.
 */
public final class MuscleArt {

    /** Start of the stored path that asks for an illustration instead of a file. */
    public static final String PREFIX = "art:";

    private static final int WIDTH = 640;
    private static final int HEIGHT = 400;
    private static final double FIGURE_HEIGHT = 380;
    private static final Color BODY = new Color(0x36404F);
    private static final Color BODY_EDGE = new Color(0x4A5568);
    private static final Map<MuscleGroup, BufferedImage> CACHE = new EnumMap<>(MuscleGroup.class);

    private MuscleArt() {
    }

    /**
     * Returns the stored path of the picture that represents a group: a photo
     * from the application resources when one was added for it (for example
     * {@code /images/exercises/back.jpg}), otherwise the drawn illustration.
     *
     * @param group the muscle group
     * @return the path, e.g. "art:CHEST"
     */
    public static String pathFor(MuscleGroup group) {
        String photo = "/images/exercises/" + group.name().toLowerCase() + ".jpg";
        return MuscleArt.class.getResource(photo) != null ? photo : PREFIX + group.name();
    }

    /**
     * Loads the illustration named by a stored path.
     *
     * @param path a path that starts with {@link #PREFIX}
     * @return the picture, or empty when the group does not exist
     */
    public static Optional<BufferedImage> fromPath(String path) {
        try {
            return Optional.of(render(MuscleGroup.valueOf(path.substring(PREFIX.length()))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Draws the illustration of a muscle group.
     *
     * @param group the muscle group
     * @return a 640 x 400 picture
     */
    public static synchronized BufferedImage render(MuscleGroup group) {
        return CACHE.computeIfAbsent(group, MuscleArt::draw);
    }

    private static Color tint(MuscleGroup group) {
        return switch (group) {
            case CHEST -> Theme.ACCENT;
            case BACK -> new Color(0x4DA3FF);
            case SHOULDERS -> Theme.WARNING;
            case ARMS -> Theme.ACCENT_ALT;
            case CORE -> new Color(0xA66BFF);
            case LEGS -> Theme.SUCCESS;
            case GLUTES -> new Color(0xFF5FA2);
            case FULL_BODY -> Theme.ACCENT;
            case CARDIO -> Theme.DANGER;
        };
    }

    private static BufferedImage draw(MuscleGroup group) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            Color color = tint(group);
            paintBackground(g2, color);
            if (group == MuscleGroup.CARDIO) {
                paintCardio(g2, color);
            } else {
                double scale = HEIGHT * 0.92 / FIGURE_HEIGHT;
                g2.translate(WIDTH / 2.0, HEIGHT * 0.04);
                g2.scale(scale, scale);
                paintBody(g2);
                paintHighlight(g2, group, color);
            }
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static void paintBackground(Graphics2D g2, Color color) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0x0B0D11), WIDTH, HEIGHT, mix(Theme.BACKGROUND, color, 0.22)));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setPaint(new RadialGradientPaint(WIDTH / 2f, HEIGHT / 2f, 260, new float[]{0f, 1f},
                new Color[]{alpha(color, 70), alpha(color, 0)}));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        // Rings like the plates of a barbell, in two corners.
        g2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 4; i++) {
            g2.setColor(alpha(color, 34 - i * 6));
            int r = 70 + i * 34;
            g2.draw(new Ellipse2D.Double(90 - r, 330 - r, r * 2, r * 2));
            g2.draw(new Ellipse2D.Double(560 - r, 60 - r, r * 2, r * 2));
        }
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(alpha(color, 26));
        for (int i = 0; i < 5; i++) {
            g2.draw(new Line2D.Double(30 + i * 24, 0, -90 + i * 24, 150));
        }
    }

    private static void paintBody(Graphics2D g2) {
        g2.setColor(BODY);
        g2.fill(new Ellipse2D.Double(-20, 5, 40, 46));
        limb(g2, 0, 52, 0, 66, 16, BODY);
        g2.setColor(BODY);
        g2.fill(torso());
        g2.setColor(BODY_EDGE);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(torso());
        for (int side = -1; side <= 1; side += 2) {
            g2.setColor(BODY);
            g2.fill(new Ellipse2D.Double(side * 66 - 19, 65, 38, 38));
            limb(g2, side * 70, 92, side * 86, 160, 26, BODY);
            limb(g2, side * 86, 162, side * 96, 228, 20, BODY);
            g2.setColor(BODY);
            g2.fill(new Ellipse2D.Double(side * 97 - 10, 228, 20, 22));
            limb(g2, side * 24, 224, side * 28, 296, 40, BODY);
            limb(g2, side * 28, 302, side * 30, 366, 30, BODY);
            g2.setColor(BODY);
            g2.fill(new RoundRectangle2D.Double(side * 30 - 17, 362, 34, 14, 10, 10));
        }
    }

    private static Path2D torso() {
        Path2D path = new Path2D.Double();
        path.moveTo(-58, 72);
        path.quadTo(0, 58, 58, 72);
        path.lineTo(46, 150);
        path.quadTo(38, 185, 44, 218);
        path.lineTo(-44, 218);
        path.quadTo(-38, 185, -46, 150);
        path.closePath();
        return path;
    }

    private static void paintHighlight(Graphics2D g2, MuscleGroup group, Color color) {
        switch (group) {
            case CHEST -> {
                for (int side = -1; side <= 1; side += 2) {
                    glowShape(g2, new RoundRectangle2D.Double(side < 0 ? -50 : 2, 84, 48, 40, 26, 26), color);
                }
            }
            case SHOULDERS -> {
                for (int side = -1; side <= 1; side += 2) {
                    glowShape(g2, new Ellipse2D.Double(side * 66 - 19, 65, 38, 38), color);
                }
            }
            case ARMS -> {
                for (int side = -1; side <= 1; side += 2) {
                    glowLimb(g2, side * 70, 92, side * 86, 160, 26, color);
                    glowLimb(g2, side * 86, 162, side * 96, 228, 20, color);
                }
            }
            case CORE -> {
                for (int row = 0; row < 3; row++) {
                    for (int side = -1; side <= 1; side += 2) {
                        glowShape(g2, new RoundRectangle2D.Double(side < 0 ? -23 : 2, 132 + row * 28, 21, 24, 10, 10),
                                color);
                    }
                }
            }
            case LEGS -> {
                for (int side = -1; side <= 1; side += 2) {
                    glowLimb(g2, side * 24, 224, side * 28, 296, 40, color);
                    glowLimb(g2, side * 28, 302, side * 30, 366, 30, color);
                }
            }
            case BACK -> {
                paintSpine(g2);
                Path2D traps = new Path2D.Double();
                traps.moveTo(0, 64);
                traps.lineTo(-44, 80);
                traps.lineTo(0, 118);
                traps.lineTo(44, 80);
                traps.closePath();
                glowShape(g2, traps, color);
                for (int side = -1; side <= 1; side += 2) {
                    Path2D lat = new Path2D.Double();
                    lat.moveTo(side * 8, 124);
                    lat.quadTo(side * 54, 100, side * 50, 128);
                    lat.quadTo(side * 40, 168, side * 14, 196);
                    lat.quadTo(side * 6, 160, side * 8, 124);
                    glowShape(g2, lat, color);
                }
            }
            case GLUTES -> {
                paintSpine(g2);
                for (int side = -1; side <= 1; side += 2) {
                    glowShape(g2, new Ellipse2D.Double(side < 0 ? -48 : 2, 204, 46, 50), color);
                }
            }
            case FULL_BODY -> {
                glowShape(g2, torso(), color);
                for (int side = -1; side <= 1; side += 2) {
                    glowShape(g2, new Ellipse2D.Double(side * 66 - 19, 65, 38, 38), color);
                    glowLimb(g2, side * 70, 92, side * 86, 160, 26, color);
                    glowLimb(g2, side * 86, 162, side * 96, 228, 20, color);
                    glowLimb(g2, side * 24, 224, side * 28, 296, 40, color);
                    glowLimb(g2, side * 28, 302, side * 30, 366, 30, color);
                }
            }
            default -> {
            }
        }
    }

    private static void paintSpine(Graphics2D g2) {
        g2.setColor(BODY_EDGE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 9f}, 0f));
        g2.draw(new Line2D.Double(0, 66, 0, 216));
    }

    private static void paintCardio(Graphics2D g2, Color color) {
        AffineTransform saved = g2.getTransform();
        g2.translate(WIDTH / 2.0, HEIGHT / 2.0 - 10);
        Path2D heart = new Path2D.Double();
        heart.moveTo(0, 90);
        heart.curveTo(-150, -10, -75, -105, 0, -42);
        heart.curveTo(75, -105, 150, -10, 0, 90);
        heart.closePath();
        g2.setPaint(new GradientPaint(0, -90, mix(color, Color.WHITE, 0.25), 0, 90, mix(color, Color.BLACK, 0.45)));
        g2.fill(heart);
        g2.setTransform(saved);
        // Heartbeat line across the picture.
        Path2D pulse = new Path2D.Double();
        pulse.moveTo(30, 232);
        pulse.lineTo(230, 232);
        pulse.lineTo(262, 232);
        pulse.lineTo(284, 168);
        pulse.lineTo(312, 290);
        pulse.lineTo(338, 196);
        pulse.lineTo(356, 232);
        pulse.lineTo(610, 232);
        g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(alpha(Color.WHITE, 40));
        g2.draw(pulse);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.TEXT);
        g2.draw(pulse);
    }

    private static void limb(Graphics2D g2, double x1, double y1, double x2, double y2, double width, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void glowLimb(Graphics2D g2, double x1, double y1, double x2, double y2, double width, Color color) {
        limb(g2, x1, y1, x2, y2, width + 12, alpha(color, 55));
        limb(g2, x1, y1, x2, y2, width, color);
        limb(g2, x1 - width * 0.18, y1, x2 - width * 0.18, y2, width * 0.28, alpha(Color.WHITE, 60));
    }

    private static void glowShape(Graphics2D g2, java.awt.Shape shape, Color color) {
        g2.setColor(alpha(color, 55));
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        java.awt.Rectangle bounds = shape.getBounds();
        g2.setPaint(new GradientPaint(bounds.x, bounds.y, mix(color, Color.WHITE, 0.30),
                bounds.x, bounds.y + Math.max(1, bounds.height), color));
        g2.fill(shape);
    }

    private static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static Color mix(Color a, Color b, double amount) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }
}

package com.nextfit.ui;

import com.nextfit.model.User;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * Small badge of the signed-in user: a circle with their initials, their name
 * and their role.
 */
public class UserChip extends JComponent {

    private static final int AVATAR = 40;
    private static final int GAP = 12;

    private final String initials;
    private final String name;
    private final String role;

    /**
     * Creates the badge.
     *
     * @param user the signed-in user
     */
    public UserChip(User user) {
        this.name = user.getFullName();
        this.role = user.getRole().getLabel();
        this.initials = initialsOf(user.getFullName());
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics bold = getFontMetrics(getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics plain = getFontMetrics(getFont().deriveFont(Font.PLAIN, 12f));
        int text = Math.max(bold.stringWidth(name), plain.stringWidth(role));
        return new Dimension(AVATAR + GAP + text + 4, AVATAR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = (getHeight() - AVATAR) / 2;
            g2.setColor(Theme.ACCENT);
            g2.fill(new Ellipse2D.Double(0, y, AVATAR, AVATAR));

            g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
            g2.setColor(Theme.BACKGROUND);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (AVATAR - fm.stringWidth(initials)) / 2,
                    y + (AVATAR + fm.getAscent() - fm.getDescent()) / 2);

            int textX = AVATAR + GAP;
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(Theme.TEXT);
            g2.drawString(name, textX, y + 17);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(role, textX, y + 33);
        } finally {
            g2.dispose();
        }
    }

    private static String initialsOf(String fullName) {
        String[] words = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length && sb.length() < 2; i++) {
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }
        return sb.toString();
    }
}
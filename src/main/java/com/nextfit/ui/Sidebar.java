package com.nextfit.ui;

import com.nextfit.ui.components.Icons;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Side menu of the main window. It lists the modules the user may open, marks
 * the active one with the accent color and collapses to icons with a short
 * animation.
 */
public class Sidebar extends JPanel {

    private static final int EXPANDED_WIDTH = 248;
    private static final int COLLAPSED_WIDTH = 76;
    private static final int TEXT_VISIBLE_FROM = 150;
    private static final int ITEM_HEIGHT = 46;
    private static final int ICON_SIZE = 22;
    private static final int ICON_X = (COLLAPSED_WIDTH - ICON_SIZE) / 2;

    private final List<NavButton> buttons = new ArrayList<>();
    private final Timer animation;
    private final NavButton toggle;
    private double currentWidth = EXPANDED_WIDTH;
    private int targetWidth = EXPANDED_WIDTH;
    private boolean expanded = true;

    /**
     * Creates the menu.
     *
     * @param modules  the modules to list, already filtered for the user
     * @param onSelect called with the module the user clicks
     */
    public Sidebar(List<ModulePanel> modules, Consumer<ModulePanel> onSelect) {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setOpaque(true);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        for (ModulePanel module : modules) {
            NavButton button = new NavButton(module.title(), module.icon());
            button.addActionListener(e -> onSelect.accept(module));
            button.putClientProperty("module", module);
            buttons.add(button);
            list.add(button);
        }

        toggle = new NavButton("Contraer menú", "menu");
        toggle.addActionListener(e -> toggle());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        footer.add(toggle, BorderLayout.CENTER);

        add(new Header(), BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        animation = new Timer(12, e -> step());
    }

    /**
     * Highlights the button of a module.
     *
     * @param module the active module
     */
    public void select(ModulePanel module) {
        for (NavButton button : buttons) {
            button.markActive(button.getClientProperty("module") == module);
        }
    }

    private void toggle() {
        expanded = !expanded;
        targetWidth = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        toggle.changeLabel(expanded ? "Contraer menú" : "Expandir menú");
        animation.start();
    }

    private void step() {
        double delta = targetWidth - currentWidth;
        if (Math.abs(delta) < 1) {
            currentWidth = targetWidth;
            animation.stop();
        } else {
            currentWidth += delta * 0.25;
        }
        setPreferredSize(new Dimension((int) Math.round(currentWidth), 0));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.SURFACE_ALT);
        g.fillRect(getWidth() - 1, 0, 1, getHeight());
    }

    /** Logo and name at the top of the menu. */
    private static final class Header extends JComponent {

        Header() {
            setPreferredSize(new Dimension(0, 84));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int size = 40;
                int x = (COLLAPSED_WIDTH - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(Theme.ACCENT);
                g2.fill(new RoundRectangle2D.Double(x, y, size, size, 14, 14));
                Icons.of("training", 22, Theme.BACKGROUND).paintIcon(this, g2, x + 9, y + 9);
                if (getWidth() >= TEXT_VISIBLE_FROM) {
                    g2.setColor(Theme.TEXT);
                    g2.setFont(getFont().deriveFont(Font.BOLD, 22f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("NextFit", x + size + 14, y + (size + fm.getAscent() - fm.getDescent()) / 2);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** A menu entry painted by hand: icon, label, hover and selected states. */
    private static final class NavButton extends JButton {

        private final String icon;
        private String label;
        private boolean selected;
        private boolean hover;

        NavButton(String label, String icon) {
            this.label = label;
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, ITEM_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT));
            setAlignmentX(LEFT_ALIGNMENT);
            getAccessibleContext().setAccessibleName(label);
            setToolTipText(label);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void markActive(boolean active) {
            this.selected = active;
            repaint();
        }

        void changeLabel(String label) {
            this.label = label;
            setToolTipText(label);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (selected) {
                    g2.setColor(withAlpha(Theme.ACCENT, 36));
                    g2.fill(new RoundRectangle2D.Double(8, 2, w - 16, h - 4, 14, 14));
                    g2.setColor(Theme.ACCENT);
                    g2.fill(new RoundRectangle2D.Double(0, h * 0.25, 4, h * 0.5, 4, 4));
                } else if (hover) {
                    g2.setColor(Theme.SURFACE_ALT);
                    g2.fill(new RoundRectangle2D.Double(8, 2, w - 16, h - 4, 14, 14));
                }
                Color color = selected ? Theme.ACCENT : hover ? Theme.TEXT : Theme.TEXT_MUTED;
                Icons.of(icon, ICON_SIZE, color).paintIcon(this, g2, ICON_X, (h - ICON_SIZE) / 2);
                if (w >= TEXT_VISIBLE_FROM) {
                    g2.setColor(color);
                    g2.setFont(getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN, 15f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(label, ICON_X + ICON_SIZE + 16, (h + fm.getAscent() - fm.getDescent()) / 2);
                }
            } finally {
                g2.dispose();
            }
        }

        private static Color withAlpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
    }
}
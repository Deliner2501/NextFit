package com.nextfit.ui;

import com.nextfit.model.Role;
import com.nextfit.ui.components.Icons;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Temporary screen for a module that is not built yet. It keeps the menu
 * complete while each team member replaces their placeholder with the real
 * panel.
 */
public class PlaceholderPanel implements ModulePanel {

    private final String id;
    private final String title;
    private final String icon;
    private final Set<Role> allowedRoles = EnumSet.noneOf(Role.class);
    private final JPanel view = new JPanel(new GridBagLayout());

    /**
     * Creates the placeholder.
     *
     * @param id          the module identifier
     * @param title       the text shown in the menu
     * @param icon        the icon name (see {@code Icons})
     * @param description what the module will do
     * @param roles       the roles that may open it; none means every role
     */
    public PlaceholderPanel(String id, String title, String icon, String description, Role... roles) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.allowedRoles.addAll(Arrays.asList(roles));

        view.setBackground(Theme.BACKGROUND);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel(Icons.of(icon, 72, Theme.SURFACE_ALT));
        iconLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN, 14f));
        descriptionLabel.setForeground(Theme.TEXT_MUTED);
        descriptionLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel soon = new JLabel("Próximamente");
        soon.setFont(soon.getFont().deriveFont(Font.BOLD, 12f));
        soon.setForeground(Theme.ACCENT);
        soon.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        box.add(iconLabel);
        box.add(javax.swing.Box.createVerticalStrut(18));
        box.add(titleLabel);
        box.add(javax.swing.Box.createVerticalStrut(6));
        box.add(descriptionLabel);
        box.add(javax.swing.Box.createVerticalStrut(14));
        box.add(soon);
        box.setBorder(BorderFactory.createEmptyBorder());
        view.add(box);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String icon() {
        return icon;
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public boolean allowedFor(Role role) {
        return allowedRoles.isEmpty() || allowedRoles.contains(role);
    }
}
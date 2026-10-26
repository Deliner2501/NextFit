package com.nextfit.ui;

import com.nextfit.model.Role;
import com.nextfit.ui.components.HeroBanner;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Adds a photo banner with a title above any {@link ModulePanel}. It forwards
 * every call to the module it wraps, so the module does not change and the
 * main window cannot tell the difference.
 */
public class BannerModule implements ModulePanel {

    private static final int BANNER_HEIGHT = 120;

    private final ModulePanel inner;
    private final JPanel view = new JPanel(new BorderLayout());

    /**
     * Creates the wrapper.
     *
     * @param inner     the module that is shown under the banner
     * @param title     big text over the photo
     * @param subtitle  smaller text under the title
     * @param photoPath stored path of the photo
     * @param focusY    vertical point of the photo to keep, from 0 (top) to 1 (bottom)
     */
    public BannerModule(ModulePanel inner, String title, String subtitle, String photoPath, double focusY) {
        this.inner = inner;
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 28f));
        titleLabel.setForeground(Theme.TEXT);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);
        JPanel texts = new JPanel(new java.awt.GridLayout(2, 1));
        texts.add(titleLabel);
        texts.add(subtitleLabel);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(20, 28, 0, 28));
        top.add(new HeroBanner(texts, photoPath, 0.5, focusY, BANNER_HEIGHT), BorderLayout.CENTER);
        view.setBackground(Theme.BACKGROUND);
        view.add(top, BorderLayout.NORTH);
        view.add(inner.view(), BorderLayout.CENTER);
    }

    @Override
    public String id() {
        return inner.id();
    }

    @Override
    public String title() {
        return inner.title();
    }

    @Override
    public String icon() {
        return inner.icon();
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        inner.onShow();
    }

    @Override
    public boolean allowedFor(Role role) {
        return inner.allowedFor(role);
    }
}

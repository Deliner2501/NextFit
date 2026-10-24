package com.nextfit;

import com.nextfit.ui.Theme;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Application entry point. Repositories and services are wired here and only here.
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts the application.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("NextFit");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 800);
            frame.setLocationRelativeTo(null);
            frame.add(new JLabel("NextFit", SwingConstants.CENTER));
            frame.setVisible(true);
        });
    }
}
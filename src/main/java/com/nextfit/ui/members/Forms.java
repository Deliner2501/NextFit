package com.nextfit.ui.members;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Small helpers shared by the tabs of the member record, so every tab looks
 * the same and the code that builds them stays short.
 */
final class Forms {

    private Forms() {
    }

    static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    static JLabel messageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(label.getFont().deriveFont(12f));
        return label;
    }

    static void showError(JLabel label, String text) {
        label.setForeground(Theme.DANGER);
        label.setText(text);
    }

    static void showSuccess(JLabel label, String text) {
        label.setForeground(Theme.SUCCESS);
        label.setText(text);
    }

    static JButton accentButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Theme.ACCENT);
        button.setForeground(Theme.BACKGROUND);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setPreferredSize(new Dimension(Math.max(140, text.length() * 9 + 24), 36));
        return button;
    }

    /** Reads a decimal number typed by the user; accepts a comma or a dot. */
    static double parseNumber(String field, String text) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new InvalidDataException(field, "debe ser un número");
        }
    }

    static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SURFACE_ALT);
        table.setSelectionForeground(Theme.TEXT);
        table.getTableHeader().setBackground(Theme.SURFACE_ALT);
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setReorderingAllowed(false);
    }

    static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }
}
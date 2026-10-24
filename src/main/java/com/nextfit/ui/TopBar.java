package com.nextfit.ui;

import com.nextfit.model.User;
import com.nextfit.ui.components.Icons;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Bar at the top of the main window: the name of the open screen, today's
 * date, the signed-in user and the sign-out button.
 */
public class TopBar extends JPanel {

    private final JLabel titleLabel = new JLabel();

    /**
     * Creates the bar.
     *
     * @param user     the signed-in user
     * @param onLogout what to do when the user asks to sign out
     */
    public TopBar(User user, Runnable onLogout) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(Theme.TEXT);
        JLabel dateLabel = new JLabel(todayText());
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 13f));
        dateLabel.setForeground(Theme.TEXT_MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLabel);
        left.add(dateLabel);

        JButton logout = new JButton(Icons.of("logout", 22, Theme.TEXT_MUTED));
        logout.setToolTipText("Cerrar sesión");
        logout.getAccessibleContext().setAccessibleName("Cerrar sesión");
        logout.putClientProperty("JButton.buttonType", "toolBar");
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setPreferredSize(new Dimension(44, 44));
        logout.addActionListener(e -> onLogout.run());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        right.add(new UserChip(user));
        right.add(logout);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    /**
     * Changes the title shown at the left.
     *
     * @param title the name of the open screen
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    private static String todayText() {
        String text = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.of("es", "CO")));
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
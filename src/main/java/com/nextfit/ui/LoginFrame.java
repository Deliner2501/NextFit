package com.nextfit.ui;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.User;
import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import com.nextfit.ui.components.BrandPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sign-in window. It only collects the credentials and shows the result;
 * the rules live in {@link AuthService}. The password check runs in a
 * background thread so the window never freezes.
 */
public class LoginFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());
    private static final int FORM_WIDTH = 360;

    private final AuthService auth;
    private final Session session;
    private final Runnable onSuccess;

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Ingresar");

    /**
     * Creates the window.
     *
     * @param auth      the sign-in rules
     * @param session   the session that is started when the credentials are correct
     * @param onSuccess what to do after a successful sign-in (open the main window)
     */
    public LoginFrame(AuthService auth, Session session, Runnable onSuccess) {
        super("NextFit · Iniciar sesión");
        this.auth = auth;
        this.session = session;
        this.onSuccess = onSuccess;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new BrandPanel(), BorderLayout.WEST);
        add(buildFormSide(), BorderLayout.CENTER);
        setSize(1040, 660);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Theme.SURFACE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(FORM_WIDTH, 470));

        JLabel title = new JLabel("Bienvenido");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel("Ingresa con tu cuenta para gestionar el gimnasio");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(Theme.TEXT_MUTED);

        usernameField.putClientProperty("JTextField.placeholderText", "Tu nombre de usuario");
        usernameField.putClientProperty("JTextField.padding", new Insets(10, 14, 10, 14));
        passwordField.putClientProperty("JTextField.placeholderText", "Tu contraseña");
        passwordField.putClientProperty("JTextField.padding", new Insets(10, 14, 10, 14));
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        usernameField.setFont(usernameField.getFont().deriveFont(15f));
        passwordField.setFont(passwordField.getFont().deriveFont(15f));
        usernameField.setPreferredSize(new Dimension(FORM_WIDTH, 46));
        passwordField.setPreferredSize(new Dimension(FORM_WIDTH, 46));

        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 13f));

        loginButton.setBackground(Theme.ACCENT);
        loginButton.setForeground(Theme.BACKGROUND);
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 15f));
        loginButton.setPreferredSize(new Dimension(FORM_WIDTH, 48));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> attemptLogin());

        JLabel hint = new JLabel("Primer ingreso: usuario admin · contraseña admin123");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));
        hint.setForeground(Theme.TEXT_MUTED);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;

        c.gridy = 0;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 32, 0);
        form.add(subtitle, c);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("Usuario"), c);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 18, 0);
        form.add(usernameField, c);
        c.gridy = 4;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("Contraseña"), c);
        c.gridy = 5;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(passwordField, c);
        c.gridy = 6;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(errorLabel, c);
        c.gridy = 7;
        c.insets = new Insets(0, 0, 22, 0);
        form.add(loginButton, c);
        c.gridy = 8;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(hint, c);

        side.add(form);
        return side;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(Theme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder());
        return label;
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        setBusy(true);
        errorLabel.setText(" ");
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return auth.login(username, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    User user = get();
                    session.start(user);
                    onSuccess.run();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Verificando…" : "Ingresar");
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
    }

    private void showError(Throwable cause) {
        if (cause instanceof NextFitException) {
            errorLabel.setText(cause.getMessage());
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected error while signing in", cause);
            errorLabel.setText("Ocurrió un error inesperado. Inténtalo de nuevo.");
        }
        passwordField.setText("");
        passwordField.requestFocusInWindow();
        shake();
    }

    private void shake() {
        Point origin = getLocation();
        int[] offsets = {-10, 10, -8, 8, -5, 5, -2, 2};
        int[] step = {0};
        Timer timer = new Timer(28, null);
        timer.addActionListener(e -> {
            if (step[0] >= offsets.length) {
                timer.stop();
                setLocation(origin);
            } else {
                setLocation(origin.x + offsets[step[0]++], origin.y);
            }
        });
        timer.start();
    }
}
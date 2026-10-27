package com.nextfit.ui.users;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.UserService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * Modal form to create a user or to edit the name and role of an existing one.
 * The sign-in name and the password are only asked when creating; a password
 * is replaced later with its own button. Every rule lives in {@link UserService}.
 */
class UserFormDialog extends JDialog {

    private final UserService service;
    private final User existing;     // null when a new user is being created
    private final Runnable onSaved;

    private final JTextField usernameField = new JTextField();
    private final JTextField fullNameField = new JTextField();
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");

    /**
     * Creates the dialog.
     *
     * @param owner    the window the dialog belongs to
     * @param service  the service used to save
     * @param existing the user to edit, or null to create a new one
     * @param onSaved  what to run after a successful save, to refresh the list
     */
    UserFormDialog(Window owner, UserService service, User existing, Runnable onSaved) {
        super(owner, existing == null ? "Nuevo usuario" : "Editar usuario", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.existing = existing;
        this.onSaved = onSaved;
        buildForm();
        fillFromExisting();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildForm() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        usernameField.putClientProperty("JTextField.placeholderText", "3 a 20 caracteres, sin espacios");
        fullNameField.putClientProperty("JTextField.placeholderText", "Ej. Laura Pérez");
        passwordField.putClientProperty("JTextField.placeholderText", "Mínimo 8, con letras y números");
        confirmField.putClientProperty("JTextField.placeholderText", "Repite la contraseña");
        roleBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof Role role ? role.getLabel() : "", index, selected, focus);
            }
        });
        usernameField.setPreferredSize(new Dimension(320, usernameField.getPreferredSize().height));
        errorLabel.setForeground(Theme.DANGER);

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = new JButton("Guardar");
        saveButton.setBackground(Theme.ACCENT);
        saveButton.setForeground(Theme.BACKGROUND);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(e -> save());
        getRootPane().setDefaultButton(saveButton);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints bc = new GridBagConstraints();
        bc.insets = new Insets(0, 8, 0, 0);
        buttons.add(cancelButton, bc);
        buttons.add(saveButton, bc);

        int row = 0;
        if (existing == null) {
            addRow(content, row++, "Usuario", usernameField);
        }
        addRow(content, row++, "Nombre completo", fullNameField);
        addRow(content, row++, "Rol", roleBox);
        if (existing == null) {
            addRow(content, row++, "Contraseña", passwordField);
            addRow(content, row++, "Confirmar", confirmField);
        }
        addRow(content, row++, "", errorLabel);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        content.add(buttons, c);
        setContentPane(content);
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 0, 6, 14);
        c.anchor = GridBagConstraints.WEST;
        JLabel text = new JLabel(label);
        text.setForeground(Theme.TEXT_MUTED);
        panel.add(text, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void fillFromExisting() {
        roleBox.setSelectedItem(existing == null ? Role.RECEPTION : existing.getRole());
        if (existing != null) {
            fullNameField.setText(existing.getFullName());
        }
    }

    private void save() {
        try {
            Role role = (Role) roleBox.getSelectedItem();
            if (existing == null) {
                String password = new String(passwordField.getPassword());
                if (!password.equals(new String(confirmField.getPassword()))) {
                    throw new InvalidDataException("contraseña", "las dos contraseñas no coinciden");
                }
                service.create(usernameField.getText().trim(), fullNameField.getText().trim(), password, role);
            } else {
                service.update(existing.getId(), fullNameField.getText().trim(), role);
            }
            onSaved.run();
            dispose();
        } catch (NextFitException e) {
            errorLabel.setText(e.getMessage());
            pack();
        }
    }
}

package com.nextfit.ui.training;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Trainer;
import com.nextfit.service.TrainerService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Modal form to register a new trainer or edit an existing one.
 * It builds a {@link Trainer} and hands it to {@link TrainerService}, which
 * validates it; errors are shown inside the form and the form stays open.
 */
public class TrainerFormDialog extends JDialog {

    private final TrainerService service;
    private final Trainer existing;     // null cuando se está registrando uno nuevo
    private final Runnable onSaved;

    private final JTextField nameField = new JTextField();
    private final JTextField documentField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField birthDateField = new JTextField();
    private final JTextField specialtyField = new JTextField();
    private final JLabel errorLabel = new JLabel(" ");

    /**
     * @param owner    window the dialog belongs to
     * @param service  trainer service used to save
     * @param existing the trainer to edit, or null to register a new one
     * @param onSaved  action to run after a successful save (to refresh the list)
     */
    public TrainerFormDialog(Window owner, TrainerService service, Trainer existing, Runnable onSaved) {
        super(owner, existing == null ? "Nuevo entrenador" : "Editar entrenador", ModalityType.APPLICATION_MODAL);
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

        nameField.putClientProperty("JTextField.placeholderText", "Nombre y apellido");
        documentField.putClientProperty("JTextField.placeholderText", "Solo números");
        phoneField.putClientProperty("JTextField.placeholderText", "Solo números");
        emailField.putClientProperty("JTextField.placeholderText", "Opcional");
        birthDateField.putClientProperty("JTextField.placeholderText", "AAAA-MM-DD");
        specialtyField.putClientProperty("JTextField.placeholderText", "Ej. Hipertrofia");
        nameField.setPreferredSize(new Dimension(320, nameField.getPreferredSize().height));
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
        addRow(content, row++, "Nombre completo", nameField);
        addRow(content, row++, "Documento", documentField);
        addRow(content, row++, "Teléfono", phoneField);
        addRow(content, row++, "Correo", emailField);
        addRow(content, row++, "Fecha de nacimiento", birthDateField);
        addRow(content, row++, "Especialidad", specialtyField);
        addRow(content, row++, "", errorLabel);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        content.add(buttons, c);
        setContentPane(content);
    }

    // Agrega una fila "etiqueta + control" al formulario.
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

    // Si se está editando, se cargan los valores actuales.
    private void fillFromExisting() {
        if (existing != null) {
            nameField.setText(existing.getFullName());
            documentField.setText(existing.getDocumentNumber());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            birthDateField.setText(existing.getBirthDate().toString());
            specialtyField.setText(existing.getSpecialty());
        }
    }

    // Arma el entrenador, lo guarda con el servicio y cierra; si hay error, lo muestra en el formulario.
    private void save() {
        try {
            LocalDate birthDate = parseBirthDate();
            Trainer trainer = new Trainer(
                    existing == null ? 0 : existing.getId(),
                    nameField.getText().trim(),
                    documentField.getText().trim(),
                    phoneField.getText().trim(),
                    emailField.getText().trim(),
                    birthDate,
                    specialtyField.getText().trim(),
                    existing == null ? "" : existing.getPhotoPath(),
                    existing == null || existing.isActive());
            if (existing == null) {
                service.register(trainer);
            } else {
                service.update(trainer);
            }
            onSaved.run();
            dispose();
        } catch (NextFitException e) {
            errorLabel.setText(e.getMessage());
            pack();
        }
    }

    // La fecha se escribe como AAAA-MM-DD; vacía o mal escrita se avisa con un mensaje claro.
    private LocalDate parseBirthDate() {
        String text = birthDateField.getText().trim();
        if (text.isEmpty()) {
            return null;   // el validador responde "es obligatoria"
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("fecha de nacimiento",
                    "escríbela como AAAA-MM-DD, por ejemplo 1990-05-24");
        }
    }
}
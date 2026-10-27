package com.nextfit.ui.billing;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.MembershipPlan;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.ui.Formats;
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

/**
 * Modal form to create a membership plan or edit an existing one. It builds a
 * {@link MembershipPlan} and hands it to {@link MembershipPlanService}, which
 * validates it; errors are shown inside the form and the form stays open.
 */
public class PlanFormDialog extends JDialog {

    private final MembershipPlanService service;
    private final MembershipPlan existing;     // null when a new plan is being created
    private final Runnable onSaved;

    private final JTextField nameField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JTextField durationField = new JTextField();
    private final JTextField priceField = new JTextField();
    private final JLabel errorLabel = new JLabel(" ");

    /**
     * Creates the dialog.
     *
     * @param owner    the window the dialog belongs to
     * @param service  the plan service used to save
     * @param existing the plan to edit, or null to create a new one
     * @param onSaved  what to run after a successful save, to refresh the list
     */
    public PlanFormDialog(Window owner, MembershipPlanService service, MembershipPlan existing,
                          Runnable onSaved) {
        super(owner, existing == null ? "Nuevo plan" : "Editar plan", ModalityType.APPLICATION_MODAL);
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

        nameField.putClientProperty("JTextField.placeholderText", "Ej. Mensual");
        descriptionField.putClientProperty("JTextField.placeholderText", "Opcional, hasta 120 caracteres");
        durationField.putClientProperty("JTextField.placeholderText", "Días, ej. 30");
        priceField.putClientProperty("JTextField.placeholderText", "Pesos, ej. 80000");
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
        addRow(content, row++, "Nombre", nameField);
        addRow(content, row++, "Descripción", descriptionField);
        addRow(content, row++, "Duración (días)", durationField);
        addRow(content, row++, "Precio", priceField);
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
        if (existing != null) {
            nameField.setText(existing.getName());
            descriptionField.setText(existing.getDescription());
            durationField.setText(String.valueOf(existing.getDurationDays()));
            priceField.setText(String.valueOf(existing.getPrice()));
        }
    }

    private void save() {
        try {
            MembershipPlan plan = new MembershipPlan(
                    existing == null ? 0 : existing.getId(),
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    Formats.parseWholeNumber("duración", durationField.getText()),
                    Formats.parseMoney("precio", priceField.getText()),
                    existing == null || existing.isActive());
            if (existing == null) {
                service.create(plan);
            } else {
                service.update(plan);
            }
            onSaved.run();
            dispose();
        } catch (NextFitException e) {
            errorLabel.setText(e.getMessage());
            pack();
        }
    }
}

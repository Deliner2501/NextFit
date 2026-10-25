package com.nextfit.ui.members;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.MemberAvatar;
import com.nextfit.util.PhotoStorage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Form to register a new member or to edit an existing one. It only collects
 * what the user types and delegates every rule to {@link MemberService}.
 */
public class MemberFormDialog extends JDialog {

    /** Format in which dates are typed and shown, e.g. 24/10/2026. */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final MemberService memberService;
    private final PhotoStorage photoStorage;
    private final Member existing;
    private final Runnable onSaved;

    private final JTextField fullNameField = new JTextField();
    private final JTextField documentField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField birthDateField = new JTextField();
    private final JTextField emergencyField = new JTextField();
    private final MemberAvatar avatar = new MemberAvatar(96);
    private final JLabel errorLabel = new JLabel(" ");
    private Path chosenPhoto;

    /**
     * Creates the form.
     *
     * @param owner         the window that opens the form
     * @param memberService the service that registers and updates members
     * @param photoStorage  where the chosen photo is copied
     * @param existing      the member to edit, or null to register a new one
     * @param onSaved       what to do after the member was saved (refresh the list)
     */
    public MemberFormDialog(Window owner, MemberService memberService, PhotoStorage photoStorage,
                            Member existing, Runnable onSaved) {
        super(owner, existing == null ? "Nuevo socio" : "Editar socio", ModalityType.APPLICATION_MODAL);
        this.memberService = memberService;
        this.photoStorage = photoStorage;
        this.existing = existing;
        this.onSaved = onSaved;
        buildScreen();
        fillFromExisting();
        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void buildScreen() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JLabel title = new JLabel(getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Theme.TEXT);
        content.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        form.add(buildPhotoRow(), constraints(row++, 0, 2));
        addField(form, row++, "Nombre completo *", fullNameField, "Ej. Ana María Pérez");
        addField(form, row++, "Documento *", documentField, "Solo números");
        addField(form, row++, "Teléfono *", phoneField, "Solo números");
        addField(form, row++, "Correo", emailField, "Opcional");
        addField(form, row++, "Fecha de nacimiento *", birthDateField, "DD/MM/AAAA");
        addField(form, row, "Contacto de emergencia", emergencyField, "Nombre y teléfono (opcional)");
        content.add(form, BorderLayout.CENTER);

        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setFont(errorLabel.getFont().deriveFont(12f));
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(errorLabel, BorderLayout.NORTH);
        south.add(buildButtons(), BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel buildPhotoRow() {
        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        photoRow.setOpaque(false);
        JButton chooseButton = new JButton("Elegir foto...");
        chooseButton.addActionListener(e -> choosePhoto());
        photoRow.add(avatar);
        photoRow.add(chooseButton);
        return photoRow;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Guardar");
        save.setBackground(Theme.ACCENT);
        save.setForeground(Theme.BACKGROUND);
        save.setFont(save.getFont().deriveFont(Font.BOLD));
        save.addActionListener(e -> save());
        buttons.add(cancel);
        buttons.add(save);
        getRootPane().setDefaultButton(save);
        return buttons;
    }

    private void addField(JPanel form, int row, String labelText, JTextField field, String hint) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Theme.TEXT_MUTED);
        field.putClientProperty("JTextField.placeholderText", hint);
        field.setPreferredSize(new Dimension(280, 34));
        form.add(label, constraints(row, 0, 1));
        form.add(field, constraints(row, 1, 1));
    }

    private GridBagConstraints constraints(int row, int column, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.gridwidth = width;
        c.anchor = GridBagConstraints.WEST;
        c.fill = column == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.weightx = column == 1 ? 1 : 0;
        c.insets = new Insets(6, 0, 6, column == 0 ? 16 : 0);
        return c;
    }

    private void fillFromExisting() {
        if (existing == null) {
            avatar.setMember("", "");
            return;
        }
        fullNameField.setText(existing.getFullName());
        documentField.setText(existing.getDocumentNumber());
        phoneField.setText(existing.getPhone());
        emailField.setText(existing.getEmail());
        birthDateField.setText(existing.getBirthDate().format(DATE_FORMAT));
        emergencyField.setText(existing.getEmergencyContact());
        avatar.setMember(existing.getFullName(), existing.getPhotoPath());
    }

    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes JPG o PNG", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chosenPhoto = chooser.getSelectedFile().toPath();
            avatar.setMember(fullNameField.getText(), chosenPhoto.toString());
        }
    }

    private void save() {
        try {
            LocalDate birthDate = parseBirthDate();
            Member member = buildMember(birthDate);
            if (existing == null) {
                memberService.register(member);
            } else {
                memberService.update(member);
            }
            MemberAvatar.clearCache();
            dispose();
            onSaved.run();
        } catch (NextFitException e) {
            errorLabel.setText(e.getMessage());
        } catch (DateTimeParseException e) {
            errorLabel.setText("La fecha de nacimiento debe tener el formato DD/MM/AAAA, por ejemplo 24/05/2001");
        }
    }

    private LocalDate parseBirthDate() {
        return LocalDate.parse(birthDateField.getText().trim(), DATE_FORMAT);
    }

    private Member buildMember(LocalDate birthDate) {
        String document = documentField.getText().trim();
        String photoPath = existing == null ? "" : existing.getPhotoPath();
        if (chosenPhoto != null && !document.isEmpty()) {
            photoPath = photoStorage.copy(chosenPhoto, document);
        }
        long id = existing == null ? 0 : existing.getId();
        LocalDate joinDate = existing == null ? LocalDate.now() : existing.getJoinDate();
        boolean active = existing == null || existing.isActive();
        return new Member(id, fullNameField.getText().trim(), document, phoneField.getText().trim(),
                emailField.getText().trim(), birthDate, joinDate, photoPath,
                emergencyField.getText().trim(), active);
    }
}
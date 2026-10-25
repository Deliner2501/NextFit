package com.nextfit.ui.members;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Member;
import com.nextfit.service.HealthService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.BmiGauge;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.Optional;

/**
 * Tab with the health profile of a member and the gauge of the body mass
 * index. It collects what the user types and delegates to {@link HealthService}.
 */
class HealthTab extends JPanel {

    private static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    private final HealthService healthService;
    private final Member member;
    private final BmiGauge gauge = new BmiGauge();
    private final JTextField heightField = new JTextField();
    private final JComboBox<String> bloodTypeBox = new JComboBox<>(BLOOD_TYPES);
    private final JTextArea conditionsArea = new JTextArea(3, 24);
    private final JTextArea allergiesArea = new JTextArea(2, 24);
    private final JLabel message = Forms.messageLabel();

    HealthTab(HealthService healthService, Member member) {
        super(new GridBagLayout());
        this.healthService = healthService;
        this.member = member;
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        buildForm();
        refresh();
    }

    private void buildForm() {
        heightField.setPreferredSize(new Dimension(160, 34));
        heightField.putClientProperty("JTextField.placeholderText", "Ej. 175");
        conditionsArea.setLineWrap(true);
        conditionsArea.setWrapStyleWord(true);
        allergiesArea.setLineWrap(true);
        allergiesArea.setWrapStyleWord(true);

        JButton save = Forms.accentButton("Guardar perfil");
        save.addActionListener(e -> save());

        add(gauge, constraints(0, 0, 2, GridBagConstraints.NONE));
        addRow(1, "Estatura (cm) *", heightField);
        addRow(2, "Tipo de sangre *", bloodTypeBox);
        addRow(3, "Condiciones médicas", new JScrollPane(conditionsArea));
        addRow(4, "Alergias", new JScrollPane(allergiesArea));
        add(save, constraints(5, 1, 1, GridBagConstraints.NONE));
        add(message, constraints(6, 1, 1, GridBagConstraints.HORIZONTAL));

        GridBagConstraints filler = constraints(7, 0, 2, GridBagConstraints.BOTH);
        filler.weighty = 1;
        add(new JPanel() {
            {
                setOpaque(false);
            }
        }, filler);
    }

    private void addRow(int row, String labelText, java.awt.Component field) {
        GridBagConstraints labelConstraints = constraints(row, 0, 1, GridBagConstraints.NONE);
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(10, 0, 6, 16);
        add(Forms.mutedLabel(labelText), labelConstraints);
        GridBagConstraints fieldConstraints = constraints(row, 1, 1,
                field instanceof JScrollPane ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE);
        fieldConstraints.weightx = 1;
        add(field, fieldConstraints);
    }

    private GridBagConstraints constraints(int row, int column, int width, int fill) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.gridwidth = width;
        c.fill = fill;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 0);
        return c;
    }

    /** Loads the stored profile and the current body mass index. */
    void refresh() {
        Optional<HealthProfile> profile = healthService.findProfile(member.getId());
        if (profile.isPresent()) {
            heightField.setText(String.format(Locale.ROOT, "%.1f", profile.get().getHeightCm()));
            bloodTypeBox.setSelectedItem(profile.get().getBloodType());
            conditionsArea.setText(profile.get().getMedicalConditions());
            allergiesArea.setText(profile.get().getAllergies());
        }
        gauge.setBmi(healthService.currentBmi(member.getId()).orElse(null));
    }

    private void save() {
        try {
            double height = Forms.parseNumber("estatura", heightField.getText());
            HealthProfile profile = new HealthProfile(member.getId(), height,
                    (String) bloodTypeBox.getSelectedItem(),
                    conditionsArea.getText().trim(), allergiesArea.getText().trim());
            healthService.saveProfile(profile);
            gauge.setBmi(healthService.currentBmi(member.getId()).orElse(null));
            Forms.showSuccess(message, "Perfil guardado");
        } catch (NextFitException e) {
            Forms.showError(message, e.getMessage());
        }
    }
}
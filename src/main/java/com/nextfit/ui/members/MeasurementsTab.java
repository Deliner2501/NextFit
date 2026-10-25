package com.nextfit.ui.members;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.service.HealthService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.LineChart;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tab with the body measurements of a member: the weight chart, a form to add
 * a new measurement and the history. It delegates to {@link HealthService}.
 */
class MeasurementsTab extends JPanel {

    private static final DateTimeFormatter CHART_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final String[] COLUMNS = {"Fecha", "Peso (kg)", "Cintura (cm)", "Pecho (cm)", "Cadera (cm)", "IMC"};

    private final HealthService healthService;
    private final Member member;
    private final LineChart chart = new LineChart("Peso", "kg");
    private final JTextField dateField = new JTextField(9);
    private final JTextField weightField = new JTextField(5);
    private final JTextField waistField = new JTextField(5);
    private final JTextField chestField = new JTextField(5);
    private final JTextField hipsField = new JTextField(5);
    private final JLabel message = Forms.messageLabel();
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    MeasurementsTab(HealthService healthService, Member member) {
        super(new BorderLayout(0, 12));
        this.healthService = healthService;
        this.member = member;
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(16, 28, 20, 28));
        chart.setPreferredSize(new Dimension(560, 250));
        chart.setEmptyMessage("Aún no hay mediciones");
        add(chart, BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(0, 8));
        lower.setOpaque(false);
        lower.add(buildForm(), BorderLayout.NORTH);
        JTable table = new JTable(tableModel);
        Forms.styleTable(table);
        lower.add(Forms.scroll(table), BorderLayout.CENTER);
        lower.add(message, BorderLayout.SOUTH);
        add(lower, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setOpaque(false);
        dateField.setText(LocalDate.now().format(MemberFormDialog.DATE_FORMAT));
        form.add(Forms.mutedLabel("Fecha"));
        form.add(dateField);
        addMeasure(form, "Peso kg", weightField);
        addMeasure(form, "Cintura cm", waistField);
        addMeasure(form, "Pecho cm", chestField);
        addMeasure(form, "Cadera cm", hipsField);
        JButton add = Forms.accentButton("Agregar");
        add.addActionListener(e -> addMeasurement());
        form.add(add);
        return form;
    }

    private void addMeasure(JPanel form, String label, JTextField field) {
        form.add(Forms.mutedLabel(label));
        form.add(field);
    }

    /** Reloads the chart and the history from the service. */
    void refresh() {
        List<Measurement> history = healthService.measurementHistory(member.getId());
        List<LineChart.Point> points = new ArrayList<>();
        for (Measurement m : history) {
            points.add(new LineChart.Point(m.getDate().format(CHART_DATE), m.getWeightKg()));
        }
        chart.setData(points);

        Optional<HealthProfile> profile = healthService.findProfile(member.getId());
        tableModel.setRowCount(0);
        for (int i = history.size() - 1; i >= 0; i--) {
            Measurement m = history.get(i);
            String bmi = profile.map(p -> String.format(Locale.ROOT, "%.1f",
                    healthService.calculateBmi(m.getWeightKg(), p.getHeightCm()))).orElse("—");
            tableModel.addRow(new Object[]{m.getDate().format(MemberFormDialog.DATE_FORMAT),
                    m.getWeightKg(), m.getWaistCm(), m.getChestCm(), m.getHipsCm(), bmi});
        }
    }

    private void addMeasurement() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), MemberFormDialog.DATE_FORMAT);
            Measurement measurement = new Measurement(0, member.getId(), date,
                    Forms.parseNumber("peso", weightField.getText()),
                    Forms.parseNumber("cintura", waistField.getText()),
                    Forms.parseNumber("pecho", chestField.getText()),
                    Forms.parseNumber("cadera", hipsField.getText()));
            healthService.addMeasurement(measurement);
            weightField.setText("");
            waistField.setText("");
            chestField.setText("");
            hipsField.setText("");
            refresh();
            Forms.showSuccess(message, "Medición agregada");
        } catch (DateTimeParseException e) {
            Forms.showError(message, "La fecha debe tener el formato DD/MM/AAAA, por ejemplo 24/10/2026");
        } catch (NextFitException e) {
            Forms.showError(message, e.getMessage());
        }
    }
}
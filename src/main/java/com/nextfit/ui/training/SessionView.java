package com.nextfit.ui.training;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.TrainingSession;
import com.nextfit.service.MemberService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainingSessionService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Screen to log training sessions. The user selects a member; if the member has an
 * active routine, a form lets them record a session (day, date, duration, notes) and
 * the history of sessions is listed below. Rules live in {@link TrainingSessionService}.
 */
public class SessionView extends JPanel {

    private final TrainingSessionService sessions;
    private final RoutineAssignmentService assignments;
    private final RoutineService routines;

    private final MemberPickerPanel picker;
    private final JPanel detail = new JPanel(new BorderLayout(0, 14));
    private Member selected;   // socio seleccionado (null si no hay)

    /**
     * @param sessions    session service
     * @param assignments assignment service, to know the member's current routine
     * @param routines    routine service, to list the days of that routine
     * @param members     member service, to search members
     */
    public SessionView(TrainingSessionService sessions, RoutineAssignmentService assignments,
                       RoutineService routines, MemberService members) {
        super(new BorderLayout(20, 0));
        this.sessions = sessions;
        this.assignments = assignments;
        this.routines = routines;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        this.picker = new MemberPickerPanel("Sesiones de entrenamiento", members, member -> {
            selected = member;
            renderDetail();
        });
        detail.setOpaque(false);
        add(picker, BorderLayout.WEST);
        add(detail, BorderLayout.CENTER);
        renderDetail();
    }

    /** Reloads the member list and the detail of the selected member. */
    public void refresh() {
        picker.reload();
        renderDetail();
    }

    // Dibuja el panel derecho según el socio seleccionado.
    private void renderDetail() {
        detail.removeAll();
        if (selected == null) {
            JLabel hint = new JLabel("Selecciona un socio para registrar o ver sus sesiones.", JLabel.CENTER);
            hint.setForeground(Theme.TEXT_MUTED);
            detail.add(hint, BorderLayout.CENTER);
        } else {
            detail.add(buildFormCard(), BorderLayout.NORTH);
            detail.add(buildHistory(), BorderLayout.CENTER);
        }
        detail.revalidate();
        detail.repaint();
    }

    // Tarjeta superior: formulario para registrar una sesión, o aviso si no hay rutina activa.
    private JPanel buildFormCard() {
        JLabel name = new JLabel(selected.getFullName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        name.setForeground(Theme.TEXT);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel cardTitle = new JLabel("Registrar entrenamiento");
        cardTitle.setForeground(Theme.ACCENT);
        cardTitle.setFont(cardTitle.getFont().deriveFont(Font.BOLD, 12f));
        card.add(cardTitle, BorderLayout.NORTH);

        Optional<RoutineAssignment> current = assignments.findCurrentFor(selected.getId());
        if (current.isEmpty()) {
            JLabel none = new JLabel("Este socio no tiene una rutina activa. Asígnale una en la pestaña Asignación.");
            none.setForeground(Theme.TEXT_MUTED);
            card.add(none, BorderLayout.CENTER);
        } else {
            card.add(buildForm(current.get()), BorderLayout.CENTER);
        }

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(name, BorderLayout.NORTH);
        header.add(card, BorderLayout.CENTER);
        return header;
    }

    // Campos del formulario: día de la rutina, fecha, duración, notas y botón.
    private JPanel buildForm(RoutineAssignment assignment) {
        Routine routine = routines.findById(assignment.getRoutineId());
        JComboBox<String> dayBox = new JComboBox<>();
        for (RoutineDay day : routine.getDays()) {
            dayBox.addItem(day.getName());
        }
        JTextField dateField = new JTextField(LocalDate.now().toString(), 9);
        dateField.putClientProperty("JTextField.placeholderText", "AAAA-MM-DD");
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(45, 5, 300, 5));
        JTextField notesField = new JTextField(28);
        notesField.putClientProperty("JTextField.placeholderText", "Notas (opcional)");

        JButton register = new JButton("Registrar sesión");
        register.setBackground(Theme.ACCENT);
        register.setForeground(Theme.BACKGROUND);
        register.setFont(register.getFont().deriveFont(Font.BOLD));
        register.addActionListener(e -> register(assignment, (String) dayBox.getSelectedItem(),
                dateField.getText().trim(), (Integer) durationSpinner.getValue(), notesField.getText().trim()));

        JLabel routineLabel = new JLabel("Rutina: " + routine.getName());
        routineLabel.setForeground(Theme.TEXT);
        routineLabel.setFont(routineLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);
        row1.add(label("Día"));
        row1.add(dayBox);
        row1.add(label("Fecha"));
        row1.add(dateField);
        row1.add(label("Minutos"));
        row1.add(durationSpinner);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.add(notesField);
        row2.add(register);

        JPanel form = new JPanel(new GridLayout(3, 1, 0, 10));
        form.setOpaque(false);
        form.add(routineLabel);
        form.add(row1);
        form.add(row2);
        return form;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    // Arma la sesión y la registra con el servicio; los errores de reglas se muestran en un aviso.
    private void register(RoutineAssignment assignment, String dayName, String dateText,
                          int minutes, String notes) {
        try {
            LocalDate date = parseDate(dateText);
            sessions.log(new TrainingSession(0, selected.getId(), assignment.getId(),
                    dayName == null ? "" : dayName, date, minutes, notes));
        } catch (NextFitException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo registrar", JOptionPane.WARNING_MESSAGE);
        }
        renderDetail();
    }

    // La fecha se escribe como AAAA-MM-DD; si está mal escrita se avisa con un mensaje claro.
    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("fecha", "escríbela como AAAA-MM-DD, por ejemplo 2026-10-25");
        }
    }

    // Historial de sesiones del socio, de la más reciente a la más antigua.
    private JPanel buildHistory() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Fecha", "Día", "Duración", "Notas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (TrainingSession session : sessions.findByMember(selected.getId())) {
            model.addRow(new Object[]{session.getDate(), session.getDayName(),
                    session.getDurationMinutes() + " min", session.getNotes()});
        }
        JLabel title = new JLabel("Historial de sesiones (" + model.getRowCount() + ")");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}
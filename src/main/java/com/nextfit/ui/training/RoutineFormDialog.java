package com.nextfit.ui.training;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Exercise;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainerService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Routine builder: a modal form to create or edit a routine. A routine has a name,
 * an objective, a trainer and one or more days; each day has a list of exercises
 * with sets, repetitions and rest. It builds a {@link Routine} and hands it to
 * {@link RoutineService}, which validates it; errors are shown inside the form.
 */
public class RoutineFormDialog extends JDialog {

    private final RoutineService service;
    private final Routine existing;          // null cuando se está creando una rutina nueva
    private final Runnable onSaved;
    private final List<Exercise> exercises;  // catálogo disponible para elegir
    private final List<Trainer> trainers;    // solo entrenadores activos

    private final JTextField nameField = new JTextField();
    private final JTextField objectiveField = new JTextField();
    private final JComboBox<Trainer> trainerBox = new JComboBox<>();
    private final JTabbedPane daysTabs = new JTabbedPane();
    private final List<DayPanel> dayPanels = new ArrayList<>();
    private final JLabel errorLabel = new JLabel(" ");

    /**
     * @param owner     window the dialog belongs to
     * @param service   routine service used to save
     * @param trainers  trainer service, to list the active trainers
     * @param catalog   exercise service, to list the exercises to choose from
     * @param existing  the routine to edit, or null to create a new one
     * @param onSaved   action to run after a successful save (to refresh the list)
     */
    public RoutineFormDialog(Window owner, RoutineService service, TrainerService trainers,
                             ExerciseService catalog, Routine existing, Runnable onSaved) {
        super(owner, existing == null ? "Nueva rutina" : "Editar rutina", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.existing = existing;
        this.onSaved = onSaved;
        this.exercises = catalog.findAll();
        this.trainers = trainers.findAll().stream().filter(Trainer::isActive).toList();
        buildForm();
        fillFromExisting();
        setMinimumSize(new Dimension(760, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildForm() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        // Datos generales de la rutina.
        nameField.putClientProperty("JTextField.placeholderText", "Ej. Fuerza para principiantes");
        objectiveField.putClientProperty("JTextField.placeholderText", "Ej. Ganar masa muscular");
        for (Trainer trainer : trainers) {
            trainerBox.addItem(trainer);
        }
        trainerBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Trainer t ? t.getFullName() : "");
                return this;
            }
        });
        JPanel general = new JPanel(new GridBagLayout());
        general.setOpaque(false);
        addRow(general, 0, "Nombre", nameField);
        addRow(general, 1, "Objetivo", objectiveField);
        addRow(general, 2, "Entrenador", trainerBox);

        // Días: una pestaña por día, con botones para agregar y quitar.
        JButton addDay = new JButton("+ Agregar día");
        addDay.addActionListener(e -> addDay(null));
        JButton removeDay = new JButton("Quitar día");
        removeDay.addActionListener(e -> removeSelectedDay());
        JPanel dayButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dayButtons.setOpaque(false);
        JLabel daysLabel = new JLabel("Días de la rutina");
        daysLabel.setForeground(Theme.TEXT);
        dayButtons.add(daysLabel);
        dayButtons.add(addDay);
        dayButtons.add(removeDay);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(dayButtons, BorderLayout.NORTH);
        center.add(daysTabs, BorderLayout.CENTER);

        errorLabel.setForeground(Theme.DANGER);
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Guardar rutina");
        save.setBackground(Theme.ACCENT);
        save.setForeground(Theme.BACKGROUND);
        save.setFont(save.getFont().deriveFont(Font.BOLD));
        save.addActionListener(e -> save());
        getRootPane().setDefaultButton(save);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(errorLabel, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.EAST);

        content.add(general, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 0, 5, 14);
        c.anchor = GridBagConstraints.WEST;
        JLabel text = new JLabel(label);
        text.setForeground(Theme.TEXT_MUTED);
        panel.add(text, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(5, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    // Al editar se cargan los datos actuales; al crear se empieza con un día vacío.
    private void fillFromExisting() {
        if (existing == null) {
            addDay(null);
            return;
        }
        nameField.setText(existing.getName());
        objectiveField.setText(existing.getObjective());
        for (int i = 0; i < trainers.size(); i++) {
            if (trainers.get(i).getId() == existing.getTrainerId()) {
                trainerBox.setSelectedIndex(i);
            }
        }
        for (RoutineDay day : existing.getDays()) {
            addDay(day);
        }
        daysTabs.setSelectedIndex(0);   // al editar se empieza viendo el primer día
    }

    // Agrega una pestaña de día (vacía o con los datos de un día existente).
    private void addDay(RoutineDay day) {
        DayPanel panel = new DayPanel(exercises, day);
        dayPanels.add(panel);
        daysTabs.addTab("Día " + dayPanels.size(), panel);
        daysTabs.setSelectedComponent(panel);
    }

    private void removeSelectedDay() {
        int index = daysTabs.getSelectedIndex();
        if (index < 0) {
            return;
        }
        dayPanels.remove(index);
        daysTabs.remove(index);
        // Se renumeran las pestañas para que siempre digan Día 1, Día 2...
        for (int i = 0; i < daysTabs.getTabCount(); i++) {
            daysTabs.setTitleAt(i, "Día " + (i + 1));
        }
    }

    // Arma la rutina, la guarda con el servicio y cierra; si hay error, lo muestra en el formulario.
    private void save() {
        try {
            if (trainers.isEmpty()) {
                errorLabel.setText("Primero registra un entrenador activo en la pestaña Entrenadores.");
                return;
            }
            Trainer trainer = (Trainer) trainerBox.getSelectedItem();
            List<RoutineDay> days = new ArrayList<>();
            for (DayPanel panel : dayPanels) {
                days.add(panel.toRoutineDay());
            }
            Routine routine = new Routine(
                    existing == null ? 0 : existing.getId(),
                    nameField.getText().trim(),
                    objectiveField.getText().trim(),
                    trainer.getId(),
                    existing == null ? LocalDate.now() : existing.getCreatedAt(),
                    days);
            if (existing == null) {
                service.create(routine);
            } else {
                service.update(routine);
            }
            onSaved.run();
            dispose();
        } catch (NextFitException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    /** One day of the routine: a name and a list of exercise rows. */
    private static class DayPanel extends JPanel {

        private final List<Exercise> exercises;
        private final JTextField dayNameField = new JTextField();
        private final JPanel rowsPanel = new JPanel();
        private final List<LineRow> rows = new ArrayList<>();

        DayPanel(List<Exercise> exercises, RoutineDay day) {
            super(new BorderLayout(0, 10));
            this.exercises = exercises;
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            dayNameField.putClientProperty("JTextField.placeholderText", "Nombre del día, ej. Pecho y tríceps");
            JButton addExercise = new JButton("+ Ejercicio");
            addExercise.addActionListener(e -> addRow(null));
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            top.add(dayNameField, BorderLayout.CENTER);
            top.add(addExercise, BorderLayout.EAST);

            rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
            rowsPanel.setOpaque(false);
            JPanel rowsHolder = new JPanel(new BorderLayout());   // mantiene las filas pegadas arriba
            rowsHolder.setOpaque(false);
            rowsHolder.add(rowsPanel, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(rowsHolder);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setPreferredSize(new Dimension(680, 220));

            add(top, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);

            if (day == null) {
                addRow(null);
            } else {
                dayNameField.setText(day.getName());
                for (RoutineExercise line : day.getExercises()) {
                    addRow(line);
                }
            }
        }

        private void addRow(RoutineExercise line) {
            LineRow row = new LineRow(exercises, line, this::removeRow);
            rows.add(row);
            rowsPanel.add(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
        }

        private void removeRow(LineRow row) {
            rows.remove(row);
            rowsPanel.remove(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
        }

        RoutineDay toRoutineDay() {
            List<RoutineExercise> lines = new ArrayList<>();
            for (LineRow row : rows) {
                lines.add(row.toRoutineExercise());
            }
            return new RoutineDay(dayNameField.getText().trim(), lines);
        }
    }

    /** One exercise line of a day: exercise, sets, repetitions, rest and a remove button. */
    private static class LineRow extends JPanel {

        private final JComboBox<Exercise> exerciseBox = new JComboBox<>();
        private final JSpinner setsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        private final JSpinner repsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        private final JSpinner restSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 600, 15));

        LineRow(List<Exercise> exercises, RoutineExercise line, Consumer<LineRow> onRemove) {
            super(new FlowLayout(FlowLayout.LEFT, 8, 4));
            setOpaque(false);
            for (Exercise exercise : exercises) {
                exerciseBox.addItem(exercise);
            }
            exerciseBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    setText(value instanceof Exercise e ? e.getName() : "");
                    return this;
                }
            });
            exerciseBox.setPreferredSize(new Dimension(230, exerciseBox.getPreferredSize().height));
            if (line != null) {
                for (int i = 0; i < exercises.size(); i++) {
                    if (exercises.get(i).getId() == line.getExerciseId()) {
                        exerciseBox.setSelectedIndex(i);
                    }
                }
                setsSpinner.setValue(line.getSets());
                repsSpinner.setValue(line.getRepetitions());
                restSpinner.setValue(line.getRestSeconds());
            }
            JButton remove = new JButton("Quitar");
            remove.addActionListener(e -> onRemove.accept(this));

            add(exerciseBox);
            add(labeled("Series", setsSpinner));
            add(labeled("Reps", repsSpinner));
            add(labeled("Descanso (s)", restSpinner));
            add(remove);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
        }

        private static JPanel labeled(String text, JSpinner spinner) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            panel.setOpaque(false);
            JLabel label = new JLabel(text);
            label.setForeground(Theme.TEXT_MUTED);
            panel.add(label);
            panel.add(spinner);
            return panel;
        }

        RoutineExercise toRoutineExercise() {
            Exercise selected = (Exercise) exerciseBox.getSelectedItem();
            // Si el catálogo está vacío no hay ejercicio; id 0 hará que el servicio lo rechace.
            long exerciseId = selected == null ? 0 : selected.getId();
            return new RoutineExercise(exerciseId, (Integer) setsSpinner.getValue(),
                    (Integer) repsSpinner.getValue(), (Integer) restSpinner.getValue());
        }
    }
}
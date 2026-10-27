package com.nextfit.ui.training;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Routine;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainerService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen with the list of routines. It shows them in a table and opens the
 * {@link RoutineFormDialog} to create or edit one. Rules live in {@link RoutineService}.
 */
public class RoutineListView extends JPanel {

    private final RoutineService routines;
    private final TrainerService trainers;
    private final ExerciseService exercises;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Nombre", "Objetivo", "Entrenador", "Días", "Series totales", "Creada"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;   // solo lectura; se edita con el formulario
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private List<Routine> shown = new ArrayList<>();

    /**
     * @param routines  routine service
     * @param trainers  trainer service, to show the trainer's name
     * @param exercises exercise service, used by the builder
     */
    public RoutineListView(RoutineService routines, TrainerService trainers, ExerciseService exercises) {
        super(new BorderLayout(0, 16));
        this.routines = routines;
        this.trainers = trainers;
        this.exercises = exercises;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Rutinas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JButton newButton = new JButton("+ Nueva rutina");
        newButton.setBackground(Theme.ACCENT);
        newButton.setForeground(Theme.BACKGROUND);
        newButton.setFont(newButton.getFont().deriveFont(Font.BOLD));
        newButton.addActionListener(e -> openForm(null));
        JButton editButton = new JButton("Editar");
        editButton.addActionListener(e -> editSelected());

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(newButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        // Doble clic sobre una fila abre el constructor para editarla.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        return scroll;
    }

    /** Reloads the routines from the service. */
    public void refresh() {
        shown = routines.findAll();
        model.setRowCount(0);
        for (Routine routine : shown) {
            model.addRow(new Object[]{routine.getName(), routine.getObjective(), trainerName(routine),
                    routine.getDays().size(), routine.getTotalSets(), routine.getCreatedAt()});
        }
        countLabel.setText(shown.size() == 1 ? "1 rutina" : shown.size() + " rutinas");
    }

    // Nombre del entrenador de la rutina; si ya no se encuentra, se muestra un guion.
    private String trainerName(Routine routine) {
        try {
            return trainers.findById(routine.getTrainerId()).getFullName();
        } catch (NextFitException e) {
            return "-";
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una rutina de la tabla.");
        } else {
            openForm(shown.get(row));
        }
    }

    private void openForm(Routine routine) {
        new RoutineFormDialog(SwingUtilities.getWindowAncestor(this), routines, trainers, exercises,
                routine, this::refresh).setVisible(true);
    }
}
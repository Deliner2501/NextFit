package com.nextfit.ui.training;

import com.nextfit.model.Trainer;
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
 * Screen with the list of trainers. It shows them in a table and lets the user
 * register, edit, deactivate and reactivate them. Rules live in {@link TrainerService}.
 */
public class TrainerListView extends JPanel {

    private final TrainerService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Nombre", "Documento", "Teléfono", "Especialidad", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;   // la tabla es solo de lectura; se edita con el formulario
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JButton toggleButton = new JButton("Desactivar");
    private List<Trainer> shown = new ArrayList<>();

    /** @param service trainer service */
    public TrainerListView(TrainerService service) {
        super(new BorderLayout(0, 16));
        this.service = service;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Entrenadores");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JButton newButton = new JButton("+ Nuevo entrenador");
        newButton.setBackground(Theme.ACCENT);
        newButton.setForeground(Theme.BACKGROUND);
        newButton.setFont(newButton.getFont().deriveFont(Font.BOLD));
        newButton.addActionListener(e -> openForm(null));
        JButton editButton = new JButton("Editar");
        editButton.addActionListener(e -> editSelected());
        toggleButton.addActionListener(e -> toggleSelected());

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(toggleButton);
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
        table.getSelectionModel().addListSelectionListener(e -> updateToggleText());
        // Doble clic sobre una fila abre el formulario de edición.
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

    /** Reloads the trainers from the service. */
    public void refresh() {
        shown = service.findAll();
        model.setRowCount(0);
        for (Trainer trainer : shown) {
            model.addRow(new Object[]{trainer.getFullName(), trainer.getDocumentNumber(),
                    trainer.getPhone(), trainer.getSpecialty(), trainer.isActive() ? "Activo" : "Inactivo"});
        }
        countLabel.setText(shown.size() == 1 ? "1 entrenador" : shown.size() + " entrenadores");
        updateToggleText();
    }

    // El botón cambia de texto según el estado del entrenador seleccionado.
    private void updateToggleText() {
        Trainer selected = selectedTrainer();
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Desactivar");
    }

    // Devuelve el entrenador de la fila seleccionada, o null si no hay ninguna.
    private Trainer selectedTrainer() {
        int row = table.getSelectedRow();
        return row < 0 ? null : shown.get(row);
    }

    private void editSelected() {
        Trainer selected = selectedTrainer();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un entrenador de la tabla.");
        } else {
            openForm(selected);
        }
    }

    private void toggleSelected() {
        Trainer selected = selectedTrainer();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un entrenador de la tabla.");
            return;
        }
        if (selected.isActive()) {
            service.deactivate(selected.getId());
        } else {
            service.reactivate(selected.getId());
        }
        refresh();
    }

    private void openForm(Trainer trainer) {
        new TrainerFormDialog(SwingUtilities.getWindowAncestor(this), service, trainer, this::refresh)
                .setVisible(true);
    }
}
package com.nextfit.ui.training;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.service.MemberService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Screen to assign routines to members. On the left the user searches and selects
 * a member; on the right it sees the member's current routine, can assign, complete
 * or cancel it, and reads the history. Rules live in {@link RoutineAssignmentService}.
 */
public class AssignmentView extends JPanel {

    private final RoutineAssignmentService assignments;
    private final RoutineService routines;
    private final MemberService members;

    private final JTextField searchField = new JTextField();
    private final DefaultTableModel membersModel = readOnlyModel("Nombre", "Documento");
    private final JTable membersTable = new JTable(membersModel);
    private final JPanel detail = new JPanel(new BorderLayout(0, 14));
    private final DefaultTableModel historyModel = readOnlyModel("Rutina", "Fecha", "Estado");

    private List<Member> shownMembers = new ArrayList<>();
    private Member selected;   // socio seleccionado (null si no hay)

    /**
     * @param assignments assignment service
     * @param routines    routine service, to list and show routines
     * @param members     member service, to search members
     */
    public AssignmentView(RoutineAssignmentService assignments, RoutineService routines, MemberService members) {
        super(new BorderLayout(20, 0));
        this.assignments = assignments;
        this.routines = routines;
        this.members = members;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildMemberPicker(), BorderLayout.WEST);
        detail.setOpaque(false);
        add(detail, BorderLayout.CENTER);
        refresh();
    }

    // Modelo de tabla de solo lectura con los encabezados dados.
    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // Columna izquierda: título, buscador y tabla de socios.
    private JPanel buildMemberPicker() {
        JLabel title = new JLabel("Asignación de rutinas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        searchField.putClientProperty("JTextField.placeholderText", "Buscar socio por nombre o documento...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadMembers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadMembers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadMembers();
            }
        });
        membersTable.setRowHeight(32);
        membersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        membersTable.setShowVerticalLines(false);
        membersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = membersTable.getSelectedRow();
                selected = row < 0 ? null : shownMembers.get(row);
                renderDetail();
            }
        });
        JScrollPane scroll = new JScrollPane(membersTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 10));
        top.setOpaque(false);
        top.add(title);
        top.add(searchField);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(340, 0));
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Reloads the member list and the detail of the selected member. */
    public void refresh() {
        loadMembers();
        renderDetail();
    }

    // Carga los socios que coinciden con la búsqueda y mantiene la selección si el socio sigue en la lista.
    private void loadMembers() {
        Long keepId = selected == null ? null : selected.getId();
        shownMembers = members.search(searchField.getText());
        membersModel.setRowCount(0);
        for (Member member : shownMembers) {
            membersModel.addRow(new Object[]{member.getFullName(), member.getDocumentNumber()});
        }
        if (keepId != null) {
            for (int i = 0; i < shownMembers.size(); i++) {
                if (shownMembers.get(i).getId() == keepId) {
                    membersTable.setRowSelectionInterval(i, i);
                }
            }
        }
    }

    // Dibuja el panel derecho según el socio seleccionado.
    private void renderDetail() {
        detail.removeAll();
        if (selected == null) {
            JLabel hint = new JLabel("Selecciona un socio para ver o asignar su rutina.", JLabel.CENTER);
            hint.setForeground(Theme.TEXT_MUTED);
            detail.add(hint, BorderLayout.CENTER);
        } else {
            detail.add(buildMemberHeader(), BorderLayout.NORTH);
            detail.add(buildHistory(), BorderLayout.CENTER);
        }
        detail.revalidate();
        detail.repaint();
    }

    // Parte superior del detalle: nombre del socio y su rutina actual con los botones.
    private JPanel buildMemberHeader() {
        JLabel name = new JLabel(selected.getFullName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        name.setForeground(Theme.TEXT);
        JLabel document = new JLabel("Documento " + selected.getDocumentNumber()
                + (selected.isActive() ? "" : " · INACTIVO"));
        document.setForeground(Theme.TEXT_MUTED);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        Optional<RoutineAssignment> current = assignments.findCurrentFor(selected.getId());
        JLabel cardTitle = new JLabel("Rutina actual");
        cardTitle.setForeground(Theme.ACCENT);
        cardTitle.setFont(cardTitle.getFont().deriveFont(Font.BOLD, 12f));
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        if (current.isPresent()) {
            RoutineAssignment assignment = current.get();
            Routine routine = routines.findById(assignment.getRoutineId());
            int dayCount = routine.getDays().size();
            JLabel routineName = new JLabel(routine.getName() + "  ·  " + dayCount
                    + (dayCount == 1 ? " día" : " días") + "  ·  " + routine.getTotalSets() + " series");
            routineName.setForeground(Theme.TEXT);
            routineName.setFont(routineName.getFont().deriveFont(Font.BOLD, 15f));
            JLabel since = new JLabel("Asignada el " + assignment.getAssignedAt() + " · " + routine.getObjective());
            since.setForeground(Theme.TEXT_MUTED);
            JPanel texts = new JPanel(new GridLayout(2, 1));
            texts.setOpaque(false);
            texts.add(routineName);
            texts.add(since);
            card.add(texts, BorderLayout.CENTER);
            JButton complete = new JButton("Completar");
            complete.addActionListener(e -> close(assignment, true));
            JButton cancel = new JButton("Cancelar rutina");
            cancel.addActionListener(e -> close(assignment, false));
            buttons.add(complete);
            buttons.add(cancel);
        } else {
            JLabel none = new JLabel("Este socio no tiene una rutina activa.");
            none.setForeground(Theme.TEXT_MUTED);
            card.add(none, BorderLayout.CENTER);
            JButton assign = new JButton("Asignar rutina");
            assign.setBackground(Theme.ACCENT);
            assign.setForeground(Theme.BACKGROUND);
            assign.setFont(assign.getFont().deriveFont(Font.BOLD));
            assign.addActionListener(e -> assignRoutine());
            buttons.add(assign);
        }
        card.add(buttons, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        names.add(name);
        names.add(document);
        header.add(names, BorderLayout.NORTH);
        header.add(card, BorderLayout.CENTER);
        return header;
    }

    // Historial de asignaciones del socio, de la más reciente a la más antigua.
    private JPanel buildHistory() {
        JLabel title = new JLabel("Historial");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        historyModel.setRowCount(0);
        List<RoutineAssignment> history = new ArrayList<>(assignments.findHistoryFor(selected.getId()));
        history.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        for (RoutineAssignment item : history) {
            historyModel.addRow(new Object[]{routines.findById(item.getRoutineId()).getName(),
                    item.getAssignedAt(), item.getStatus().getLabel()});
        }
        JTable table = new JTable(historyModel);
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

    // Pide elegir una rutina de la lista y la asigna; los errores de reglas se muestran en un aviso.
    private void assignRoutine() {
        List<Routine> available = routines.findAll();
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero crea una rutina en la pestaña Rutinas.");
            return;
        }
        JComboBox<Routine> box = new JComboBox<>(available.toArray(new Routine[0]));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, isSelected, focus);
                setText(value instanceof Routine r ? r.getName() : "");
                return this;
            }
        });
        int answer = JOptionPane.showConfirmDialog(this, box, "Elige la rutina para " + selected.getFullName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            run(() -> assignments.assign(selected.getId(), ((Routine) box.getSelectedItem()).getId()));
        }
    }

    // Completa o cancela la asignación actual, previa confirmación.
    private void close(RoutineAssignment assignment, boolean complete) {
        String text = complete ? "¿Marcar la rutina como completada?" : "¿Cancelar la rutina del socio?";
        if (JOptionPane.showConfirmDialog(this, text, "Confirmar", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            run(() -> {
                if (complete) {
                    assignments.complete(assignment.getId());
                } else {
                    assignments.cancel(assignment.getId());
                }
            });
        }
    }

    // Ejecuta una acción del servicio; si una regla la rechaza, se muestra el mensaje en español.
    private void run(Runnable action) {
        try {
            action.run();
        } catch (NextFitException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo completar", JOptionPane.WARNING_MESSAGE);
        }
        renderDetail();
    }
}
package com.nextfit.ui.billing;

import com.nextfit.model.MembershipPlan;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.ui.Formats;
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
 * Screen with the membership plans the gym sells. It shows them in a table and
 * lets the user create, edit, deactivate and reactivate them. The rules live in
 * {@link MembershipPlanService}; this class only collects clicks and shows data.
 */
public class PlanListView extends JPanel {

    private final MembershipPlanService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Plan", "Duración", "Precio", "Descripción", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JButton toggleButton = new JButton("Desactivar");
    private List<MembershipPlan> shown = new ArrayList<>();

    /**
     * Creates the screen.
     *
     * @param service the service that manages the plans
     */
    public PlanListView(MembershipPlanService service) {
        super(new BorderLayout(0, 16));
        this.service = service;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Planes de membresía");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JButton newButton = new JButton("+ Nuevo plan");
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
        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.getSelectionModel().addListSelectionListener(e -> updateToggleText());
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

    /** Reloads the plans from the service. */
    public void refresh() {
        shown = service.findAll();
        model.setRowCount(0);
        for (MembershipPlan plan : shown) {
            model.addRow(new Object[]{plan.getName(), plan.getDurationDays() + " días",
                    Formats.money(plan.getPrice()), plan.getDescription(),
                    plan.isActive() ? "En venta" : "Retirado"});
        }
        countLabel.setText(shown.size() == 1 ? "1 plan" : shown.size() + " planes");
        updateToggleText();
    }

    private void updateToggleText() {
        MembershipPlan selected = selectedPlan();
        toggleButton.setText(selected != null && !selected.isActive() ? "Volver a vender" : "Retirar de la venta");
    }

    private MembershipPlan selectedPlan() {
        int row = table.getSelectedRow();
        return row < 0 ? null : shown.get(row);
    }

    private void editSelected() {
        MembershipPlan selected = selectedPlan();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un plan de la tabla.");
        } else {
            openForm(selected);
        }
    }

    private void toggleSelected() {
        MembershipPlan selected = selectedPlan();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un plan de la tabla.");
            return;
        }
        if (selected.isActive()) {
            service.deactivate(selected.getId());
        } else {
            service.reactivate(selected.getId());
        }
        refresh();
    }

    private void openForm(MembershipPlan plan) {
        new PlanFormDialog(SwingUtilities.getWindowAncestor(this), service, plan, this::refresh)
                .setVisible(true);
    }
}

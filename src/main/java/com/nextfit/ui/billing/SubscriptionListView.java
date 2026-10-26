package com.nextfit.ui.billing;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Subscription;
import com.nextfit.model.SubscriptionStatus;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.Formats;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen with every membership sold: who has it, until when and in what state.
 * The staff can filter by state, search by member and cancel a membership.
 * The rules live in {@link SubscriptionService}; this class only shows data.
 */
public class SubscriptionListView extends JPanel {

    private static final int SOON_DAYS = 7;
    private static final int STATE_COLUMN = 4;

    private enum Filter {
        ALL("Todas"), ACTIVE("Activas"), EXPIRING("Por vencer (7 días)"), UPCOMING("Por iniciar"),
        EXPIRED("Vencidas"), CANCELLED("Canceladas");

        private final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private final SubscriptionService subscriptions;
    private final MembershipPlanService plans;
    private final MemberService members;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Socio", "Plan", "Inicio", "Fin", "Estado", "Días restantes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JTextField searchField = new JTextField();
    private final JComboBox<Filter> filterBox = new JComboBox<>(Filter.values());
    private List<Subscription> shown = new ArrayList<>();

    /**
     * Creates the screen.
     *
     * @param subscriptions the service that knows the memberships
     * @param plans         the service used to show plan names
     * @param members       the service used to show member names
     */
    public SubscriptionListView(SubscriptionService subscriptions, MembershipPlanService plans,
                                MemberService members) {
        super(new BorderLayout(0, 16));
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.members = members;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Suscripciones");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        searchField.putClientProperty("JTextField.placeholderText", "Buscar por nombre o documento");
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        filterBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Filter filter) {
                    setText(filter.label);
                }
                return this;
            }
        });
        filterBox.addActionListener(e -> refresh());
        JButton cancelButton = new JButton("Cancelar membresía");
        cancelButton.addActionListener(e -> cancelSelected());

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(searchField);
        actions.add(filterBox);
        actions.add(cancelButton);

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
        table.getColumnModel().getColumn(STATE_COLUMN).setCellRenderer(new StateRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        return scroll;
    }

    /** Reloads the memberships from the service, applying the search text and the filter. */
    public void refresh() {
        Map<Long, Member> memberById = new HashMap<>();
        for (Member member : members.findAll()) {
            memberById.put(member.getId(), member);
        }
        Map<Long, MembershipPlan> planById = new HashMap<>();
        for (MembershipPlan plan : plans.findAll()) {
            planById.put(plan.getId(), plan);
        }
        String wanted = searchField.getText().trim().toLowerCase();
        Filter filter = (Filter) filterBox.getSelectedItem();

        shown = new ArrayList<>();
        model.setRowCount(0);
        for (Subscription s : subscriptions.findAll()) {
            Member member = memberById.get(s.getMemberId());
            String name = member == null ? "Socio #" + s.getMemberId() : member.getFullName();
            String document = member == null ? "" : member.getDocumentNumber();
            if (!wanted.isEmpty() && !name.toLowerCase().contains(wanted) && !document.contains(wanted)) {
                continue;
            }
            if (!matches(s, filter)) {
                continue;
            }
            MembershipPlan plan = planById.get(s.getPlanId());
            long daysLeft = subscriptions.daysLeftOf(s);
            shown.add(s);
            model.addRow(new Object[]{name, plan == null ? "Plan #" + s.getPlanId() : plan.getName(),
                    Formats.date(s.getStartDate()), Formats.date(s.getEndDate()), stateText(s),
                    subscriptions.statusOf(s) == SubscriptionStatus.ACTIVE ? String.valueOf(daysLeft) : "-"});
        }
        countLabel.setText(shown.size() == 1 ? "1 suscripción" : shown.size() + " suscripciones");
    }

    private boolean matches(Subscription s, Filter filter) {
        SubscriptionStatus status = subscriptions.statusOf(s);
        return switch (filter == null ? Filter.ALL : filter) {
            case ALL -> true;
            case ACTIVE -> status == SubscriptionStatus.ACTIVE;
            case EXPIRING -> status == SubscriptionStatus.ACTIVE && subscriptions.daysLeftOf(s) <= SOON_DAYS;
            case UPCOMING -> status == SubscriptionStatus.UPCOMING;
            case EXPIRED -> status == SubscriptionStatus.EXPIRED;
            case CANCELLED -> status == SubscriptionStatus.CANCELLED;
        };
    }

    private String stateText(Subscription s) {
        SubscriptionStatus status = subscriptions.statusOf(s);
        if (status == SubscriptionStatus.ACTIVE && subscriptions.daysLeftOf(s) <= SOON_DAYS) {
            return "Por vencer";
        }
        return status.getLabel();
    }

    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una suscripción de la tabla.");
            return;
        }
        Subscription selected = shown.get(row);
        int answer = JOptionPane.showConfirmDialog(this,
                "¿Cancelar la membresía de " + model.getValueAt(row, 0) + "? El socio perderá el acceso.",
                "Cancelar membresía", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            subscriptions.cancel(selected.getId());
            refresh();
        } catch (NextFitException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo cancelar",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Colors the state text: green when active, amber when it ends soon, red when cancelled. */
    private static final class StateRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setForeground(colorOf(String.valueOf(value)));
            setFont(getFont().deriveFont(Font.BOLD));
            return this;
        }

        private static Color colorOf(String state) {
            if (state.equals(SubscriptionStatus.ACTIVE.getLabel())) {
                return Theme.SUCCESS;
            }
            if (state.equals("Por vencer")) {
                return Theme.WARNING;
            }
            if (state.equals(SubscriptionStatus.CANCELLED.getLabel())) {
                return Theme.DANGER;
            }
            return Theme.TEXT_MUTED;
        }
    }
}

package com.nextfit.ui.users;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.Session;
import com.nextfit.service.UserService;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen of the users module, only for the administrator: lists the accounts
 * and lets the user create, edit, reset the password and enable or disable
 * them. The rules live in {@link UserService}; this class only collects clicks.
 */
public class UsersPanel implements ModulePanel {

    private static final int STATE_COLUMN = 3;

    private final UserService service;
    private final Session session;
    private final JPanel view = new JPanel(new BorderLayout(0, 16));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Usuario", "Nombre", "Rol", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JButton toggleButton = new JButton("Desactivar");
    private List<User> shown = new ArrayList<>();

    /**
     * Creates the screen.
     *
     * @param service the service that manages the users
     * @param session the session, to know who is acting
     */
    public UsersPanel(UserService service, Session session) {
        this.service = service;
        this.session = session;
        view.setBackground(Theme.BACKGROUND);
        view.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        view.add(buildHeader(), BorderLayout.NORTH);
        view.add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public String id() {
        return "users";
    }

    @Override
    public String title() {
        return "Usuarios";
    }

    @Override
    public String icon() {
        return "shield";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** Only the administrator manages accounts. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN;
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Usuarios del sistema");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JButton newButton = new JButton("+ Nuevo usuario");
        newButton.setBackground(Theme.ACCENT);
        newButton.setForeground(Theme.BACKGROUND);
        newButton.setFont(newButton.getFont().deriveFont(Font.BOLD));
        newButton.addActionListener(e -> openForm(null));
        JButton editButton = new JButton("Editar");
        editButton.addActionListener(e -> editSelected());
        JButton passwordButton = new JButton("Restablecer contraseña");
        passwordButton.addActionListener(e -> resetPassword());
        toggleButton.addActionListener(e -> toggleSelected());

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(passwordButton);
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
        table.getColumnModel().getColumn(STATE_COLUMN).setCellRenderer(new StateCellRenderer());
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

    private void refresh() {
        shown = service.findAll();
        model.setRowCount(0);
        for (User user : shown) {
            model.addRow(new Object[]{user.getUsername(), user.getFullName(), user.getRole().getLabel(),
                    user.isActive() ? "Activo" : "Desactivado"});
        }
        countLabel.setText(shown.size() == 1 ? "1 usuario" : shown.size() + " usuarios");
        updateToggleText();
    }

    private void updateToggleText() {
        User selected = selectedUser();
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Desactivar");
    }

    private User selectedUser() {
        int row = table.getSelectedRow();
        return row < 0 || row >= shown.size() ? null : shown.get(row);
    }

    private User requireSelection() {
        User selected = selectedUser();
        if (selected == null) {
            JOptionPane.showMessageDialog(view, "Selecciona un usuario de la tabla.");
        }
        return selected;
    }

    private void editSelected() {
        User selected = requireSelection();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(User user) {
        new UserFormDialog(SwingUtilities.getWindowAncestor(view), service, user, this::refresh)
                .setVisible(true);
    }

    private void resetPassword() {
        User selected = requireSelection();
        if (selected == null) {
            return;
        }
        JPasswordField field = new JPasswordField(20);
        int answer = JOptionPane.showConfirmDialog(view,
                new Object[]{"Nueva contraseña para " + selected.getUsername()
                        + " (mínimo 8, con letras y números):", field},
                "Restablecer contraseña", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            service.resetPassword(selected.getId(), new String(field.getPassword()));
            JOptionPane.showMessageDialog(view, "Contraseña actualizada.");
        } catch (NextFitException e) {
            showError(e);
        }
    }

    private void toggleSelected() {
        User selected = requireSelection();
        if (selected == null) {
            return;
        }
        try {
            if (selected.isActive()) {
                long actingId = session.getCurrentUser().map(User::getId).orElse(-1L);
                service.deactivate(selected.getId(), actingId);
            } else {
                service.reactivate(selected.getId());
            }
            refresh();
        } catch (NextFitException e) {
            showError(e);
        }
    }

    private void showError(NextFitException e) {
        JOptionPane.showMessageDialog(view, e.getMessage(), "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    /** Shows "Activo" in green and "Desactivado" in gray. */
    private static final class StateCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground("Activo".equals(value) ? Theme.SUCCESS : Theme.TEXT_MUTED);
            setFont(getFont().deriveFont(Font.BOLD));
            return this;
        }
    }
}

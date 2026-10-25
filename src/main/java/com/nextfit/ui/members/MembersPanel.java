package com.nextfit.ui.members;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.Icons;
import com.nextfit.ui.components.MemberAvatar;
import com.nextfit.util.PhotoStorage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Screen of the members module: list with search, registration, editing and
 * deactivation. It only calls {@link MemberService}; it holds no business rules.
 */
public class MembersPanel implements ModulePanel {

    private static final String LIST_CARD = "list";
    private static final String EMPTY_CARD = "empty";
    private static final String[] COLUMNS = {"", "Nombre", "Documento", "Teléfono", "Ingreso", "Estado"};
    private static final int AVATAR_COLUMN = 0;
    private static final int STATE_COLUMN = 5;

    private final MemberService memberService;
    private final PhotoStorage photoStorage;

    private final JPanel view = new JPanel(new BorderLayout(0, 16));
    private final JLabel summary = new JLabel(" ");
    private final JTextField searchField = new JTextField();
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JButton editButton = new JButton("Editar");
    private final JButton toggleButton = new JButton("Dar de baja");
    private List<Member> shown = List.of();

    /**
     * Creates the screen.
     *
     * @param memberService the service that manages the members
     * @param photoStorage  where member photos are copied
     */
    public MembersPanel(MemberService memberService, PhotoStorage photoStorage) {
        this.memberService = memberService;
        this.photoStorage = photoStorage;
        buildScreen();
        reload();
    }

    @Override
    public String id() {
        return "members";
    }

    @Override
    public String title() {
        return "Socios";
    }

    @Override
    public String icon() {
        return "members";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        reload();
    }

    private void buildScreen() {
        view.setBackground(Theme.BACKGROUND);
        view.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        view.add(buildHeader(), BorderLayout.NORTH);
        center.setOpaque(false);
        center.add(buildTable(), LIST_CARD);
        center.add(buildEmptyState(), EMPTY_CARD);
        view.add(center, BorderLayout.CENTER);
        view.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Socios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        summary.setForeground(Theme.TEXT_MUTED);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(summary);

        searchField.putClientProperty("JTextField.placeholderText", "Buscar por nombre o documento");
        searchField.setPreferredSize(new Dimension(280, 38));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reload();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reload();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reload();
            }
        });
        JButton newButton = accentButton("Nuevo socio");
        newButton.addActionListener(e -> openForm(null));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.add(searchField);
        actions.add(newButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(56);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SURFACE_ALT);
        table.setSelectionForeground(Theme.TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setBackground(Theme.SURFACE_ALT);
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(AVATAR_COLUMN).setMaxWidth(64);
        table.getColumnModel().getColumn(AVATAR_COLUMN).setCellRenderer(new AvatarCellRenderer());
        table.getColumnModel().getColumn(STATE_COLUMN).setCellRenderer(new StateCellRenderer());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedMember() != null) {
                    openForm(selectedMember());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    private JPanel buildEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(Theme.SURFACE);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(Icons.of("members", 72, Theme.SURFACE_ALT));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Todavía no hay socios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel("Registra el primero con el botón «Nuevo socio».");
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(icon);
        box.add(Box.createVerticalStrut(16));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(hint);
        empty.add(box);
        return empty;
    }

    private JPanel buildFooter() {
        editButton.addActionListener(e -> openForm(selectedMember()));
        toggleButton.addActionListener(e -> toggleActive());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setOpaque(false);
        footer.add(editButton);
        footer.add(toggleButton);
        updateButtons();
        return footer;
    }

    private JButton accentButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Theme.ACCENT);
        button.setForeground(Theme.BACKGROUND);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setPreferredSize(new Dimension(130, 38));
        return button;
    }

    private void reload() {
        List<Member> all = memberService.findAll();
        long active = all.stream().filter(Member::isActive).count();
        summary.setText(all.size() + " socios · " + active + " activos");
        shown = memberService.search(searchField.getText());
        tableModel.setRowCount(0);
        for (Member member : shown) {
            tableModel.addRow(new Object[]{member, member.getFullName(), member.getDocumentNumber(),
                    member.getPhone(), member.getJoinDate().format(MemberFormDialog.DATE_FORMAT),
                    member.isActive() ? "Activo" : "De baja"});
        }
        cards.show(center, all.isEmpty() ? EMPTY_CARD : LIST_CARD);
        updateButtons();
    }

    private Member selectedMember() {
        int row = table.getSelectedRow();
        return row < 0 || row >= shown.size() ? null : shown.get(row);
    }

    private void updateButtons() {
        Member selected = selectedMember();
        editButton.setEnabled(selected != null);
        toggleButton.setEnabled(selected != null);
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Dar de baja");
    }

    private void openForm(Member existing) {
        new MemberFormDialog(SwingUtilities.getWindowAncestor(view), memberService, photoStorage,
                existing, this::reload).setVisible(true);
    }

    private void toggleActive() {
        Member selected = selectedMember();
        if (selected == null) {
            return;
        }
        try {
            if (selected.isActive()) {
                int answer = JOptionPane.showConfirmDialog(view,
                        "¿Dar de baja a " + selected.getFullName() + "? Sus datos se conservan.",
                        "Dar de baja", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
                memberService.deactivate(selected.getId());
            } else {
                memberService.reactivate(selected.getId());
            }
            reload();
        } catch (NextFitException e) {
            JOptionPane.showMessageDialog(view, e.getMessage(), "No se pudo completar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Draws the round photo (or initials) of the member in the first column. */
    private static final class AvatarCellRenderer implements TableCellRenderer {

        private final MemberAvatar avatar = new MemberAvatar(40);
        private final JPanel cell = new JPanel(new GridBagLayout());

        AvatarCellRenderer() {
            cell.add(avatar);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Member member = (Member) value;
            avatar.setMember(member.getFullName(), member.getPhotoPath());
            cell.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return cell;
        }
    }

    /** Shows "Activo" in green and "De baja" in gray. */
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
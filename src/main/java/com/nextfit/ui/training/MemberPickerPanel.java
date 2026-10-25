package com.nextfit.ui.training;

import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable left column to find and select a member: a title, a search box and a
 * table of members. The screen that uses it is told which member was selected.
 */
public class MemberPickerPanel extends JPanel {

    private final MemberService members;
    private final Consumer<Member> onSelected;   // se llama con el socio elegido (o null si se deselecciona)

    private final JTextField searchField = new JTextField();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Nombre", "Documento"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<Member> shown = new ArrayList<>();

    /**
     * @param title      title shown above the search box
     * @param members    member service used to search
     * @param onSelected action to run when the user selects a member
     */
    public MemberPickerPanel(String title, MemberService members, Consumer<Member> onSelected) {
        super(new BorderLayout(0, 12));
        this.members = members;
        this.onSelected = onSelected;
        setOpaque(false);
        setPreferredSize(new Dimension(340, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setForeground(Theme.TEXT);
        searchField.putClientProperty("JTextField.placeholderText", "Buscar socio por nombre o documento...");
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
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                onSelected.accept(row < 0 ? null : shown.get(row));
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 10));
        top.setOpaque(false);
        top.add(titleLabel);
        top.add(searchField);
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        reload();
    }

    /** Reloads the list with the current search text, keeping the selected member if it is still listed. */
    public void reload() {
        long keepId = selectedMember() == null ? -1 : selectedMember().getId();
        shown = members.search(searchField.getText());
        model.setRowCount(0);
        for (Member member : shown) {
            model.addRow(new Object[]{member.getFullName(), member.getDocumentNumber()});
        }
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).getId() == keepId) {
                table.setRowSelectionInterval(i, i);
            }
        }
    }

    /** Returns the selected member, or null when none is selected. */
    public Member selectedMember() {
        int row = table.getSelectedRow();
        return row < 0 || row >= shown.size() ? null : shown.get(row);
    }
}
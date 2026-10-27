package com.nextfit.ui.billing;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipSale;
import com.nextfit.service.PaymentService;
import com.nextfit.ui.Formats;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen with the payments received, the newest first. The staff can search
 * by receipt number or member and open the receipt of any payment again. The
 * rules live in {@link PaymentService}; this class only shows data.
 */
public class PaymentListView extends JPanel {

    private final PaymentService payments;
    private final MemberService members;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Recibo", "Fecha", "Socio", "Método", "Total", "Atendido por"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JLabel totalLabel = new JLabel(" ");
    private final JTextField searchField = new JTextField();
    private List<Payment> shown = new ArrayList<>();

    /**
     * Creates the screen.
     *
     * @param payments the service that knows the payments
     * @param members  the service used to show member names
     */
    public PaymentListView(PaymentService payments, MemberService members) {
        super(new BorderLayout(0, 16));
        this.payments = payments;
        this.members = members;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Pagos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        searchField.putClientProperty("JTextField.placeholderText", "Buscar por recibo o socio");
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
        JButton receiptButton = new JButton("Ver comprobante");
        receiptButton.setBackground(Theme.ACCENT);
        receiptButton.setForeground(Theme.BACKGROUND);
        receiptButton.setFont(receiptButton.getFont().deriveFont(Font.BOLD));
        receiptButton.addActionListener(e -> showReceipt());

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(searchField);
        actions.add(receiptButton);

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
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showReceipt();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        return scroll;
    }

    private JPanel buildFooter() {
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalLabel.setForeground(Theme.ACCENT);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(totalLabel);
        return footer;
    }

    /** Reloads the payments from the service, applying the search text. */
    public void refresh() {
        Map<Long, Member> memberById = new HashMap<>();
        for (Member member : members.findAll()) {
            memberById.put(member.getId(), member);
        }
        String wanted = searchField.getText().trim().toLowerCase();

        shown = new ArrayList<>();
        model.setRowCount(0);
        long sum = 0;
        for (Payment p : payments.findAll()) {
            Member member = memberById.get(p.getMemberId());
            String name = member == null ? "Socio #" + p.getMemberId() : member.getFullName();
            if (!wanted.isEmpty() && !name.toLowerCase().contains(wanted)
                    && !p.getReceiptNumber().toLowerCase().contains(wanted)) {
                continue;
            }
            shown.add(p);
            sum += p.getTotal();
            model.addRow(new Object[]{p.getReceiptNumber(), Formats.dateTime(p.getPaidAt()), name,
                    p.getMethod().getLabel(), Formats.money(p.getTotal()), p.getRegisteredBy()});
        }
        countLabel.setText(shown.size() == 1 ? "1 pago" : shown.size() + " pagos");
        totalLabel.setText("Total listado: " + Formats.money(sum));
    }

    private void showReceipt() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un pago de la tabla.");
            return;
        }
        try {
            Payment selected = shown.get(row);
            MembershipSale sale = payments.findSale(selected.getId());
            Member member = members.findById(selected.getMemberId());
            ReceiptDialog.show(SwingUtilities.getWindowAncestor(this), sale, member);
        } catch (NextFitException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo abrir el comprobante",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}

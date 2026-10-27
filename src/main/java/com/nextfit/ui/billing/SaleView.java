package com.nextfit.ui.billing;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Discount;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.MembershipSale;
import com.nextfit.service.PaymentService;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.Formats;
import com.nextfit.ui.Theme;
import com.nextfit.ui.training.MemberPickerPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

/**
 * Screen to sell a membership: the staff member finds the member, chooses the
 * plan, the payment method and an optional discount, and charges. The screen
 * only collects data and shows the total; the rules live in
 * {@link PaymentService}.
 */
public class SaleView extends JPanel {

    private enum DiscountKind {
        NONE("Sin descuento"), PERCENT("Porcentaje (%)"), FIXED("Monto fijo ($)");

        private final String label;

        DiscountKind(String label) {
            this.label = label;
        }
    }

    private final MembershipPlanService plans;
    private final SubscriptionService subscriptions;
    private final PaymentService payments;
    private final Session session;
    private final Runnable onSold;

    private final MemberPickerPanel picker;
    private final JLabel memberLabel = new JLabel("Selecciona un socio de la lista");
    private final JLabel memberInfo = new JLabel(" ");
    private final JComboBox<MembershipPlan> planBox = new JComboBox<>();
    private final JTextField startField = new JTextField();
    private final JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
    private final JComboBox<DiscountKind> discountBox = new JComboBox<>(DiscountKind.values());
    private final JTextField discountField = new JTextField();
    private final JLabel subtotalLabel = new JLabel("$ 0");
    private final JLabel discountLabel = new JLabel("$ 0");
    private final JLabel totalLabel = new JLabel("$ 0");
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton chargeButton = new JButton("Cobrar y generar comprobante");

    /**
     * Creates the screen.
     *
     * @param plans         the plan service, to list what can be sold
     * @param subscriptions the subscription service, to show the member's current membership
     * @param payments      the payment service that sells
     * @param members       the member service, to search members
     * @param session       the session, to know who is selling
     * @param onSold        what to run after a sale, so other tabs can refresh
     */
    public SaleView(MembershipPlanService plans, SubscriptionService subscriptions, PaymentService payments,
                    MemberService members, Session session, Runnable onSold) {
        super(new BorderLayout(24, 0));
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.session = session;
        this.onSold = onSold;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        this.picker = new MemberPickerPanel("Venta de membresía", members, member -> showMember(member));
        add(picker, BorderLayout.WEST);
        add(buildForm(), BorderLayout.CENTER);
        refresh();
    }

    /** Reloads the plans on sale and the member list. */
    public void refresh() {
        MembershipPlan keep = (MembershipPlan) planBox.getSelectedItem();
        planBox.removeAllItems();
        for (MembershipPlan plan : plans.findActive()) {
            planBox.addItem(plan);
            if (keep != null && keep.getId() == plan.getId()) {
                planBox.setSelectedItem(plan);
            }
        }
        picker.reload();
        updateSummary();
    }

    private JComponent buildForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        memberLabel.setFont(memberLabel.getFont().deriveFont(Font.BOLD, 20f));
        memberLabel.setForeground(Theme.TEXT);
        memberInfo.setForeground(Theme.TEXT_MUTED);
        startField.putClientProperty("JTextField.placeholderText", "Vacío = hoy (AAAA-MM-DD)");
        discountField.putClientProperty("JTextField.placeholderText", "Valor del descuento");
        discountField.setEnabled(false);
        planBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof MembershipPlan plan) {
                    setText(plan.getName() + "  ·  " + plan.getDurationDays() + " días  ·  "
                            + Formats.money(plan.getPrice()));
                }
                return this;
            }
        });
        methodBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof PaymentMethod method) {
                    setText(method.getLabel());
                }
                return this;
            }
        });
        discountBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof DiscountKind kind) {
                    setText(kind.label);
                }
                return this;
            }
        });
        planBox.addActionListener(e -> updateSummary());
        discountBox.addActionListener(e -> {
            discountField.setEnabled(discountBox.getSelectedItem() != DiscountKind.NONE);
            updateSummary();
        });
        discountField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSummary();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSummary();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSummary();
            }
        });

        for (JLabel label : List.of(subtotalLabel, discountLabel)) {
            label.setForeground(Theme.TEXT);
        }
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 30f));
        totalLabel.setForeground(Theme.ACCENT);
        messageLabel.setForeground(Theme.DANGER);
        chargeButton.setBackground(Theme.ACCENT);
        chargeButton.setForeground(Theme.BACKGROUND);
        chargeButton.setFont(chargeButton.getFont().deriveFont(Font.BOLD, 14f));
        chargeButton.addActionListener(e -> charge());

        int row = 0;
        addFull(card, row++, memberLabel, new Insets(0, 0, 4, 0));
        addFull(card, row++, memberInfo, new Insets(0, 0, 18, 0));
        addRow(card, row++, "Plan", planBox);
        addRow(card, row++, "Inicio", startField);
        addRow(card, row++, "Método de pago", methodBox);
        addRow(card, row++, "Descuento", discountBox);
        addRow(card, row++, "Valor", discountField);
        addRow(card, row++, "Subtotal", subtotalLabel);
        addRow(card, row++, "Descuento aplicado", discountLabel);
        addRow(card, row++, "Total a pagar", totalLabel);
        addFull(card, row++, messageLabel, new Insets(10, 0, 0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        card.add(chargeButton, c);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        card.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, filler);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void addFull(JPanel panel, int row, JComponent component, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        panel.add(component, c);
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 0, 6, 18);
        c.anchor = GridBagConstraints.WEST;
        JLabel text = new JLabel(label);
        text.setForeground(Theme.TEXT_MUTED);
        panel.add(text, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void showMember(Member member) {
        messageLabel.setText(" ");
        if (member == null) {
            memberLabel.setText("Selecciona un socio de la lista");
            memberInfo.setText(" ");
            return;
        }
        memberLabel.setText(member.getFullName());
        StringBuilder info = new StringBuilder("Documento " + member.getDocumentNumber());
        if (!member.isActive()) {
            info.append("  ·  Socio dado de baja");
        }
        subscriptions.findCurrentFor(member.getId()).ifPresentOrElse(
                s -> info.append("  ·  Membresía vigente hasta ").append(Formats.date(s.getEndDate()))
                        .append("; la nueva empieza a continuación"),
                () -> info.append("  ·  Sin membresía vigente"));
        memberInfo.setText(info.toString());
    }

    // Recalculates the three amounts. A discount that is still being typed counts as zero.
    private void updateSummary() {
        MembershipPlan plan = (MembershipPlan) planBox.getSelectedItem();
        long subtotal = plan == null ? 0 : plan.getPrice();
        long off;
        try {
            off = buildDiscount().amountOff(subtotal);
        } catch (NextFitException e) {
            off = 0;
        }
        subtotalLabel.setText(Formats.money(subtotal));
        discountLabel.setText(off > 0 ? "-" + Formats.money(off) : Formats.money(0));
        totalLabel.setText(Formats.money(subtotal - off));
    }

    private Discount buildDiscount() {
        DiscountKind kind = (DiscountKind) discountBox.getSelectedItem();
        if (kind == null || kind == DiscountKind.NONE) {
            return new NoDiscount();
        }
        if (kind == DiscountKind.PERCENT) {
            return new PercentageDiscount(Formats.parseWholeNumber("descuento", discountField.getText()));
        }
        return new FixedDiscount(Formats.parseMoney("descuento", discountField.getText()));
    }

    private void charge() {
        Member member = picker.selectedMember();
        MembershipPlan plan = (MembershipPlan) planBox.getSelectedItem();
        if (member == null) {
            showError("Selecciona un socio de la lista");
            return;
        }
        if (plan == null) {
            showError("No hay planes en venta; crea uno en la pestaña Planes");
            return;
        }
        try {
            LocalDate start = Formats.parseDate("inicio", startField.getText());
            Discount discount = buildDiscount();
            int answer = JOptionPane.showConfirmDialog(this,
                    "¿Cobrar " + totalLabel.getText() + " a " + member.getFullName() + "?",
                    "Confirmar venta", JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            String seller = session.getCurrentUser().map(u -> u.getUsername()).orElse("");
            MembershipSale sale = payments.sellMembership(member.getId(), plan.getId(), start,
                    (PaymentMethod) methodBox.getSelectedItem(), discount, seller);
            clearForm();
            onSold.run();
            ReceiptDialog.show(SwingUtilities.getWindowAncestor(this), sale, member);
            showMember(member);
        } catch (NextFitException e) {
            showError(e.getMessage());
        }
    }

    private void clearForm() {
        startField.setText("");
        discountBox.setSelectedItem(DiscountKind.NONE);
        discountField.setText("");
        methodBox.setSelectedItem(PaymentMethod.CASH);
        messageLabel.setText(" ");
        updateSummary();
    }

    private void showError(String text) {
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(text);
    }
}

package com.nextfit.ui.billing;

import com.nextfit.model.Member;
import com.nextfit.service.MembershipSale;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Window that shows the receipt of a sale. The text comes from
 * {@link ReceiptText}; this class only shows it and offers to copy it.
 */
public final class ReceiptDialog extends JDialog {

    private ReceiptDialog(Window owner, MembershipSale sale, Member member) {
        super(owner, "Comprobante " + sale.payment().getReceiptNumber(), ModalityType.APPLICATION_MODAL);
        String text = ReceiptText.of(sale, member);

        JLabel title = new JLabel("Pago registrado");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Theme.SUCCESS);

        JTextArea body = new JTextArea(text);
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        body.setBackground(Theme.SURFACE_ALT);
        body.setForeground(Theme.TEXT);
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JButton copyButton = new JButton("Copiar");
        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            copyButton.setText("Copiado");
        });
        JButton closeButton = new JButton("Cerrar");
        closeButton.setBackground(Theme.ACCENT);
        closeButton.setForeground(Theme.BACKGROUND);
        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD));
        closeButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(closeButton);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(copyButton);
        buttons.add(closeButton);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        content.add(title, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the receipt and waits until the user closes it.
     *
     * @param owner  the window the dialog belongs to
     * @param sale   the sale to show
     * @param member the member who paid
     */
    public static void show(Window owner, MembershipSale sale, Member member) {
        new ReceiptDialog(owner, sale, member).setVisible(true);
    }
}

package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipSale;
import com.nextfit.service.PaymentService;
import com.nextfit.ui.Formats;
import com.nextfit.ui.billing.ReceiptText;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen with the payments received, the newest first. The staff can search by
 * receipt number or member and open the receipt of any payment again. The rules
 * live in {@link PaymentService}; this class only shows data.
 */
final class PaymentsPane extends VBox {

    /** One table row: the payment with the member name already looked up. */
    private record Row(Payment payment, String member) {
    }

    private final PaymentService payments;
    private final MemberService members;
    private final TableView<Row> table = new TableView<>();
    private final Label countLabel = new Label();
    private final Label totalLabel = new Label();
    private final TextField searchField = new TextField();

    /**
     * @param payments the service that knows the payments
     * @param members  the service used to show member names
     */
    PaymentsPane(PaymentService payments, MemberService members) {
        super(16);
        this.payments = payments;
        this.members = members;
        getChildren().addAll(buildHeader(), buildTable(), buildFooter());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Pagos");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        searchField.setPromptText("Buscar por recibo o socio");
        searchField.getStyleClass().add("nf-input");
        searchField.setPrefSize(280, 44);
        searchField.textProperty().addListener((obs, old, now) -> refresh());
        Button receipt = new Button("Ver comprobante");
        receipt.getStyleClass().add("nf-primary-small");
        receipt.setOnAction(e -> showReceipt());

        HBox header = new HBox(10, titles, searchField, receipt);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<Row, String> number = FxTables.column("Recibo", 130, r -> r.payment().getReceiptNumber());
        TableColumn<Row, String> date = FxTables.column("Fecha", 170, r -> Formats.dateTime(r.payment().getPaidAt()));
        TableColumn<Row, String> member = FxTables.column("Socio", 240, Row::member);
        TableColumn<Row, String> method = FxTables.column("Método", 130, r -> r.payment().getMethod().getLabel());
        TableColumn<Row, String> total = FxTables.column("Total", 130, r -> Formats.money(r.payment().getTotal()));
        TableColumn<Row, String> by = FxTables.column("Atendido por", 140, r -> r.payment().getRegisteredBy());
        table.getColumns().addAll(List.of(number, date, member, method, total, by));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No hay pagos para mostrar"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(300);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showReceipt();
            }
        });
        return table;
    }

    private Node buildFooter() {
        totalLabel.getStyleClass().add("nf-footer-total");
        HBox footer = new HBox(totalLabel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 4, 0, 0));
        return footer;
    }

    /** Reloads the payments from the service, applying the search text. */
    void refresh() {
        Map<Long, Member> memberById = new HashMap<>();
        for (Member member : members.findAll()) {
            memberById.put(member.getId(), member);
        }
        String wanted = searchField.getText().trim().toLowerCase();
        List<Row> rows = new ArrayList<>();
        long sum = 0;
        for (Payment p : payments.findAll()) {
            Member member = memberById.get(p.getMemberId());
            String name = member == null ? "Socio #" + p.getMemberId() : member.getFullName();
            if (!wanted.isEmpty() && !name.toLowerCase().contains(wanted)
                    && !p.getReceiptNumber().toLowerCase().contains(wanted)) {
                continue;
            }
            rows.add(new Row(p, name));
            sum += p.getTotal();
        }
        table.getItems().setAll(rows);
        countLabel.setText(rows.size() == 1 ? "1 pago" : rows.size() + " pagos");
        totalLabel.setText("Total listado: " + Formats.money(sum));
    }

    private void showReceipt() {
        Row selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona un pago", "Elige un pago de la tabla primero.");
            return;
        }
        try {
            Payment payment = selected.payment();
            MembershipSale sale = payments.findSale(payment.getId());
            Member member = members.findById(payment.getMemberId());
            FxDialogs.document(this, "Comprobante " + payment.getReceiptNumber(), "Comprobante de pago",
                    ReceiptText.of(sale, member));
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
    }
}

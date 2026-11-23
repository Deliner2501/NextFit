package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipSale;
import com.nextfit.service.PaymentService;
import com.nextfit.service.mail.EmailNotifications;
import com.nextfit.service.mail.EmailNotifications.ReceiptStatus;
import com.nextfit.ui.Formats;
import com.nextfit.ui.billing.ReceiptText;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
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
import javafx.util.Duration;

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

    private final FxLoader dataLoader = new FxLoader();

    /** One table row: the payment with the member name already looked up. */
    private record Row(Payment payment, String member, ReadOnlyStringWrapper emailStatus) {
    }

    private final PaymentService payments;
    private final MemberService members;
    private final EmailNotifications email;
    private final TableView<Row> table = new TableView<>();
    private final Label countLabel = new Label();
    private final Label totalLabel = new Label();
    private final TextField searchField = new TextField();
    private final Timeline statusRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshEmailStatus()));
    private boolean checkingEmail;

    /**
     * @param payments the service that knows the payments
     * @param members  the service used to show member names
     * @param email    the email service, or null when disabled
     */
    PaymentsPane(PaymentService payments, MemberService members, EmailNotifications email) {
        super(16);
        this.payments = payments;
        this.members = members;
        this.email = email;
        getChildren().addAll(buildHeader(), buildTable(), buildFooter());
        VBox.setVgrow(table, Priority.ALWAYS);
        if (email != null) {
            statusRefresh.setCycleCount(Animation.INDEFINITE);
            sceneProperty().addListener((observable, previous, scene) -> {
                if (scene == null) {
                    statusRefresh.stop();
                } else {
                    refreshEmailStatus();
                    statusRefresh.play();
                }
            });
        }
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
        FxTables.alignNumbers(total);
        TableColumn<Row, String> by = FxTables.column("Atendido por", 140, r -> r.payment().getRegisteredBy());
        TableColumn<Row, String> delivery = new TableColumn<>("Correo");
        delivery.setPrefWidth(130);
        delivery.setSortable(false);
        delivery.setCellValueFactory(cell -> cell.getValue().emailStatus().getReadOnlyProperty());
        FxTables.colorize(delivery, status -> switch (status) {
            case "Enviado" -> "nf-state-on";
            case "Pendiente" -> "nf-state-warn";
            case "Fallido" -> "nf-state-bad";
            default -> "nf-state-off";
        });
        table.getColumns().addAll(List.of(number, date, member, method, total, by, delivery));
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
        HBox footer = new HBox(12, totalLabel);
        if (email != null) {
            Button retry = new Button("Reintentar correo");
            retry.getStyleClass().add("nf-secondary");
            retry.setOnAction(e -> retryEmail(retry));
            footer.getChildren().add(0, retry);
        }
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 4, 0, 0));
        return footer;
    }

    /** Lets staff retry a failed email after checking that Gmail did not already send it. */
    private void retryEmail(Button button) {
        Row selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona un pago", "Elige un pago de la tabla primero.");
            return;
        }
        if (!FxDialogs.confirm(this, "Reintentar correo",
                "Comprueba en Enviados de Gmail que el comprobante " + selected.payment().getReceiptNumber()
                        + " no haya salido. ¿Quieres reintentar el correo de este pago?", "Reintentar")) {
            return;
        }
        button.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return email.retryReceipt(selected.payment().getId());
            }
        };
        task.setOnSucceeded(e -> {
            button.setDisable(false);
            refreshEmailStatus();
            FxDialogs.message(this, "Correo del comprobante", task.getValue());
        });
        task.setOnFailed(e -> {
            button.setDisable(false);
            FxDialogs.message(this, "Correo del comprobante", "No se pudo consultar o actualizar el registro del envío.");
        });
        Thread thread = new Thread(task, "nextfit-retry-receipt");
        thread.setDaemon(true);
        thread.start();
    }

    /** Reloads the payments from the service, applying the search text. */
    void refresh() {
        String wanted = searchField.getText().trim().toLowerCase();
        dataLoader.load(this, () -> {
            Map<Long, Member> memberById = new HashMap<>();
            for (Member member : members.findAll()) {
                memberById.put(member.getId(), member);
            }
            List<Row> rows = new ArrayList<>();
            for (Payment payment : payments.findAll()) {
                Member member = memberById.get(payment.getMemberId());
                String name = member == null ? "Socio #" + payment.getMemberId() : member.getFullName();
                if (!wanted.isEmpty() && !name.toLowerCase().contains(wanted)
                        && !payment.getReceiptNumber().toLowerCase().contains(wanted)) {
                    continue;
                }
                rows.add(new Row(payment, name,
                        new ReadOnlyStringWrapper(email == null ? "Desactivado" : "Consultando…")));
            }
            return rows;
        }, rows -> {
            table.getItems().setAll(rows);
            countLabel.setText(rows.size() == 1 ? "1 pago" : rows.size() + " pagos");
            long total = rows.stream().mapToLong(row -> row.payment().getTotal()).sum();
            totalLabel.setText("Total listado: " + Formats.money(total));
            refreshEmailStatus();
        });
    }

    /** Reads delivery records in the background while leaving table selection intact. */
    private void refreshEmailStatus() {
        if (email == null || getScene() == null || checkingEmail || table.getItems().isEmpty()) {
            return;
        }
        List<Row> rows = List.copyOf(table.getItems());
        checkingEmail = true;
        Task<List<ReceiptStatus>> task = new Task<>() {
            @Override
            protected List<ReceiptStatus> call() {
                return rows.stream().map(row -> email.receiptStatus(row.payment())).toList();
            }
        };
        task.setOnSucceeded(e -> {
            checkingEmail = false;
            List<ReceiptStatus> statuses = task.getValue();
            for (int index = 0; index < rows.size(); index++) {
                String label = switch (statuses.get(index)) {
                    case SENT -> "Enviado";
                    case FAILED -> "Fallido";
                    case PENDING -> "Pendiente";
                    case NOT_ATTEMPTED -> "Sin intento";
                    case UNAVAILABLE -> "No disponible";
                };
                rows.get(index).emailStatus().set(label);
            }
        });
        task.setOnFailed(e -> checkingEmail = false);
        Thread thread = new Thread(task, "nextfit-receipt-status");
        thread.setDaemon(true);
        thread.start();
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

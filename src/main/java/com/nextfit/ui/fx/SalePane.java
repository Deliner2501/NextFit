package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Discount;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.RenewalDiscount;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.MembershipSale;
import com.nextfit.service.PaymentService;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.Formats;
import com.nextfit.ui.billing.ReceiptText;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Screen to sell a membership: the staff member finds the member, chooses the
 * plan, the payment method and an optional discount, and charges. The screen
 * only collects data and shows the total; the rules live in {@link PaymentService}.
 */
final class SalePane extends HBox {
    private final FxLoader dataLoader = new FxLoader();

    private static final Discount NO_BENEFIT = new NoDiscount();
    private final FxLoader benefitsLoader = new FxLoader();
    private boolean checkingBenefits;

    private final MembershipPlanService plans;
    private final SubscriptionService subscriptions;
    private final PaymentService payments;
    private final Session session;
    private final Runnable onSold;

    private final MemberPicker picker;
    private final Label memberLabel = new Label("Selecciona un socio de la lista");
    private final Label memberInfo = new Label(" ");
    private final ComboBox<MembershipPlan> planBox = new ComboBox<>();
    private final DatePicker startPicker = new DatePicker();
    private final ComboBox<PaymentMethod> methodBox = new ComboBox<>();
    private final ComboBox<Discount> discountBox = new ComboBox<>();
    private final Label subtotalLabel = new Label("$ 0");
    private final Label discountLabel = new Label("$ 0");
    private final Label totalLabel = new Label("$ 0");
    private final Label saleRecipient = new Label("Selecciona un socio");
    private final Label salePlan = new Label();
    private final Label messageLabel = new Label(" ");
    private final Button chargeButton = new Button("Cobrar y generar comprobante");

    /**
     * @param plans         the plan service, to list what can be sold
     * @param subscriptions the subscription service, to show the member's current membership
     * @param payments      the payment service that sells
     * @param members       the member service, to search members
     * @param session       the session, to know who is selling
     * @param onSold        what to run after a sale, so other tabs can refresh
     */
    SalePane(MembershipPlanService plans, SubscriptionService subscriptions, PaymentService payments,
             MemberService members, Session session, Runnable onSold) {
        super(24);
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.session = session;
        this.onSold = onSold;
        this.picker = new MemberPicker("Venta de membresía", members, this::showMember);
        Node form = buildForm();
        HBox.setHgrow(form, Priority.ALWAYS);
        getChildren().addAll(picker, form);
    }

    /** Reloads the plans on sale and the member list. */
    void refresh() {
        picker.reload();
        MembershipPlan keep = planBox.getValue();
        dataLoader.load(this, plans::findActive, found -> {
            planBox.getItems().setAll(found);
            planBox.setValue(null);
            for (MembershipPlan plan : planBox.getItems()) {
                if (keep != null && keep.getId() == plan.getId()) {
                    planBox.setValue(plan);
                }
            }
            if (planBox.getValue() == null && !planBox.getItems().isEmpty()) {
                planBox.setValue(planBox.getItems().get(0));
            }
            updateSummary();
        });
    }

    private Node buildForm() {
        memberLabel.getStyleClass().add("nf-member-name");
        memberInfo.getStyleClass().add("nf-muted");
        memberInfo.setWrapText(true);

        planBox.setConverter(converter(plan -> plan == null ? ""
                : plan.getName() + "  ·  " + Formats.days(plan.getDurationDays()) + "  ·  " + Formats.money(plan.getPrice())));
        planBox.setPromptText("No hay planes en venta");
        methodBox.getItems().addAll(PaymentMethod.values());
        methodBox.setConverter(converter(method -> method == null ? "" : method.getLabel()));
        methodBox.setValue(PaymentMethod.CASH);
        discountBox.getItems().setAll(NO_BENEFIT);
        discountBox.setConverter(converter(discount -> discount instanceof RenewalDiscount benefit
                ? benefit.label() + " · " + benefit.getValue() + " %" : "Sin beneficio"));
        discountBox.setValue(NO_BENEFIT);
        startPicker.setPromptText("Hoy");
        for (Region control : new Region[] {planBox, methodBox, discountBox, startPicker}) {
            control.setMaxWidth(Double.MAX_VALUE);
            control.setPrefHeight(44);
        }

        planBox.valueProperty().addListener((obs, old, now) -> updateSummary());
        discountBox.valueProperty().addListener((obs, old, now) -> updateSummary());

        totalLabel.getStyleClass().add("nf-total");
        messageLabel.getStyleClass().add("nf-error");
        messageLabel.setWrapText(true);
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
        messageLabel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !messageLabel.getText().isBlank(), messageLabel.textProperty()));
        chargeButton.getStyleClass().add("nf-primary");
        chargeButton.setMaxWidth(Double.MAX_VALUE);
        chargeButton.setPrefHeight(52);
        chargeButton.setOnAction(e -> charge());

        Label summaryTitle = new Label("Resumen de la venta");
        summaryTitle.getStyleClass().add("nf-card-title");
        saleRecipient.getStyleClass().add("nf-muted");
        saleRecipient.setWrapText(true);
        salePlan.getStyleClass().add("nf-muted-small");
        salePlan.setWrapText(true);
        VBox summary = new VBox(10, summaryTitle, new VBox(3, saleRecipient, salePlan),
                summaryRow("Subtotal", subtotalLabel),
                summaryRow("Descuento aplicado", discountLabel), summaryRow("Total a pagar", totalLabel));
        summary.getChildren().add(chargeButton);
        summary.getStyleClass().add("nf-summary");
        summary.setPadding(new Insets(16, 18, 16, 18));
        summary.setMaxHeight(Region.USE_PREF_SIZE);

        HBox startAndMethod = twoColumns(FxDialogs.labeled("Fecha de inicio", startPicker),
                FxDialogs.labeled("Método de pago", methodBox));
        VBox discounts = FxDialogs.labeled("Beneficio de renovación", discountBox);

        VBox card = new VBox(14, new VBox(4, memberLabel, memberInfo), FxDialogs.labeled("Plan", planBox),
                startAndMethod, discounts, messageLabel, summary);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        BorderPane checkout = new BorderPane(card);
        BorderPane.setAlignment(card, Pos.TOP_LEFT);
        BorderPane.setAlignment(summary, Pos.TOP_LEFT);
        BorderPane.setMargin(summary, new Insets(0, 0, 0, 16));
        widthProperty().addListener((obs, old, width) -> {
            boolean wide = width.doubleValue() >= 1120;
            if (wide && checkout.getRight() == null) {
                card.getChildren().remove(summary);
                summary.setPrefWidth(280);
                checkout.setRight(summary);
            } else if (!wide && checkout.getRight() != null) {
                checkout.setRight(null);
                summary.setPrefWidth(Region.USE_COMPUTED_SIZE);
                card.getChildren().add(summary);
            }
        });
        return checkout;
    }

    private static HBox twoColumns(Node left, Node right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        ((Region) left).setPrefWidth(10);
        ((Region) right).setPrefWidth(10);
        return row;
    }

    private static HBox summaryRow(String text, Label value) {
        Label caption = new Label(text);
        caption.getStyleClass().add("nf-muted");
        value.setMaxWidth(Double.MAX_VALUE);
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox row = new HBox(caption, space, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static <T> StringConverter<T> converter(Function<T, String> text) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return text.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    private void showMember(Member member) {
        checkingBenefits = member != null;
        discountBox.getItems().setAll(NO_BENEFIT);
        discountBox.setValue(NO_BENEFIT);
        refreshChargeAvailability(member);
        saleRecipient.setText(member == null ? "Selecciona un socio" : member.getFullName());
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
        benefitsLoader.load(() -> payments.availableBenefits(member.getId()), options -> {
            Member current = picker.selected();
            if (current != null && current.getId() == member.getId()) {
                checkingBenefits = false;
                discountBox.getItems().addAll(options);
                if (!options.isEmpty()) {
                    discountBox.setValue(options.getFirst());
                }
                updateSummary();
            }
        }, error -> showError("No se pudieron consultar los beneficios. Vuelve a seleccionar el socio."));
    }

    // Recalculates the amount for the selected plan and eligible benefit.
    private void updateSummary() {
        MembershipPlan plan = planBox.getValue();
        refreshChargeAvailability(picker.selected());
        salePlan.setText(plan == null ? "Sin plan" : plan.getName() + " · " + Formats.days(plan.getDurationDays()));
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

    /** Keeps the primary action disabled until a member and a plan can be charged. */
    private void refreshChargeAvailability(Member member) {
        chargeButton.setDisable(checkingBenefits || member == null || !member.isActive() || planBox.getValue() == null);
    }

    private Discount buildDiscount() {
        Discount selected = discountBox.getValue();
        return selected == null ? NO_BENEFIT : selected;
    }

    private void charge() {
        Member member = picker.selected();
        MembershipPlan plan = planBox.getValue();
        if (member == null) {
            showError("Selecciona un socio de la lista");
            return;
        }
        if (plan == null) {
            showError("No hay planes en venta; crea uno en la pestaña Planes");
            return;
        }
        try {
            LocalDate start = startPicker.getValue();
            Discount discount = buildDiscount();
            if (!FxDialogs.confirm(this, "Confirmar venta",
                    "¿Cobrar " + totalLabel.getText() + " a " + member.getFullName() + "?", "Cobrar")) {
                return;
            }
            String seller = session.getCurrentUser().map(u -> u.getUsername()).orElse("");
            MembershipSale sale = payments.sellMembership(member.getId(), plan.getId(), start,
                    methodBox.getValue(), discount, seller);
            clearForm();
            onSold.run();
            FxDialogs.document(this, "Comprobante " + sale.payment().getReceiptNumber(), "Pago registrado",
                    ReceiptText.of(sale, member));
            showMember(member);
        } catch (NextFitException e) {
            showError(e.getMessage());
        }
    }

    private void clearForm() {
        startPicker.setValue(null);
        discountBox.setValue(NO_BENEFIT);
        methodBox.setValue(PaymentMethod.CASH);
        messageLabel.setText(" ");
        updateSummary();
    }

    private void showError(String text) {
        messageLabel.setText(text);
    }
}

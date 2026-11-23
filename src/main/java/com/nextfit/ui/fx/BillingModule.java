package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.mail.EmailNotifications;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import java.util.List;

/**
 * Memberships and payments module in JavaFX. It groups its screens in tabs:
 * sale, subscriptions, payments and plans. It is for the administrator and the
 * reception only.
 */
public final class BillingModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final SalePane sale;
    private final SubscriptionsPane subscriptions;
    private final PaymentsPane payments;
    private final PlansPane plans;
    private final Tab plansTab;
    private final Session session;

    /**
     * @param planService         the service that manages the plans
     * @param subscriptionService the service that knows the memberships of each member
     * @param paymentService      the service that sells memberships
     * @param memberService       the service used to find members
     * @param session             the session, to know who is selling
     * @param email               the email service, or null when disabled
     */
    public BillingModule(MembershipPlanService planService, SubscriptionService subscriptionService,
                         PaymentService paymentService, MemberService memberService, Session session,
                         EmailNotifications email) {
        this.session = session;
        this.plans = new PlansPane(planService, session);
        this.plansTab = tab("Planes", plans);
        this.subscriptions = new SubscriptionsPane(subscriptionService, planService, memberService, session);
        this.payments = new PaymentsPane(paymentService, memberService, email);
        this.sale = new SalePane(planService, subscriptionService, paymentService, memberService, session,
                () -> {
                    subscriptions.refresh();
                    payments.refresh();
                });
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Venta", sale), tab("Suscripciones", subscriptions), tab("Pagos", payments));
        // A plan created, edited or withdrawn in one tab must show up in the other.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refreshTabs());
    }

    @Override
    public String id() {
        return "billing";
    }

    @Override
    public List<String> keywords() {
        return List.of("planes", "pagos", "cobros", "suscripción", "recibo", "membresía");
    }

    @Override
    public String title() {
        return "Membresías y pagos";
    }

    @Override
    public String icon() {
        return FxIcons.BILLING;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        if (session.hasRole(Role.ADMIN)) {
            if (!tabs.getTabs().contains(plansTab)) {
                tabs.getTabs().add(plansTab);
            }
        } else if (tabs.getTabs().contains(plansTab)) {
            if (tabs.getSelectionModel().getSelectedItem() == plansTab) {
                tabs.getSelectionModel().selectFirst();
            }
            tabs.getTabs().remove(plansTab);
        }
        refreshTabs();
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private void refreshTabs() {
        switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> sale.refresh();
            case 1 -> subscriptions.refresh();
            case 2 -> payments.refresh();
            case 3 -> plans.refresh();
            default -> {}
        }
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }
}

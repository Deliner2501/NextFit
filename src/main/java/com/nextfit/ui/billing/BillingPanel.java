package com.nextfit.ui.billing;

import com.nextfit.model.Role;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Main screen of the memberships and payments module. It groups the screens of
 * the module in tabs: sale, subscriptions, payments and plans.
 */
public class BillingPanel implements ModulePanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final SaleView sale;
    private final SubscriptionListView subscriptions;
    private final PaymentListView payments;
    private final PlanListView plans;

    /**
     * Creates the screen.
     *
     * @param planService         the service that manages the plans
     * @param subscriptionService the service that knows the memberships of each member
     * @param paymentService      the service that sells memberships
     * @param memberService       the service used to find members
     * @param session             the session, to know who is selling
     */
    public BillingPanel(MembershipPlanService planService, SubscriptionService subscriptionService,
                        PaymentService paymentService, MemberService memberService, Session session) {
        this.plans = new PlanListView(planService);
        this.subscriptions = new SubscriptionListView(subscriptionService, planService, memberService);
        this.payments = new PaymentListView(paymentService, memberService);
        this.sale = new SaleView(planService, subscriptionService, paymentService, memberService, session,
                () -> {
                    subscriptions.refresh();
                    payments.refresh();
                });
        tabs.setBackground(Theme.BACKGROUND);
        tabs.addTab("Venta", sale);
        tabs.addTab("Suscripciones", subscriptions);
        tabs.addTab("Pagos", payments);
        tabs.addTab("Planes", plans);
        // A plan created, edited or withdrawn in one tab must show up in the other.
        tabs.addChangeListener(e -> refreshSelectedTab());
    }

    @Override
    public String id() {
        return "billing";
    }

    @Override
    public String title() {
        return "Membresías y pagos";
    }

    @Override
    public String icon() {
        return "payments";
    }

    @Override
    public JComponent view() {
        return tabs;
    }

    @Override
    public void onShow() {
        sale.refresh();
        subscriptions.refresh();
        payments.refresh();
        plans.refresh();
    }

    private void refreshSelectedTab() {
        if (tabs.getSelectedComponent() == sale) {
            sale.refresh();
        } else if (tabs.getSelectedComponent() == subscriptions) {
            subscriptions.refresh();
        } else if (tabs.getSelectedComponent() == payments) {
            payments.refresh();
        } else if (tabs.getSelectedComponent() == plans) {
            plans.refresh();
        }
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }
}

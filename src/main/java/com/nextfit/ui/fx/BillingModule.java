package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

/**
 * Memberships and payments module in JavaFX. It groups its screens in tabs:
 * sale, subscriptions, payments and plans. It is for the administrator and the
 * reception only.
 */
public final class BillingModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final SalePane sale;
    private final PlansPane plans;

    /**
     * @param planService         the service that manages the plans
     * @param subscriptionService the service that knows the memberships of each member
     * @param paymentService      the service that sells memberships
     * @param memberService       the service used to find members
     * @param session             the session, to know who is selling
     */
    public BillingModule(MembershipPlanService planService, SubscriptionService subscriptionService,
                         PaymentService paymentService, MemberService memberService, Session session) {
        this.plans = new PlansPane(planService);
        this.sale = new SalePane(planService, subscriptionService, paymentService, memberService, session,
                () -> { });
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Venta", sale), tab("Suscripciones", pending()), tab("Pagos", pending()),
                tab("Planes", plans));
        // A plan created, edited or withdrawn in one tab must show up in the other.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refreshTabs());
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
        return FxIcons.BILLING;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        refreshTabs();
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private void refreshTabs() {
        sale.refresh();
        plans.refresh();
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }

    private static Node pending() {
        Label notice = new Label("Esta pestaña se migra en la siguiente entrega.");
        notice.getStyleClass().add("nf-muted");
        StackPane box = new StackPane(notice);
        box.getStyleClass().add("nf-card");
        box.setMinHeight(240);
        return box;
    }
}

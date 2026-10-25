package com.nextfit.ui.billing;

import com.nextfit.model.Role;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Main screen of the memberships and payments module. It groups the screens of
 * the module in tabs; the other tabs (sale, subscriptions, payments) are added
 * here as they are built.
 */
public class BillingPanel implements ModulePanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final PlanListView plans;

    /**
     * Creates the screen.
     *
     * @param planService the service that manages the plans
     */
    public BillingPanel(MembershipPlanService planService) {
        this.plans = new PlanListView(planService);
        tabs.setBackground(Theme.BACKGROUND);
        tabs.addTab("Planes", plans);
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
        plans.refresh();
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }
}

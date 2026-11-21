package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.AnalyticsService;
import com.nextfit.service.MemberService;
import javafx.scene.Node;
import java.util.List;

/** Analytics as a module: it adapts {@link AnalyticsView} to {@link FxModule}. */
public final class AnalyticsModule implements FxModule {

    private final AnalyticsView view;

    /**
     * @param service the source of the insights
     * @param members used to show the name of each member at risk
     */
    public AnalyticsModule(AnalyticsService service, MemberService members) {
        this.view = new AnalyticsView(service, members);
    }

    @Override
    public String id() {
        return "analytics";
    }

    @Override
    public List<String> keywords() {
        return List.of("estadísticas", "gráficas", "retención", "ingresos");
    }

    @Override
    public String title() {
        return "Analítica";
    }

    @Override
    public String icon() {
        return FxIcons.ANALYTICS;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        view.refresh();
    }

    /** The trainer does not manage memberships, so analytics is for the administrator and the reception. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }
}

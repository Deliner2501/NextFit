package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.occupancy.GymCapacity;
import javafx.scene.Node;
import java.util.List;

/** The home screen as a module: it only adapts {@link DashboardView} to {@link FxModule}. */
public final class DashboardModule implements FxModule {

    private final DashboardView view;

    /**
     * @param service the source of the home screen numbers
     * @param attendance the source of current visits
     * @param capacity the configured admission limit
     */
    public DashboardModule(DashboardService service, AttendanceService attendance, GymCapacity capacity) {
        this.view = new DashboardView(service, attendance, capacity);
    }

    @Override
    public String id() {
        return "panel";
    }

    @Override
    public List<String> keywords() {
        return List.of("inicio", "resumen", "indicadores", "dashboard");
    }

    @Override
    public String title() {
        return "Panel";
    }

    @Override
    public String icon() {
        return FxIcons.PANEL;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        view.refresh();
    }

    /** The trainer does not need the money numbers. */
    @Override
    public boolean allowedFor(Role role) {
        return role != Role.TRAINER;
    }
}

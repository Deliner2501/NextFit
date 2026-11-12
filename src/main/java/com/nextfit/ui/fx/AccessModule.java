package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.AccessService;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.SubscriptionService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

import java.util.function.Supplier;
import java.util.List;

/**
 * Access control module in JavaFX: the kiosk that reads the QR of the carnet
 * and the screen that shows and exports each member's carnet. It is for the
 * administrator and the reception.
 */
public final class AccessModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final AccessPane kiosk;
    private final CarnetPane carnets;

    /**
     * @param accessService     turns a scanned code into an entry or an exit
     * @param attendanceService lists who is inside
     * @param memberService     finds members
     * @param subscriptions     tells the membership of each member
     * @param phone             registers the scans made with a phone and gives the carnet codes
     * @param phoneUrl          gives the address of the reception computer, e.g. http://192.168.1.20:8765
     */
    public AccessModule(AccessService accessService, AttendanceService attendanceService,
                        MemberService memberService, SubscriptionService subscriptions,
                        PhoneAccess phone, Supplier<String> phoneUrl) {
        this.kiosk = new AccessPane(accessService, attendanceService, memberService, phoneUrl);
        this.carnets = new CarnetPane(memberService, subscriptions, phone, phoneUrl);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Control de acceso", kiosk), tab("Carnets", carnets));
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refreshTabs());
    }

    @Override
    public String id() {
        return "access";
    }

    @Override
    public List<String> keywords() {
        return List.of("entrada", "ingreso", "qr", "escanear", "asistencia", "carnet");
    }

    @Override
    public String title() {
        return "Control de acceso";
    }

    @Override
    public String icon() {
        return FxIcons.ACCESS;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        refreshTabs();
    }

    /** Only the administrator and the reception operate the entrance. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private void refreshTabs() {
        kiosk.refresh();
        carnets.refresh();
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }
}

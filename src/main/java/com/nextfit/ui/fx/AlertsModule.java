package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.alert.AlertMessages;
import com.nextfit.service.alert.AlertService;
import com.nextfit.service.alert.WhatsAppLink;
import javafx.scene.Node;
import java.util.List;

/**
 * Alert center in JavaFX. The administrator and the reception see who needs a call today
 * (memberships about to end, members who stopped coming and birthdays) and can open a
 * WhatsApp chat with the message already written. The trainers do not contact members, so
 * the module does not appear for them. The module decides who may open it and
 * {@link AlertsPane} only draws what the services give.
 */
public final class AlertsModule implements FxModule {

    private final AlertsPane pane;

    /**
     * @param alerts   the service that gathers the alerts of the day
     * @param messages the writer of the message for each alert
     * @param whatsApp the builder of the WhatsApp link
     */
    public AlertsModule(AlertService alerts, AlertMessages messages, WhatsAppLink whatsApp) {
        this.pane = new AlertsPane(alerts, messages, whatsApp);
    }

    @Override
    public String id() {
        return "alerts";
    }

    @Override
    public List<String> keywords() {
        return List.of("cumpleaños", "vencimientos", "inactivos", "whatsapp", "avisos");
    }

    @Override
    public String title() {
        return "Alertas";
    }

    @Override
    public String icon() {
        return AlertIcons.BELL;
    }

    @Override
    public Node view() {
        return pane;
    }

    @Override
    public void onShow() {
        pane.refresh();
    }

    /** The administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }
}

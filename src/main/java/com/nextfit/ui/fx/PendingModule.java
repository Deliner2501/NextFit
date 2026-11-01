package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.Set;

/**
 * Placeholder for a module that is not migrated to JavaFX yet. It keeps the
 * sidebar complete and is replaced by the real module without touching the shell.
 */
public final class PendingModule implements FxModule {

    private final String id;
    private final String title;
    private final String icon;
    private final Set<Role> roles;
    private final StackPane view = new StackPane();

    /**
     * @param id    the identifier
     * @param title the sidebar text
     * @param icon  the SVG path of the icon
     * @param roles the roles that see the entry
     */
    public PendingModule(String id, String title, String icon, Set<Role> roles) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.roles = roles;
        Label notice = new Label("Este módulo se migrará a la interfaz nueva en la semana 2.");
        notice.getStyleClass().add("nf-muted");
        view.getChildren().add(notice);
        view.getStyleClass().add("nf-card");
        view.setMinHeight(240);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String icon() {
        return icon;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public boolean allowedFor(Role role) {
        return roles.contains(role);
    }
}

package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.occupancy.OccupancyService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TV module: the live occupancy of the gym. It shows the screen inside the application and a
 * button that opens it in its own full-screen window, to leave it on the gym television.
 * It is for the administrator and the reception.
 */
public final class OccupancyModule implements FxModule {

    private final OccupancyService service;
    private final OccupancyView view;
    private final ScrollPane page = new ScrollPane();

    /** @param service the service that calculates the occupancy */
    public OccupancyModule(OccupancyService service) {
        this.service = service;
        this.view = new OccupancyView(service);

        Label title = new Label("Ocupación en vivo");
        title.getStyleClass().add("nf-section-title");
        Label subtitle = new Label("Lo que verá el gimnasio en el televisor");
        subtitle.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Button fullScreen = new Button("Pantalla completa");
        fullScreen.getStyleClass().add("nf-primary-small");
        fullScreen.setOnAction(e -> openFullScreen());
        HBox header = new HBox(10, titles, fullScreen);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(18, header, view);
        content.setPadding(new Insets(18, 0, 0, 0));
        page.setContent(content);
        page.setFitToWidth(true);
        page.getStyleClass().add("nf-scroll");
    }

    @Override
    public String id() {
        return "occupancy";
    }

    @Override
    public String title() {
        return "Pantalla TV";
    }

    @Override
    public String icon() {
        return FxIcons.TV;
    }

    @Override
    public Node view() {
        return page;
    }

    @Override
    public void onShow() {
        view.start();
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    // Abre la misma pantalla en una ventana propia a pantalla completa (Esc para salir).
    private void openFullScreen() {
        OccupancyView tv = new OccupancyView(service);
        VBox.setVgrow(tv, Priority.ALWAYS);
        VBox root = new VBox(tv);
        root.getStyleClass().add("nf-bg");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root);
        Scene owner = page.getScene();
        if (owner != null) {
            // La ventana del televisor usa las mismas hojas de estilo y el mismo modo claro u oscuro.
            scene.getStylesheets().setAll(owner.getStylesheets());
            root.getStyleClass().add(owner.getRoot().getStyleClass().contains("light") ? "light" : "dark");
        }
        Stage stage = new Stage();
        stage.setTitle("NextFit · Ocupación");
        stage.setScene(scene);
        stage.setFullScreenExitHint("Presiona Esc para salir");
        stage.setFullScreen(true);
        stage.setOnHidden(e -> tv.stop());
        stage.show();
        tv.start();
    }
}
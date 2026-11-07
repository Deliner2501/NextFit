package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.Session;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main window: sidebar, top bar and the screen of the chosen module. It only
 * knows the {@link FxModule} contract, so a new module appears in the menu
 * without changing this class.
 */
final class ShellView extends BorderPane {

    private final Session session;
    private final FxTheme theme;
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final StackPane host = new StackPane();
    private final List<Button> buttons = new ArrayList<>();
    private final List<FxModule> visible = new ArrayList<>();

    /**
     * @param session   the signed-in user
     * @param modules   every module of the application, in sidebar order
     * @param theme     the dark and light modes
     * @param onLogout  action to run when the user signs out
     */
    ShellView(Session session, List<FxModule> modules, FxTheme theme, Runnable onLogout) {
        this.session = session;
        this.theme = theme;
        getStyleClass().add("nf-bg");

        Role role = session.getCurrentUser().map(User::getRole).orElse(Role.RECEPTION);
        for (FxModule module : modules) {
            if (module.allowedFor(role)) {
                visible.add(module);
            }
        }
        setLeft(buildSidebar());
        setCenter(buildContent(onLogout));
        if (!visible.isEmpty()) {
            show(visible.get(0));
        }
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("nf-sidebar");
        sidebar.setPadding(new Insets(24, 16, 16, 16));
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(260);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 0, 24, 8));
        Image logo = FxImages.load("/images/logo/nextfit-logo-256.png");
        if (logo != null) {
            ImageView mark = new ImageView(logo);
            mark.setFitWidth(44);
            mark.setPreserveRatio(true);
            mark.setSmooth(true);
            brand.getChildren().add(mark);
        }
        Label name = new Label("NextFit");
        name.getStyleClass().add("nf-sidebar-brand");
        brand.getChildren().add(name);
        sidebar.getChildren().add(brand);

        for (FxModule item : visible) {
            Button button = new Button(item.title(), FxIcons.of(item.icon(), 20));
            button.getStyleClass().add("nf-nav");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> show(item));
            FxMotion.nudge(button, 4);
            buttons.add(button);
            sidebar.getChildren().add(button);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, buildPromo());
        return sidebar;
    }

    /**
     * Motivational block at the foot of the sidebar. The picture (sidebar/promo.png) already carries the
     * slogan; without it the block shows the slogan as text over a violet gradient.
     */
    private Node buildPromo() {
        StackPane promo = new StackPane();
        promo.getStyleClass().add("nf-promo");
        Image athlete = FxImages.load("/images/sidebar/promo.png");
        if (athlete != null) {
            ImageView photo = new ImageView(athlete);
            photo.setPreserveRatio(true);
            photo.setSmooth(true);
            photo.setFitWidth(228);
            promo.getChildren().add(photo);
        } else {
            Label first = new Label("Entrena.");
            first.getStyleClass().add("nf-promo-line1");
            Label second = new Label("Mejora.");
            second.getStyleClass().add("nf-promo-line1");
            Label third = new Label("Supera tus límites.");
            third.getStyleClass().add("nf-promo-line2");
            VBox lines = new VBox(0, first, second, third);
            lines.setAlignment(Pos.BOTTOM_LEFT);
            lines.setPadding(new Insets(0, 0, 18, 18));
            promo.setMinHeight(190);
            promo.setPrefHeight(230);
            promo.getChildren().add(lines);
            StackPane.setAlignment(lines, Pos.BOTTOM_LEFT);
        }
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(promo.widthProperty());
        clip.heightProperty().bind(promo.heightProperty());
        promo.setClip(clip);
        return promo;
    }

    private Node buildContent(Runnable onLogout) {
        title.getStyleClass().add("nf-title");
        subtitle.getStyleClass().add("nf-muted");
        subtitle.setGraphic(FxIcons.of(FxIcons.CALENDAR, 16));
        subtitle.setGraphicTextGap(8);
        VBox titles = new VBox(2, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button toggle = new Button();
        toggle.getStyleClass().add("nf-icon-button");
        Runnable refreshIcon = () -> toggle.setGraphic(FxIcons.of(
                theme.modeProperty().get() == FxTheme.Mode.DARK ? FxIcons.SUN : FxIcons.MOON, 20));
        refreshIcon.run();
        theme.modeProperty().addListener((obs, old, now) -> refreshIcon.run());
        toggle.setOnAction(e -> theme.toggle());

        User user = session.getCurrentUser().orElse(null);
        String fullName = user == null ? "" : user.getFullName();
        Label initial = new Label(fullName.isBlank() ? "?" : fullName.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("nf-avatar-text");
        StackPane avatar = new StackPane(initial);
        avatar.getStyleClass().add("nf-avatar");
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        Label userName = new Label(fullName);
        userName.getStyleClass().add("nf-user-name");
        Label userRole = new Label(user == null ? "" : user.getRole().getLabel());
        userRole.getStyleClass().add("nf-muted-small");
        HBox chip = new HBox(12, avatar, new VBox(2, userName, userRole));
        chip.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button();
        logout.getStyleClass().add("nf-icon-button");
        logout.setGraphic(FxIcons.of(FxIcons.LOGOUT, 20));
        logout.setOnAction(e -> onLogout.run());

        HBox top = new HBox(16, titles, toggle, chip, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(24, 0, 20, 0));

        ScrollPane scroll = new ScrollPane(host);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("nf-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(top, scroll);
        content.setPadding(new Insets(0, 28, 0, 28));
        return content;
    }

    private static String longDate(LocalDate day) {
        String text = day.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO")));
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void show(FxModule item) {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).getStyleClass().remove("selected");
            if (visible.get(i) == item) {
                buttons.get(i).getStyleClass().add("selected");
            }
        }
        title.setText(item.title());
        subtitle.setText(longDate(LocalDate.now()));
        item.onShow();
        Node screen = item.view();
        host.getChildren().setAll(screen);
        FxMotion.enter(List.of(screen), 0, 0, 10);
    }
}

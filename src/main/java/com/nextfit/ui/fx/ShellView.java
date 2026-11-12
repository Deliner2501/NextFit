package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.Session;
import com.nextfit.ui.Formats;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

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
    private static final double WIDE = 260;
    private static final double NARROW = 84;
    private static final String COLLAPSED_KEY = "sidebarCollapsed";
    private final VBox sidebar = new VBox(6);
    private final HBox brand = new HBox(12);
    private final Label brandName = new Label("NextFit");
    private static final double MIN_PROMO_HEIGHT = 110;
    private Node promoBlock;
    private boolean collapsed;
    private SearchPalette palette;

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
        palette = new SearchPalette(this::searchAll, this::openHit);
        setCenter(new StackPane(buildContent(onLogout), palette));
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.K) {
                palette.open();
                e.consume();
            }
        });
        if (!visible.isEmpty()) {
            show(visible.get(0));
        }
    }

    private Node buildSidebar() {
        sidebar.getStyleClass().add("nf-sidebar");
        sidebar.setPadding(new Insets(24, 16, 16, 16));

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
        brandName.getStyleClass().add("nf-sidebar-brand");
        brand.getChildren().add(brandName);
        sidebar.getChildren().add(brand);

        for (FxModule item : visible) {
            Button button = new Button(item.title(), FxIcons.of(item.icon(), 20));
            button.getStyleClass().add("nf-nav");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> show(item));
            Tooltip.install(button, new Tooltip(item.title()));
            FxMotion.nudge(button, 4);
            buttons.add(button);
            sidebar.getChildren().add(button);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        promoBlock = buildPromo();
        sidebar.getChildren().addAll(spacer, promoBlock);
        boolean wasCollapsed = false;
        try {
            wasCollapsed = Preferences.userNodeForPackage(ShellView.class).getBoolean(COLLAPSED_KEY, false);
        } catch (RuntimeException e) {
            // Without saved preferences the menu simply starts open.
        }
        applyCollapsed(wasCollapsed, false);
        return sidebar;
    }

    /** Opens or closes the menu: closed, it shows only the icons and leaves more room to the screen. */
    private void applyCollapsed(boolean value, boolean animate) {
        collapsed = value;
        double target = value ? NARROW : WIDE;
        sidebar.setMinWidth(0);
        if (animate) {
            Timeline slide = new Timeline(new KeyFrame(Duration.millis(260),
                    new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.maxWidthProperty(), target, Interpolator.EASE_BOTH)));
            slide.setOnFinished(e -> sidebar.setMinWidth(target));
            slide.play();
        } else {
            sidebar.setPrefWidth(target);
            sidebar.setMaxWidth(target);
            sidebar.setMinWidth(target);
        }
        brandName.setVisible(!value);
        brandName.setManaged(!value);
        brand.setAlignment(value ? Pos.CENTER : Pos.CENTER_LEFT);
        brand.setPadding(value ? new Insets(0, 0, 24, 0) : new Insets(0, 0, 24, 8));
        for (Button button : buttons) {
            button.setContentDisplay(value ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            button.setAlignment(value ? Pos.CENTER : Pos.CENTER_LEFT);
        }
        if (promoBlock != null) {
            promoBlock.setVisible(!value);
            promoBlock.setManaged(!value);
        }
        try {
            Preferences.userNodeForPackage(ShellView.class).putBoolean(COLLAPSED_KEY, value);
        } catch (RuntimeException e) {
            // Not being able to remember the choice is not worth an error.
        }
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
            // The picture is placed by hand: it always shrinks to fit the free space (never cropped) and sits at
            // the bottom of the block. When there is almost no room it is hidden.
            photo.setManaged(false);
            promo.setPrefHeight(athlete.getWidth() > 0 ? athlete.getHeight() * 228 / athlete.getWidth() : 270);
            Runnable place = () -> {
                double w = promo.getWidth();
                double h = promo.getHeight();
                photo.setVisible(h >= MIN_PROMO_HEIGHT);
                photo.setFitWidth(w);
                photo.setFitHeight(h);
                Bounds box = photo.getBoundsInLocal();
                photo.setLayoutX((w - box.getWidth()) / 2 - box.getMinX());
                photo.setLayoutY(h - box.getHeight() - box.getMinY());
            };
            promo.widthProperty().addListener((o, a1, b1) -> place.run());
            promo.heightProperty().addListener((o, a1, b1) -> place.run());
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
            promo.setPrefHeight(230);
            promo.getChildren().add(lines);
            StackPane.setAlignment(lines, Pos.BOTTOM_LEFT);
        }
        // The block may shrink so the sidebar never forces the window taller than the screen.
        promo.setMinHeight(0);
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

        Button menu = new Button("☰");
        menu.getStyleClass().add("nf-icon-button");
        menu.setStyle("-fx-font-size: 18px;");
        Tooltip.install(menu, new Tooltip("Mostrar u ocultar el menú"));
        menu.setOnAction(e -> applyCollapsed(!collapsed, true));

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

        Button search = new Button();
        search.getStyleClass().add("nf-icon-button");
        search.setGraphic(FxIcons.of(FxIcons.SEARCH, 20));
        Tooltip.install(search, new Tooltip("Buscar (Ctrl+K)"));
        search.setOnAction(e -> palette.open());

        HBox top = new HBox(16, menu, titles, search, toggle, chip, logout);
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

    // The modules whose name matches, followed by what each module finds by itself (the members, for one).
    private List<SearchHit> searchAll(String query) {
        List<SearchHit> hits = new ArrayList<>();
        for (FxModule module : visible) {
            if (GlobalSearch.matches(query, module.title())) {
                hits.add(new SearchHit(module.id(), "", module.title(), "Ir a la sección"));
            }
        }
        for (FxModule module : visible) {
            hits.addAll(module.search(query));
        }
        return hits;
    }

    private void openHit(SearchHit hit) {
        for (FxModule module : visible) {
            if (module.id().equals(hit.moduleId())) {
                show(module);
                if (!hit.targetId().isEmpty()) {
                    module.open(hit.targetId());
                }
                return;
            }
        }
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

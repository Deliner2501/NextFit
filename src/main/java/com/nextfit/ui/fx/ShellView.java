package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.Session;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Main window: sidebar, top bar and the screen of the chosen module. It only
 * knows the {@link FxModule} contract, so a new module appears in the menu
 * without changing this class.
 */
final class ShellView extends BorderPane {

    private static final PseudoClass DASHBOARD = PseudoClass.getPseudoClass("dashboard");
    private final Session session;
    private final FxTheme theme;
    private final Label title = new Label();
    private final HBox toolbar = new HBox(14);
    private final Label subtitle = new Label();
    private final StackPane host = new StackPane();
    private final List<Button> buttons = new ArrayList<>();
    private final List<FxModule> visible = new ArrayList<>();
    private static final double WIDE = 220;
    private static final double NARROW = 84;
    private static final String COLLAPSED_KEY = "sidebarCollapsed";
    private final VBox sidebar = new VBox(6);
    private final HBox brand = new HBox(12);
    private final Label brandName = new Label("NextFit");
    private final List<Label> groupLabels = new ArrayList<>();
    private static final List<List<String>> NAVIGATION = List.of(
            List.of("panel", "members", "billing", "access", "training"),
            List.of("alerts", "churn", "assistant", "analytics", "reports"),
            List.of("users", "audit", "backup"));
    private static final List<String> GROUP_TITLES = List.of("Operación", "Seguimiento", "Administración");
    private boolean collapsed;
    private Timeline sidebarSlide;
    private static final PseudoClass COLLAPSED = PseudoClass.getPseudoClass("collapsed");
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
        ScrollPane navigation = new ScrollPane(buildSidebar());
        navigation.getStyleClass().addAll("nf-scroll", "nf-navigation");
        navigation.setFitToWidth(true);
        navigation.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navigation.prefWidthProperty().bind(sidebar.prefWidthProperty().add(2));
        setLeft(navigation);
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
        sidebar.setPadding(new Insets(16, 12, 16, 12));

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

        List<FxModule> ordered = new ArrayList<>();
        for (int i = 0; i < NAVIGATION.size(); i++) {
            List<FxModule> group = NAVIGATION.get(i).stream()
                    .flatMap(id -> visible.stream().filter(module -> module.id().equals(id)))
                    .toList();
            if (!group.isEmpty()) {
                Label heading = new Label(GROUP_TITLES.get(i));
                heading.getStyleClass().add("nf-nav-heading");
                groupLabels.add(heading);
                sidebar.getChildren().add(heading);
                group.forEach(this::addNavigationButton);
                ordered.addAll(group);
            }
        }
        List<FxModule> ungrouped = visible.stream().filter(module -> !ordered.contains(module)).toList();
        ungrouped.forEach(this::addNavigationButton);
        ordered.addAll(ungrouped);
        visible.clear();
        visible.addAll(ordered);

        boolean wasCollapsed = false;
        try {
            wasCollapsed = Preferences.userNodeForPackage(ShellView.class).getBoolean(COLLAPSED_KEY, false);
        } catch (RuntimeException e) {
            // Without saved preferences the menu simply starts open.
        }
        applyCollapsed(wasCollapsed, false);
        return sidebar;
    }

    /** Adds a button for a module already filtered by the signed-in role. */
    private void addNavigationButton(FxModule item) {
        Button button = new Button(item.title(), FxIcons.of(item.icon(), 20));
        button.getStyleClass().add("nf-nav");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> show(item));
        Tooltip.install(button, new Tooltip(item.title()));
        buttons.add(button);
        sidebar.getChildren().add(button);
    }

    /** Opens or closes the menu: closed, it shows only the icons and leaves more room to the screen. */
    private void applyCollapsed(boolean value, boolean animate) {
        collapsed = value;
        sidebar.pseudoClassStateChanged(COLLAPSED, value);
        if (sidebarSlide != null) {
            sidebarSlide.stop();
        }
        double target = value ? NARROW : WIDE;
        sidebar.setMinWidth(0);
        if (animate) {
            sidebarSlide = new Timeline(new KeyFrame(Duration.millis(260),
                    new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.maxWidthProperty(), target, Interpolator.EASE_BOTH)));
            sidebarSlide.setOnFinished(e -> sidebar.setMinWidth(target));
            sidebarSlide.play();
        } else {
            sidebar.setPrefWidth(target);
            sidebar.setMaxWidth(target);
            sidebar.setMinWidth(target);
        }
        brandName.setVisible(!value);
        brandName.setManaged(!value);
        brand.setAlignment(value ? Pos.CENTER : Pos.CENTER_LEFT);
        brand.setPadding(value ? new Insets(0, 0, 24, 0) : new Insets(0, 0, 24, 8));
        for (Label heading : groupLabels) {
            heading.setVisible(!value);
            heading.setManaged(!value);
        }
        try {
            Preferences.userNodeForPackage(ShellView.class).putBoolean(COLLAPSED_KEY, value);
        } catch (RuntimeException e) {
            // Not being able to remember the choice is not worth an error.
        }
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
        toggle.setAccessibleText("Cambiar tema");
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
        HBox chip = new HBox(10, avatar, userName);
        if (user != null) {
            Tooltip.install(chip, new Tooltip(user.getRole().getLabel()));
        }
        chip.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button();
        logout.getStyleClass().add("nf-icon-button");
        logout.setGraphic(FxIcons.of(FxIcons.LOGOUT, 20));
        logout.setAccessibleText("Cerrar sesión");
        logout.setOnAction(e -> onLogout.run());

        Button search = new Button();
        search.getStyleClass().add("nf-icon-button");
        search.setGraphic(FxIcons.of(FxIcons.SEARCH, 20));
        search.setAccessibleText("Buscar");
        Tooltip.install(search, new Tooltip("Buscar (Ctrl+K)"));
        search.setOnAction(e -> palette.open());

        HBox top = toolbar;
        top.getChildren().setAll(menu, titles, search, toggle, chip, logout);
        top.getStyleClass().add("nf-toolbar");
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10, 12, 10, 12));

        ScrollPane scroll = new ScrollPane(host);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("nf-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(12, top, scroll);
        content.setPadding(new Insets(12, 18, 0, 18));
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
            if (GlobalSearch.matches(query, module.title()) || GlobalSearch.matchesAny(query, module.keywords())) {
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
        toolbar.pseudoClassStateChanged(DASHBOARD, item.id().equals("panel"));
        subtitle.setText(longDate(LocalDate.now()));
        item.onShow();
        Node screen = item.view();
        host.getChildren().setAll(screen);
        FxMotion.enter(List.of(screen), 0, 0, 10);
    }
}

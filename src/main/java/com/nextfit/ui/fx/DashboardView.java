package com.nextfit.ui.fx;

import com.nextfit.service.DashboardService;
import com.nextfit.service.DashboardService.Summary;
import com.nextfit.service.Session;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.time.YearMonth;
import java.util.Map;

/**
 * Home screen with visual hierarchy: a banner with the greeting, one large
 * feature card (income of the month with its trend), three smaller key numbers
 * beside it, and two charts of different width below. Every number is worked
 * out by {@link DashboardService}; this class only decides where to place it.
 */
final class DashboardView extends VBox {

    private static final String LAST_3 = "Últimos 3 meses";
    private static final String LAST_6 = "Últimos 6 meses";

    private final DashboardService service;
    private final Session session;

    /**
     * @param service the source of the summary
     * @param session the signed-in user, for the greeting
     */
    DashboardView(DashboardService service, Session session) {
        super(20);
        this.service = service;
        this.session = session;
        setPadding(new Insets(0, 4, 24, 0));
    }

    /** Reads the summary again and redraws the whole screen. */
    void refresh() {
        Summary summary = service.summary();
        long vigentes = Math.max(0, summary.activeSubscriptions() - summary.expiringSoon());

        KpiCard active = new KpiCard("Membresías activas", "0", "Socios con acceso hoy", FxIcons.MEMBERS,
                KpiCard.Kind.LIME);
        KpiCard expiring = new KpiCard("Por vencer", "0", "Terminan en 7 días o menos", FxIcons.BILLING,
                KpiCard.Kind.VIOLET);
        KpiCard today = new KpiCard("Asistencia de hoy", "0", "Entradas registradas", FxIcons.ACCESS,
                KpiCard.Kind.LIME);
        FxMotion.countUp(active.value(), summary.activeSubscriptions(), n -> Long.toString(n));
        FxMotion.countUp(expiring.value(), summary.expiringSoon(), n -> Long.toString(n));
        FxMotion.countUp(today.value(), summary.checkInsToday(), n -> Long.toString(n));
        VBox side = new VBox(16, active, expiring, today);
        for (var child : side.getChildren()) {
            VBox.setVgrow(child, Priority.ALWAYS);
            ((Region) child).setMaxWidth(Double.MAX_VALUE);
        }

        getChildren().setAll(
                hero(),
                split(featureCard(summary), side, 66),
                split(card("Estado de las membresías", DashboardCharts.statusDonut(vigentes, summary)),
                        card("Asistencia por día de la semana", DashboardCharts.weekdayBars(summary.checkInsByWeekday())),
                        40));
        // The blocks rise one after another and the three small numbers follow a moment later.
        FxMotion.enter(getChildren(), 0, 90);
        FxMotion.enter(side.getChildren(), 240, 110);
    }

    private Region hero() {
        Region photo = FxImages.cover("/images/banners/dashboard.jpg", 0.5, 0.5);
        Region shade = new Region();
        shade.getStyleClass().add("nf-hero-shade");

        Label hello = new Label("Hola, ");
        hello.getStyleClass().add("nf-hero-title");
        Label name = new Label(session.getCurrentUser().map(u -> u.getFullName()).orElse("bienvenido"));
        name.getStyleClass().add("nf-hero-name");
        Label subtitle = new Label("Así va el gimnasio hoy");
        subtitle.getStyleClass().add("nf-hero-text");
        VBox text = new VBox(4, new HBox(hello, name), subtitle);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setPadding(new Insets(0, 0, 0, 40));

        Region bar = new Region();
        bar.getStyleClass().add("nf-hero-bar");
        bar.setMinWidth(6);
        bar.setMaxWidth(6);
        bar.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);

        // A diagonal green band on the right edge, drawn again whenever the banner changes size.
        Pane stripes = new Pane();
        stripes.setMouseTransparent(true);
        Polygon band = new Polygon();
        band.getStyleClass().add("nf-hero-stripe");
        stripes.getChildren().add(band);

        StackPane hero = new StackPane(photo, shade, stripes, bar, text);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMinHeight(150);
        hero.setPrefHeight(150);
        hero.setMaxHeight(150);
        Runnable drawBand = () -> {
            double w = hero.getWidth();
            double h = hero.getHeight();
            band.getPoints().setAll(w - 150, h, w - 100, h, w, h - 110, w, h - 165);
        };
        hero.widthProperty().addListener((obs, old, now) -> drawBand.run());
        hero.heightProperty().addListener((obs, old, now) -> drawBand.run());
        roundCorners(hero, 20);
        return hero;
    }

    private static void roundCorners(Region region, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    /** Places two cards side by side; the first one takes {@code percentFirst} percent of the width. */
    private static GridPane split(Region first, Region second, double percentFirst) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(percentFirst);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(100 - percentFirst);
        grid.getColumnConstraints().addAll(left, right);
        grid.add(first, 0, 0);
        grid.add(second, 1, 0);
        first.setMaxWidth(Double.MAX_VALUE);
        second.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    /** The protagonist card: the income of the month, big, with the trend of the last months under it. */
    private Region featureCard(Summary summary) {
        Label caption = new Label("Ingresos del mes");
        caption.getStyleClass().add("nf-muted");
        Label value = new Label(Formats.money(0));
        value.getStyleClass().add("nf-kpi-xl");
        Label hint = new Label("Desde el primer día del mes · tendencia de los últimos meses");
        hint.getStyleClass().add("nf-muted-small");
        StackPane badge = new StackPane(FxIcons.of(FxIcons.ANALYTICS, 26));
        badge.getStyleClass().add("nf-badge-solid");
        badge.setMinSize(56, 56);
        badge.setMaxSize(56, 56);
        VBox texts = new VBox(4, caption, value, hint);
        HBox.setHgrow(texts, Priority.ALWAYS);

        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll(LAST_3, LAST_6);
        range.setValue(LAST_6);
        HBox head = new HBox(16, badge, texts, range);
        head.setAlignment(Pos.TOP_LEFT);

        Map<YearMonth, Long> byMonth = summary.incomeByMonth();
        StackPane chartHolder = new StackPane(DashboardCharts.incomeChart(byMonth, 6));
        range.valueProperty().addListener((obs, old, now) -> chartHolder.getChildren()
                .setAll(DashboardCharts.incomeChart(byMonth, LAST_3.equals(now) ? 3 : 6)));
        VBox.setVgrow(chartHolder, Priority.ALWAYS);

        VBox card = new VBox(14, head, chartHolder);
        card.getStyleClass().add("nf-card-feature");
        card.setPadding(new Insets(26));
        FxMotion.countUp(value, summary.incomeThisMonth(), Formats::money);
        return card;
    }

    private static Region card(String title, Region content) {
        Label label = new Label(title);
        label.getStyleClass().add("nf-card-title");
        VBox box = new VBox(12, label, content);
        box.getStyleClass().add("nf-card");
        box.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }
}

package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Badge;
import com.nextfit.service.gamification.GamificationService;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The achievements tab of the member file: the training days, the current and the best
 * streak, and every badge of the gym, with the earned ones highlighted. It only shows what
 * {@link GamificationService} calculates; it has no rule of its own.
 */
final class AchievementsPane extends ScrollPane {

    private static final double TILE_WIDTH = 260;

    private final Member member;
    private final GamificationService gamification;
    private final KpiCard daysCard = new KpiCard("Días entrenados", "0", "", FxIcons.CALENDAR, KpiCard.Kind.FEATURE);
    private final KpiCard currentCard = new KpiCard("Racha actual", "0", "", FxIcons.ANALYTICS, KpiCard.Kind.VIOLET);
    private final KpiCard bestCard = new KpiCard("Mejor racha", "0", "", FxIcons.TRAINING, KpiCard.Kind.LIME);
    private final FlowPane badgeList = new FlowPane(16, 16);

    /**
     * @param member       the member whose achievements are shown
     * @param gamification the service that calculates the streaks and the badges
     */
    AchievementsPane(Member member, GamificationService gamification) {
        this.member = member;
        this.gamification = gamification;

        HBox kpis = new HBox(16, daysCard, currentCard, bestCard);
        for (KpiCard card : List.of(daysCard, currentCard, bestCard)) {
            card.setPrefWidth(1);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        Label title = new Label("Insignias");
        title.getStyleClass().add("nf-card-title");
        Label rule = new Label("Una racha cuenta las semanas seguidas con al menos "
                + GamificationService.MIN_DAYS_PER_WEEK + " días distintos de entrenamiento. "
                + "Una insignia ganada no se pierde.");
        rule.getStyleClass().add("nf-muted");
        rule.setWrapText(true);

        VBox content = new VBox(16, kpis, title, rule, badgeList);
        content.setPadding(new Insets(16, 4, 16, 0));
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("nf-scroll");
        refresh();
    }

    /** Asks the service again and updates the numbers and the badges. */
    void refresh() {
        Achievements achievements;
        try {
            achievements = gamification.achievementsOf(member.getId());
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
            return;
        }
        int days = achievements.trainingDays();
        daysCard.value().setText(String.valueOf(days));
        daysCard.hint().setText(days == 1 ? "día distinto en el gimnasio" : "días distintos en el gimnasio");

        int current = achievements.streak().current();
        currentCard.value().setText(String.valueOf(current));
        currentCard.hint().setText(current == 0 ? "Sin racha por ahora" : weeks(current) + " seguidas");

        int best = achievements.streak().best();
        bestCard.value().setText(String.valueOf(best));
        bestCard.hint().setText(best == 0 ? "Todavía sin racha" : weeks(best) + " seguidas como máximo");

        badgeList.getChildren().clear();
        for (Badge badge : Badge.values()) {
            badgeList.getChildren().add(tile(badge, achievements.badges().contains(badge)));
        }
    }

    // One card per badge: the earned ones are lit, the others stay faded with "Por ganar".
    private static VBox tile(Badge badge, boolean earned) {
        StackPane icon = new StackPane(FxIcons.of(FxIcons.TRAINING, 24));
        icon.getStyleClass().add("nf-badge");
        icon.setMinSize(48, 48);
        icon.setMaxSize(48, 48);

        Label name = new Label(badge.getLabel());
        name.getStyleClass().add("nf-card-title");
        Label description = new Label(badge.getDescription());
        description.getStyleClass().add("nf-muted-small");
        description.setWrapText(true);
        Label status = new Label(earned ? "✓ Ganada" : "Por ganar");
        status.getStyleClass().add(earned ? "nf-achievement-earned-text" : "nf-muted-small");

        VBox tile = new VBox(8, icon, name, description, status);
        tile.setPadding(new Insets(16));
        tile.setPrefWidth(TILE_WIDTH);
        tile.setMinWidth(TILE_WIDTH);
        tile.getStyleClass().addAll("nf-card", "nf-achievement",
                earned ? "nf-achievement-earned" : "nf-achievement-locked");
        return tile;
    }

    private static String weeks(int count) {
        return count + (count == 1 ? " semana" : " semanas");
    }
}

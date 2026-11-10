package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.gamification.GamificationService;
import com.nextfit.ui.Formats;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The file of one member: a header with the photo and the main data, and four tabs
 * (health, measurements, attendance and achievements). It builds no rule of its own: each tab
 * asks its service.
 */
final class MemberFicha extends VBox {

    private final HealthPane healthPane;
    private final MeasurementsPane measurementsPane;
    private final AttendancePane attendancePane;
    private final AchievementsPane achievementsPane;

    /**
     * @param member     the member to show
     * @param health     the service of the health profile and the measurements
     * @param attendance the service of the visits
     * @param gamification the service of the streaks and the badges
     * @param onBack     called when the user presses the back button
     */
    MemberFicha(Member member, HealthService health, AttendanceService attendance,
                GamificationService gamification, Runnable onBack) {
        super(16);
        this.healthPane = new HealthPane(member, health);
        this.measurementsPane = new MeasurementsPane(member, health, healthPane::refresh);
        this.attendancePane = new AttendancePane(member, attendance);
        this.achievementsPane = new AchievementsPane(member, gamification);

        Button back = new Button("← Volver a socios");
        back.getStyleClass().add("nf-secondary");
        back.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        back.setOnAction(e -> onBack.run());

        Label name = new Label(member.getFullName());
        name.getStyleClass().add("nf-section-title");
        Label details = new Label("Documento " + member.getDocumentNumber() + " · " + member.getPhone()
                + " · " + (member.isActive() ? "Activo" : "De baja") + " desde " + Formats.date(member.getJoinDate()));
        details.getStyleClass().add("nf-muted");
        details.setWrapText(true);
        VBox texts = new VBox(2, name, details);
        HBox identity = new HBox(14, MemberAvatar.of(member, 64), texts);
        identity.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Salud", healthPane), tab("Mediciones", measurementsPane),
                tab("Asistencia", attendancePane), tab("Logros", achievementsPane));
        // A visit or a measurement recorded elsewhere must show when the user comes back to the tab.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            healthPane.refresh();
            measurementsPane.refresh();
            attendancePane.refresh();
            achievementsPane.refresh();
        });
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(back, identity, tabs);
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}

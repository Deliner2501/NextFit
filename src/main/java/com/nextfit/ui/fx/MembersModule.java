package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.model.Role;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MemberService;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.Session;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.gamification.GamificationService;
import com.nextfit.util.PhotoStorage;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.function.Supplier;

/**
 * Members module in JavaFX. The administrator, the reception and the trainers can
 * see the members and open the file of each one; only the administrator and the
 * reception can register, edit or deactivate them. The module decides who may edit
 * and the screens only ask it, so they never know about roles. It shows the list or,
 * after "Ver ficha", the file of the selected member.
 */
public final class MembersModule implements FxModule {

    private static final int MAX_HITS = 6;

    private final MemberService memberService;
    private final Session session;
    private final HealthService healthService;
    private final AttendanceService attendanceService;
    private final GamificationService gamificationService;
    private final MembersPane list;
    private final CarnetPane carnets;
    private final TabPane tabs = new TabPane();
    private final Tab listTab;
    private final Tab carnetTab;
    private final StackPane root = new StackPane();

    /**
     * @param memberService     the service that manages the members
     * @param healthService     the service of the health profile and the measurements
     * @param attendanceService the service of the visits
     * @param gamificationService the service of the streaks and the badges
     * @param photoStorage      where the photos of the members are copied
     * @param session           the session, to know who is working
     * @param subscriptions     the membership data shown on the carnet
     * @param phone             issues the mobile portal QR codes
     * @param phoneUrl          supplies the mobile portal address
     */
    public MembersModule(MemberService memberService, HealthService healthService,
                         AttendanceService attendanceService, GamificationService gamificationService,
                         PhotoStorage photoStorage, Session session, SubscriptionService subscriptions,
                         PhoneAccess phone, Supplier<String> phoneUrl) {
        this.memberService = memberService;
        this.session = session;
        this.healthService = healthService;
        this.attendanceService = attendanceService;
        this.gamificationService = gamificationService;
        this.list = new MembersPane(memberService, photoStorage, this::canEdit, this::openFicha);
        this.carnets = new CarnetPane(memberService, subscriptions, phone, phoneUrl);
        listTab = tab("Listado", list);
        carnetTab = tab("Carnets", carnets);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().add(listTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == carnetTab && canEdit()) {
                carnets.refresh();
            } else if (selected == listTab) {
                list.refresh();
            }
        });
        root.getChildren().add(tabs);
    }

    @Override
    public String id() {
        return "members";
    }

    @Override
    public List<String> keywords() {
        return List.of("socios", "clientes", "carnet", "qr", "ficha", "salud", "medidas");
    }

    @Override
    public String title() {
        return "Socios";
    }

    @Override
    public String icon() {
        return FxIcons.MEMBERS;
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        if (canEdit()) {
            if (!tabs.getTabs().contains(carnetTab)) {
                tabs.getTabs().add(carnetTab);
            }
        } else {
            tabs.getTabs().remove(carnetTab);
        }
        showList();
    }

    /** The administrator, the reception and the trainers can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION || role == Role.TRAINER;
    }

    @Override
    public List<SearchHit> search(String query) {
        return GlobalSearch.memberHits(id(), query, memberService.findAll(), MAX_HITS);
    }

    @Override
    public void open(String targetId) {
        try {
            openFicha(memberService.findById(Long.parseLong(targetId)));
        } catch (RuntimeException e) {
            showList();
        }
    }

    // Replaces the list with the file of the member; the file's back button brings the list back.
    private void openFicha(Member member) {
        root.getChildren().setAll(new MemberFicha(member, healthService, attendanceService,
                gamificationService, this::showList));
    }

    private void showList() {
        root.getChildren().setAll(tabs);
        if (tabs.getSelectionModel().getSelectedItem() == listTab) {
            list.refresh();
        } else {
            tabs.getSelectionModel().select(listTab);
        }
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        return new Tab(title, holder);
    }

    private boolean canEdit() {
        return session.hasRole(Role.ADMIN) || session.hasRole(Role.RECEPTION);
    }
}

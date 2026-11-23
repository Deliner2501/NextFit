package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.model.Role;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberPasswordService;
import com.nextfit.service.MemberService;
import com.nextfit.service.SelfAccessService;
import com.nextfit.service.Session;
import com.nextfit.service.occupancy.GymCapacity;
import com.nextfit.service.face.FaceAccessService;
import com.nextfit.service.face.FaceEnrollmentService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Access control module in JavaFX: who is inside now and the
 * registration of the faces and of the passwords with which the members enter. It is for
 * the administrator and the reception.
 */
public final class AccessModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final InsidePane inside;
    private final FacePane face;
    private final PasswordPane password;

    /**
     * @param attendanceService lists who is inside
     * @param memberService     finds members
     * @param faceAccess        lets a member in by the face
     * @param faceEnrollment    registers and deletes faces
     * @param dataDir           the data folder (the face models are inside)
     * @param actor             the user signed in now, for the audit log
     * @param selfAccess        lets a member in with the document and the password
     * @param memberPasswords   creates and resets the passwords of the members
     * @param capacity          the gym capacity settings
     * @param session           the current operator
     */
    public AccessModule(AttendanceService attendanceService,
                        MemberService memberService,
                        FaceAccessService faceAccess, FaceEnrollmentService faceEnrollment,
                        Path dataDir, Supplier<String> actor,
                        SelfAccessService selfAccess, MemberPasswordService memberPasswords,
                        GymCapacity capacity, Session session) {
        // How each member enters, shown as labels in the lists of the face and password tabs.
        Function<List<Member>, java.util.Map<Long, List<String>>> tags = candidates -> {
            var faces = faceEnrollment.registeredIds();
            var passwords = memberPasswords.registeredIds(candidates);
            var labels = new java.util.HashMap<Long, List<String>>();
            for (Member member : candidates) {
                List<String> found = new ArrayList<>(2);
                if (faces.contains(member.getId())) {
                    found.add("Rostro");
                }
                if (passwords.contains(member.getId())) {
                    found.add("Contraseña");
                }
                if (!found.isEmpty()) {
                    labels.put(member.getId(), List.copyOf(found));
                }
            }
            return labels;
        };
        this.inside = new InsidePane(attendanceService, memberService, capacity, session);
        this.face = new FacePane(faceAccess, faceEnrollment, memberService, dataDir, actor, tags);
        this.password = new PasswordPane(selfAccess, memberPasswords, memberService, actor, tags);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Dentro ahora", inside), tab("Rostro", face), tab("Contraseña", password));
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refreshTabs());
    }

    @Override
    public String id() {
        return "access";
    }

    @Override
    public List<String> keywords() {
        return List.of("entrada", "ingreso", "asistencia", "rostro", "cara", "facial", "camara", "contraseña", "clave");
    }

    @Override
    public String title() {
        return "Control de acceso";
    }

    @Override
    public String icon() {
        return FxIcons.ACCESS;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        refreshTabs();
    }

    /** Only the administrator and the reception operate the entrance. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private void refreshTabs() {
        switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> inside.refresh();
            case 1 -> face.refresh();
            case 2 -> password.refresh();
            default -> {}
        }
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }
}

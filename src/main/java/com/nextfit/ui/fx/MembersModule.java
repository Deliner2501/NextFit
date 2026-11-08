package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.MemberService;
import com.nextfit.service.Session;
import com.nextfit.util.PhotoStorage;
import javafx.scene.Node;

/**
 * Members module in JavaFX. The administrator, the reception and the trainers can
 * see the members; only the administrator and the reception can register, edit or
 * deactivate them. The module decides who may edit and the screens only ask it, so
 * they never know about roles.
 */
public final class MembersModule implements FxModule {

    private final Session session;
    private final MembersPane list;

    /**
     * @param memberService the service that manages the members
     * @param photoStorage  where the photos of the members are copied
     * @param session       the session, to know who is working
     */
    public MembersModule(MemberService memberService, PhotoStorage photoStorage, Session session) {
        this.session = session;
        this.list = new MembersPane(memberService, photoStorage, this::canEdit);
    }

    @Override
    public String id() {
        return "members";
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
        return list;
    }

    @Override
    public void onShow() {
        list.refresh();
    }

    /** The administrator, the reception and the trainers can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION || role == Role.TRAINER;
    }

    private boolean canEdit() {
        return session.hasRole(Role.ADMIN) || session.hasRole(Role.RECEPTION);
    }
}

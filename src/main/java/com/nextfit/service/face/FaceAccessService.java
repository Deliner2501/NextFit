package com.nextfit.service.face;

import com.nextfit.model.FaceTemplate;
import com.nextfit.model.Member;
import com.nextfit.persistence.FaceTemplateRepository;
import com.nextfit.service.AccessResult;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Lets a member in by the face. It receives what the camera sees, recognizes the member with several
 * safety checks (confidence, margin over the second candidate, several frames in a row, pause after a
 * confirmation) and then lets the usual access rules decide: active membership, capacity and so on.
 */
public class FaceAccessService {

    public static final double DEFAULT_THRESHOLD = 0.45;
    public static final double DEFAULT_MARGIN = 0.05;
    public static final int CONFIRMATIONS = 3;
    public static final Duration COOLDOWN = Duration.ofSeconds(8);
    /** Frames with a face nobody knows before telling the person. */
    static final int UNKNOWN_FRAMES = 10;
    private static final int UNKNOWN_PAUSE = -40;

    private final FaceTemplateRepository templates;
    private final MemberLookup members;
    private final Function<Member, AccessResult> admit;
    private final Function<String, AccessResult> deny;
    private final FaceStabilizer stabilizer;
    private final double threshold;
    private final double margin;
    private volatile List<FaceTemplate> cache;
    private int unknownFrames;

    /**
     * @param templates where the faces are stored
     * @param members   to find the recognized member
     * @param admit     the usual access rules for a member (enter or leave)
     * @param deny      writes a denied access with a message
     * @param clock     the clock
     */
    public FaceAccessService(FaceTemplateRepository templates, MemberLookup members,
                             Function<Member, AccessResult> admit, Function<String, AccessResult> deny, Clock clock) {
        this(templates, members, admit, deny, clock, DEFAULT_THRESHOLD, DEFAULT_MARGIN);
    }

    /**
     * @param templates where the faces are stored
     * @param members   to find the recognized member
     * @param admit     the usual access rules for a member (enter or leave)
     * @param deny      writes a denied access with a message
     * @param clock     the clock
     * @param threshold the minimum similarity to accept a face
     * @param margin    how much better than the second member the best one must be
     */
    public FaceAccessService(FaceTemplateRepository templates, MemberLookup members,
                             Function<Member, AccessResult> admit, Function<String, AccessResult> deny, Clock clock,
                             double threshold, double margin) {
        this.templates = templates;
        this.members = members;
        this.admit = admit;
        this.deny = deny;
        this.stabilizer = new FaceStabilizer(CONFIRMATIONS, COOLDOWN, clock);
        this.threshold = threshold;
        this.margin = margin;
        this.cache = templates.findAll();
    }

    /** Reads the faces again; call it after registering or deleting one. */
    public void refresh() {
        cache = templates.findAll();
    }

    /**
     * Processes one picture of the camera. Faces looking to the side are ignored: they are not reliable.
     *
     * @param frame what the camera saw
     * @return the access result when someone was just let in, left or denied; otherwise empty
     */
    public Optional<AccessResult> onFrame(FaceFrame frame) {
        if (!frame.isFrontal()) {
            unknownFrames = 0;
            stabilizer.feed(Optional.empty());
            return Optional.empty();
        }
        Optional<Long> recognized = FaceMatcher.best(frame.vector(), cache, threshold, margin)
                .map(FaceMatcher.Match::memberId);
        if (recognized.isEmpty()) {
            stabilizer.feed(Optional.empty());
            unknownFrames++;
            if (unknownFrames == UNKNOWN_FRAMES) {
                unknownFrames = UNKNOWN_PAUSE;
                return Optional.of(deny.apply("Rostro no reconocido. Usa el carnet (QR) o el documento."));
            }
            return Optional.empty();
        }
        unknownFrames = 0;
        Optional<Long> confirmed = stabilizer.feed(recognized);
        if (confirmed.isEmpty()) {
            return Optional.empty();
        }
        Optional<Member> member = members.find(confirmed.get());
        if (member.isEmpty()) {
            return Optional.of(deny.apply("El rostro pertenece a un socio que ya no existe."));
        }
        if (!member.get().isActive()) {
            return Optional.of(deny.apply("Socio inactivo: " + member.get().getFullName()));
        }
        return Optional.of(admit.apply(member.get()));
    }
}

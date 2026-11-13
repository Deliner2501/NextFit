package com.nextfit.service.face;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.AccessResult.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nextfit.service.face.FaceTestData.face;
import static com.nextfit.service.face.FaceTestData.member;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceAccessServiceTest {

    private final FaceEnrollmentServiceTest.MemoryTemplates repo = new FaceEnrollmentServiceTest.MemoryTemplates();
    private final Map<Long, Member> members = Map.of(
            1L, member(1, "Ana Pérez", true),
            2L, member(2, "Luis Gómez", false));
    private final List<Member> admitted = new ArrayList<>();
    private final List<String> denied = new ArrayList<>();
    private FaceAccessService service;

    @BeforeEach
    void setUp() {
        repo.replace(1, List.of(face(1, 0, 0)));
        repo.replace(2, List.of(face(0, 1, 0)));
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneOffset.UTC);
        service = new FaceAccessService(repo, id -> Optional.ofNullable(members.get(id)),
                m -> {
                    admitted.add(m);
                    return new AccessResult(Outcome.ENTRY, m, "Bienvenido", null, -1);
                },
                text -> {
                    denied.add(text);
                    return new AccessResult(Outcome.DENIED, null, text, null, -1);
                }, clock);
    }

    private static FaceFrame frame(float[] vector) {
        return new FaceFrame(new byte[0], vector, 0);
    }

    private Optional<AccessResult> feed(float[] vector, int times) {
        Optional<AccessResult> last = Optional.empty();
        for (int i = 0; i < times; i++) {
            Optional<AccessResult> result = service.onFrame(frame(vector));
            if (result.isPresent()) {
                last = result;
            }
        }
        return last;
    }

    @Test
    void letsTheRecognizedMemberInAfterSeveralFrames() {
        assertTrue(feed(face(1, 0, 0), 2).isEmpty());
        Optional<AccessResult> result = feed(face(1, 0, 0), 1);

        assertTrue(result.isPresent());
        assertEquals(Outcome.ENTRY, result.get().outcome());
        assertEquals(1, admitted.get(0).getId());
    }

    @Test
    void doesNotEnterAndLeaveWhileTheMemberStaysInFront() {
        feed(face(1, 0, 0), 30);

        assertEquals(1, admitted.size());
    }

    @Test
    void noFaceDoesNothing() {
        assertTrue(service.onFrame(new FaceFrame(new byte[0], null, 0)).isEmpty());
        assertTrue(admitted.isEmpty() && denied.isEmpty());
    }

    @Test
    void anUnknownFaceIsDeniedOnceAfterAWhile() {
        assertTrue(feed(face(0, 0, 1), FaceAccessService.UNKNOWN_FRAMES - 1).isEmpty());
        Optional<AccessResult> result = feed(face(0, 0, 1), 1);

        assertEquals(Outcome.DENIED, result.orElseThrow().outcome());
        feed(face(0, 0, 1), 20);
        assertEquals(1, denied.size());
    }

    @Test
    void anInactiveMemberIsDeniedAndNeverAdmitted() {
        Optional<AccessResult> result = feed(face(0, 1, 0), FaceAccessService.CONFIRMATIONS);

        assertEquals(Outcome.DENIED, result.orElseThrow().outcome());
        assertTrue(admitted.isEmpty());
        assertTrue(denied.get(0).contains("inactivo"));
    }

    @Test
    void refreshPicksUpNewFaces() {
        repo.replace(1, List.of(face(0, 0, 1)));
        assertTrue(feed(face(0, 0, 1), 3).isEmpty());

        service.refresh();
        assertTrue(feed(face(0, 0, 1), 3).isPresent());
        assertEquals(1, admitted.get(0).getId());
    }

    @Test
    void facesLookingToTheSideAreIgnored() {
        for (int i = 0; i < 20; i++) {
            assertTrue(service.onFrame(new FaceFrame(new byte[0], face(1, 0, 0), 0.8)).isEmpty());
        }
        assertTrue(admitted.isEmpty() && denied.isEmpty());
    }
}

package com.nextfit.service.audit;

import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import com.nextfit.service.security.AttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionAuditTest {

    private List<String> events;
    private AuthService auth;
    private Session session;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
        AuditLog audit = (username, action, detail) -> events.add(username + "|" + action + "|" + detail);
        auth = new AuthService(new NoUsers(), AttemptLimiter.NONE, audit);
        session = new Session();
        session.start(new User(1, "ana", "Ana Pérez", "hash", Role.ADMIN, true));
    }

    @Test
    void logoutEndsTheSessionAndWritesIt() {
        auth.logout(session);

        assertTrue(session.getCurrentUser().isEmpty());
        assertEquals(List.of("ana|LOGOUT|"), events);
    }

    @Test
    void expireEndsTheSessionAndWritesTheReason() {
        auth.expire(session);

        assertTrue(session.getCurrentUser().isEmpty());
        assertEquals(List.of("ana|SESSION_EXPIRED|Sin actividad"), events);
    }

    @Test
    void closingWithNobodySignedInWritesNothing() {
        session.end();

        auth.logout(session);
        auth.expire(session);

        assertTrue(events.isEmpty());
    }

    /** A user storage that is always empty: these tests never sign in. */
    private static final class NoUsers implements UserRepository {

        @Override
        public List<User> findAll() {
            return List.of();
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            return user;
        }
    }
}
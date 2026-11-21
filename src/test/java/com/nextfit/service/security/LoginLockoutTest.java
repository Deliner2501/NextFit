package com.nextfit.service.security;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginLockoutTest {

    private static final String PASSWORD = "clave1234";

    private MovableClock clock;
    private InMemoryAttemptLimiter limiter;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        limiter = new InMemoryAttemptLimiter(3, 5, clock);
        InMemoryUsers users = new InMemoryUsers();
        users.save(new User(0, "ana", "Ana Pérez", PasswordHasher.hash(PASSWORD), Role.ADMIN, true));
        auth = new AuthService(users, limiter);
    }

    private void wrongPassword() {
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", "mala-clave"));
    }

    @Test
    void doesNotBlockBeforeTheLimit() {
        limiter.failed("ana");
        limiter.failed("ana");

        assertDoesNotThrow(() -> limiter.beforeAttempt("ana"));
    }

    @Test
    void blocksAfterTheMaxFailuresInARow() {
        for (int i = 0; i < 3; i++) {
            limiter.failed("ana");
        }

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> limiter.beforeAttempt("ana"));
        assertTrue(error.getMessage().contains("5 minutos"));
    }

    @Test
    void theMessageCountsTheMinutesThatAreLeft() {
        for (int i = 0; i < 3; i++) {
            limiter.failed("ana");
        }
        clock.advance(Duration.ofMinutes(3));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> limiter.beforeAttempt("ana"));
        assertTrue(error.getMessage().contains("2 minutos"));
    }

    @Test
    void unlocksWhenTheTimePasses() {
        for (int i = 0; i < 3; i++) {
            limiter.failed("ana");
        }
        clock.advance(Duration.ofMinutes(5));

        assertDoesNotThrow(() -> limiter.beforeAttempt("ana"));
    }

    @Test
    void aSuccessClearsTheFailures() {
        limiter.failed("ana");
        limiter.failed("ana");
        limiter.succeeded("ana");
        limiter.failed("ana");
        limiter.failed("ana");

        assertDoesNotThrow(() -> limiter.beforeAttempt("ana"));
    }

    @Test
    void namesAreComparedWithoutCase() {
        for (int i = 0; i < 3; i++) {
            limiter.failed("ANA");
        }

        assertThrows(BusinessRuleException.class, () -> limiter.beforeAttempt("ana"));
    }

    @Test
    void theNoneLimiterNeverBlocks() {
        for (int i = 0; i < 100; i++) {
            AttemptLimiter.NONE.failed("ana");
        }

        assertDoesNotThrow(() -> AttemptLimiter.NONE.beforeAttempt("ana"));
    }

    @Test
    void theConfigurationMustBeGreaterThanZero() {
        assertThrows(InvalidDataException.class, () -> new InMemoryAttemptLimiter(0, 5, clock));
        assertThrows(InvalidDataException.class, () -> new InMemoryAttemptLimiter(3, 0, clock));
    }

    @Test
    void loginIsBlockedAfterTooManyWrongPasswords() {
        wrongPassword();
        wrongPassword();
        wrongPassword();

        // Aunque ahora se escriba la clave correcta, el nombre sigue bloqueado.
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> auth.login("ana", PASSWORD));
        assertTrue(error.getMessage().contains("intentos"));
    }

    @Test
    void aCorrectLoginClearsTheFailures() {
        wrongPassword();
        wrongPassword();
        assertEquals("ana", auth.login("ana", PASSWORD).getUsername());
        wrongPassword();
        wrongPassword();

        assertEquals("ana", auth.login("ana", PASSWORD).getUsername());
    }

    @Test
    void loginWorksAgainWhenTheBlockEnds() {
        wrongPassword();
        wrongPassword();
        wrongPassword();
        clock.advance(Duration.ofMinutes(5));

        assertEquals("ana", auth.login("ana", PASSWORD).getUsername());
    }

    @Test
    void withoutALimiterNobodyIsBlocked() {
        InMemoryUsers users = new InMemoryUsers();
        users.save(new User(0, "ana", "Ana Pérez", PasswordHasher.hash(PASSWORD), Role.ADMIN, true));
        AuthService open = new AuthService(users);
        for (int i = 0; i < 10; i++) {
            assertThrows(BusinessRuleException.class, () -> open.login("ana", "mala-clave"));
        }

        assertEquals("ana", open.login("ana", PASSWORD).getUsername());
    }

    // Un reloj que se puede adelantar, para probar el paso del tiempo sin esperar.
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-07T15:00:00Z");

        void advance(Duration time) {
            now = now.plus(time);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Bogota");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final class InMemoryUsers implements UserRepository {
        private final List<User> stored = new ArrayList<>();

        @Override
        public List<User> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<User> findById(long id) {
            return stored.stream().filter(u -> u.getId() == id).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return stored.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public User save(User user) {
            User toStore = user.getId() == 0 ? user.withId(stored.size() + 1L) : user;
            stored.removeIf(u -> u.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }
}
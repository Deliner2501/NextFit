package com.nextfit.service.audit;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.security.InMemoryAttemptLimiter;
import com.nextfit.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAuditTest {

    private static final String PASSWORD = "clave1234";

    private List<String> events;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
        AuditLog audit = (username, action, detail) -> events.add(username + "|" + action + "|" + detail);
        InMemoryUsers users = new InMemoryUsers();
        users.save(new User(0, "ana", "Ana Pérez", PasswordHasher.hash(PASSWORD), Role.ADMIN, true));
        users.save(new User(0, "luis", "Luis Gómez", PasswordHasher.hash(PASSWORD), Role.TRAINER, false));
        auth = new AuthService(users, new InMemoryAttemptLimiter(2, 5, Clock.systemUTC()), audit);
    }

    @Test
    void aGoodLoginIsWrittenWithTheRole() {
        auth.login("ana", PASSWORD);

        assertEquals(List.of("ana|LOGIN|Rol: Administrador"), events);
    }

    @Test
    void aWrongPasswordIsWrittenWithTheTypedName() {
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", "mala-clave"));

        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("ana|" + AuditAction.LOGIN_FAILED));
    }

    @Test
    void anUnknownUserIsWrittenAsAFailedLogin() {
        assertThrows(BusinessRuleException.class, () -> auth.login("fantasma", PASSWORD));

        assertTrue(events.get(0).startsWith("fantasma|" + AuditAction.LOGIN_FAILED));
    }

    @Test
    void aDisabledUserIsAFailedLoginNotALogin() {
        assertThrows(BusinessRuleException.class, () -> auth.login("luis", PASSWORD));

        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("luis|" + AuditAction.LOGIN_FAILED));
    }

    @Test
    void anAttemptWhileBlockedIsWrittenAsLockout() {
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", "mala-clave"));
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", "mala-clave"));
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", PASSWORD));

        assertTrue(events.get(2).startsWith("ana|" + AuditAction.LOCKOUT));
    }

    @Test
    void thePasswordIsNeverWritten() {
        assertThrows(BusinessRuleException.class, () -> auth.login("ana", "mala-clave"));
        auth.login("ana", PASSWORD);

        assertTrue(events.stream().noneMatch(e -> e.contains("mala-clave") || e.contains(PASSWORD)));
    }

    @Test
    void anEmptyNameWritesNothing() {
        assertThrows(InvalidDataException.class, () -> auth.login("  ", PASSWORD));

        assertTrue(events.isEmpty());
    }

    /** Keeps the users in a list, without a file. */
    private static final class InMemoryUsers implements UserRepository {

        private final List<User> users = new ArrayList<>();

        @Override
        public List<User> findAll() {
            return List.copyOf(users);
        }

        @Override
        public Optional<User> findById(long id) {
            return users.stream().filter(u -> u.getId() == id).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public User save(User user) {
            User stored = user.withId(users.size() + 1L);
            users.add(stored);
            return stored;
        }
    }
}
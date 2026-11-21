package com.nextfit.service.audit;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.UserService;
import com.nextfit.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuditTest {

    private List<String> events;
    private UserService service;
    private User admin;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
        AuditLog audit = (username, action, detail) -> events.add(username + "|" + action + "|" + detail);
        service = new UserService(new InMemoryUsers(), new UserValidator(), audit, () -> "boss");
        admin = service.create("admin", "Administrador", "clave1234", Role.ADMIN);
        events.clear();
    }

    @Test
    void createWritesWhoCreatedAndTheRole() {
        service.create("recepcion1", "Laura Pérez", "clave1234", Role.RECEPTION);

        assertEquals(List.of("boss|USER_CREATED|Usuario 'recepcion1' con rol Recepción"), events);
    }

    @Test
    void updateWritesTheNewRole() {
        User trainer = service.create("entrenador1", "Carlos", "clave1234", Role.TRAINER);
        events.clear();

        service.update(trainer.getId(), "Carlos Ruiz", Role.RECEPTION);

        assertEquals(List.of("boss|USER_UPDATED|Usuario 'entrenador1', rol Recepción"), events);
    }

    @Test
    void resetPasswordWritesTheUserButNotThePassword() {
        service.resetPassword(admin.getId(), "nueva-clave99");

        assertEquals(List.of("boss|PASSWORD_RESET|Usuario 'admin'"), events);
        assertTrue(events.stream().noneMatch(e -> e.contains("nueva-clave99")));
    }

    @Test
    void deactivateAndReactivateAreWritten() {
        User trainer = service.create("entrenador1", "Carlos", "clave1234", Role.TRAINER);
        events.clear();

        service.deactivate(trainer.getId(), admin.getId());
        service.reactivate(trainer.getId());

        assertEquals(List.of(
                "boss|USER_DEACTIVATED|Usuario 'entrenador1'",
                "boss|USER_REACTIVATED|Usuario 'entrenador1'"), events);
    }

    @Test
    void doingNothingWritesNothing() {
        User trainer = service.create("entrenador1", "Carlos", "clave1234", Role.TRAINER);
        events.clear();

        service.reactivate(trainer.getId());

        assertTrue(events.isEmpty());
    }

    @Test
    void aRejectedChangeWritesNothing() {
        assertThrows(InvalidDataException.class, () -> service.create("ab", "Nombre", "clave1234", Role.TRAINER));
        assertThrows(BusinessRuleException.class, () -> service.create("admin", "Otro", "clave1234", Role.TRAINER));
        assertThrows(BusinessRuleException.class, () -> service.deactivate(admin.getId(), admin.getId()));

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
            if (user.getId() == 0) {
                User stored = user.withId(users.size() + 1L);
                users.add(stored);
                return stored;
            }
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId() == user.getId()) {
                    users.set(i, user);
                }
            }
            return user;
        }
    }
}
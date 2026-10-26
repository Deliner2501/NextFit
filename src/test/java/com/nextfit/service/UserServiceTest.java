package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.util.PasswordHasher;
import com.nextfit.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private UserService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new UserService(new InMemoryUsers(), new UserValidator());
        admin = service.create("admin", "Administrador", "clave1234", Role.ADMIN);
    }

    @Test
    void createStoresAnActiveUserWithAHashedPassword() {
        User created = service.create("recepcion1", "Laura Pérez", "clave1234", Role.RECEPTION);

        assertTrue(created.getId() > 0);
        assertTrue(created.isActive());
        assertNotEquals("clave1234", created.getPasswordHash());
        assertTrue(PasswordHasher.verify("clave1234", created.getPasswordHash()));
    }

    @Test
    void createRejectsInvalidData() {
        assertThrows(InvalidDataException.class, () -> service.create("ab", "Nombre", "clave1234", Role.TRAINER));
        assertThrows(InvalidDataException.class, () -> service.create("entrenador", "Nombre", "corta", Role.TRAINER));
        assertThrows(InvalidDataException.class, () -> service.create("entrenador", "Nombre", "clave1234", null));
    }

    @Test
    void createRejectsARepeatedUsernameIgnoringCase() {
        assertThrows(BusinessRuleException.class,
                () -> service.create("ADMIN", "Otro", "clave1234", Role.RECEPTION));
    }

    @Test
    void updateChangesNameAndRoleButKeepsPassword() {
        User trainer = service.create("coach", "Coach", "clave1234", Role.TRAINER);

        User changed = service.update(trainer.getId(), "Coach Principal", Role.RECEPTION);

        assertEquals("Coach Principal", changed.getFullName());
        assertEquals(Role.RECEPTION, changed.getRole());
        assertEquals(trainer.getPasswordHash(), changed.getPasswordHash());
    }

    @Test
    void theOnlyActiveAdministratorCannotLoseTheRole() {
        assertThrows(BusinessRuleException.class,
                () -> service.update(admin.getId(), "Administrador", Role.RECEPTION));
    }

    @Test
    void anAdministratorCanChangeRoleWhenAnotherOneExists() {
        service.create("admin2", "Segundo", "clave1234", Role.ADMIN);

        assertEquals(Role.TRAINER, service.update(admin.getId(), "Administrador", Role.TRAINER).getRole());
    }

    @Test
    void resetPasswordReplacesTheHash() {
        service.resetPassword(admin.getId(), "nueva5678");

        User stored = service.findAll().get(0);
        assertTrue(PasswordHasher.verify("nueva5678", stored.getPasswordHash()));
        assertFalse(PasswordHasher.verify("clave1234", stored.getPasswordHash()));
    }

    @Test
    void resetPasswordRejectsAWeakPassword() {
        assertThrows(InvalidDataException.class, () -> service.resetPassword(admin.getId(), "123"));
    }

    @Test
    void deactivateAndReactivateAUser() {
        User trainer = service.create("coach", "Coach", "clave1234", Role.TRAINER);

        assertFalse(service.deactivate(trainer.getId(), admin.getId()).isActive());
        assertTrue(service.reactivate(trainer.getId()).isActive());
    }

    @Test
    void nobodyCanDeactivateTheirOwnAccount() {
        service.create("admin2", "Segundo", "clave1234", Role.ADMIN);

        assertThrows(BusinessRuleException.class, () -> service.deactivate(admin.getId(), admin.getId()));
    }

    @Test
    void theLastActiveAdministratorCannotBeDeactivated() {
        User second = service.create("admin2", "Segundo", "clave1234", Role.ADMIN);
        service.deactivate(second.getId(), admin.getId());

        User receptionist = service.create("recep", "Recepción", "clave1234", Role.RECEPTION);
        assertThrows(BusinessRuleException.class, () -> service.deactivate(admin.getId(), receptionist.getId()));
    }

    @Test
    void unknownUserIsReported() {
        assertThrows(ResourceNotFoundException.class, () -> service.reactivate(999));
    }

    /** Users kept in memory so the tests never touch files. */
    private static final class InMemoryUsers implements UserRepository {

        private final List<User> stored = new ArrayList<>();
        private long nextId = 1;

        @Override
        public List<User> findAll() {
            return List.copyOf(stored);
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
            User toStore = user.getId() == 0 ? user.withId(nextId++) : user;
            stored.removeIf(u -> u.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }
}

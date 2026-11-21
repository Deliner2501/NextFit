package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.Role;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Subscription;
import com.nextfit.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Saves objects through the real CSV repositories and reads them back from new repositories. */
class CsvRepositoriesRoundTripTest {

    @TempDir
    Path folder;

    @Test
    void paymentsKeepTheirDiscountMethodAndAmounts() {
        Path file = folder.resolve("payments.csv");
        CsvPaymentRepository repository = new CsvPaymentRepository(file);
        LocalDateTime when = LocalDateTime.of(2026, 11, 10, 9, 30);
        repository.save(new Payment(0, 1, 1, when, PaymentMethod.CASH, 80_000, new NoDiscount(), "recepcion"));
        repository.save(new Payment(0, 2, 1, when, PaymentMethod.CARD, 80_000, new PercentageDiscount(10), "recepcion"));
        repository.save(new Payment(0, 3, 2, when, PaymentMethod.TRANSFER, 80_000, new FixedDiscount(5_000), "admin"));

        List<Payment> all = new CsvPaymentRepository(file).findAll();

        assertEquals(3, all.size());
        assertEquals(80_000, all.get(0).getTotal());
        assertEquals(72_000, all.get(1).getTotal());
        assertEquals(75_000, all.get(2).getTotal());
        assertEquals(PaymentMethod.TRANSFER, all.get(2).getMethod());
        assertEquals("admin", all.get(2).getRegisteredBy());
        assertEquals(when, all.get(0).getPaidAt());
        assertEquals(2, new CsvPaymentRepository(file).findByMemberId(1).size());
    }

    @Test
    void subscriptionsKeepTheCancelledFlag() {
        Path file = folder.resolve("subscriptions.csv");
        CsvSubscriptionRepository repository = new CsvSubscriptionRepository(file);
        Subscription saved = repository.save(new Subscription(0, 1, 2, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), false));
        repository.save(new Subscription(saved.getId(), 1, 2, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), true));

        List<Subscription> all = new CsvSubscriptionRepository(file).findAll();

        assertEquals(1, all.size(), "saving with the same id replaces the row");
        assertTrue(all.get(0).isCancelled());
        assertEquals(LocalDate.of(2026, 11, 30), all.get(0).getEndDate());
    }

    @Test
    void anOpenVisitIsRememberedAsOpenAndClosesLater() {
        Path file = folder.resolve("attendance.csv");
        CsvAttendanceRepository repository = new CsvAttendanceRepository(file);
        LocalDateTime in = LocalDateTime.of(2026, 11, 12, 7, 15);
        AttendanceRecord open = repository.save(new AttendanceRecord(0, 5, in, null));

        CsvAttendanceRepository reopened = new CsvAttendanceRepository(file);
        assertTrue(reopened.findOpenByMemberId(5).isPresent());
        assertTrue(reopened.findOpenByMemberId(5).orElseThrow().isOpen());

        reopened.save(new AttendanceRecord(open.getId(), 5, in, in.plusHours(1)));

        CsvAttendanceRepository closed = new CsvAttendanceRepository(file);
        assertFalse(closed.findOpenByMemberId(5).isPresent());
        assertEquals(in.plusHours(1), closed.findByMemberId(5).get(0).getCheckOut().orElseThrow());
    }

    @Test
    void aRoutineKeepsItsDaysAndExercisesInOrder() {
        Path routines = folder.resolve("routines.csv");
        Path lines = folder.resolve("routine_lines.csv");
        Routine routine = new Routine(0, "Fuerza, nivel \"1\"", "Ganar masa muscular", 3, LocalDate.of(2026, 10, 1),
                List.of(new RoutineDay("Espalda y bíceps", List.of(
                                new RoutineExercise(7, 4, 10, 60), new RoutineExercise(9, 3, 12, 45))),
                        new RoutineDay("Pierna", List.of(new RoutineExercise(2, 5, 5, 120)))));
        new CsvRoutineRepository(routines, lines).save(routine);

        Routine read = new CsvRoutineRepository(routines, lines).findAll().get(0);

        assertEquals("Fuerza, nivel \"1\"", read.getName());
        assertEquals(2, read.getDays().size());
        assertEquals("Espalda y bíceps", read.getDays().get(0).getName());
        assertEquals(List.of(7L, 9L), read.getDays().get(0).getExercises().stream()
                .map(RoutineExercise::getExerciseId).toList());
        assertEquals(120, read.getDays().get(1).getExercises().get(0).getRestSeconds());
        assertEquals(1, new CsvRoutineRepository(routines, lines).findByTrainerId(3).size());
        assertTrue(new CsvRoutineRepository(routines, lines).findByTrainerId(99).isEmpty());
    }

    @Test
    void savingARoutineAgainReplacesItsExercisesInsteadOfAddingMore() {
        Path routines = folder.resolve("routines.csv");
        Path lines = folder.resolve("routine_lines.csv");
        CsvRoutineRepository repository = new CsvRoutineRepository(routines, lines);
        Routine saved = repository.save(new Routine(0, "Rutina", "Objetivo", 1, LocalDate.now(),
                List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(1, 3, 10, 60),
                        new RoutineExercise(2, 3, 10, 60))))));
        repository.save(new Routine(saved.getId(), "Rutina", "Objetivo", 1, LocalDate.now(),
                List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(1, 3, 10, 60))))));

        Routine read = new CsvRoutineRepository(routines, lines).findById(saved.getId()).orElseThrow();

        assertEquals(1, read.getDays().get(0).getExercises().size());
    }

    @Test
    void usersKeepTheirHashRoleAndActiveFlag() {
        Path file = folder.resolve("users.csv");
        CsvUserRepository repository = new CsvUserRepository(file);
        String hash = "pbkdf2$210000$jDwvB7IGjYXWMArGOrHbNg==$abc+/=";
        repository.save(new User(0, "ana.perez", "Ana Pérez", hash, Role.RECEPTION, true));
        repository.save(new User(0, "luis", "Luis", hash, Role.TRAINER, false));

        CsvUserRepository reopened = new CsvUserRepository(file);

        User ana = reopened.findByUsername("ana.perez").orElseThrow();
        assertEquals(hash, ana.getPasswordHash());
        assertEquals(Role.RECEPTION, ana.getRole());
        assertFalse(reopened.findByUsername("luis").orElseThrow().isActive());
        assertTrue(reopened.findByUsername("nadie").isEmpty());
    }

    @Test
    void aMalformedPaymentRowIsReportedNotIgnored() throws IOException {
        Path file = folder.resolve("payments.csv");
        Files.writeString(file, "id,subscriptionId,memberId,paidAt,method,subtotal,discountType,discountValue,registeredBy\n"
                + "1,1,1,not-a-date,CASH,80000,NONE,0,recepcion\n", StandardCharsets.UTF_8);

        assertThrows(PersistenceException.class, () -> new CsvPaymentRepository(file).findAll());
    }
}

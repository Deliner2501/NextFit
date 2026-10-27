package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.MembershipPlan;
import com.nextfit.service.BillingFakeRepositories.FakePlanRepository;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipPlanServiceTest {

    private MembershipPlanService service;

    @BeforeEach
    void setUp() {
        service = new MembershipPlanService(new FakePlanRepository(), new MembershipPlanValidator());
    }

    static MembershipPlan newPlan(String name, int days, long price) {
        return new MembershipPlan(0, name, "Acceso a todas las zonas", days, price, true);
    }

    @Test
    void createAssignsAnIdentifier() {
        MembershipPlan stored = service.create(newPlan("Mensual", 30, 80_000));

        assertTrue(stored.getId() > 0);
        assertEquals("Mensual", service.findById(stored.getId()).getName());
    }

    @Test
    void createRejectsInvalidData() {
        assertThrows(InvalidDataException.class, () -> service.create(newPlan("", 30, 80_000)));
        assertThrows(InvalidDataException.class, () -> service.create(newPlan("Mensual", 0, 80_000)));
        assertThrows(InvalidDataException.class, () -> service.create(newPlan("Mensual", 30, 0)));
    }

    @Test
    void createRejectsARepeatedNameIgnoringCase() {
        service.create(newPlan("Mensual", 30, 80_000));

        assertThrows(BusinessRuleException.class, () -> service.create(newPlan("mensual", 30, 90_000)));
    }

    @Test
    void updateKeepsTheSameNameForTheSamePlan() {
        MembershipPlan stored = service.create(newPlan("Mensual", 30, 80_000));

        MembershipPlan changed = service.update(
                new MembershipPlan(stored.getId(), "Mensual", "Nueva descripción", 30, 85_000, true));

        assertEquals(85_000, changed.getPrice());
    }

    @Test
    void updateRejectsTheNameOfAnotherPlan() {
        service.create(newPlan("Mensual", 30, 80_000));
        MembershipPlan other = service.create(newPlan("Trimestral", 90, 210_000));

        assertThrows(BusinessRuleException.class, () -> service.update(
                new MembershipPlan(other.getId(), "Mensual", "", 90, 210_000, true)));
    }

    @Test
    void updateFailsWhenThePlanDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> service.update(
                new MembershipPlan(99, "Anual", "", 365, 700_000, true)));
    }

    @Test
    void deactivatedPlansLeaveTheActiveListButStayStored() {
        MembershipPlan stored = service.create(newPlan("Mensual", 30, 80_000));

        service.deactivate(stored.getId());

        assertTrue(service.findActive().isEmpty());
        assertEquals(1, service.findAll().size());
        assertFalse(service.findById(stored.getId()).isActive());
    }

    @Test
    void reactivateBringsThePlanBack() {
        MembershipPlan stored = service.create(newPlan("Mensual", 30, 80_000));
        service.deactivate(stored.getId());

        service.reactivate(stored.getId());

        assertEquals(1, service.findActive().size());
    }

    @Test
    void endDateCountsTheStartingDayAsTheFirstDay() {
        MembershipPlan plan = newPlan("Semanal", 7, 25_000);

        assertEquals(java.time.LocalDate.of(2026, 11, 2), plan.endDateFor(java.time.LocalDate.of(2026, 10, 27)));
    }
}

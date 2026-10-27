package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.BmiCategory;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeHealthProfileRepository;
import com.nextfit.service.FakeRepositories.FakeMeasurementRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthServiceTest {

    private HealthService service;
    private Member member;

    @BeforeEach
    void setUp() {
        MemberService memberService = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = memberService.register(FakeRepositories.newMember("1065123456"));
        service = new HealthService(memberService, new FakeHealthProfileRepository(),
                new FakeMeasurementRepository(), new HealthValidator());
    }

    private Measurement measurement(LocalDate date, double weightKg) {
        return new Measurement(0, member.getId(), date, weightKg, 85, 100, 95);
    }

    @Test
    void bmiIsWeightOverHeightSquaredRoundedToOneDecimal() {
        assertEquals(22.9, service.calculateBmi(70, 175), 0.0001);
    }

    @Test
    void bmiIsClassifiedByTheLimitsOfTheWorldHealthOrganization() {
        assertEquals(BmiCategory.UNDERWEIGHT, service.classify(18.4));
        assertEquals(BmiCategory.NORMAL, service.classify(18.5));
        assertEquals(BmiCategory.OVERWEIGHT, service.classify(25.0));
        assertEquals(BmiCategory.OBESITY, service.classify(30.0));
    }

    @Test
    void currentBmiUsesTheLatestWeightAndTheProfileHeight() {
        service.saveProfile(new HealthProfile(member.getId(), 175, "O+", "", ""));
        service.addMeasurement(measurement(LocalDate.of(2026, 9, 1), 80));
        service.addMeasurement(measurement(LocalDate.of(2026, 10, 1), 70));

        assertEquals(22.9, service.currentBmi(member.getId()).orElseThrow(), 0.0001);
    }

    @Test
    void currentBmiIsEmptyWithoutAProfileOrMeasurements() {
        assertTrue(service.currentBmi(member.getId()).isEmpty());
    }

    @Test
    void measurementHistoryIsOrderedFromOldestToNewest() {
        service.addMeasurement(measurement(LocalDate.of(2026, 10, 1), 70));
        service.addMeasurement(measurement(LocalDate.of(2026, 9, 1), 80));

        assertEquals(LocalDate.of(2026, 9, 1), service.measurementHistory(member.getId()).get(0).getDate());
        assertEquals(LocalDate.of(2026, 10, 1), service.latestMeasurement(member.getId()).orElseThrow().getDate());
    }

    @Test
    void addMeasurementRejectsAnImpossibleWeight() {
        assertThrows(InvalidDataException.class,
                () -> service.addMeasurement(measurement(LocalDate.of(2026, 10, 1), 5)));
    }

    @Test
    void saveProfileRejectsAnUnknownBloodType() {
        assertThrows(InvalidDataException.class,
                () -> service.saveProfile(new HealthProfile(member.getId(), 175, "Z+", "", "")));
    }

    @Test
    void saveProfileRejectsAnUnknownMember() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.saveProfile(new HealthProfile(99, 175, "O+", "", "")));
    }
}
package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HealthValidatorTest {

    private final HealthValidator validator = new HealthValidator();

    private static HealthProfile profile(double height, String blood, String conditions, String allergies) {
        return new HealthProfile(1, height, blood, conditions, allergies);
    }

    private static Measurement measurement(LocalDate date, double weight, double waist, double chest, double hips) {
        return new Measurement(0, 1, date, weight, waist, chest, hips);
    }

    private String badField(Measurement measurement) {
        return assertThrows(InvalidDataException.class, () -> validator.validateMeasurement(measurement)).getFieldName();
    }

    @Test
    void aCompleteProfileIsAccepted() {
        assertDoesNotThrow(() -> validator.validateProfile(profile(175, "O+", "Asma leve", "Ninguna")));
    }

    @Test
    void theHeightIsBetweenOneHundredAndTwoHundredFiftyCentimeters() {
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(99, "O+", "", "")));
        assertDoesNotThrow(() -> validator.validateProfile(profile(100, "O+", "", "")));
        assertDoesNotThrow(() -> validator.validateProfile(profile(250, "O+", "", "")));
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(251, "O+", "", "")));
    }

    @Test
    void onlyTheEightRealBloodTypesAreAccepted() {
        for (String type : new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}) {
            assertDoesNotThrow(() -> validator.validateProfile(profile(170, type, "", "")));
        }
        assertEquals("tipo de sangre", assertThrows(InvalidDataException.class,
                () -> validator.validateProfile(profile(170, "C+", "", ""))).getFieldName());
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(170, null, "", "")));
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(170, "o+", "", "")));
    }

    @Test
    void conditionsAndAllergiesAreLimitedToTwoHundredCharacters() {
        assertDoesNotThrow(() -> validator.validateProfile(profile(170, "O+", "x".repeat(200), "x".repeat(200))));
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(170, "O+", "x".repeat(201), "")));
        assertThrows(InvalidDataException.class, () -> validator.validateProfile(profile(170, "O+", "", "x".repeat(201))));
    }

    @Test
    void aCompleteMeasurementIsAccepted() {
        assertDoesNotThrow(() -> validator.validateMeasurement(measurement(LocalDate.now(), 80, 85, 100, 95)));
    }

    @Test
    void theDateIsRequiredAndCannotBeInTheFuture() {
        assertEquals("fecha", badField(measurement(null, 80, 85, 100, 95)));
        assertEquals("fecha", badField(measurement(LocalDate.now().plusDays(1), 80, 85, 100, 95)));
    }

    @Test
    void eachBodyValueHasItsOwnRange() {
        LocalDate today = LocalDate.now();
        assertEquals("peso", badField(measurement(today, 19, 85, 100, 95)));
        assertEquals("peso", badField(measurement(today, 401, 85, 100, 95)));
        assertEquals("cintura", badField(measurement(today, 80, 19, 100, 95)));
        assertEquals("pecho", badField(measurement(today, 80, 85, 301, 95)));
        assertEquals("cadera", badField(measurement(today, 80, 85, 100, 0)));
        assertDoesNotThrow(() -> validator.validateMeasurement(measurement(today, 20, 20, 20, 20)));
        assertDoesNotThrow(() -> validator.validateMeasurement(measurement(today, 400, 300, 300, 300)));
    }
}

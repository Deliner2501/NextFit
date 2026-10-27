package com.nextfit.service;

import com.nextfit.model.BmiCategory;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.persistence.HealthProfileRepository;
import com.nextfit.persistence.MeasurementRepository;
import com.nextfit.validation.HealthValidator;

import java.util.List;
import java.util.Optional;

/**
 * Rules of the health follow-up of a member: the health profile, the body
 * measurements over time and the body mass index. It knows nothing about
 * screens or files.
 */
public class HealthService {

    private static final double CM_PER_METER = 100.0;

    private final MemberService memberService;
    private final HealthProfileRepository profiles;
    private final MeasurementRepository measurements;
    private final HealthValidator validator;

    /**
     * Creates the service.
     *
     * @param memberService the service used to check that the member exists
     * @param profiles      the health profile storage
     * @param measurements  the measurement storage
     * @param validator     the checker of health data
     */
    public HealthService(MemberService memberService, HealthProfileRepository profiles,
                         MeasurementRepository measurements, HealthValidator validator) {
        this.memberService = memberService;
        this.profiles = profiles;
        this.measurements = measurements;
        this.validator = validator;
    }

    /**
     * Calculates the body mass index, rounded to one decimal.
     *
     * @param weightKg the weight in kilograms
     * @param heightCm the height in centimeters
     * @return the body mass index
     */
    public double calculateBmi(double weightKg, double heightCm) {
        double heightMeters = heightCm / CM_PER_METER;
        double bmi = weightKg / (heightMeters * heightMeters);
        return Math.round(bmi * 10) / 10.0;
    }

    /**
     * Returns the weight category of a body mass index.
     *
     * @param bmi the body mass index
     * @return the category
     */
    public BmiCategory classify(double bmi) {
        return BmiCategory.fromBmi(bmi);
    }

    /**
     * Stores the health profile of a member, replacing the previous one.
     *
     * @param profile the profile to store
     * @return the stored profile
     * @throws com.nextfit.exceptions.ResourceNotFoundException when the member does not exist
     * @throws com.nextfit.exceptions.InvalidDataException      when some field is not valid
     */
    public HealthProfile saveProfile(HealthProfile profile) {
        memberService.findById(profile.getMemberId());
        validator.validateProfile(profile);
        return profiles.save(profile);
    }

    /**
     * Looks the health profile of a member up.
     *
     * @param memberId the identifier of the member
     * @return the profile, or empty when the member has none yet
     */
    public Optional<HealthProfile> findProfile(long memberId) {
        return profiles.findByMemberId(memberId);
    }

    /**
     * Adds a new body measurement to a member.
     *
     * @param measurement the measurement to add
     * @return the stored measurement, with its identifier
     * @throws com.nextfit.exceptions.ResourceNotFoundException when the member does not exist
     * @throws com.nextfit.exceptions.InvalidDataException      when some field is not valid
     */
    public Measurement addMeasurement(Measurement measurement) {
        memberService.findById(measurement.getMemberId());
        validator.validateMeasurement(measurement);
        return measurements.save(measurement.withId(0));
    }

    /**
     * Returns the measurements of a member, from the oldest to the newest.
     *
     * @param memberId the identifier of the member
     * @return the measurements
     */
    public List<Measurement> measurementHistory(long memberId) {
        return measurements.findByMemberId(memberId);
    }

    /**
     * Returns the most recent measurement of a member.
     *
     * @param memberId the identifier of the member
     * @return the latest measurement, or empty when there is none
     */
    public Optional<Measurement> latestMeasurement(long memberId) {
        List<Measurement> history = measurementHistory(memberId);
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    /**
     * Calculates the current body mass index of a member from the latest
     * weight and the height of the health profile.
     *
     * @param memberId the identifier of the member
     * @return the index, or empty when the member has no profile or no measurement
     */
    public Optional<Double> currentBmi(long memberId) {
        Optional<HealthProfile> profile = findProfile(memberId);
        Optional<Measurement> latest = latestMeasurement(memberId);
        if (profile.isEmpty() || latest.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(calculateBmi(latest.get().getWeightKg(), profile.get().getHeightCm()));
    }
}
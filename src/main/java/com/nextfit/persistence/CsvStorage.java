package com.nextfit.persistence;

import java.nio.file.Path;

/** The storage of CSV files: one file per kind of data inside the data folder. */
public final class CsvStorage implements Storage {

    private final UserRepository users;
    private final MemberRepository members;
    private final HealthProfileRepository healthProfiles;
    private final MeasurementRepository measurements;
    private final MembershipPlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final AttendanceRepository attendance;
    private final FaceTemplateRepository faceTemplates;
    private final AuditRepository audit;
    private final TrainerRepository trainers;
    private final ExerciseRepository exercises;
    private final RoutineRepository routines;
    private final AssignmentRepository assignments;
    private final TrainingSessionRepository trainingSessions;

    /**
     * Opens the CSV files of the data folder, creating the ones that do not exist.
     *
     * @param dataDir the folder that holds the CSV files
     */
    public CsvStorage(Path dataDir) {
        users = new CsvUserRepository(dataDir.resolve("users.csv"));
        members = new CsvMemberRepository(dataDir.resolve("members.csv"));
        healthProfiles = new CsvHealthProfileRepository(dataDir.resolve("health_profiles.csv"));
        measurements = new CsvMeasurementRepository(dataDir.resolve("measurements.csv"));
        plans = new CsvMembershipPlanRepository(dataDir.resolve("plans.csv"));
        subscriptions = new CsvSubscriptionRepository(dataDir.resolve("subscriptions.csv"));
        payments = new CsvPaymentRepository(dataDir.resolve("payments.csv"));
        attendance = new CsvAttendanceRepository(dataDir.resolve("attendance.csv"));
        faceTemplates = new CsvFaceTemplateRepository(dataDir.resolve("face_templates.csv"));
        audit = new CsvAuditRepository(dataDir.resolve("audit.csv"));
        trainers = new CsvTrainerRepository(dataDir.resolve("trainers.csv"));
        exercises = new CsvExerciseRepository(dataDir.resolve("exercises.csv"));
        routines = new CsvRoutineRepository(dataDir.resolve("routines.csv"), dataDir.resolve("routine_lines.csv"));
        assignments = new CsvAssignmentRepository(dataDir.resolve("assignments.csv"));
        trainingSessions = new CsvTrainingSessionRepository(dataDir.resolve("training_sessions.csv"));
    }

    @Override
    public UserRepository users() {
        return users;
    }

    @Override
    public MemberRepository members() {
        return members;
    }

    @Override
    public HealthProfileRepository healthProfiles() {
        return healthProfiles;
    }

    @Override
    public MeasurementRepository measurements() {
        return measurements;
    }

    @Override
    public MembershipPlanRepository plans() {
        return plans;
    }

    @Override
    public SubscriptionRepository subscriptions() {
        return subscriptions;
    }

    @Override
    public PaymentRepository payments() {
        return payments;
    }

    @Override
    public AttendanceRepository attendance() {
        return attendance;
    }

    @Override
    public FaceTemplateRepository faceTemplates() {
        return faceTemplates;
    }

    @Override
    public AuditRepository audit() {
        return audit;
    }

    @Override
    public TrainerRepository trainers() {
        return trainers;
    }

    @Override
    public ExerciseRepository exercises() {
        return exercises;
    }

    @Override
    public RoutineRepository routines() {
        return routines;
    }

    @Override
    public AssignmentRepository assignments() {
        return assignments;
    }

    @Override
    public TrainingSessionRepository trainingSessions() {
        return trainingSessions;
    }

    @Override
    public String description() {
        return "archivos CSV";
    }
}

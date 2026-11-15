package com.nextfit.persistence.jdbc;

import com.nextfit.persistence.AssignmentRepository;
import com.nextfit.persistence.AttendanceRepository;
import com.nextfit.persistence.AuditRepository;
import com.nextfit.persistence.ExerciseRepository;
import com.nextfit.persistence.FaceTemplateRepository;
import com.nextfit.persistence.HealthProfileRepository;
import com.nextfit.persistence.MeasurementRepository;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.persistence.MembershipPlanRepository;
import com.nextfit.persistence.PaymentRepository;
import com.nextfit.persistence.RoutineRepository;
import com.nextfit.persistence.SubscriptionRepository;
import com.nextfit.persistence.TrainerRepository;
import com.nextfit.persistence.TrainingSessionRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.persistence.Storage;

/** The storage of the Oracle database: every repository works on the same connections. */
public final class JdbcStorage implements Storage {

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
     * Creates the repositories on the given connections.
     *
     * @param connections where the connections come from
     */
    public JdbcStorage(ConnectionSource connections) {
        users = new JdbcUserRepository(connections);
        members = new JdbcMemberRepository(connections);
        healthProfiles = new JdbcHealthProfileRepository(connections);
        measurements = new JdbcMeasurementRepository(connections);
        plans = new JdbcMembershipPlanRepository(connections);
        subscriptions = new JdbcSubscriptionRepository(connections);
        payments = new JdbcPaymentRepository(connections);
        attendance = new JdbcAttendanceRepository(connections);
        faceTemplates = new JdbcFaceTemplateRepository(connections);
        audit = new JdbcAuditRepository(connections);
        trainers = new JdbcTrainerRepository(connections);
        exercises = new JdbcExerciseRepository(connections);
        routines = new JdbcRoutineRepository(connections);
        assignments = new JdbcAssignmentRepository(connections);
        trainingSessions = new JdbcTrainingSessionRepository(connections);
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
        return "base de datos Oracle";
    }
}

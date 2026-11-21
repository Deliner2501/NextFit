package com.nextfit.persistence;

import java.nio.file.Path;

/**
 * Where the application keeps its data: one repository for each kind of data.
 * There are two kinds of storage (CSV files and the Oracle database), and the
 * services receive repositories from here without knowing which one it is.
 */
public interface Storage {

    /** @return the repository of users */
    UserRepository users();

    /** @return the repository of members */
    MemberRepository members();

    /** @return the repository of health profiles */
    HealthProfileRepository healthProfiles();

    /** @return the repository of body measurements */
    MeasurementRepository measurements();

    /** @return the repository of membership plans */
    MembershipPlanRepository plans();

    /** @return the repository of subscriptions */
    SubscriptionRepository subscriptions();

    /** @return the repository of payments */
    PaymentRepository payments();

    /** @return the repository of attendance records */
    AttendanceRepository attendance();

    /** @return the repository of face templates */
    FaceTemplateRepository faceTemplates();

    /** @return the repository of the audit log */
    AuditRepository audit();

    /** @return the repository of trainers */
    TrainerRepository trainers();

    /** @return the repository of exercises */
    ExerciseRepository exercises();

    /** @return the repository of routines */
    RoutineRepository routines();

    /** @return the repository of routine assignments */
    AssignmentRepository assignments();

    /** @return the repository of training sessions */
    TrainingSessionRepository trainingSessions();

    /** @return the repository of the passwords that members create to enter by themselves */
    MemberCredentialRepository memberCredentials();

    /** Reads the signing key and makes it available in the given local file for offline use. */
    default byte[] accessKey(Path localFile) {
        return AccessKeyStore.loadOrCreate(localFile);
    }

    /** @return a short text that says which storage is in use, to show in the console */
    String description();
}

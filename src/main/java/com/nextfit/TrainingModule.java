package com.nextfit;

import com.nextfit.persistence.CsvStorage;
import com.nextfit.persistence.Storage;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.MemberService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.SubscriptionChecker;
import com.nextfit.service.TrainerService;
import com.nextfit.service.TrainingSessionService;
import com.nextfit.validation.ExerciseValidator;
import com.nextfit.validation.RoutineValidator;
import com.nextfit.validation.TrainerValidator;
import com.nextfit.validation.TrainingSessionValidator;

import java.nio.file.Path;

/**
 * Builds and connects everything the training module needs (repositories,
 * validators and services). Main only creates this object, so all the wiring
 * of the module lives in one place and Main stays small.
 */
public final class TrainingModule {

    private final TrainerService trainers;
    private final MemberService members;
    private final ExerciseService exercises;
    private final RoutineService routines;
    private final RoutineAssignmentService assignments;
    private final TrainingSessionService sessions;

    /**
     * Builds the module on CSV files.
     *
     * @param dataDir       folder where the CSV files are stored
     * @param members       member service (provided by the members module)
     * @param subscriptions membership check (provided by the billing module)
     */
    public TrainingModule(Path dataDir, MemberService members, SubscriptionChecker subscriptions) {
        this(new CsvStorage(dataDir), members, subscriptions);
    }

    /**
     * Builds the module on the storage chosen at the start (CSV files or Oracle).
     *
     * @param storage       where the repositories come from
     * @param members       member service (provided by the members module)
     * @param subscriptions membership check (provided by the billing module)
     */
    public TrainingModule(Storage storage, MemberService members, SubscriptionChecker subscriptions) {
        // Los servicios se crean de abajo hacia arriba: primero los que no dependen de otros.
        this.members = members;
        trainers = new TrainerService(storage.trainers(), new TrainerValidator());
        exercises = new ExerciseService(storage.exercises(), new ExerciseValidator());
        routines = new RoutineService(storage.routines(), new RoutineValidator(), trainers, exercises);
        assignments = new RoutineAssignmentService(storage.assignments(), members, routines, subscriptions);
        sessions = new TrainingSessionService(storage.trainingSessions(),
                new TrainingSessionValidator(), assignments, routines, subscriptions);
    }

    /** The member service received at creation (the members module owns it). */
    public MemberService members() {
        return members;
    }

    public TrainerService trainers() {
        return trainers;
    }

    public ExerciseService exercises() {
        return exercises;
    }

    public RoutineService routines() {
        return routines;
    }

    public RoutineAssignmentService assignments() {
        return assignments;
    }

    public TrainingSessionService sessions() {
        return sessions;
    }
}

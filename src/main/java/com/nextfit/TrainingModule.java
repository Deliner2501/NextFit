package com.nextfit;

import com.nextfit.persistence.CsvAssignmentRepository;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvRoutineRepository;
import com.nextfit.persistence.CsvTrainerRepository;
import com.nextfit.persistence.CsvTrainingSessionRepository;
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
     * @param dataDir       folder where the CSV files are stored
     * @param members       member service (provided by the members module)
     * @param subscriptions membership check (provided by the billing module)
     */
    public TrainingModule(Path dataDir, MemberService members, SubscriptionChecker subscriptions) {
        // Los servicios se crean de abajo hacia arriba: primero los que no dependen de otros.
        this.members = members;
        trainers = new TrainerService(
                new CsvTrainerRepository(dataDir.resolve("trainers.csv")), new TrainerValidator());
        exercises = new ExerciseService(
                new CsvExerciseRepository(dataDir.resolve("exercises.csv")), new ExerciseValidator());
        routines = new RoutineService(
                new CsvRoutineRepository(dataDir.resolve("routines.csv"), dataDir.resolve("routine_lines.csv")),
                new RoutineValidator(), trainers, exercises);
        assignments = new RoutineAssignmentService(
                new CsvAssignmentRepository(dataDir.resolve("assignments.csv")),
                members, routines, subscriptions);
        sessions = new TrainingSessionService(
                new CsvTrainingSessionRepository(dataDir.resolve("training_sessions.csv")),
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
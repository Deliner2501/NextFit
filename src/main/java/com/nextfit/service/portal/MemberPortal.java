package com.nextfit.service.portal;

import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.TrainingSession;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.TrainingSessionService;
import com.nextfit.service.gamification.GamificationService;

import java.util.List;
import java.util.Optional;

/**
 * Gathers what a member sees from the phone: the membership, the day of the routine to train and
 * the progress. The member is found with the access code of the carnet. It only reads from the
 * services that already exist; it draws nothing and changes nothing, so the same summary can feed
 * a web page, a mobile app or a screen of the gym.
 */
public class MemberPortal {

    private final PhoneAccess phone;
    private final SubscriptionService subscriptions;
    private final MembershipPlanService plans;
    private final RoutineAssignmentService assignments;
    private final RoutineService routines;
    private final ExerciseService exercises;
    private final TrainingSessionService sessions;
    private final GamificationService gamification;

    /**
     * Creates the portal.
     *
     * @param phone         finds the member that owns an access code
     * @param subscriptions the source of the membership
     * @param plans         the source of the plan names
     * @param assignments   the source of the routine assigned to the member
     * @param routines      the source of the routines
     * @param exercises     the catalog of exercises
     * @param sessions      the sessions the member already did
     * @param gamification  the source of the streaks and the badges
     */
    public MemberPortal(PhoneAccess phone, SubscriptionService subscriptions, MembershipPlanService plans,
                        RoutineAssignmentService assignments, RoutineService routines, ExerciseService exercises,
                        TrainingSessionService sessions, GamificationService gamification) {
        this.phone = phone;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.assignments = assignments;
        this.routines = routines;
        this.exercises = exercises;
        this.sessions = sessions;
        this.gamification = gamification;
    }

    /**
     * Builds the summary of the member that owns an access code.
     *
     * @param token the access code of the carnet
     * @return the summary, or empty when the code belongs to nobody
     */
    public Optional<PortalSummary> summaryFor(String token) {
        return phone.findMember(token).map(member -> new PortalSummary(member, membershipOf(member),
                workoutOf(member), gamification.achievementsOf(member.getId())));
    }

    private Optional<PortalMembership> membershipOf(Member member) {
        return subscriptions.findCurrentFor(member.getId()).map(subscription -> new PortalMembership(
                plans.findById(subscription.getPlanId()).getName(), subscription.getEndDate(),
                subscriptions.daysLeftOf(subscription)));
    }

    private Optional<PortalWorkout> workoutOf(Member member) {
        Optional<RoutineAssignment> current = assignments.findCurrentFor(member.getId());
        if (current.isEmpty()) {
            return Optional.empty();
        }
        RoutineAssignment assignment = current.get();
        Routine routine = routines.findById(assignment.getRoutineId());
        List<TrainingSession> done = sessions.findByMember(member.getId()).stream()
                .filter(session -> session.getAssignmentId() == assignment.getId())
                .toList();
        int index = WorkoutPlanner.nextDayIndex(routine, done);
        if (index < 0) {
            return Optional.empty();
        }
        RoutineDay day = routine.getDays().get(index);
        List<PortalExercise> lines = day.getExercises().stream()
                .map(line -> {
                    var exercise = exercises.findById(line.getExerciseId());
                    return new PortalExercise(exercise.getName(), exercise.getMuscleGroup().getLabel(),
                            line.getSets(), line.getRepetitions(), line.getRestSeconds());
                })
                .toList();
        return Optional.of(new PortalWorkout(routine.getName(), day.getName(), index + 1,
                routine.getDays().size(), lines));
    }
}

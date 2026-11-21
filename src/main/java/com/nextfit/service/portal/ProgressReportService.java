package com.nextfit.service.portal;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.TrainingSession;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainingSessionService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Gathers what the portal shows besides the day to train: the days that come next in the routine, the
 * last visits and the change of the body measures. It only reads; it changes nothing.
 */
public class ProgressReportService {

    public static final int UPCOMING_DAYS = 3;
    public static final int RECENT_VISITS = 5;

    private final RoutineAssignmentService assignments;
    private final RoutineService routines;
    private final TrainingSessionService sessions;
    private final AttendanceService attendance;
    private final HealthService health;

    /**
     * @param assignments the routine assigned to each member
     * @param routines    the routines and their days
     * @param sessions    the training sessions already done
     * @param attendance  the visits to the gym
     * @param health      the body measures
     */
    public ProgressReportService(RoutineAssignmentService assignments, RoutineService routines,
                                 TrainingSessionService sessions, AttendanceService attendance,
                                 HealthService health) {
        this.assignments = assignments;
        this.routines = routines;
        this.sessions = sessions;
        this.attendance = attendance;
        this.health = health;
    }

    /**
     * Builds the report of a member.
     *
     * @param member the member
     * @return the report; a part is empty when the member has no data for it
     */
    public ProgressReport of(Member member) {
        return new ProgressReport(upcomingDays(member), recentVisits(member), bodyProgress(member));
    }

    /** The days after the one to train today, going around the routine; never the same day twice. */
    private List<ProgressReport.UpcomingDay> upcomingDays(Member member) {
        Optional<RoutineAssignment> current = assignments.findCurrentFor(member.getId());
        if (current.isEmpty()) {
            return List.of();
        }
        RoutineAssignment assignment = current.get();
        Routine routine = routines.findById(assignment.getRoutineId());
        List<TrainingSession> done = sessions.findByMember(member.getId()).stream()
                .filter(session -> session.getAssignmentId() == assignment.getId())
                .toList();
        int today = WorkoutPlanner.nextDayIndex(routine, done);
        int count = routine.getDays().size();
        List<ProgressReport.UpcomingDay> days = new ArrayList<>();
        for (int step = 1; step < count && days.size() < UPCOMING_DAYS && today >= 0; step++) {
            int index = (today + step) % count;
            var day = routine.getDays().get(index);
            days.add(new ProgressReport.UpcomingDay(index + 1, day.getName(), day.getExercises().size()));
        }
        return days;
    }

    private List<ProgressReport.RecentVisit> recentVisits(Member member) {
        return attendance.findByMember(member.getId()).stream()
                .sorted(Comparator.comparing(AttendanceRecord::getCheckIn).reversed())
                .limit(RECENT_VISITS)
                .map(record -> new ProgressReport.RecentVisit(record.getCheckIn().toLocalDate(),
                        record.getCheckIn().toLocalTime(),
                        record.getCheckOut().map(out -> {
                            Duration stay = Duration.between(record.getCheckIn(), out);
                            return stay.isNegative() ? Duration.ZERO : stay;
                        })))
                .toList();
    }

    private Optional<ProgressReport.BodyProgress> bodyProgress(Member member) {
        List<Measurement> history = health.measurementHistory(member.getId()).stream()
                .sorted(Comparator.comparing(Measurement::getDate).thenComparing(Measurement::getId))
                .toList();
        if (history.isEmpty()) {
            return Optional.empty();
        }
        Measurement first = history.get(0);
        Measurement last = history.get(history.size() - 1);
        return Optional.of(new ProgressReport.BodyProgress(first.getDate(), first.getWeightKg(),
                last.getWeightKg(), history.size(), health.currentBmi(member.getId())));
    }
}

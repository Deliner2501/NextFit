package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.TrainingSessionRepository;
import com.nextfit.validation.TrainingSessionValidator;

import java.util.Comparator;
import java.util.List;

/** Business rules for logging the training sessions of the members. */
public class TrainingSessionService {

    private final TrainingSessionRepository sessions;
    private final TrainingSessionValidator validator;
    private final RoutineAssignmentService assignments;
    private final RoutineService routines;
    private final SubscriptionChecker subscriptions;

    public TrainingSessionService(TrainingSessionRepository sessions, TrainingSessionValidator validator,
                                  RoutineAssignmentService assignments, RoutineService routines,
                                  SubscriptionChecker subscriptions) {
        this.sessions = sessions;
        this.validator = validator;
        this.assignments = assignments;
        this.routines = routines;
        this.subscriptions = subscriptions;
    }

    /** Returns the sessions of the member, newest first. */
    public List<TrainingSession> findByMember(long memberId) {
        return sessions.findByMemberId(memberId).stream()
                .sorted(Comparator.comparing(TrainingSession::getDate).reversed())
                .toList();
    }

    /**
     * Logs a session. Rules: the data is valid; the member has a valid membership;
     * the assignment belongs to the member and is ACTIVE; the day exists in the routine.
     */
    public TrainingSession log(TrainingSession session) {
        validator.validate(session);
        if (!subscriptions.hasActiveSubscription(session.getMemberId())) {
            throw new BusinessRuleException("El socio no tiene una membresía vigente");
        }
        RoutineAssignment assignment = assignments.findById(session.getAssignmentId());
        if (assignment.getMemberId() != session.getMemberId()) {
            throw new BusinessRuleException("La asignación no pertenece a este socio");
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("La asignación ya no está activa");
        }
        ensureDayExists(assignment.getRoutineId(), session.getDayName());
        // withId(0) fuerza a que el repositorio asigne un id nuevo.
        return sessions.save(session.withId(0));
    }

    // Regla: el día entrenado debe ser uno de los días de la rutina asignada.
    private void ensureDayExists(long routineId, String dayName) {
        boolean exists = routines.findById(routineId).getDays().stream()
                .anyMatch(day -> day.getName().equals(dayName));
        if (!exists) {
            throw new BusinessRuleException("La rutina no tiene un día llamado \"" + dayName + "\"");
        }
    }
}
package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.Member;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.persistence.AssignmentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for assigning routines to members. A member can have only
 * one ACTIVE assignment at a time.
 */
public class RoutineAssignmentService {

    private final AssignmentRepository assignments;
    private final MemberService members;
    private final RoutineService routines;
    private final SubscriptionChecker subscriptions;

    public RoutineAssignmentService(AssignmentRepository assignments, MemberService members,
                                    RoutineService routines, SubscriptionChecker subscriptions) {
        this.assignments = assignments;
        this.members = members;
        this.routines = routines;
        this.subscriptions = subscriptions;
    }

    /**
     * Contract used by other modules: the routine the member is following now.
     *
     * @return the ACTIVE assignment of the member, or empty if there is none
     */
    public Optional<RoutineAssignment> findCurrentFor(long memberId) {
        return assignments.findByMemberId(memberId).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .findFirst();
    }

    /** Returns every assignment (any status) of the member, as history. */
    public List<RoutineAssignment> findHistoryFor(long memberId) {
        return assignments.findByMemberId(memberId);
    }

    /** Returns the assignment with the given id, or throws ResourceNotFoundException. */
    public RoutineAssignment findById(long id) {
        return assignments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("asignación", String.valueOf(id)));
    }

    /**
     * Assigns a routine to a member, starting today.
     * Rules: the member exists, is active and has a valid membership;
     * the routine exists; the member has no other ACTIVE assignment.
     */
    public RoutineAssignment assign(long memberId, long routineId) {
        Member member = members.findById(memberId);      // lanza si no existe
        routines.findById(routineId);                    // lanza si no existe
        if (!member.isActive()) {
            throw new BusinessRuleException("El socio está inactivo");
        }
        if (!subscriptions.hasActiveSubscription(memberId)) {
            throw new BusinessRuleException("El socio no tiene una membresía vigente");
        }
        if (findCurrentFor(memberId).isPresent()) {
            throw new BusinessRuleException(
                    "El socio ya tiene una rutina activa; cancélala o complétala primero");
        }
        // Id 0: el repositorio asigna el id real.
        return assignments.save(new RoutineAssignment(
                0, memberId, routineId, LocalDate.now(), AssignmentStatus.ACTIVE));
    }

    /** Marks an ACTIVE assignment as COMPLETED. */
    public RoutineAssignment complete(long assignmentId) {
        return close(assignmentId, AssignmentStatus.COMPLETED);
    }

    /** Marks an ACTIVE assignment as CANCELLED. */
    public RoutineAssignment cancel(long assignmentId) {
        return close(assignmentId, AssignmentStatus.CANCELLED);
    }

    // Solo se puede cerrar una asignación que siga activa.
    private RoutineAssignment close(long assignmentId, AssignmentStatus newStatus) {
        RoutineAssignment assignment = findById(assignmentId);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se puede cerrar una asignación activa");
        }
        return assignments.save(assignment.withStatus(newStatus));
    }
}
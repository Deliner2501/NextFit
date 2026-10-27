package com.nextfit.model;

import java.time.LocalDate;

/**
 * A routine assigned to a member. It links a member with a routine and
 * remembers when it was assigned and in which state it is.
 * Immutable: a change of state produces a new copy ({@link #withStatus}).
 */
public class RoutineAssignment {

    private final long id;
    private final long memberId;
    private final long routineId;
    private final LocalDate assignedAt;
    private final AssignmentStatus status;

    /**
     * Creates an assignment. Member and routine are referenced by id only.
     *
     * @param id         unique id (0 means "not stored yet")
     * @param memberId   id of the member who follows the routine
     * @param routineId  id of the assigned routine
     * @param assignedAt date of the assignment
     * @param status     current state (active, completed or cancelled)
     */
    public RoutineAssignment(long id, long memberId, long routineId,
                             LocalDate assignedAt, AssignmentStatus status) {
        this.id = id;
        this.memberId = memberId;
        this.routineId = routineId;
        this.assignedAt = assignedAt;
        this.status = status;
    }

    /** Returns a copy with the given id (used by the repository on first save). */
    public RoutineAssignment withId(long newId) {
        return new RoutineAssignment(newId, memberId, routineId, assignedAt, status);
    }

    /** Returns a copy with the given status. */
    public RoutineAssignment withStatus(AssignmentStatus newStatus) {
        return new RoutineAssignment(id, memberId, routineId, assignedAt, newStatus);
    }

    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public long getRoutineId() {
        return routineId;
    }

    public LocalDate getAssignedAt() {
        return assignedAt;
    }

    public AssignmentStatus getStatus() {
        return status;
    }
}
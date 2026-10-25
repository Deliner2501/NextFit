package com.nextfit.persistence;

import com.nextfit.model.RoutineAssignment;

import java.util.List;
import java.util.Optional;

/** Storage contract for routine assignments. */
public interface AssignmentRepository {

    /** Returns every stored assignment. */
    List<RoutineAssignment> findAll();

    /** Returns the assignment with the given id, or empty if it does not exist. */
    Optional<RoutineAssignment> findById(long id);

    /** Returns all assignments (any status) of the given member. */
    List<RoutineAssignment> findByMemberId(long memberId);

    /** Stores an assignment: id 0 creates a new one, any other id replaces it. */
    RoutineAssignment save(RoutineAssignment assignment);
}
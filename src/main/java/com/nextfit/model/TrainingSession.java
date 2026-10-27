package com.nextfit.model;

import java.time.LocalDate;

/**
 * One training session done by a member: which day of the assigned routine
 * was trained, when, for how long, and an optional note.
 * An assignment says what the member SHOULD do; a session records what was DONE.
 * Immutable: the only change allowed is a copy with an id ({@link #withId}).
 */
public class TrainingSession {

    private final long id;
    private final long memberId;
    private final long assignmentId;
    private final String dayName;
    private final LocalDate date;
    private final int durationMinutes;
    private final String notes;

    /**
     * Creates a session.
     *
     * @param id              unique id (0 means "not stored yet")
     * @param memberId        id of the member who trained
     * @param assignmentId    id of the assignment being followed
     * @param dayName         name of the routine day trained (for example "Día 1 - Pecho")
     * @param date            date of the session
     * @param durationMinutes how long the session lasted
     * @param notes           free comment, may be empty
     */
    public TrainingSession(long id, long memberId, long assignmentId, String dayName,
                           LocalDate date, int durationMinutes, String notes) {
        this.id = id;
        this.memberId = memberId;
        this.assignmentId = assignmentId;
        this.dayName = dayName;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    /** Returns a copy with the given id (used by the repository on first save). */
    public TrainingSession withId(long newId) {
        return new TrainingSession(newId, memberId, assignmentId, dayName, date, durationMinutes, notes);
    }

    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public String getDayName() {
        return dayName;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getNotes() {
        return notes;
    }
}
package com.nextfit.model;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * One visit of a member to the gym. While the member is still inside, the
 * exit time does not exist yet and the record is said to be open.
 */
public class AttendanceRecord {

    private final long id;
    private final long memberId;
    private final LocalDateTime checkIn;
    private final LocalDateTime checkOut;

    /**
     * Creates an attendance record.
     *
     * @param id       the identifier, or 0 when it has not been assigned yet
     * @param memberId the identifier of the member who entered
     * @param checkIn  the moment the member entered
     * @param checkOut the moment the member left, or null while the member is still inside
     */
    public AttendanceRecord(long id, long memberId, LocalDateTime checkIn, LocalDateTime checkOut) {
        this.id = id;
        this.memberId = memberId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    /**
     * Tells whether the member is still inside the gym.
     *
     * @return true when no exit time has been recorded
     */
    public boolean isOpen() {
        return checkOut == null;
    }

    /**
     * Returns a copy of this record with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public AttendanceRecord withId(long newId) {
        return new AttendanceRecord(newId, memberId, checkIn, checkOut);
    }

    /**
     * Returns a copy of this record closed with the given exit time.
     *
     * @param exitTime the moment the member left
     * @return the closed copy
     */
    public AttendanceRecord withCheckOut(LocalDateTime exitTime) {
        return new AttendanceRecord(id, memberId, checkIn, exitTime);
    }

    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public Optional<LocalDateTime> getCheckOut() {
        return Optional.ofNullable(checkOut);
    }
}
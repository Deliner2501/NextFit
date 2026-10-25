package com.nextfit.persistence;

import com.nextfit.model.AttendanceRecord;

import java.util.List;
import java.util.Optional;

/**
 * Storage of gym attendance records. Services depend on this interface, so the
 * CSV implementation can be replaced by a database one without changing them.
 */
public interface AttendanceRepository {

    /**
     * Returns every attendance record.
     *
     * @return the records
     */
    List<AttendanceRecord> findAll();

    /**
     * Returns every visit of a member, from the oldest to the newest.
     *
     * @param memberId the identifier of the member
     * @return the records, empty when the member has none
     */
    List<AttendanceRecord> findByMemberId(long memberId);

    /**
     * Looks up the visit in which the member is still inside the gym.
     *
     * @param memberId the identifier of the member
     * @return the open record, or empty when the member is not inside
     */
    Optional<AttendanceRecord> findOpenByMemberId(long memberId);

    /**
     * Inserts the record when its id is 0 (assigning a new one) or replaces the
     * stored record with the same id.
     *
     * @param record the record to store
     * @return the stored record, with its identifier
     */
    AttendanceRecord save(AttendanceRecord record);
}
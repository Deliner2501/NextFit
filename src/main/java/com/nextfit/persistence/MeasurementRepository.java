package com.nextfit.persistence;

import com.nextfit.model.Measurement;

import java.util.List;

/**
 * Storage of body measurements. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface MeasurementRepository {

    /**
     * Returns every measurement of a member, from the oldest to the newest.
     *
     * @param memberId the identifier of the member
     * @return the measurements, empty when the member has none
     */
    List<Measurement> findByMemberId(long memberId);

    /**
     * Inserts the measurement when its id is 0 (assigning a new one) or replaces
     * the stored measurement with the same id.
     *
     * @param measurement the measurement to store
     * @return the stored measurement, with its identifier
     */
    Measurement save(Measurement measurement);
}
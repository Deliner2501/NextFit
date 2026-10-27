package com.nextfit.persistence;

import com.nextfit.model.TrainingSession;

import java.util.List;
import java.util.Optional;

/** Storage contract for training sessions. Sessions are only added, never edited or deleted. */
public interface TrainingSessionRepository {

    /** Returns every stored session. */
    List<TrainingSession> findAll();

    /** Returns the session with the given id, or empty if it does not exist. */
    Optional<TrainingSession> findById(long id);

    /** Returns all sessions of the given member. */
    List<TrainingSession> findByMemberId(long memberId);

    /** Stores a session: id 0 creates a new one, any other id replaces it. */
    TrainingSession save(TrainingSession session);
}
package com.nextfit.persistence;

import com.nextfit.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Storage of users. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface UserRepository {

    /**
     * Returns every stored user.
     *
     * @return the users
     */
    List<User> findAll();

    /**
     * Looks a user up by identifier.
     *
     * @param id the identifier
     * @return the user, or empty when it does not exist
     */
    Optional<User> findById(long id);

    /**
     * Looks a user up by sign-in name, ignoring case.
     *
     * @param username the sign-in name
     * @return the user, or empty when it does not exist
     */
    Optional<User> findByUsername(String username);

    /**
     * Inserts the user when its id is 0 (assigning a new one) or replaces the
     * stored user with the same id.
     *
     * @param user the user to store
     * @return the stored user, with its identifier
     */
    User save(User user);
}
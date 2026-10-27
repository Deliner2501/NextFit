package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Role;
import com.nextfit.model.User;

/**
 * Converts users to and from CSV rows. It only knows the column layout.
 */
public final class UserCsvMapper {

    /** Header of the users file. */
    public static final String[] COLUMNS = {"id", "username", "fullName", "passwordHash", "role", "active"};

    private UserCsvMapper() {
    }

    /**
     * Converts a user to a CSV row.
     *
     * @param user the user
     * @return the row
     */
    public static String[] toRow(User user) {
        return new String[]{
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getFullName(),
                user.getPasswordHash(),
                user.getRole().name(),
                String.valueOf(user.isActive())
        };
    }

    /**
     * Converts a CSV row to a user.
     *
     * @param row the row
     * @return the user
     * @throws PersistenceException when the row is malformed
     */
    public static User fromRow(String[] row) {
        try {
            return new User(
                    Long.parseLong(row[0]),
                    row[1],
                    row[2],
                    row[3],
                    Role.valueOf(row[4]),
                    Boolean.parseBoolean(row[5]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de usuario inválida en el archivo de usuarios", e);
        }
    }
}
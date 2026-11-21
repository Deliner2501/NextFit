package com.nextfit.persistence.jdbc;

import java.sql.Connection;

/**
 * Gives connections to a relational database. The JDBC repositories depend on
 * this contract and not on Oracle, so the tests can give them another database.
 */
@FunctionalInterface
public interface ConnectionSource {

    /**
     * Opens a new connection. The caller closes it.
     *
     * @return an open connection
     */
    Connection open();
}

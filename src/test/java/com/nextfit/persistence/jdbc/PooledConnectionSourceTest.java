package com.nextfit.persistence.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PooledConnectionSourceTest {

    /** Fake connections: they count how many were opened and remember what was done to them. */
    private static final class FakeDatabase {
        private int opened;
        private boolean valid = true;
        private final List<String> events = new ArrayList<>();

        Connection open() {
            int number = ++opened;
            boolean[] autoCommit = {true};
            return (Connection) Proxy.newProxyInstance(FakeDatabase.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "isValid":
                                return valid;
                            case "isClosed":
                                return false;
                            case "getAutoCommit":
                                return autoCommit[0];
                            case "setAutoCommit":
                                autoCommit[0] = (Boolean) args[0];
                                return null;
                            case "rollback":
                                events.add("rollback " + number);
                                return null;
                            case "close":
                                events.add("close " + number);
                                return null;
                            default:
                                return null;
                        }
                    });
        }
    }

    @Test
    void aConnectionThatWasClosedIsLentAgainInsteadOfOpeningAnotherOne() throws Exception {
        FakeDatabase database = new FakeDatabase();
        PooledConnectionSource pool = new PooledConnectionSource(database::open);

        pool.open().close();
        pool.open().close();

        assertEquals(1, database.opened);
    }

    @Test
    void twoConnectionsInUseAtTheSameTimeAreTwoDifferentConnections() throws Exception {
        FakeDatabase database = new FakeDatabase();
        PooledConnectionSource pool = new PooledConnectionSource(database::open);

        Connection first = pool.open();
        Connection second = pool.open();

        assertEquals(2, database.opened);
        first.close();
        second.close();
    }

    @Test
    void aTransactionLeftOpenIsRolledBackWhenTheConnectionComesBack() throws Exception {
        FakeDatabase database = new FakeDatabase();
        PooledConnectionSource pool = new PooledConnectionSource(database::open);

        Connection connection = pool.open();
        connection.setAutoCommit(false);
        connection.close();

        assertEquals(List.of("rollback 1"), database.events);
        assertTrue(pool.open().getAutoCommit());
    }

    @Test
    void closingTwiceDoesNotGiveTheSameConnectionToTwoPeople() throws Exception {
        FakeDatabase database = new FakeDatabase();
        PooledConnectionSource pool = new PooledConnectionSource(database::open);

        Connection connection = pool.open();
        connection.close();
        connection.close();
        pool.open();
        pool.open();

        assertEquals(2, database.opened);
        assertTrue(connection.isClosed());
    }

    @Test
    void connectionsThatAreNoLongerValidAreDiscardedAndReplaced() throws Exception {
        FakeDatabase database = new FakeDatabase();
        PooledConnectionSource pool = new PooledConnectionSource(database::open);
        pool.open().close();

        database.valid = false;
        Connection fresh = pool.open();

        assertEquals(2, database.opened);
        assertTrue(database.events.contains("close 1"));
        assertFalse(fresh.isClosed());
    }
}

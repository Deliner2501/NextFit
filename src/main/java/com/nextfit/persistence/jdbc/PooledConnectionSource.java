package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps a few connections open and lends them, because opening a connection to
 * a database in the cloud takes a long time and every repository call needs one.
 * The code that receives a connection closes it as always: closing gives it back
 * to the pool instead of really closing it.
 */
public final class PooledConnectionSource implements ConnectionSource {

    private static final int MAX_CONNECTIONS = 4;
    private static final long WAIT_SECONDS = 15;
    private static final int VALID_SECONDS = 2;

    private final ConnectionSource source;
    private final BlockingQueue<Connection> idle = new LinkedBlockingQueue<>();
    private final Semaphore permits = new Semaphore(MAX_CONNECTIONS, true);

    /**
     * Creates the pool.
     *
     * @param source where the real connections come from
     */
    public PooledConnectionSource(ConnectionSource source) {
        this.source = source;
    }

    /**
     * Lends a connection: a free one when there is one, a new one otherwise.
     * It waits when all of them are in use.
     *
     * @return a connection that goes back to the pool when it is closed
     * @throws PersistenceException when no connection is free in time or Oracle cannot be reached
     */
    @Override
    public Connection open() {
        try {
            if (!permits.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new PersistenceException("No hay conexiones libres con la base de datos", null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PersistenceException("Se interrumpió la espera de una conexión", e);
        }
        try {
            return lend(takeValidOrOpen());
        } catch (RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    private Connection takeValidOrOpen() {
        Connection candidate = idle.poll();
        while (candidate != null) {
            if (isUsable(candidate)) {
                return candidate;
            }
            closeQuietly(candidate);
            candidate = idle.poll();
        }
        return source.open();
    }

    private static boolean isUsable(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(VALID_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }

    private Connection lend(Connection real) {
        AtomicBoolean returned = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "close" -> {
                            if (returned.compareAndSet(false, true)) {
                                giveBack(real);
                            }
                            return null;
                        }
                        case "isClosed" -> {
                            return returned.get() || real.isClosed();
                        }
                        default -> {
                            try {
                                return method.invoke(real, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    }
                });
    }

    /** Leaves the connection as it was found (no open transaction, autocommit on) and keeps it for later. */
    private void giveBack(Connection real) {
        try {
            if (!real.getAutoCommit()) {
                real.rollback();
                real.setAutoCommit(true);
            }
            idle.offer(real);
        } catch (SQLException e) {
            closeQuietly(real);
        } finally {
            permits.release();
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            // nothing else can be done with a connection that does not close
        }
    }
}

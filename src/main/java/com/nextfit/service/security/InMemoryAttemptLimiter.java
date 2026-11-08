package com.nextfit.service.security;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Blocks a sign-in name for a while after too many wrong attempts in a row. The failures
 * live in memory, so they are cleared when the application closes. The clock is injected,
 * so tests can move the time.
 */
public class InMemoryAttemptLimiter implements AttemptLimiter {

    /** Wrong attempts in a row that block a user. */
    public static final int DEFAULT_MAX_ATTEMPTS = 5;
    /** Minutes a blocked user must wait. */
    public static final int DEFAULT_LOCK_MINUTES = 5;

    /** The failures of one name and, when it is blocked, until when. */
    private static final class State {
        private int failures;
        private Instant lockedUntil;
    }

    private final int maxAttempts;
    private final Duration lockTime;
    private final Clock clock;
    private final Map<String, State> states = new HashMap<>();

    /**
     * Creates the limiter with the default values and the system clock.
     */
    public InMemoryAttemptLimiter() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_MINUTES, Clock.systemDefaultZone());
    }

    /**
     * Creates the limiter.
     *
     * @param maxAttempts wrong attempts in a row that block a user, greater than zero
     * @param lockMinutes minutes of the block, greater than zero
     * @param clock       the source of the current time
     * @throws InvalidDataException when a number is not greater than zero
     */
    public InMemoryAttemptLimiter(int maxAttempts, int lockMinutes, Clock clock) {
        if (maxAttempts <= 0) {
            throw new InvalidDataException("intentos permitidos", "deben ser más de cero");
        }
        if (lockMinutes <= 0) {
            throw new InvalidDataException("minutos de bloqueo", "deben ser más de cero");
        }
        this.maxAttempts = maxAttempts;
        this.lockTime = Duration.ofMinutes(lockMinutes);
        this.clock = clock;
    }

    @Override
    public synchronized void beforeAttempt(String username) {
        State state = states.get(key(username));
        if (state == null || state.lockedUntil == null) {
            return;
        }
        Instant now = clock.instant();
        if (now.isBefore(state.lockedUntil)) {
            long seconds = Duration.between(now, state.lockedUntil).toSeconds();
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new BusinessRuleException("Demasiados intentos fallidos. Intenta de nuevo en "
                    + minutes + (minutes == 1 ? " minuto" : " minutos"));
        }
        states.remove(key(username));   // el bloqueo ya terminó: se empieza de cero
    }

    @Override
    public synchronized void failed(String username) {
        State state = states.computeIfAbsent(key(username), k -> new State());
        state.failures++;
        if (state.failures >= maxAttempts) {
            state.lockedUntil = clock.instant().plus(lockTime);
            state.failures = 0;
        }
    }

    @Override
    public synchronized void succeeded(String username) {
        states.remove(key(username));
    }

    // Se cuenta por nombre escrito, sin importar mayúsculas, para que no se evite el límite cambiándolas.
    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
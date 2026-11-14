package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.InvalidDataException;

import java.time.Clock;
import java.time.LocalDate;

/**
 * A limit of tokens per day for the whole application. The AI service is paid by use, so when the
 * limit is reached the assistant stops answering until tomorrow instead of spending more. The count
 * starts again every day and when NextFit is closed and opened again; the clock is injected so tests
 * can change the day.
 */
public class DailyTokenBudget {

    /** Limit of tokens per day, around one dollar with the usual model. */
    public static final int DEFAULT_MAX_TOKENS = 300_000;

    private final int maxTokens;
    private final Clock clock;
    private LocalDate day;
    private long used;

    /**
     * Creates the budget.
     *
     * @param maxTokens the tokens that may be used each day
     * @param clock     the source of today's date
     * @throws InvalidDataException when the limit is not greater than zero
     */
    public DailyTokenBudget(int maxTokens, Clock clock) {
        if (maxTokens <= 0) {
            throw new InvalidDataException("límite de tokens", "debe ser mayor que cero");
        }
        this.maxTokens = maxTokens;
        this.clock = clock;
    }

    /**
     * Checks that there is something left to spend today.
     *
     * @throws AiException when today's limit was reached
     */
    public synchronized void ensureAvailable() {
        startNewDayIfNeeded();
        if (used >= maxTokens) {
            throw new AiException("Se alcanzó el límite de uso diario del asistente. Intenta de nuevo mañana");
        }
    }

    /**
     * Counts the tokens of a call that was made.
     *
     * @param inputTokens  tokens that were sent
     * @param outputTokens tokens that were written
     */
    public synchronized void record(int inputTokens, int outputTokens) {
        startNewDayIfNeeded();
        used += (long) inputTokens + outputTokens;
    }

    /**
     * Returns the tokens used today.
     *
     * @return the count
     */
    public synchronized long usedToday() {
        startNewDayIfNeeded();
        return used;
    }

    private void startNewDayIfNeeded() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
    }
}
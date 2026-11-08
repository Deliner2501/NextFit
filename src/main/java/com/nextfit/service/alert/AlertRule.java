package com.nextfit.service.alert;

import java.time.LocalDate;
import java.util.List;

/**
 * One way of finding alerts. {@link AlertService} asks every rule it has, so a
 * new kind of alert is a new class that implements this interface and the
 * service does not change.
 */
@FunctionalInterface
public interface AlertRule {

    /**
     * Finds the alerts that apply on a day.
     *
     * @param today the day to evaluate
     * @return the alerts found, possibly empty
     */
    List<Alert> evaluate(LocalDate today);
}

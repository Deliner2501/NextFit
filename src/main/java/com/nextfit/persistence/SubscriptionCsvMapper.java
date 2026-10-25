package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Subscription;

import java.time.LocalDate;

/**
 * Converts subscriptions to and from CSV rows. It only knows the column layout.
 */
public final class SubscriptionCsvMapper {

    /** Header of the subscriptions file. */
    public static final String[] COLUMNS = {"id", "memberId", "planId", "startDate", "endDate", "cancelled"};

    private SubscriptionCsvMapper() {
    }

    /**
     * Converts a subscription to a CSV row.
     *
     * @param subscription the subscription
     * @return the row
     */
    public static String[] toRow(Subscription subscription) {
        return new String[]{
                String.valueOf(subscription.getId()),
                String.valueOf(subscription.getMemberId()),
                String.valueOf(subscription.getPlanId()),
                subscription.getStartDate().toString(),
                subscription.getEndDate().toString(),
                String.valueOf(subscription.isCancelled())
        };
    }

    /**
     * Converts a CSV row to a subscription.
     *
     * @param row the row
     * @return the subscription
     * @throws PersistenceException when the row is malformed
     */
    public static Subscription fromRow(String[] row) {
        try {
            return new Subscription(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    Long.parseLong(row[2]),
                    LocalDate.parse(row[3]),
                    LocalDate.parse(row[4]),
                    Boolean.parseBoolean(row[5]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de suscripción inválida en el archivo de suscripciones", e);
        }
    }
}

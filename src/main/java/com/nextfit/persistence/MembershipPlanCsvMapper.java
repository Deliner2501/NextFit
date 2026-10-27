package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.MembershipPlan;

/**
 * Converts membership plans to and from CSV rows. It only knows the column layout.
 */
public final class MembershipPlanCsvMapper {

    /** Header of the plans file. */
    public static final String[] COLUMNS = {"id", "name", "description", "durationDays", "price", "active"};

    private MembershipPlanCsvMapper() {
    }

    /**
     * Converts a plan to a CSV row.
     *
     * @param plan the plan
     * @return the row
     */
    public static String[] toRow(MembershipPlan plan) {
        return new String[]{
                String.valueOf(plan.getId()),
                plan.getName(),
                plan.getDescription(),
                String.valueOf(plan.getDurationDays()),
                String.valueOf(plan.getPrice()),
                String.valueOf(plan.isActive())
        };
    }

    /**
     * Converts a CSV row to a plan.
     *
     * @param row the row
     * @return the plan
     * @throws PersistenceException when the row is malformed
     */
    public static MembershipPlan fromRow(String[] row) {
        try {
            return new MembershipPlan(
                    Long.parseLong(row[0]),
                    row[1],
                    row[2],
                    Integer.parseInt(row[3]),
                    Long.parseLong(row[4]),
                    Boolean.parseBoolean(row[5]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de plan inválida en el archivo de planes", e);
        }
    }
}

package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Member;

import java.time.LocalDate;

/**
 * Converts members to and from CSV rows. It only knows the column layout.
 * Dates are written in ISO format (for example 2026-10-24).
 */
public final class MemberCsvMapper {

    /** Header of the members file. */
    public static final String[] COLUMNS = {"id", "fullName", "documentNumber", "phone", "email",
            "birthDate", "joinDate", "photoPath", "emergencyContact", "active"};

    private MemberCsvMapper() {
    }

    /**
     * Converts a member to a CSV row.
     *
     * @param member the member
     * @return the row
     */
    public static String[] toRow(Member member) {
        return new String[]{
                String.valueOf(member.getId()),
                member.getFullName(),
                member.getDocumentNumber(),
                member.getPhone(),
                member.getEmail(),
                member.getBirthDate().toString(),
                member.getJoinDate().toString(),
                member.getPhotoPath(),
                member.getEmergencyContact(),
                String.valueOf(member.isActive())
        };
    }

    /**
     * Converts a CSV row to a member.
     *
     * @param row the row
     * @return the member
     * @throws PersistenceException when the row is malformed
     */
    public static Member fromRow(String[] row) {
        try {
            return new Member(
                    Long.parseLong(row[0]),
                    row[1],
                    row[2],
                    row[3],
                    row[4],
                    LocalDate.parse(row[5]),
                    LocalDate.parse(row[6]),
                    row[7],
                    row[8],
                    Boolean.parseBoolean(row[9]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de socio inválida en el archivo de socios", e);
        }
    }
}
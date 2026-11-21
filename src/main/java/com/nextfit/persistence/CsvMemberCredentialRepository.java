package com.nextfit.persistence;

import com.nextfit.model.MemberCredential;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Keeps the member passwords in a CSV file: one row per member with the hash of the password. */
public class CsvMemberCredentialRepository implements MemberCredentialRepository {

    static final String[] COLUMNS = {"member_id", "password_hash"};

    private final CsvStore store;

    /** @param file the CSV file, created when it does not exist */
    public CsvMemberCredentialRepository(Path file) {
        this.store = new CsvStore(file, COLUMNS);
    }

    @Override
    public synchronized Optional<MemberCredential> find(long memberId) {
        for (String[] row : store.readAll()) {
            if (Long.parseLong(row[0].trim()) == memberId) {
                return Optional.of(new MemberCredential(memberId, row[1]));
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void save(MemberCredential credential) {
        List<String[]> rows = rowsWithout(credential.memberId());
        rows.add(toRow(credential));
        store.writeAll(rows);
    }

    @Override
    public synchronized void delete(long memberId) {
        store.writeAll(rowsWithout(memberId));
    }

    static String[] toRow(MemberCredential credential) {
        return new String[]{String.valueOf(credential.memberId()), credential.passwordHash()};
    }

    private List<String[]> rowsWithout(long memberId) {
        List<String[]> rows = new ArrayList<>();
        for (String[] row : store.readAll()) {
            if (Long.parseLong(row[0].trim()) != memberId) {
                rows.add(row);
            }
        }
        return rows;
    }
}

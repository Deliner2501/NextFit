package com.nextfit.persistence;

import com.nextfit.model.Member;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores members in a CSV file through {@link CsvStore}.
 */
public class CsvMemberRepository implements MemberRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvMemberRepository(Path file) {
        this.store = new CsvStore(file, MemberCsvMapper.COLUMNS);
    }

    @Override
    public List<Member> findAll() {
        List<Member> members = new ArrayList<>();
        for (String[] row : store.readAll()) {
            members.add(MemberCsvMapper.fromRow(row));
        }
        return members;
    }

    @Override
    public Optional<Member> findById(long id) {
        return findAll().stream().filter(m -> m.getId() == id).findFirst();
    }

    @Override
    public Optional<Member> findByDocumentNumber(String documentNumber) {
        return findAll().stream()
                .filter(m -> m.getDocumentNumber().equals(documentNumber))
                .findFirst();
    }

    @Override
    public Member save(Member member) {
        List<Member> members = findAll();
        Member toStore = member;
        if (member.getId() == 0) {
            long nextId = members.stream().mapToLong(Member::getId).max().orElse(0) + 1;
            toStore = member.withId(nextId);
            members.add(toStore);
        } else {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getId() == member.getId()) {
                    members.set(i, member);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Member m : members) {
            rows.add(MemberCsvMapper.toRow(m));
        }
        store.writeAll(rows);
        return toStore;
    }
}
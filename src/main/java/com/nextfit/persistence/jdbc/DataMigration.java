package com.nextfit.persistence.jdbc;

import com.nextfit.model.AuditEntry;
import com.nextfit.model.FaceTemplate;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.persistence.Storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Copies every kind of data from one storage to another (the CSV files to Oracle).
 * Rows keep their identifiers, and a row that already exists in the target is
 * updated instead of repeated, so the copy can be run again without duplicates.
 * A row that fails does not stop the copy: it is reported and the next one goes on.
 */
public final class DataMigration {

    private static final int MAX_ERRORS_SHOWN = 3;

    private final Storage source;
    private final Storage target;

    /**
     * Creates the migration.
     *
     * @param source the storage to read from
     * @param target the storage to write to
     */
    public DataMigration(Storage source, Storage target) {
        this.source = source;
        this.target = target;
    }

    /** What happened with one kind of data. */
    public static final class EntityResult {
        private final String name;
        private final int read;
        private final int written;
        private final List<String> errors;

        EntityResult(String name, int read, int written, List<String> errors) {
            this.name = name;
            this.read = read;
            this.written = written;
            this.errors = List.copyOf(errors);
        }

        /** @return the name of the data, for example "members" */
        public String name() {
            return name;
        }

        /** @return how many rows were read from the source */
        public int read() {
            return read;
        }

        /** @return how many rows were written to the target */
        public int written() {
            return written;
        }

        /** @return the messages of the rows that failed */
        public List<String> errors() {
            return errors;
        }

        /** @return true when every row was copied */
        public boolean isComplete() {
            return errors.isEmpty();
        }

        /** @return the line to show on the console */
        public String describe() {
            StringBuilder line = new StringBuilder(isComplete() ? "OK     " : "ERROR  ");
            line.append(name).append(": ").append(read).append(" leidos, ").append(written).append(" copiados");
            int shown = Math.min(MAX_ERRORS_SHOWN, errors.size());
            for (int i = 0; i < shown; i++) {
                line.append(System.lineSeparator()).append("         - ").append(errors.get(i));
            }
            if (errors.size() > shown) {
                line.append(System.lineSeparator()).append("         ... y ")
                        .append(errors.size() - shown).append(" errores mas");
            }
            return line.toString();
        }
    }

    /**
     * Copies everything, in the order the foreign keys need (members before their
     * subscriptions, routines before their assignments, and so on).
     *
     * @return one result per kind of data, in the order they were copied
     */
    public List<EntityResult> run() {
        List<EntityResult> results = new ArrayList<>();
        List<Member> members = source.members().findAll();

        results.add(copy("users", source.users().findAll(), target.users()::save));
        results.add(copy("members", members, target.members()::save));
        results.add(copy("membership_plans", source.plans().findAll(), target.plans()::save));
        results.add(copy("subscriptions", source.subscriptions().findAll(), target.subscriptions()::save));
        results.add(copy("payments", source.payments().findAll(), target.payments()::save));
        results.add(copy("attendance", source.attendance().findAll(), target.attendance()::save));
        results.add(copy("health_profiles", healthProfilesOf(members), target.healthProfiles()::save));
        results.add(copy("measurements", measurementsOf(members), target.measurements()::save));
        results.add(copy("trainers", source.trainers().findAll(), target.trainers()::save));
        results.add(copy("exercises", source.exercises().findAll(), target.exercises()::save));
        results.add(copy("routines", source.routines().findAll(), target.routines()::save));
        results.add(copy("assignments", source.assignments().findAll(), target.assignments()::save));
        results.add(copy("training_sessions", source.trainingSessions().findAll(), target.trainingSessions()::save));
        results.add(copyFaces());
        results.add(copyAudit());
        return results;
    }

    /**
     * Counts the rows of every kind of data in a storage.
     *
     * @param storage the storage to count
     * @return the number of rows by name, in the same order as {@link #run()}
     */
    public static Map<String, Integer> count(Storage storage) {
        List<Member> members = storage.members().findAll();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("users", storage.users().findAll().size());
        counts.put("members", members.size());
        counts.put("membership_plans", storage.plans().findAll().size());
        counts.put("subscriptions", storage.subscriptions().findAll().size());
        counts.put("payments", storage.payments().findAll().size());
        counts.put("attendance", storage.attendance().findAll().size());
        counts.put("health_profiles", healthProfilesOf(storage, members).size());
        counts.put("measurements", measurementsOf(storage, members).size());
        counts.put("trainers", storage.trainers().findAll().size());
        counts.put("exercises", storage.exercises().findAll().size());
        counts.put("routines", storage.routines().findAll().size());
        counts.put("assignments", storage.assignments().findAll().size());
        counts.put("training_sessions", storage.trainingSessions().findAll().size());
        counts.put("face_templates", storage.faceTemplates().findAll().size());
        counts.put("audit_log", storage.audit().findAll().size());
        return counts;
    }

    private <T> EntityResult copy(String name, List<T> items, Consumer<T> save) {
        int written = 0;
        List<String> errors = new ArrayList<>();
        for (T item : items) {
            try {
                save.accept(item);
                written++;
            } catch (RuntimeException e) {
                errors.add(reasonOf(e));
            }
        }
        return new EntityResult(name, items.size(), written, errors);
    }

    /** The faces of a member are replaced as a whole, so running again does not repeat them. */
    private EntityResult copyFaces() {
        List<FaceTemplate> templates = source.faceTemplates().findAll();
        Map<Long, List<float[]>> byMember = new LinkedHashMap<>();
        for (FaceTemplate template : templates) {
            byMember.computeIfAbsent(template.memberId(), key -> new ArrayList<>()).add(template.vector());
        }
        int written = 0;
        List<String> errors = new ArrayList<>();
        for (Map.Entry<Long, List<float[]>> entry : byMember.entrySet()) {
            try {
                target.faceTemplates().replace(entry.getKey(), entry.getValue());
                written += entry.getValue().size();
            } catch (RuntimeException e) {
                errors.add("socio " + entry.getKey() + ": " + reasonOf(e));
            }
        }
        return new EntityResult("face_templates", templates.size(), written, errors);
    }

    /** Audit entries are only added, so the ones the target already has are skipped. */
    private EntityResult copyAudit() {
        List<AuditEntry> entries = source.audit().findAll();
        Set<Long> existing = new HashSet<>();
        for (AuditEntry entry : target.audit().findAll()) {
            existing.add(entry.id());
        }
        int alreadyThere = 0;
        int written = 0;
        List<String> errors = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (existing.contains(entry.id())) {
                alreadyThere++;
                continue;
            }
            try {
                target.audit().append(entry);
                written++;
            } catch (RuntimeException e) {
                errors.add(reasonOf(e));
            }
        }
        return new EntityResult("audit_log", entries.size(), written + alreadyThere, errors);
    }

    private List<HealthProfile> healthProfilesOf(List<Member> members) {
        return healthProfilesOf(source, members);
    }

    private static List<HealthProfile> healthProfilesOf(Storage storage, List<Member> members) {
        List<HealthProfile> profiles = new ArrayList<>();
        for (Member member : members) {
            Optional<HealthProfile> profile = storage.healthProfiles().findByMemberId(member.getId());
            profile.ifPresent(profiles::add);
        }
        return profiles;
    }

    private List<Measurement> measurementsOf(List<Member> members) {
        return measurementsOf(source, members);
    }

    private static List<Measurement> measurementsOf(Storage storage, List<Member> members) {
        List<Measurement> measurements = new ArrayList<>();
        for (Member member : members) {
            measurements.addAll(storage.measurements().findByMemberId(member.getId()));
        }
        return measurements;
    }

    private static String reasonOf(Throwable error) {
        StringBuilder reason = new StringBuilder(String.valueOf(error.getMessage()));
        Throwable cause = error.getCause();
        while (cause != null) {
            reason.append(" -> ").append(cause.getMessage() == null ? cause.getClass().getSimpleName()
                    : cause.getMessage().trim());
            cause = cause.getCause();
        }
        return reason.toString();
    }
}

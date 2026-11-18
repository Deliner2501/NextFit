package com.nextfit.persistence;

import com.nextfit.model.Member;
import com.nextfit.model.Routine;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Writes a complete set of CSV files in the formats used by the offline repositories. */
public final class CsvSnapshot {

    private CsvSnapshot() {
    }

    /** Exports into an empty directory owned by the caller. */
    public static void write(Storage source, Path directory) {
        List<Member> members = source.members().findAll();
        write(directory, "users.csv", UserCsvMapper.COLUMNS,
                source.users().findAll(), UserCsvMapper::toRow);
        write(directory, "members.csv", MemberCsvMapper.COLUMNS,
                members, MemberCsvMapper::toRow);
        write(directory, "health_profiles.csv", HealthProfileCsvMapper.COLUMNS,
                members.stream().flatMap(member -> source.healthProfiles().findByMemberId(member.getId()).stream()).toList(),
                HealthProfileCsvMapper::toRow);
        write(directory, "measurements.csv", MeasurementCsvMapper.COLUMNS,
                members.stream().flatMap(member -> source.measurements().findByMemberId(member.getId()).stream()).toList(),
                MeasurementCsvMapper::toRow);
        write(directory, "plans.csv", MembershipPlanCsvMapper.COLUMNS,
                source.plans().findAll(), MembershipPlanCsvMapper::toRow);
        write(directory, "subscriptions.csv", SubscriptionCsvMapper.COLUMNS,
                source.subscriptions().findAll(), SubscriptionCsvMapper::toRow);
        write(directory, "payments.csv", PaymentCsvMapper.COLUMNS,
                source.payments().findAll(), PaymentCsvMapper::toRow);
        write(directory, "attendance.csv", AttendanceCsvMapper.COLUMNS,
                source.attendance().findAll(), AttendanceCsvMapper::toRow);
        write(directory, "audit.csv", AuditCsvMapper.COLUMNS,
                source.audit().findAll(), AuditCsvMapper::toRow);
        write(directory, "trainers.csv", TrainerCsvMapper.COLUMNS,
                source.trainers().findAll(), TrainerCsvMapper::toRow);
        write(directory, "exercises.csv", ExerciseCsvMapper.COLUMNS,
                source.exercises().findAll(), ExerciseCsvMapper::toRow);
        write(directory, "assignments.csv", AssignmentCsvMapper.COLUMNS,
                source.assignments().findAll(), AssignmentCsvMapper::toRow);
        write(directory, "training_sessions.csv", TrainingSessionCsvMapper.COLUMNS,
                source.trainingSessions().findAll(), TrainingSessionCsvMapper::toRow);
        List<Routine> routines = source.routines().findAll();
        write(directory, "routines.csv", RoutineCsvMapper.ROUTINE_COLUMNS, routines, RoutineCsvMapper::toRoutineRow);
        new CsvStore(directory.resolve("routine_lines.csv"), RoutineCsvMapper.LINE_COLUMNS)
                .writeAll(routines.stream().flatMap(routine -> RoutineCsvMapper.toLineRows(routine).stream()).toList());
        write(directory, "face_templates.csv", CsvFaceTemplateRepository.COLUMNS, source.faceTemplates().findAll(),
                template -> CsvFaceTemplateRepository.toRow(template.memberId(), template.vector()));
        write(directory, "member_credentials.csv", CsvMemberCredentialRepository.COLUMNS,
                members.stream().flatMap(member -> source.memberCredentials().find(member.getId()).stream()).toList(),
                CsvMemberCredentialRepository::toRow);
    }

    private static <T> void write(Path directory, String name, String[] columns, List<T> items,
                                  Function<T, String[]> toRow) {
        new CsvStore(directory.resolve(name), columns).writeAll(items.stream().map(toRow).toList());
    }
}

package com.nextfit;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.Member;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.service.MemberService;
import com.nextfit.validation.MemberValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Manual demo of the training module in the console. Not part of the application. */
public class TrainingDemo {

    public static void main(String[] args) throws Exception {
        // Carpeta aparte para no mezclar con los datos reales de la aplicación.
        Path dir = Path.of("data-demo");
        Files.createDirectories(dir);

        MemberService members = new MemberService(
                new CsvMemberRepository(dir.resolve("members.csv")), new MemberValidator());
        // Membresía falsa: siempre vigente.
        TrainingModule training = new TrainingModule(dir, members, memberId -> true);

        Trainer trainer = training.trainers().register(new Trainer(0, "Carlos Pérez", "2" + System.nanoTime() % 100000000,
                "3001234567", "", LocalDate.of(1990, 5, 10), "Hipertrofia", "", true));
        System.out.println("Entrenador: " + trainer.getId() + " " + trainer.getFullName());

        Exercise exercise = training.exercises().create(new Exercise(0, "Press de banca " + System.nanoTime() % 1000,
                "Empuje horizontal", MuscleGroup.CHEST, Difficulty.BEGINNER, ""));
        System.out.println("Ejercicio: " + exercise.getId() + " " + exercise.getName());

        RoutineDay day = new RoutineDay("Día 1", List.of(new RoutineExercise(exercise.getId(), 3, 10, 60)));
        Routine routine = training.routines().create(new Routine(0, "Rutina básica", "Fuerza",
                trainer.getId(), LocalDate.now(), List.of(day)));
        System.out.println("Rutina: " + routine.getId() + " " + routine.getName()
                + " (" + routine.getTotalSets() + " series)");

        Member member = members.register(new Member(0, "Ana Gómez", "1" + System.nanoTime() % 100000000,
                "3105551234", "", LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", true));
        System.out.println("Socio: " + member.getId() + " " + member.getFullName());

        RoutineAssignment assignment = training.assignments().assign(member.getId(), routine.getId());
        System.out.println("Asignación: " + assignment.getId() + " estado " + assignment.getStatus().getLabel());

        TrainingSession session = training.sessions().log(new TrainingSession(0, member.getId(),
                assignment.getId(), "Día 1", LocalDate.now(), 45, "Buen entrenamiento"));
        System.out.println("Sesión: " + session.getId() + " " + session.getDurationMinutes() + " min");

        // Esto debe fallar: el socio ya tiene una rutina activa.
        try {
            training.assignments().assign(member.getId(), routine.getId());
        } catch (RuntimeException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
        System.out.println("Listo. Mira los archivos en la carpeta data-demo.");
    }
}
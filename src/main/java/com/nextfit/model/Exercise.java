package com.nextfit.model;

/**
 * An exercise of the gym catalog, for example "Bench press". It only describes
 * the exercise; sets, repetitions and rest belong to {@code RoutineExercise}.
 * The class is immutable.
 */
public class Exercise {

    // EXPLICACIÓN: esta clase es el ejercicio "en general" del catálogo (por
    // ejemplo "Press de banca"). NO guarda series, repeticiones ni descanso,
    // porque eso cambia de una rutina a otra. Eso va en RoutineExercise, y así
    // el mismo ejercicio se reutiliza en muchas rutinas.
    //
    // Todos los atributos son "final": una vez creado el ejercicio no se puede
    // modificar. Para "cambiar" uno se crea una copia con los datos nuevos. Eso
    // evita que otra parte del programa lo altere sin querer.
    private final long id;
    private final String name;
    private final String description;
    private final MuscleGroup muscleGroup;
    private final Difficulty difficulty;
    private final String imagePath;

    /**
     * Creates an exercise.
     *
     * @param id          the identifier, or 0 when it has not been assigned yet
     * @param name        the exercise name
     * @param description a short explanation of how it is done
     * @param muscleGroup the main muscle group
     * @param difficulty  the difficulty level
     * @param imagePath   the path of the exercise image (may be empty)
     */
    public Exercise(long id, String name, String description, MuscleGroup muscleGroup,
                    Difficulty difficulty, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.imagePath = imagePath;
    }

    /**
     * Returns a copy of this exercise with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Exercise withId(long newId) {
        // EXPLICACIÓN: un ejercicio nuevo llega con id 0. Al guardarlo, el
        // repositorio le calcula el número real y usa este método para obtener
        // una copia con ese id. Es la misma técnica que usa User de Deiner.
        return new Exercise(newId, name, description, muscleGroup, difficulty, imagePath);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getImagePath() {
        return imagePath;
    }
}
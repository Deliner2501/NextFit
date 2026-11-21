package com.nextfit.service.suggest;

import com.nextfit.model.Routine;
import java.util.List;

/**
 * The result of a suggestion: a routine draft that is not saved yet, and the notes that
 * explain the adjustments made for the member.
 *
 * @param routine the suggested routine, with id 0 and no trainer
 * @param notes   warnings to show to the trainer
 */
public record SuggestedRoutine(Routine routine, List<String> notes) {

    public SuggestedRoutine {
        // EXPLICACIÓN: se copia la lista para que quede inmutable, como en Routine.
        notes = List.copyOf(notes);
    }
}
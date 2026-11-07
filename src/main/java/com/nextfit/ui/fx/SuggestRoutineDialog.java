package com.nextfit.ui.fx;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Member;
import com.nextfit.model.TrainingGoal;
import com.nextfit.service.MemberService;
import com.nextfit.service.suggest.SuggestedRoutine;
import com.nextfit.service.suggest.SuggestionRequest;
import com.nextfit.service.suggest.SuggestionService;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Small dialog that asks what the trainer wants to suggest: the member, the goal,
 * the level and the days per week. It does not build the routine itself; it asks
 * {@link SuggestionService} and returns the draft.
 */
final class SuggestRoutineDialog {

    private SuggestRoutineDialog() {
    }

    /**
     * Shows the dialog and builds the suggestion.
     *
     * @param owner       a node of the window the dialog belongs to
     * @param members     the service that lists the members
     * @param suggestions the service that builds the routine draft
     * @return the suggested routine, or empty if the user cancelled
     */
    static Optional<SuggestedRoutine> ask(Node owner, MemberService members, SuggestionService suggestions) {
        List<Member> activeMembers = members.findAll().stream().filter(Member::isActive).toList();
        ComboBox<Member> memberBox = combo(activeMembers, Member::getFullName);
        ComboBox<TrainingGoal> goalBox = combo(List.of(TrainingGoal.values()), TrainingGoal::getLabel);
        ComboBox<Difficulty> levelBox = combo(List.of(Difficulty.values()), Difficulty::getLabel);
        ComboBox<Integer> daysBox = combo(List.of(2, 3, 4, 5), d -> d + " días por semana");
        goalBox.getSelectionModel().selectFirst();
        levelBox.getSelectionModel().selectFirst();
        daysBox.getSelectionModel().select(Integer.valueOf(3));

        VBox fields = new VBox(14,
                FxDialogs.labeled("Socio", memberBox),
                FxDialogs.labeled("Objetivo", goalBox),
                FxDialogs.labeled("Nivel", levelBox),
                FxDialogs.labeled("Días", daysBox));
        fields.setPrefWidth(420);

        AtomicReference<SuggestedRoutine> result = new AtomicReference<>();
        FxDialogs.form(owner, "Sugerir rutina", "Sugerir", fields, () -> {
            if (memberBox.getValue() == null) {
                throw new BusinessRuleException("Elige el socio para el que es la rutina.");
            }
            SuggestionRequest request = suggestions.requestFor(memberBox.getValue().getId(),
                    goalBox.getValue(), levelBox.getValue(), daysBox.getValue());
            result.set(suggestions.suggest(request));
        });
        // Si el usuario cancela, la acción nunca corre y el resultado queda vacío.
        return Optional.ofNullable(result.get());
    }

    // Crea un ComboBox que muestra cada opción con el texto que da "label".
    private static <T> ComboBox<T> combo(List<T> items, Function<T, String> label) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().setAll(items);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : label.apply(item);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
        box.getStyleClass().add("nf-input");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
}
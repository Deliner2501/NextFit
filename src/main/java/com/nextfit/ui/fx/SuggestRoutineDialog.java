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
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Small dialog that asks what the trainer wants to suggest: the member, the goal,
 * the level and the days per week. It does not build the routine itself; either it asks
 * {@link SuggestionService} and returns the draft, or it only returns the request so
 * the caller can ask the AI in the background.
 */
final class SuggestRoutineDialog {

    private static final String AI_PRIVACY =
            "La IA solo recibe el objetivo, el nivel, los días y los ejercicios permitidos. "
                    + "No recibe el nombre del socio ni sus datos de salud.";

    private SuggestRoutineDialog() {
    }

    /**
     * The fields of the dialog and the way to read them.
     *
     * @param member the member to suggest a routine for
     * @param goal   the goal of the routine
     * @param level  the experience of the member
     * @param days   the training days per week
     * @param node   the fields drawn one under the other
     */
    private record Form(ComboBox<Member> member, ComboBox<TrainingGoal> goal, ComboBox<Difficulty> level,
                        ComboBox<Integer> days, VBox node) {

        static Form create(MemberService members) {
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
            return new Form(memberBox, goalBox, levelBox, daysBox, fields);
        }

        SuggestionRequest request(SuggestionService suggestions) {
            if (member.getValue() == null) {
                throw new BusinessRuleException("Elige el socio para el que es la rutina.");
            }
            return suggestions.requestFor(member.getValue().getId(), goal.getValue(), level.getValue(),
                    days.getValue());
        }
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
        Form form = Form.create(members);
        AtomicReference<SuggestedRoutine> result = new AtomicReference<>();
        FxDialogs.form(owner, "Sugerir rutina", "Sugerir", form.node(),
                () -> result.set(suggestions.suggest(form.request(suggestions))));
        // Si el usuario cancela, la acción nunca corre y el resultado queda vacío.
        return Optional.ofNullable(result.get());
    }

    /**
     * Shows the dialog to ask the AI for a routine. It only collects the request: the call to the AI takes
     * seconds, so the caller runs it in the background.
     *
     * @param owner       a node of the window the dialog belongs to
     * @param members     the service that lists the members
     * @param suggestions the service that reads the health data of the member and builds the request
     * @return the request, or empty if the user cancelled
     */
    static Optional<SuggestionRequest> askForAi(Node owner, MemberService members, SuggestionService suggestions) {
        Form form = Form.create(members);
        Label privacy = new Label(AI_PRIVACY);
        privacy.getStyleClass().add("nf-muted");
        privacy.setWrapText(true);
        form.node().getChildren().add(privacy);
        AtomicReference<SuggestionRequest> result = new AtomicReference<>();
        FxDialogs.form(owner, "Sugerir rutina con IA", "Pedir a la IA", form.node(),
                () -> result.set(form.request(suggestions)));
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
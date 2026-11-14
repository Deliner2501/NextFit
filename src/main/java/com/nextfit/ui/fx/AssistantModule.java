package com.nextfit.ui.fx;

import com.nextfit.ai.AssistantChat;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Role;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Assistant module in JavaFX, for the administrator and the reception: a chat where they ask about
 * the gym ("who may leave?", "which memberships expire?"). The screen only writes and shows the
 * messages; the answers, the limits and the permissions come from {@link AssistantChat}. When the
 * assistant has no key it shows how to turn it on, and the rest of NextFit works as always.
 */
public final class AssistantModule implements FxModule {

    private static final double BUBBLE_MAX_WIDTH = 640;
    private static final List<String> EXAMPLES = List.of(
            "¿Quién está en riesgo de irse?",
            "¿Qué membresías vencen pronto?",
            "¿Quién cumple años hoy?");

    private final Optional<AssistantChat> chat;
    private final VBox messages = new VBox(10);
    private final ScrollPane scroll = new ScrollPane(messages);
    private final TextField input = new TextField();
    private final Button send = new Button("Enviar");
    private final VBox view = new VBox(16);
    private boolean busy;

    /**
     * @param chat the chat of the assistant, or empty when the assistant is off
     */
    public AssistantModule(Optional<AssistantChat> chat) {
        this.chat = chat;
        view.getChildren().add(buildHeader());
        view.getChildren().add(chat.isPresent() ? buildChat() : buildOff());
        FxMotion.enter(view.getChildren(), 0, 90);
    }

    @Override
    public String id() {
        return "assistant";
    }

    @Override
    public String title() {
        return "Asistente";
    }

    @Override
    public String icon() {
        return FxIcons.CHAT;
    }

    @Override
    public List<String> keywords() {
        return List.of("chat", "ia", "inteligencia artificial", "pregunta", "preguntale");
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        input.requestFocus();
    }

    /** The assistant answers about members and money, so it is for the administrator and the reception. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private Node buildHeader() {
        Label title = new Label("Pregúntale a tu gimnasio");
        title.getStyleClass().add("nf-card-title");
        Label subtitle = new Label("Pregunta en tus palabras. El asistente solo consulta, nunca cambia datos.");
        subtitle.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(10, titles);
        header.setAlignment(Pos.CENTER_LEFT);
        if (chat.isPresent()) {
            Button restart = new Button("Nueva conversación");
            restart.getStyleClass().add("nf-secondary");
            restart.setOnAction(e -> startOver());
            header.getChildren().add(restart);
        }
        return header;
    }

    private Node buildOff() {
        Label title = new Label("El asistente está apagado");
        title.getStyleClass().add("nf-card-title");
        Label how = new Label("Para encenderlo, agrega la línea ai.apiKey=TU_CLAVE al archivo nextfit.properties "
                + "y vuelve a abrir NextFit. Mientras tanto todo lo demás funciona igual.");
        how.getStyleClass().add("nf-muted");
        how.setWrapText(true);
        VBox card = new VBox(8, title, how);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private Node buildChat() {
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("nf-scroll");
        messages.setPadding(new Insets(4));
        messages.heightProperty().addListener((obs, was, now) -> scroll.setVvalue(1.0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        FlowPane examples = new FlowPane(8, 8);
        for (String example : EXAMPLES) {
            Button button = new Button(example);
            button.getStyleClass().add("nf-secondary");
            button.setOnAction(e -> ask(example));
            examples.getChildren().add(button);
        }

        input.setPromptText("Escribe tu pregunta y presiona Enter");
        input.getStyleClass().add("nf-input");
        input.setPrefHeight(44);
        input.setOnAction(e -> ask(input.getText()));
        HBox.setHgrow(input, Priority.ALWAYS);
        send.getStyleClass().add("nf-primary");
        send.setPrefHeight(44);
        send.setOnAction(e -> ask(input.getText()));
        HBox inputRow = new HBox(10, input, send);

        VBox card = new VBox(12, scroll, examples, inputRow);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        VBox.setVgrow(card, Priority.ALWAYS);
        addBubble("Hola, soy el asistente de NextFit. ¿En qué te ayudo?", "nf-chat-assistant", false);
        return card;
    }

    private void ask(String text) {
        String question = text == null ? "" : text.strip();
        if (question.isEmpty() || busy) {
            return;
        }
        input.clear();
        addBubble(question, "nf-chat-user", true);
        Node thinking = addBubble("Pensando…", "nf-chat-assistant", false);
        setBusy(true);
        // EXPLICACIÓN: la consulta a internet tarda segundos; se hace en otro hilo para no congelar la ventana
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return chat.orElseThrow().ask(question);
            }
        };
        task.setOnSucceeded(e -> {
            messages.getChildren().remove(thinking);
            addBubble(task.getValue(), "nf-chat-assistant", false);
            setBusy(false);
        });
        task.setOnFailed(e -> {
            messages.getChildren().remove(thinking);
            Throwable error = task.getException();
            addBubble(error instanceof NextFitException ? error.getMessage()
                    : "No pude responder en este momento. Intenta de nuevo", "nf-chat-error", false);
            setBusy(false);
        });
        Thread thread = new Thread(task, "assistant-question");
        thread.setDaemon(true);
        thread.start();
    }

    private void startOver() {
        chat.ifPresent(AssistantChat::startOver);
        messages.getChildren().clear();
        addBubble("Conversación nueva. ¿En qué te ayudo?", "nf-chat-assistant", false);
    }

    private void setBusy(boolean value) {
        busy = value;
        input.setDisable(value);
        send.setDisable(value);
        if (!value) {
            input.requestFocus();
        }
    }

    private Node addBubble(String text, String styleClass, boolean fromUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(BUBBLE_MAX_WIDTH);
        label.getStyleClass().addAll("nf-chat-bubble", styleClass);
        HBox row = new HBox(label);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messages.getChildren().add(row);
        return row;
    }
}
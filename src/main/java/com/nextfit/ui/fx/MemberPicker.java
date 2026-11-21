package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable column to find and select a member: a title, a search box and a
 * list. The screen that uses it is told which member was selected. It is shared
 * by the modules that work on one member at a time (sales, training).
 */
public final class MemberPicker extends VBox {

    private final MemberService members;
    private final Consumer<Member> onSelected;
    private final Function<Member, List<String>> tags;
    private final TextField search = new TextField();
    private final ListView<Member> list = new ListView<>();
    private boolean reloading;

    /**
     * @param title      the title above the search box
     * @param members    the member service used to search
     * @param onSelected action to run with the chosen member, or null when the selection is cleared
     */
    public MemberPicker(String title, MemberService members, Consumer<Member> onSelected) {
        this(title, members, onSelected, member -> List.of());
    }

    /**
     * @param title      the title above the search box
     * @param members    the member service used to search
     * @param onSelected action to run with the chosen member, or null when the selection is cleared
     * @param tags       short labels shown at the right of each member (for example how he enters)
     */
    public MemberPicker(String title, MemberService members, Consumer<Member> onSelected,
                        Function<Member, List<String>> tags) {
        super(12);
        this.members = members;
        this.onSelected = onSelected;
        this.tags = tags;

        Label heading = new Label(title);
        heading.getStyleClass().add("nf-section-title");
        search.setPromptText("Buscar por nombre o documento...");
        search.getStyleClass().add("nf-input");
        search.setPrefHeight(44);
        search.textProperty().addListener((obs, old, now) -> reload());

        list.getStyleClass().add("nf-list");
        list.setPlaceholder(new Label("No hay socios para mostrar"));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(member.getFullName());
                name.getStyleClass().add("nf-list-name");
                Label document = new Label(member.getDocumentNumber() + (member.isActive() ? "" : "  ·  dado de baja"));
                document.getStyleClass().add("nf-muted-small");
                setText(null);
                // The labels go next to the document, so the name keeps the whole first line.
                HBox second = new HBox(8, document);
                second.setAlignment(Pos.CENTER_LEFT);
                for (String tag : MemberPicker.this.tags.apply(member)) {
                    Label chip = new Label(tag);
                    chip.getStyleClass().add("nf-tag");
                    // Also inline, so the label is visible even if the style sheet was not reloaded.
                    chip.setStyle("-fx-background-color: -nf-accent-soft; -fx-background-radius: 10; "
                            + "-fx-text-fill: -nf-accent-text; -fx-font-size: 11px; -fx-font-weight: bold; "
                            + "-fx-padding: 2 8 2 8;");
                    chip.setMinWidth(Region.USE_PREF_SIZE);
                    second.getChildren().add(chip);
                }
                VBox text = new VBox(2, name, second);
                // A very long name is cut with "..." instead of making the list wider.
                name.setMinWidth(0);
                text.prefWidthProperty().bind(widthProperty().subtract(30));
                text.setMaxWidth(Region.USE_PREF_SIZE);
                setGraphic(text);
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (!reloading) {
                onSelected.accept(now);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        getChildren().addAll(heading, search, list);
        setPrefWidth(340);
        setMinWidth(300);
        setMaxWidth(340);
        reload();
    }

    /** Reloads the list with the current search text, keeping the selected member if it is still listed. */
    public void reload() {
        Member previous = selected();
        long keepId = previous == null ? -1 : previous.getId();
        reloading = true;
        list.getItems().setAll(members.search(search.getText()));
        reloading = false;
        boolean found = false;
        for (int i = 0; i < list.getItems().size(); i++) {
            if (list.getItems().get(i).getId() == keepId) {
                list.getSelectionModel().select(i);
                found = true;
            }
        }
        if (previous != null && !found) {
            onSelected.accept(null);
        }
    }

    /** Draws the labels of every member again, after one of them changed. */
    public void refreshTags() {
        list.refresh();
    }

    /**
     * Returns the selected member.
     *
     * @return the member, or null when none is selected
     */
    public Member selected() {
        return list.getSelectionModel().getSelectedItem();
    }
}
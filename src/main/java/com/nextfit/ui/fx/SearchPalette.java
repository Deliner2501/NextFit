package com.nextfit.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Overlay with a search box and a list of results, opened with Ctrl+K or the search button.
 * It does not know where the results come from: it asks a function and tells a callback
 * which result the user chose.
 */
final class SearchPalette extends StackPane {

    private static final double ROW_HEIGHT = 58;
    private static final double MAX_LIST_HEIGHT = 348;

    private final Function<String, List<SearchHit>> source;
    private final Consumer<SearchHit> onChoose;
    private final TextField field = new TextField();
    private final ListView<SearchHit> list = new ListView<>();
    private final Label empty = new Label();

    /**
     * @param source   returns the results for the typed text
     * @param onChoose called with the result the user picked, after the palette closes
     */
    SearchPalette(Function<String, List<SearchHit>> source, Consumer<SearchHit> onChoose) {
        this.source = source;
        this.onChoose = onChoose;
        setVisible(false);
        setStyle("-fx-background-color: rgba(10, 10, 20, 0.45);");
        setAlignment(Pos.TOP_CENTER);

        field.setPromptText("Buscar socios o secciones…");
        field.getStyleClass().add("nf-input");
        field.setPrefHeight(44);

        list.getStyleClass().add("nf-list");
        list.setFocusTraversable(false);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(SearchHit hit, boolean emptyCell) {
                super.updateItem(hit, emptyCell);
                if (emptyCell || hit == null) {
                    setGraphic(null);
                    return;
                }
                Label name = new Label(hit.title());
                name.getStyleClass().add("nf-list-name");
                Label detail = new Label(hit.detail());
                detail.getStyleClass().add("nf-muted-small");
                setGraphic(new VBox(2, name, detail));
            }
        });
        list.setOnMouseClicked(e -> choose(list.getSelectionModel().getSelectedItem()));

        empty.getStyleClass().add("nf-muted");
        empty.setPadding(new Insets(6, 4, 2, 4));

        VBox card = new VBox(12, field, list, empty);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(560);
        card.setMaxHeight(USE_PREF_SIZE);
        StackPane.setMargin(card, new Insets(90, 24, 0, 24));
        getChildren().add(card);

        field.textProperty().addListener((obs, old, now) -> refresh());
        field.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });
        setOnMousePressed(e -> {
            if (e.getTarget() == this) {
                close();
            }
        });
        refresh();
    }

    /** Shows the palette with an empty search box. */
    void open() {
        field.clear();
        refresh();
        setVisible(true);
        field.requestFocus();
    }

    void close() {
        setVisible(false);
    }

    private void onKey(KeyEvent e) {
        int size = list.getItems().size();
        int selected = list.getSelectionModel().getSelectedIndex();
        if (e.getCode() == KeyCode.DOWN && size > 0) {
            list.getSelectionModel().select(Math.min(size - 1, selected + 1));
            e.consume();
        } else if (e.getCode() == KeyCode.UP && size > 0) {
            list.getSelectionModel().select(Math.max(0, selected - 1));
            e.consume();
        } else if (e.getCode() == KeyCode.ENTER) {
            choose(selected >= 0 ? list.getSelectionModel().getSelectedItem() : (size > 0 ? list.getItems().get(0) : null));
            e.consume();
        }
    }

    private void refresh() {
        String text = field.getText();
        List<SearchHit> hits = text == null || text.isBlank() ? List.of() : source.apply(text);
        list.getItems().setAll(hits);
        boolean any = !hits.isEmpty();
        list.setVisible(any);
        list.setManaged(any);
        list.setPrefHeight(Math.min(MAX_LIST_HEIGHT, hits.size() * ROW_HEIGHT + 8));
        if (any) {
            list.getSelectionModel().selectFirst();
        }
        empty.setText(text == null || text.isBlank()
                ? "Escribe un nombre, documento, teléfono o el nombre de una sección."
                : any ? "" : "Sin resultados para «" + text.trim() + "».");
        empty.setVisible(!empty.getText().isEmpty());
        empty.setManaged(empty.isVisible());
    }

    private void choose(SearchHit hit) {
        if (hit == null) {
            return;
        }
        close();
        onChoose.accept(hit);
    }
}

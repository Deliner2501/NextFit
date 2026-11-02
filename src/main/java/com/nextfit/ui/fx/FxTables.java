package com.nextfit.ui.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/**
 * Helpers shared by the JavaFX tables: a text column built from a function, and
 * a state column whose text is colored by what it says. Keeping them here means
 * no screen repeats the same cell code.
 */
final class FxTables {

    private static final String[] STATE_CLASSES = {"nf-state-on", "nf-state-warn", "nf-state-bad", "nf-state-off"};

    private FxTables() {
    }

    /**
     * Creates a column that shows one text of each row.
     *
     * @param title the header
     * @param width the preferred width
     * @param value how to read the text from a row
     * @param <T>   the type of the rows
     * @return the column
     */
    static <T> TableColumn<T, String> column(String title, double width, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    /**
     * Colors the text of a column according to its content.
     *
     * @param column  the column to color
     * @param styleOf the style class for a text: one of {@code nf-state-on} (green),
     *                {@code nf-state-warn} (amber), {@code nf-state-bad} (red) or
     *                {@code nf-state-off} (gray)
     * @param <T>     the type of the rows
     */
    static <T> void colorize(TableColumn<T, String> column, Function<String, String> styleOf) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                for (String style : STATE_CLASSES) {
                    getStyleClass().remove(style);
                }
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(value);
                    getStyleClass().add(styleOf.apply(value));
                }
            }
        });
    }
}

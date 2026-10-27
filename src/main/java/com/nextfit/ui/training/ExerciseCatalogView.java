package com.nextfit.ui.training;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.service.ExerciseService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.ImageCard;
import com.nextfit.ui.components.MuscleArt;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen of the exercise catalog: a search box, two filters and a grid of cards.
 * It only shows data and forwards actions; the rules live in {@link ExerciseService}.
 */
public class ExerciseCatalogView extends JPanel {

    private static final int CARD_WIDTH = 210;
    private static final int CARD_HEIGHT = 230;
    private static final int GAP = 16;

    private final ExerciseService service;
    private final Path imagesFolder;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> muscleGroupFilter = new JComboBox<>(labels("Todos los grupos",
            MuscleGroup.values()));
    private final JComboBox<String> difficultyFilter = new JComboBox<>(labels("Todas las dificultades",
            Difficulty.values()));
    private final JLabel countLabel = new JLabel(" ");
    private final JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JScrollPane scroll;

    private List<Exercise> shown = new ArrayList<>();
    private int columns = 0;   // columnas actuales de la cuadrícula (se recalcula al cambiar el ancho)

    /**
     * @param service      exercise service
     * @param imagesFolder folder where exercise pictures are stored
     */
    public ExerciseCatalogView(ExerciseService service, Path imagesFolder) {
        super(new BorderLayout(0, 16));
        this.service = service;
        this.imagesFolder = imagesFolder;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(buildHeader(), BorderLayout.NORTH);

        gridHolder.setBackground(Theme.BACKGROUND);
        scroll = new JScrollPane(gridHolder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        // Al cambiar el ancho de la ventana, se recalcula cuántas tarjetas caben por fila.
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                rebuildGridIfColumnsChanged();
            }
        });
        refresh();
    }

    // "Todos" + las etiquetas de cada valor del enum; la posición 0 significa "sin filtro".
    private static String[] labels(String all, Enum<?>[] values) {
        String[] result = new String[values.length + 1];
        result[0] = all;
        for (int i = 0; i < values.length; i++) {
            result[i + 1] = values[i] instanceof MuscleGroup m ? m.getLabel() : ((Difficulty) values[i]).getLabel();
        }
        return result;
    }

    // Parte superior: título, contador, buscador, filtros y botón de crear.
    private JPanel buildHeader() {
        JLabel title = new JLabel("Catálogo de ejercicios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        countLabel.setForeground(Theme.TEXT_MUTED);

        searchField.putClientProperty("JTextField.placeholderText", "Buscar por nombre...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        muscleGroupFilter.addActionListener(e -> refresh());
        difficultyFilter.addActionListener(e -> refresh());

        JButton newButton = new JButton("+ Nuevo ejercicio");
        newButton.setBackground(Theme.ACCENT);
        newButton.setForeground(Theme.BACKGROUND);
        newButton.setFont(newButton.getFont().deriveFont(Font.BOLD));
        newButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newButton.addActionListener(e -> openForm(null));

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(countLabel);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);
        searchField.setColumns(16);
        filters.add(searchField);
        filters.add(muscleGroupFilter);
        filters.add(difficultyFilter);
        filters.add(newButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);
        return header;
    }

    /** Reloads the exercises from the service applying the current search and filters. */
    public void refresh() {
        int groupIndex = muscleGroupFilter.getSelectedIndex();
        int difficultyIndex = difficultyFilter.getSelectedIndex();
        MuscleGroup group = groupIndex <= 0 ? null : MuscleGroup.values()[groupIndex - 1];
        Difficulty difficulty = difficultyIndex <= 0 ? null : Difficulty.values()[difficultyIndex - 1];
        shown = service.search(searchField.getText(), group, difficulty);
        countLabel.setText(shown.size() == 1 ? "1 ejercicio" : shown.size() + " ejercicios");
        rebuildGrid();
    }

    // Solo reconstruye la cuadrícula si cambió el número de columnas.
    private void rebuildGridIfColumnsChanged() {
        if (computeColumns() != columns) {
            rebuildGrid();
        }
    }

    private int computeColumns() {
        int width = scroll.getViewport().getWidth();
        return Math.max(1, (width + GAP) / (CARD_WIDTH + GAP));
    }

    // Dibuja una tarjeta por ejercicio en una cuadrícula de tamaño exacto.
    private void rebuildGrid() {
        columns = computeColumns();
        gridHolder.removeAll();
        if (shown.isEmpty()) {
            JLabel empty = new JLabel("No hay ejercicios. Crea el primero con «+ Nuevo ejercicio».");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
            gridHolder.add(empty);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, columns, GAP, GAP));
            grid.setOpaque(false);
            for (Exercise exercise : shown) {
                // Sin foto propia, la tarjeta muestra la ilustración de su grupo muscular.
                String picture = exercise.getImagePath() == null || exercise.getImagePath().isBlank()
                        ? MuscleArt.pathFor(exercise.getMuscleGroup()) : exercise.getImagePath();
                grid.add(new ImageCard(exercise.getName(), exercise.getMuscleGroup().getLabel()
                        + " · " + exercise.getDifficulty().getLabel(), picture,
                        CARD_WIDTH, CARD_HEIGHT, () -> openForm(exercise)));
            }
            gridHolder.add(grid);
        }
        gridHolder.revalidate();
        gridHolder.repaint();
    }

    // Abre el formulario para crear (exercise null) o editar un ejercicio.
    private void openForm(Exercise exercise) {
        new ExerciseFormDialog(SwingUtilities.getWindowAncestor(this), service, imagesFolder,
                exercise, this::refresh).setVisible(true);
    }
}
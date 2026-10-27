package com.nextfit.ui.training;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.service.ExerciseService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.PictureSource;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Modal form to create a new exercise or edit an existing one.
 * It never validates by itself: it builds an {@link Exercise} and hands it to
 * {@link ExerciseService}, which applies the rules; an error is shown inside the form.
 */
public class ExerciseFormDialog extends JDialog {

    private static final int PREVIEW_WIDTH = 160;
    private static final int PREVIEW_HEIGHT = 100;

    private final ExerciseService service;
    private final Path imagesFolder;
    private final Exercise existing;      // null cuando se está creando uno nuevo
    private final Runnable onSaved;

    private final JTextField nameField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(4, 20);
    private final JComboBox<String> muscleGroupBox = new JComboBox<>(
            Arrays.stream(MuscleGroup.values()).map(MuscleGroup::getLabel).toArray(String[]::new));
    private final JComboBox<String> difficultyBox = new JComboBox<>(
            Arrays.stream(Difficulty.values()).map(Difficulty::getLabel).toArray(String[]::new));
    private final JLabel previewLabel = new JLabel("Sin imagen", JLabel.CENTER);
    private final JLabel errorLabel = new JLabel(" ");

    private String imagePath;             // ruta de la imagen elegida (puede ser vacía)
    private boolean newImageChosen;       // true si el usuario eligió un archivo nuevo

    /**
     * @param owner        window the dialog belongs to
     * @param service      exercise service used to save
     * @param imagesFolder folder where chosen pictures are copied
     * @param existing     the exercise to edit, or null to create a new one
     * @param onSaved      action to run after a successful save (to refresh the list)
     */
    public ExerciseFormDialog(Window owner, ExerciseService service, Path imagesFolder,
                              Exercise existing, Runnable onSaved) {
        super(owner, existing == null ? "Nuevo ejercicio" : "Editar ejercicio", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.imagesFolder = imagesFolder;
        this.existing = existing;
        this.onSaved = onSaved;
        this.imagePath = existing == null ? "" : existing.getImagePath();
        buildForm();
        fillFromExisting();
        pack();
        setLocationRelativeTo(owner);
    }

    // Construye todos los controles del formulario.
    private void buildForm() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        nameField.putClientProperty("JTextField.placeholderText", "Ej. Press de banca");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(360, 90));

        previewLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        previewLabel.setForeground(Theme.TEXT_MUTED);
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Theme.BACKGROUND);
        JButton chooseButton = new JButton("Elegir imagen...");
        chooseButton.addActionListener(e -> chooseImage());
        JPanel imageRow = new JPanel(new GridBagLayout());
        imageRow.setOpaque(false);
        GridBagConstraints ic = new GridBagConstraints();
        ic.anchor = GridBagConstraints.WEST;
        imageRow.add(previewLabel, ic);
        ic.insets = new Insets(0, 12, 0, 0);
        imageRow.add(chooseButton, ic);

        errorLabel.setForeground(Theme.DANGER);

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = new JButton("Guardar");
        saveButton.setBackground(Theme.ACCENT);
        saveButton.setForeground(Theme.BACKGROUND);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(e -> save());
        getRootPane().setDefaultButton(saveButton);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints bc = new GridBagConstraints();
        bc.insets = new Insets(0, 8, 0, 0);
        buttons.add(cancelButton, bc);
        buttons.add(saveButton, bc);

        int row = 0;
        addRow(content, row++, "Nombre", nameField);
        addRow(content, row++, "Descripción", descriptionScroll);
        addRow(content, row++, "Grupo muscular", muscleGroupBox);
        addRow(content, row++, "Dificultad", difficultyBox);
        addRow(content, row++, "Imagen", imageRow);
        addRow(content, row++, "", errorLabel);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        content.add(buttons, c);

        setContentPane(content);
    }

    // Agrega una fila "etiqueta + control" al formulario.
    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 0, 6, 14);
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel text = new JLabel(label);
        text.setForeground(Theme.TEXT_MUTED);
        panel.add(text, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    // Si se está editando, se cargan los valores actuales en los controles.
    private void fillFromExisting() {
        if (existing != null) {
            nameField.setText(existing.getName());
            descriptionArea.setText(existing.getDescription());
            muscleGroupBox.setSelectedIndex(existing.getMuscleGroup().ordinal());
            difficultyBox.setSelectedIndex(existing.getDifficulty().ordinal());
        }
        showPreview();
    }

    // Muestra la imagen elegida (recortada) o el texto "Sin imagen".
    private void showPreview() {
        var picture = PictureSource.load(imagePath);
        if (picture.isPresent()) {
            previewLabel.setText(null);
            previewLabel.setIcon(new ImageIcon(
                    PictureSource.cover(picture.get(), PREVIEW_WIDTH, PREVIEW_HEIGHT)));
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("Sin imagen");
        }
    }

    // Abre el selector de archivos; solo guarda la ruta, la copia se hace al guardar.
    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (png, jpg, jpeg)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imagePath = chooser.getSelectedFile().getAbsolutePath();
            newImageChosen = true;
            showPreview();
        }
    }

    // Arma el ejercicio, lo guarda con el servicio y cierra; si hay error, lo muestra en el formulario.
    private void save() {
        try {
            String finalImagePath = newImageChosen ? copyImageToFolder(imagePath) : imagePath;
            Exercise exercise = new Exercise(
                    existing == null ? 0 : existing.getId(),
                    nameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    MuscleGroup.values()[muscleGroupBox.getSelectedIndex()],
                    Difficulty.values()[difficultyBox.getSelectedIndex()],
                    finalImagePath);
            if (existing == null) {
                service.create(exercise);
            } else {
                service.update(exercise);
            }
            onSaved.run();
            dispose();
        } catch (NextFitException e) {
            // Los errores de validación y de reglas ya traen un mensaje en español.
            errorLabel.setText(e.getMessage());
            pack();
        } catch (IOException e) {
            errorLabel.setText("No se pudo copiar la imagen: " + e.getMessage());
            pack();
        }
    }

    // Copia la imagen elegida a la carpeta de la aplicación para no depender del archivo original.
    private String copyImageToFolder(String sourcePath) throws IOException {
        Files.createDirectories(imagesFolder);
        String name = new File(sourcePath).getName();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
        Path target = imagesFolder.resolve("exercise-" + System.currentTimeMillis() + extension);
        Files.copy(Path.of(sourcePath), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }
}
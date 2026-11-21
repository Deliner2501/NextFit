package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.exceptions.PersistenceException;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Asks where to save a file, runs the exporter and tells the user the result. The screens use it so none of
 * them repeats the dialogs; the file formats themselves live in the export package.
 */
final class FileExport {

    /** The folder of the last file saved, so the next dialog opens there. */
    private static File lastFolder;

    private FileExport() {
    }

    /**
     * Shows the "save as" dialog and writes the file with the given exporter.
     *
     * @param owner         a node of the window the dialogs belong to
     * @param dialogTitle   the title of the "save as" dialog
     * @param suggestedName the proposed file name, with its extension
     * @param description   the name of the file type, for example "Documento PDF"
     * @param extension     the extension without a dot, for example "pdf"
     * @param writer        writes the file at the chosen path; may throw a {@link NextFitException}
     */
    static void save(Node owner, String dialogTitle, String suggestedName, String description, String extension,
                     Consumer<Path> writer) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + extension));
        if (lastFolder != null && lastFolder.isDirectory()) {
            chooser.setInitialDirectory(lastFolder);
        }
        File chosen = chooser.showSaveDialog(owner.getScene().getWindow());
        if (chosen == null) {
            return;
        }
        if (!chosen.getName().toLowerCase().endsWith("." + extension)) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + "." + extension);
        }
        try {
            writer.accept(chosen.toPath());
        } catch (NextFitException e) {
            FxDialogs.error(owner, e);
            return;
        } catch (RuntimeException e) {
            FxDialogs.error(owner, new PersistenceException("No se pudo crear el archivo: " + e.getMessage(), e));
            return;
        }
        lastFolder = chosen.getParentFile();
        if (FxDialogs.confirm(owner, "Archivo guardado", chosen.getName() + "\n" + chosen.getParent(), "Abrir")) {
            open(owner, chosen);
        }
    }

    private static void open(Node owner, File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return;
            }
        } catch (Exception ignored) {
            // The message below explains it; the file is already saved.
        }
        FxDialogs.message(owner, "No se pudo abrir", "El archivo está guardado, ábrelo desde su carpeta.");
    }
}

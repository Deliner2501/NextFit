package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Role;
import com.nextfit.service.BackupInfo;
import com.nextfit.service.BackupService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Backup module in JavaFX, only for the administrator: makes a backup of the data, restores one, and
 * saves a copy somewhere else (a flash drive, for example). The rules live in {@link BackupService}.
 */
public final class BackupModule implements FxModule {

    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BackupService service;
    private final TableView<BackupInfo> table = new TableView<>();
    private final Label countLabel = new Label();
    private final VBox view = new VBox(16);

    /**
     * @param service the service that makes and restores the backups
     */
    public BackupModule(BackupService service) {
        this.service = service;
        view.getChildren().addAll(buildHeader(), buildExplanation(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    @Override
    public String id() {
        return "backup";
    }

    @Override
    public String title() {
        return "Respaldo";
    }

    @Override
    public String icon() {
        return FxIcons.BACKUP;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** Only the administrator can make or restore backups. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN;
    }

    private Node buildHeader() {
        Label title = new Label("Respaldo de datos");
        title.getStyleClass().add("nf-card-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button restore = secondary("Restaurar seleccionado", this::restoreSelected);
        Button fromFile = secondary("Restaurar desde archivo", this::restoreFromFile);
        Button copy = secondary("Guardar copia en…", this::saveCopy);
        Button create = new Button("+ Crear respaldo ahora");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> createNow());

        HBox header = new HBox(10, titles, restore, fromFile, copy, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildExplanation() {
        Label text = new Label("Un respaldo guarda los socios, pagos, rutinas, usuarios y las fotos de los ejercicios. "
                + "NextFit hace uno cada día al abrirse y conserva los últimos 14. Antes de restaurar, guarda una copia de "
                + "lo que hay ahora, así que siempre se puede volver atrás.\nCarpeta de respaldos: "
                + service.backupDir());
        text.setWrapText(true);
        text.getStyleClass().add("nf-muted");
        VBox card = new VBox(text);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private static Button secondary(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("nf-secondary");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Node buildTable() {
        TableColumn<BackupInfo, String> name = FxTables.column("Archivo", 420, BackupInfo::name);
        TableColumn<BackupInfo, String> date = FxTables.column("Fecha", 200, b -> b.createdAt().format(MOMENT));
        TableColumn<BackupInfo, String> size = FxTables.column("Tamaño", 140, b -> size(b.size()));
        table.getColumns().addAll(List.of(name, date, size));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Todavía no hay respaldos"));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(280);
        return table;
    }

    private static String size(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.forLanguageTag("es-CO"), "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f MB", bytes / (1024.0 * 1024));
    }

    private void refresh() {
        List<BackupInfo> found = service.list();
        table.getItems().setAll(found);
        countLabel.setText(found.size() == 1 ? "1 respaldo" : found.size() + " respaldos");
    }

    private void createNow() {
        try {
            Path file = service.createBackup();
            refresh();
            FxDialogs.message(view, "Respaldo creado", file.getFileName().toString());
        } catch (NextFitException e) {
            FxDialogs.error(view, e);
        }
    }

    private void restoreSelected() {
        BackupInfo selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(view, "Selecciona un respaldo", "Elige un respaldo de la tabla primero.");
            return;
        }
        restore(selected.file());
    }

    private void restoreFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir un respaldo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo de NextFit (zip)", "*.zip"));
        File chosen = chooser.showOpenDialog(view.getScene().getWindow());
        if (chosen != null) {
            restore(chosen.toPath());
        }
    }

    private void restore(Path zip) {
        boolean confirmed = FxDialogs.confirm(view, "Restaurar respaldo",
                "Se van a reemplazar los datos actuales por los de " + zip.getFileName()
                        + ".\nAntes se guarda una copia de lo que hay ahora.", "Restaurar");
        if (!confirmed) {
            return;
        }
        try {
            Path safety = service.restore(zip);
            refresh();
            FxDialogs.message(view, "Respaldo restaurado", "Cierra NextFit y ábrelo de nuevo para ver los datos "
                    + "restaurados.\nTus datos anteriores quedaron en " + safety.getFileName());
        } catch (NextFitException e) {
            FxDialogs.error(view, e);
        }
    }

    private void saveCopy() {
        BackupInfo selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(view, "Selecciona un respaldo", "Elige el respaldo que quieres copiar.");
            return;
        }
        FileExport.save(view, "Guardar copia del respaldo", selected.name(), "Respaldo de NextFit (zip)", "zip", target -> {
            try {
                Files.copy(selected.file(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new PersistenceException("No se pudo copiar el respaldo", e);
            }
        });
    }
}

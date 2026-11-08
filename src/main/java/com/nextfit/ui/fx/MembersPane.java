package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.ui.Formats;
import com.nextfit.util.PhotoStorage;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Screen with the members of the gym: a table with a search box where the user can
 * register, edit, deactivate and reactivate them. The rules live in
 * {@link MemberService}; this class only collects clicks and shows data.
 */
final class MembersPane extends VBox {

    private static final String ACTIVE = "Activo";

    private final MemberService service;
    private final PhotoStorage photoStorage;
    private final BooleanSupplier canEdit;
    private final TableView<Member> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final Label countLabel = new Label();
    private final Button editButton = new Button("Editar");
    private final Button toggleButton = new Button("Dar de baja");
    private final Button createButton = new Button("+ Nuevo socio");

    /**
     * @param service      the service that manages the members
     * @param photoStorage where the photos of the members are copied
     * @param canEdit      tells whether the signed-in user may register, edit and deactivate
     */
    MembersPane(MemberService service, PhotoStorage photoStorage, BooleanSupplier canEdit) {
        super(16);
        this.service = service;
        this.photoStorage = photoStorage;
        this.canEdit = canEdit;
        getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Socios");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        searchField.setPromptText("Buscar por nombre o documento");
        searchField.getStyleClass().add("nf-input");
        searchField.setPrefSize(280, 44);
        searchField.textProperty().addListener((obs, old, now) -> refresh());
        editButton.getStyleClass().add("nf-secondary");
        editButton.setOnAction(e -> editSelected());
        toggleButton.getStyleClass().add("nf-secondary");
        toggleButton.setOnAction(e -> toggleSelected());
        createButton.getStyleClass().add("nf-primary-small");
        createButton.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, searchField, editButton, toggleButton, createButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<Member, Member> photo = new TableColumn<>("");
        photo.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        photo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                setGraphic(empty || member == null ? null : MemberAvatar.of(member, 34));
            }
        });
        photo.setPrefWidth(70);
        photo.setSortable(false);
        TableColumn<Member, String> name = FxTables.column("Nombre", 240, Member::getFullName);
        TableColumn<Member, String> document = FxTables.column("Documento", 150, Member::getDocumentNumber);
        TableColumn<Member, String> phone = FxTables.column("Teléfono", 150, Member::getPhone);
        TableColumn<Member, String> joined = FxTables.column("Ingreso", 140, m -> Formats.date(m.getJoinDate()));
        TableColumn<Member, String> state = FxTables.column("Estado", 120, m -> m.isActive() ? ACTIVE : "De baja");
        FxTables.colorize(state, text -> ACTIVE.equals(text) ? "nf-state-on" : "nf-state-off");
        table.getColumns().addAll(List.of(photo, name, document, phone, joined, state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Todavía no hay socios. Registra el primero con «+ Nuevo socio»."));
        table.getStyleClass().add("nf-table");
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateButtons());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelected();
            }
        });
        table.setMinHeight(320);
        return table;
    }

    /** Reloads the members from the service, keeping the selected one if it is still listed. */
    void refresh() {
        Member previous = table.getSelectionModel().getSelectedItem();
        List<Member> all = service.findAll();
        long active = all.stream().filter(Member::isActive).count();
        countLabel.setText(all.size() + (all.size() == 1 ? " socio · " : " socios · ")
                + active + (active == 1 ? " activo" : " activos"));
        table.getItems().setAll(service.search(searchField.getText()));
        if (previous != null) {
            for (Member member : table.getItems()) {
                if (member.getId() == previous.getId()) {
                    table.getSelectionModel().select(member);
                }
            }
        }
        boolean editable = canEdit.getAsBoolean();
        for (Button button : List.of(editButton, toggleButton, createButton)) {
            button.setVisible(editable);
            button.setManaged(editable);
        }
        updateButtons();
    }

    private void updateButtons() {
        Member selected = table.getSelectionModel().getSelectedItem();
        editButton.setDisable(selected == null);
        toggleButton.setDisable(selected == null);
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Dar de baja");
    }

    private void editSelected() {
        if (!canEdit.getAsBoolean()) {
            return;
        }
        Member selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    // Deactivates or reactivates the selected member; a rule that rejects it is shown in a dialog.
    private void toggleSelected() {
        Member selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            if (selected.isActive()) {
                boolean confirmed = FxDialogs.confirm(this, "Dar de baja",
                        "¿Dar de baja a " + selected.getFullName() + "? Sus datos se conservan.", "Dar de baja");
                if (!confirmed) {
                    return;
                }
                service.deactivate(selected.getId());
            } else {
                service.reactivate(selected.getId());
            }
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
        refresh();
    }

    // Opens the form to register (member null) or edit a member.
    private void openForm(Member member) {
        MemberForm.open(this, service, photoStorage, member, this::refresh);
    }
}

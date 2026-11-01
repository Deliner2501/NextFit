package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.service.Session;
import com.nextfit.service.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Users module in JavaFX, only for the administrator: lists the accounts and
 * lets the user create, edit, reset the password and enable or disable them.
 * The rules live in {@link UserService}; this class only collects clicks.
 */
public final class UsersModule implements FxModule {

    private final UserService service;
    private final Session session;
    private final TableView<User> table = new TableView<>();
    private final Label countLabel = new Label();
    private final Button toggleButton = new Button("Desactivar");
    private final VBox view = new VBox(16);

    /**
     * @param service the service that manages the users
     * @param session the session, to know who is acting
     */
    public UsersModule(UserService service, Session session) {
        this.service = service;
        this.session = session;
        view.getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    @Override
    public String id() {
        return "users";
    }

    @Override
    public String title() {
        return "Usuarios";
    }

    @Override
    public String icon() {
        return FxIcons.USERS;
    }

    @Override
    public Node view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** Only the administrator manages accounts. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN;
    }

    private Node buildHeader() {
        Label title = new Label("Usuarios del sistema");
        title.getStyleClass().add("nf-card-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button edit = secondary("Editar", this::editSelected);
        Button password = secondary("Restablecer contraseña", this::resetPassword);
        toggleButton.getStyleClass().add("nf-secondary");
        toggleButton.setOnAction(e -> toggleSelected());
        Button create = new Button("+ Nuevo usuario");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, edit, password, toggleButton, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private static Button secondary(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("nf-secondary");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Node buildTable() {
        TableColumn<User, String> username = column("Usuario", 200, User::getUsername);
        TableColumn<User, String> name = column("Nombre", 340, User::getFullName);
        TableColumn<User, String> role = column("Rol", 200, u -> u.getRole().getLabel());
        TableColumn<User, String> state = column("Estado", 160, u -> u.isActive() ? "Activo" : "Desactivado");
        state.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("nf-state-on", "nf-state-off");
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(value);
                    getStyleClass().add("Activo".equals(value) ? "nf-state-on" : "nf-state-off");
                }
            }
        });
        table.getColumns().addAll(java.util.List.of(username, name, role, state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Aún no hay usuarios"));
        table.getStyleClass().add("nf-table");
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateToggleText());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelected();
            }
        });
        table.setMinHeight(320);
        return table;
    }

    private TableColumn<User, String> column(String title, double width,
                                             java.util.function.Function<User, String> value) {
        TableColumn<User, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    private void refresh() {
        table.getItems().setAll(service.findAll());
        int total = table.getItems().size();
        countLabel.setText(total == 1 ? "1 usuario" : total + " usuarios");
        updateToggleText();
    }

    private void updateToggleText() {
        User selected = table.getSelectionModel().getSelectedItem();
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Desactivar");
    }

    private User requireSelection() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(view, "Selecciona un usuario", "Elige un usuario de la tabla primero.");
        }
        return selected;
    }

    private void editSelected() {
        User selected = requireSelection();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(User existing) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("3 a 20 caracteres, sin espacios");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Ej. Laura Pérez");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mínimo 8, con letras y números");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repite la contraseña");
        ComboBox<Role> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(Role.values());
        roleBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : role.getLabel();
            }

            @Override
            public Role fromString(String text) {
                return null;
            }
        });
        roleBox.setValue(existing == null ? Role.RECEPTION : existing.getRole());
        roleBox.setMaxWidth(Double.MAX_VALUE);
        for (TextField field : new TextField[] {usernameField, fullNameField, passwordField, confirmField}) {
            field.getStyleClass().add("nf-input");
            field.setPrefHeight(44);
        }
        if (existing != null) {
            fullNameField.setText(existing.getFullName());
        }

        VBox fields = new VBox(12);
        if (existing == null) {
            fields.getChildren().add(FxDialogs.labeled("Usuario", usernameField));
        }
        fields.getChildren().addAll(FxDialogs.labeled("Nombre completo", fullNameField),
                FxDialogs.labeled("Rol", roleBox));
        if (existing == null) {
            fields.getChildren().addAll(FxDialogs.labeled("Contraseña", passwordField),
                    FxDialogs.labeled("Confirmar", confirmField));
        }
        fields.setPadding(new Insets(0));

        FxDialogs.form(view, existing == null ? "Nuevo usuario" : "Editar usuario", "Guardar", fields, () -> {
            if (existing == null) {
                String password = passwordField.getText();
                if (!password.equals(confirmField.getText())) {
                    throw new InvalidDataException("contraseña", "las dos contraseñas no coinciden");
                }
                service.create(usernameField.getText().trim(), fullNameField.getText().trim(), password,
                        roleBox.getValue());
            } else {
                service.update(existing.getId(), fullNameField.getText().trim(), roleBox.getValue());
            }
            refresh();
        });
    }

    private void resetPassword() {
        User selected = requireSelection();
        if (selected == null) {
            return;
        }
        PasswordField field = new PasswordField();
        field.setPromptText("Mínimo 8, con letras y números");
        field.getStyleClass().add("nf-input");
        field.setPrefHeight(44);
        FxDialogs.form(view, "Restablecer contraseña", "Guardar",
                FxDialogs.labeled("Nueva contraseña para " + selected.getUsername(), field), () -> {
                    service.resetPassword(selected.getId(), field.getText());
                });
    }

    private void toggleSelected() {
        User selected = requireSelection();
        if (selected == null) {
            return;
        }
        try {
            if (selected.isActive()) {
                long actingId = session.getCurrentUser().map(User::getId).orElse(-1L);
                service.deactivate(selected.getId(), actingId);
            } else {
                service.reactivate(selected.getId());
            }
            refresh();
        } catch (NextFitException e) {
            FxDialogs.error(view, e);
        }
    }
}

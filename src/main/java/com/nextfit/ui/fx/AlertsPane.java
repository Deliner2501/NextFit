package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.service.alert.Alert;
import com.nextfit.service.alert.AlertMessages;
import com.nextfit.service.alert.AlertService;
import com.nextfit.service.alert.AlertType;
import com.nextfit.service.alert.WhatsAppLink;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The screen of the alert center: three cards with the numbers of the day, the list of alerts
 * with a filter by type, and, for the selected alert, the message for the member with the
 * buttons to open WhatsApp or copy it. The alerts, the message and the link come from
 * {@link AlertService}, {@link AlertMessages} and {@link WhatsAppLink}; this class only joins
 * them on screen and sends nothing by itself.
 */
final class AlertsPane extends VBox {

    private static final String ALL = "Todas las alertas";
    private static final String COPY_TEXT = "Copiar mensaje";
    private static final String NOTHING_SELECTED = "Elige una alerta de la lista para ver el mensaje "
            + "que se le enviará al socio.";

    private final AlertService alerts;
    private final AlertMessages messages;
    private final WhatsAppLink whatsApp;
    private final KpiCard expiringCard = new KpiCard("Por vencer", "0", "", FxIcons.CALENDAR, KpiCard.Kind.FEATURE);
    private final KpiCard inactiveCard = new KpiCard("Inactivos", "0", "", FxIcons.MEMBERS, KpiCard.Kind.VIOLET);
    private final KpiCard birthdayCard = new KpiCard("Cumpleaños", "0", "", AlertIcons.CAKE, KpiCard.Kind.LIME);
    private final ComboBox<String> filter = new ComboBox<>();
    private final Label countLabel = new Label();
    private final TableView<Alert> table = new TableView<>();
    private final Label preview = new Label();
    private final Button whatsAppButton = new Button("Abrir WhatsApp");
    private final Button copyButton = new Button(COPY_TEXT);
    private List<Alert> all = new ArrayList<>();

    /**
     * @param alerts   the service that gathers the alerts of the day
     * @param messages the writer of the message for each alert
     * @param whatsApp the builder of the WhatsApp link
     */
    AlertsPane(AlertService alerts, AlertMessages messages, WhatsAppLink whatsApp) {
        super(16);
        this.alerts = alerts;
        this.messages = messages;
        this.whatsApp = whatsApp;

        expiringCard.hint().setText("Vencen en los próximos " + AlertService.EXPIRING_DAYS + " días");
        inactiveCard.hint().setText("Sin venir hace " + AlertService.INACTIVE_DAYS + " días o más");
        birthdayCard.hint().setText(AlertService.BIRTHDAY_DAYS_AHEAD == 0
                ? "Cumplen años hoy"
                : "Cumplen años en los próximos " + AlertService.BIRTHDAY_DAYS_AHEAD + " días");

        filter.getItems().add(ALL);
        for (AlertType type : AlertType.values()) {
            filter.getItems().add(type.getLabel());
        }
        filter.getSelectionModel().selectFirst();
        filter.setPrefHeight(44);
        filter.setOnAction(e -> showRows());

        getChildren().addAll(buildHeader(), buildCards(), buildTable(), buildMessageCard());
        VBox.setVgrow(table, Priority.ALWAYS);
        FxMotion.enter(getChildren(), 0, 90);
        showMessage(null);
    }

    /** Asks the service for the alerts of today again and updates the cards and the list. */
    void refresh() {
        Map<AlertType, Long> counts;
        try {
            all = alerts.findAll();
            counts = alerts.countByType();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
            return;
        }
        expiringCard.value().setText(String.valueOf(counts.get(AlertType.EXPIRING)));
        inactiveCard.value().setText(String.valueOf(counts.get(AlertType.INACTIVE)));
        birthdayCard.value().setText(String.valueOf(counts.get(AlertType.BIRTHDAY)));
        showRows();
    }

    private Node buildHeader() {
        Label title = new Label("Centro de alertas");
        title.getStyleClass().add("nf-card-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(10, titles, filter);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildCards() {
        HBox cards = new HBox(18, expiringCard, inactiveCard, birthdayCard);
        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
        }
        return cards;
    }

    private Node buildTable() {
        TableColumn<Alert, String> type = FxTables.column("Tipo", 140, a -> a.type().getLabel());
        FxTables.colorize(type, AlertsPane::styleOf);
        TableColumn<Alert, String> member = FxTables.column("Socio", 260, a -> a.member().getFullName());
        TableColumn<Alert, String> phone = FxTables.column("Teléfono", 150, a -> a.member().getPhone());
        TableColumn<Alert, String> detail = FxTables.column("Detalle", 360, AlertText::detail);
        table.getColumns().addAll(List.of(type, member, phone, detail));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> showMessage(now));
        table.setPlaceholder(new Label("Hoy no hay alertas. Todo en orden."));
        table.getStyleClass().add("nf-table");
        // A small preferred height lets the table take only the space that is left, so the
        // message card below keeps its full height on a 1080p screen.
        table.setMinHeight(200);
        table.setPrefHeight(240);
        return table;
    }

    private Node buildMessageCard() {
        Label title = new Label("Mensaje para el socio");
        title.getStyleClass().add("nf-card-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        preview.setWrapText(true);

        copyButton.getStyleClass().add("nf-secondary");
        copyButton.setOnAction(e -> copyMessage());
        whatsAppButton.getStyleClass().add("nf-primary-small");
        whatsAppButton.setOnAction(e -> openWhatsApp());
        for (Button button : List.of(copyButton, whatsAppButton)) {
            button.setMinWidth(Region.USE_PREF_SIZE);
        }

        // The buttons go next to the title and the message gets the whole width of the card,
        // so a long message wraps to more lines instead of being cut.
        HBox top = new HBox(10, title, copyButton, whatsAppButton);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, top, preview);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.getStyleClass().add("nf-card");
        return card;
    }

    // Applies the chosen filter to the alerts that are already loaded.
    private void showRows() {
        int index = filter.getSelectionModel().getSelectedIndex();
        AlertType chosen = index <= 0 ? null : AlertType.values()[index - 1];
        List<Alert> rows = all.stream()
                .filter(alert -> chosen == null || alert.type() == chosen)
                .toList();
        table.getItems().setAll(rows);
        table.getSelectionModel().clearSelection();
        countLabel.setText(rows.isEmpty() ? "Sin alertas por ahora"
                : rows.size() == 1 ? "1 alerta" : rows.size() + " alertas");
        showMessage(null);
    }

    // Shows the message of the selected alert and enables the buttons when there is something to send.
    private void showMessage(Alert selected) {
        preview.getStyleClass().removeAll("nf-muted", "nf-alert-message");
        if (selected == null) {
            preview.setText(NOTHING_SELECTED);
            preview.getStyleClass().add("nf-muted");
            whatsAppButton.setDisable(true);
            copyButton.setDisable(true);
            return;
        }
        String text = messages.textFor(selected);
        boolean validPhone = linkFor(selected, text).isPresent();
        preview.getStyleClass().add("nf-alert-message");
        preview.setText(validPhone ? text
                : text + "\n\nEl teléfono de este socio no sirve para WhatsApp. Corrígelo en su ficha.");
        whatsAppButton.setDisable(!validPhone);
        copyButton.setDisable(false);
    }

    private Optional<String> linkFor(Alert alert, String text) {
        return whatsApp.forMessage(alert.member().getPhone(), text);
    }

    private void openWhatsApp() {
        Alert selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Optional<String> link = linkFor(selected, messages.textFor(selected));
        if (link.isEmpty()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(link.get()));
                return;
            }
        } catch (Exception ignored) {
            // The message below explains what to do instead.
        }
        FxDialogs.message(this, "No se pudo abrir WhatsApp",
                "Usa «Copiar mensaje» y pégalo en el chat del socio.");
    }

    private void copyMessage() {
        Alert selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(messages.textFor(selected));
        Clipboard.getSystemClipboard().setContent(content);
        copyButton.setText("Copiado ✓");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> copyButton.setText(COPY_TEXT));
        pause.play();
    }

    // Red for what is about to be lost, amber for who is drifting away, green for the good news.
    private static String styleOf(String label) {
        if (AlertType.EXPIRING.getLabel().equals(label)) {
            return "nf-state-bad";
        }
        if (AlertType.INACTIVE.getLabel().equals(label)) {
            return "nf-state-warn";
        }
        return "nf-state-on";
    }
}

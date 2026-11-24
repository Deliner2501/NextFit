package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.Role;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.Session;
import com.nextfit.service.occupancy.GymCapacity;
import com.nextfit.ui.Formats;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Shows open visits in arrival order and lets staff record a missed departure. */
final class InsidePane extends VBox {

    private static final double CARD_WIDTH = 280;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/MM", Locale.forLanguageTag("es-CO"));

    private final FxLoader dataLoader = new FxLoader();
    private final AttendanceService attendance;
    private final MemberService members;
    private final GymCapacity capacity;
    private final Session session;
    private final Button settings = new Button("Configurar aforo");
    private final Label details = new Label("Consultando ocupación…");
    private final ProgressBar fill = new ProgressBar(0);
    private final FlowPane cards = new FlowPane(12, 12);
    private final Label empty = new Label("No hay socios dentro en este momento.");
    private Map<VisitCard, Node> rendered = Map.of();

    /**
     * @param attendance the source of open visits
     * @param members the source of visitor names and photos
     * @param capacity the current admission limit
     * @param session the current operator
     */
    InsidePane(AttendanceService attendance, MemberService members, GymCapacity capacity, Session session) {
        super(14);
        this.attendance = attendance;
        this.members = members;
        this.capacity = capacity;
        this.session = session;

        Label title = new Label("Dentro ahora");
        title.getStyleClass().add("nf-section-title");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox header = new HBox(12, title, space);
        header.setAlignment(Pos.CENTER_LEFT);
        settings.getStyleClass().add("nf-secondary");
        settings.setMinWidth(Region.USE_PREF_SIZE);
        settings.setOnAction(event -> configureCapacity());
        settings.setVisible(false);
        settings.managedProperty().bind(settings.visibleProperty());
        header.getChildren().add(settings);

        details.getStyleClass().add("nf-card-title");
        details.setWrapText(true);
        fill.getStyleClass().add("nf-occupancy-bar");
        fill.setMaxWidth(Double.MAX_VALUE);
        VBox overview = new VBox(8, details, fill);
        overview.getStyleClass().add("nf-card");
        overview.setPadding(new Insets(14));
        overview.setMaxWidth(390);

        Label listTitle = new Label("Personas dentro · de la primera a la última entrada");
        listTitle.getStyleClass().add("nf-muted-small");
        empty.getStyleClass().add("nf-muted");
        cards.setAlignment(Pos.TOP_LEFT);
        cards.setRowValignment(javafx.geometry.VPos.TOP);

        VBox content = new VBox(10, empty, cards);
        content.setPadding(new Insets(0, 4, 8, 0));
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("nf-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(header, overview, listTitle, scroll);

        Timeline updates = new Timeline(new KeyFrame(Duration.seconds(10), event -> refresh()));
        updates.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((observable, before, scene) -> {
            if (scene == null) {
                updates.stop();
            } else {
                updates.play();
            }
        });
    }

    /** Updates the visitor cards, capacity and remaining spots. */
    void refresh() {
        settings.setVisible(session.hasRole(Role.ADMIN));
        dataLoader.load(this, () -> new InsideSnapshot(attendance.findInside().stream()
                .sorted(Comparator.comparing(AttendanceRecord::getCheckIn).thenComparingLong(AttendanceRecord::getId))
                .toList(), members.findAll().stream().collect(Collectors.toMap(Member::getId, member -> member))),
                this::display);
    }

    private void display(InsideSnapshot snapshot) {
        Map<VisitCard, Node> next = new LinkedHashMap<>();
        for (AttendanceRecord open : snapshot.records()) {
            Member member = snapshot.members().get(open.getMemberId());
            String name = member == null ? "Socio #" + open.getMemberId() : member.getFullName();
            String day = open.getCheckIn().toLocalDate().equals(LocalDate.now())
                    ? "" : open.getCheckIn().format(DAY) + " · ";
            VisitCard key = new VisitCard(open.getId(), name, day + open.getCheckIn().format(TIME),
                    member == null ? null : member.getPhotoPath());
            Node card = rendered.get(key);
            next.put(key, card == null ? buildCard(open, member, key) : card);
        }
        List<Node> ordered = List.copyOf(next.values());
        if (!cards.getChildren().equals(ordered)) {
            cards.getChildren().setAll(ordered);
        }
        rendered = next;
        empty.setVisible(next.isEmpty());
        empty.setManaged(next.isEmpty());

        int count = snapshot.records().size();
        int limit = capacity.limit();
        details.setText(count + " de " + limit + " personas · " + Math.max(0, limit - count) + " cupos libres");
        fill.setProgress(Math.min(1.0, (double) count / limit));
    }

    private Node buildCard(AttendanceRecord record, Member member, VisitCard information) {
        Label name = new Label(information.name());
        name.getStyleClass().add("nf-list-name");
        name.setWrapText(true);
        name.setMinHeight(Region.USE_PREF_SIZE);
        Label time = new Label("Ingreso · " + information.time());
        time.getStyleClass().add("nf-muted-small");
        VBox text = new VBox(3, name, time);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox identity = new HBox(10, text);
        identity.setAlignment(Pos.CENTER_LEFT);
        if (member != null) {
            identity.getChildren().add(0, MemberAvatar.of(member, 34));
        }
        Button exit = new Button("Registrar salida");
        exit.getStyleClass().add("nf-secondary");
        exit.setOnAction(event -> closeVisit(exit, record, information.name()));
        VBox card = new VBox(8, identity, exit);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(14));
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setMinHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private record InsideSnapshot(List<AttendanceRecord> records, Map<Long, Member> members) {}
    private record VisitCard(long id, String name, String time, String photoPath) {}

    private void configureCapacity() {
        TextField limit = new TextField(Integer.toString(capacity.limit()));
        limit.getStyleClass().add("nf-input");
        FxDialogs.form(this, "Configurar aforo", "Guardar", FxDialogs.labeled("Máximo de personas", limit),
                () -> capacity.update(Formats.parseWholeNumber("aforo", limit.getText()), session));
        refresh();
    }

    private void closeVisit(Button exit, AttendanceRecord record, String name) {
        if (!FxDialogs.confirm(exit, "Registrar salida",
                "¿Registrar la salida de " + name + "? Úsalo solo si olvidó marcarla.", "Registrar salida")) {
            return;
        }
        try {
            attendance.checkOut(record.getMemberId());
        } catch (NextFitException e) {
            FxDialogs.error(exit, e);
        }
        refresh();
    }
}

package com.nextfit.ui.fx;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * The attendance tab of the member file: how many times the member came and the list of
 * visits with their entry, exit and duration. It only shows data; check-ins are made at
 * the access control.
 */
final class AttendancePane extends VBox {

    private final Member member;
    private final AttendanceService attendance;
    private final KpiCard totalCard = new KpiCard("Visitas en total", "0", "", FxIcons.ACCESS, KpiCard.Kind.FEATURE);
    private final KpiCard monthCard = new KpiCard("Este mes", "0", "", FxIcons.CALENDAR, KpiCard.Kind.VIOLET);
    private final KpiCard lastCard = new KpiCard("Última visita", "—", "", FxIcons.MEMBERS, KpiCard.Kind.LIME);
    private final TableView<AttendanceRecord> table = new TableView<>();

    /**
     * @param member     the member whose visits are shown
     * @param attendance the service that knows the visits
     */
    AttendancePane(Member member, AttendanceService attendance) {
        super(16);
        this.member = member;
        this.attendance = attendance;
        setPadding(new Insets(16, 4, 0, 0));

        HBox kpis = new HBox(16, totalCard, monthCard, lastCard);
        for (KpiCard card : List.of(totalCard, monthCard, lastCard)) {
            card.setPrefWidth(1);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        getChildren().addAll(kpis, buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private TableView<AttendanceRecord> buildTable() {
        TableColumn<AttendanceRecord, String> in = FxTables.column("Entrada", 200,
                r -> Formats.dateTime(r.getCheckIn()));
        TableColumn<AttendanceRecord, String> out = FxTables.column("Salida", 200,
                r -> r.getCheckOut().map(Formats::dateTime).orElse("—"));
        TableColumn<AttendanceRecord, String> duration = FxTables.column("Duración", 140,
                AttendanceSummary::duration);
        FxTables.colorize(duration, text -> "Dentro".equals(text) ? "nf-state-on" : "nf-state-off");
        table.getColumns().addAll(List.of(in, out, duration));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(44);
        table.setPlaceholder(new Label("Este socio todavía no ha entrado al gimnasio."));
        table.getStyleClass().add("nf-table");
        table.setMinHeight(220);
        return table;
    }

    /** Reads the visits again and updates the numbers and the table. */
    void refresh() {
        List<AttendanceRecord> records = attendance.findByMember(member.getId());
        AttendanceSummary summary = AttendanceSummary.of(records, YearMonth.now());
        totalCard.value().setText(String.valueOf(summary.total()));
        totalCard.hint().setText(summary.total() == 1 ? "visita registrada" : "visitas registradas");
        monthCard.value().setText(String.valueOf(summary.thisMonth()));
        monthCard.hint().setText(summary.thisMonth() == 1 ? "visita este mes" : "visitas este mes");
        lastCard.value().setText(summary.last().map(m -> Formats.date(m.toLocalDate())).orElse("—"));
        lastCard.hint().setText(summary.last().map(m -> String.format("a las %02d:%02d", m.getHour(), m.getMinute()))
                .orElse("Sin visitas todavía"));
        List<AttendanceRecord> newestFirst = new ArrayList<>(records);
        newestFirst.sort((a, b) -> b.getCheckIn().compareTo(a.getCheckIn()));
        table.getItems().setAll(newestFirst);
    }
}

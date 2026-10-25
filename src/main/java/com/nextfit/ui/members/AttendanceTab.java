package com.nextfit.ui.members;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tab with the entry and exit of a member and the history of visits. The rules
 * (active member, valid membership, not already inside) live in
 * {@link AttendanceService}; this tab only shows the result.
 */
class AttendanceTab extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static final String[] COLUMNS = {"Entrada", "Salida", "Duración"};

    private final AttendanceService attendanceService;
    private final Member member;
    private final JLabel status = new JLabel(" ");
    private final JButton checkInButton = Forms.accentButton("Registrar entrada");
    private final JButton checkOutButton = new JButton("Registrar salida");
    private final JLabel message = Forms.messageLabel();
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    AttendanceTab(AttendanceService attendanceService, Member member) {
        super(new BorderLayout(0, 14));
        this.attendanceService = attendanceService;
        this.member = member;
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        status.setFont(status.getFont().deriveFont(Font.BOLD, 18f));
        checkInButton.addActionListener(e -> run(() -> attendanceService.checkIn(member.getId()), "Entrada registrada"));
        checkOutButton.addActionListener(e -> run(() -> attendanceService.checkOut(member.getId()), "Salida registrada"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(checkInButton);
        buttons.add(checkOutButton);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(status, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.CENTER);
        top.add(message, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        Forms.styleTable(table);
        add(Forms.scroll(table), BorderLayout.CENTER);
        refresh();
    }

    /** Reloads the state and the history from the service. */
    void refresh() {
        List<AttendanceRecord> records = attendanceService.findByMember(member.getId());
        Optional<AttendanceRecord> open = records.stream().filter(AttendanceRecord::isOpen).findFirst();
        if (open.isPresent()) {
            status.setForeground(Theme.SUCCESS);
            status.setText("Dentro del gimnasio desde " + open.get().getCheckIn().format(DATE_TIME));
        } else {
            status.setForeground(Theme.TEXT_MUTED);
            status.setText("Fuera del gimnasio");
        }
        checkInButton.setEnabled(open.isEmpty());
        checkOutButton.setEnabled(open.isPresent());

        tableModel.setRowCount(0);
        for (int i = records.size() - 1; i >= 0; i--) {
            AttendanceRecord record = records.get(i);
            LocalDateTime out = record.getCheckOut().orElse(null);
            tableModel.addRow(new Object[]{record.getCheckIn().format(DATE_TIME),
                    out == null ? "Dentro" : out.format(DATE_TIME),
                    out == null ? "—" : durationText(Duration.between(record.getCheckIn(), out))});
        }
    }

    private String durationText(Duration duration) {
        return duration.toHours() + " h " + String.format("%02d", duration.toMinutesPart()) + " min";
    }

    private void run(Runnable action, String successText) {
        try {
            action.run();
            refresh();
            Forms.showSuccess(message, successText);
        } catch (NextFitException e) {
            Forms.showError(message, e.getMessage());
        }
    }
}
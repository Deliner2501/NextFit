package com.nextfit.ui.members;

import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.MemberAvatar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;

/**
 * Record of one member: personal summary on top and three tabs (health,
 * measurements and attendance). Each tab is its own class, so this one only
 * assembles them and refreshes the tab that is shown.
 */
public class MemberDetailPanel extends JPanel {

    /**
     * Creates the record.
     *
     * @param member            the member to show
     * @param healthService     the service used by the health and measurement tabs
     * @param attendanceService the service used by the attendance tab
     * @param onBack            what to do when the user goes back to the list
     */
    public MemberDetailPanel(Member member, HealthService healthService,
                             AttendanceService attendanceService, Runnable onBack) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(buildHeader(member, onBack), BorderLayout.NORTH);

        HealthTab health = new HealthTab(healthService, member);
        MeasurementsTab measurements = new MeasurementsTab(healthService, member);
        AttendanceTab attendance = new AttendanceTab(attendanceService, member);
        List<Runnable> refreshers = List.of(health::refresh, measurements::refresh, attendance::refresh);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Salud", health);
        tabs.addTab("Mediciones", measurements);
        tabs.addTab("Asistencia", attendance);
        tabs.addChangeListener(e -> refreshers.get(tabs.getSelectedIndex()).run());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader(Member member, Runnable onBack) {
        JButton back = new JButton("← Volver a socios");
        back.addActionListener(e -> onBack.run());
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.add(back);

        MemberAvatar avatar = new MemberAvatar(64);
        avatar.setMember(member.getFullName(), member.getPhotoPath());
        JLabel name = new JLabel(member.getFullName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 24f));
        name.setForeground(Theme.TEXT);
        String state = member.isActive() ? "Activo" : "De baja";
        JLabel summary = new JLabel(member.getPersonType() + " · Documento " + member.getDocumentNumber()
                + " · " + member.ageOn(LocalDate.now()) + " años · " + state);
        summary.setForeground(Theme.TEXT_MUTED);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(name);
        texts.add(Box.createVerticalStrut(4));
        texts.add(summary);

        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        identity.setOpaque(false);
        identity.add(avatar);
        identity.add(texts);

        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        header.add(backRow, BorderLayout.NORTH);
        header.add(identity, BorderLayout.CENTER);
        return header;
    }
}
package com.nextfit.ui.dashboard;

import com.nextfit.model.Role;
import com.nextfit.service.DashboardService;
import com.nextfit.service.DashboardService.Summary;
import com.nextfit.service.Session;
import com.nextfit.ui.Formats;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;
import com.nextfit.ui.components.BarChart;
import com.nextfit.ui.components.DonutChart;
import com.nextfit.ui.components.LineChart;
import com.nextfit.ui.components.StatCard;
import com.nextfit.ui.components.HeroBanner;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Home screen: indicator cards and charts about memberships, income and
 * attendance. It asks {@link DashboardService} for one summary and only decides
 * how to draw it; every number is worked out in the service.
 */
public class DashboardPanel implements ModulePanel {

    private static final String[] WEEKDAYS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final DashboardService service;
    private final Session session;

    private final JPanel view = new JPanel(new BorderLayout());
    private final JLabel greeting = new JLabel(" ");
    private final JLabel dateLabel = new JLabel(" ");
    private final StatCard activeCard = new StatCard("Membresías activas", "members", Theme.ACCENT,
            v -> String.format("%,d", v));
    private final StatCard incomeCard = new StatCard("Ingresos del mes", "payments", Theme.SUCCESS, Formats::money);
    private final StatCard expiringCard = new StatCard("Por vencer", "shield", Theme.WARNING,
            v -> String.format("%,d", v));
    private final StatCard attendanceCard = new StatCard("Asistencia de hoy", "chart", Theme.ACCENT_ALT,
            v -> String.format("%,d", v));
    private final DonutChart statusChart = new DonutChart("Estado de las membresías");
    private final BarChart weekdayChart = new BarChart("Asistencia por día de la semana");
    private final LineChart incomeChart = new LineChart("Ingresos de los últimos meses", "millones de pesos");

    /**
     * Creates the screen.
     *
     * @param service the service that works out the numbers
     * @param session the session, to greet the signed-in user
     */
    public DashboardPanel(DashboardService service, Session session) {
        this.service = service;
        this.session = session;
        buildScreen();
    }

    @Override
    public String id() {
        return "dashboard";
    }

    @Override
    public String title() {
        return "Panel";
    }

    @Override
    public String icon() {
        return "dashboard";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        Summary summary = service.summary();
        greeting.setText("Hola, " + session.getCurrentUser().map(u -> u.getFullName()).orElse("bienvenido"));
        dateLabel.setText(Formats.date(summary.today()));
        activeCard.setValue(summary.activeSubscriptions());
        incomeCard.setValue(summary.incomeThisMonth());
        expiringCard.setValue(summary.expiringSoon());
        attendanceCard.setValue(summary.checkInsToday());
        showStatus(summary);
        showAttendance(summary.checkInsByWeekday());
        showIncome(summary.incomeByMonth());
    }

    /** The trainer does not need the money numbers, so the home screen is for the other roles. */
    @Override
    public boolean allowedFor(Role role) {
        return role != Role.TRAINER;
    }

    private void buildScreen() {
        activeCard.setCaption("socios con acceso hoy");
        expiringCard.setCaption("terminan en 7 días o menos");
        attendanceCard.setCaption("entradas registradas");
        incomeCard.setCaption("desde el primer día del mes");
        statusChart.setCenterLabel("membresías");
        statusChart.setEmptyMessage("Aún no hay membresías vendidas");
        weekdayChart.setEmptyMessage("Aún no hay entradas registradas");
        incomeChart.setDecimals(2);
        incomeChart.setEmptyMessage("Aún no hay pagos");

        greeting.setFont(greeting.getFont().deriveFont(Font.BOLD, 28f));
        greeting.setForeground(Theme.TEXT);
        dateLabel.setForeground(Theme.TEXT_MUTED);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(greeting);
        titles.add(dateLabel);

        HeroBanner banner = new HeroBanner(titles);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));

        cards.setOpaque(false);
        cards.add(activeCard);
        cards.add(incomeCard);
        cards.add(expiringCard);
        cards.add(attendanceCard);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(card(statusChart));
        charts.add(card(weekdayChart));
        charts.setPreferredSize(new Dimension(0, 300));

        JPanel income = card(incomeChart);
        income.setPreferredSize(new Dimension(0, 300));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
        body.add(banner);
        body.add(javax.swing.Box.createVerticalStrut(18));
        body.add(cards);
        body.add(javax.swing.Box.createVerticalStrut(16));
        body.add(charts);
        body.add(javax.swing.Box.createVerticalStrut(16));
        body.add(income);
        for (JComponent part : List.of(banner, cards, charts, income)) {
            part.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        }
        charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        income.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));

        JPanel padded = new JPanel(new BorderLayout());
        padded.setBackground(Theme.BACKGROUND);
        padded.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        padded.add(body, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(padded);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        view.setBackground(Theme.BACKGROUND);
        view.add(scroll, BorderLayout.CENTER);
    }

    /** Puts a chart on a rounded-looking surface so it reads as a card like the indicators. */
    private JPanel card(JComponent chart) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void showStatus(Summary s) {
        long endingSoon = s.expiringSoon();
        List<DonutChart.Slice> slices = new ArrayList<>();
        slices.add(new DonutChart.Slice("Vigentes", s.activeSubscriptions() - endingSoon, Theme.ACCENT));
        slices.add(new DonutChart.Slice("Por vencer", endingSoon, Theme.WARNING));
        slices.add(new DonutChart.Slice("Por iniciar", s.upcoming(), Theme.SUCCESS));
        slices.add(new DonutChart.Slice("Vencidas", s.expired(), Theme.DANGER));
        slices.add(new DonutChart.Slice("Canceladas", s.cancelled(), Theme.TEXT_MUTED));
        statusChart.setData(slices);
    }

    private void showAttendance(Map<DayOfWeek, Long> byWeekday) {
        List<BarChart.Bar> bars = new ArrayList<>();
        long total = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            long count = byWeekday.getOrDefault(day, 0L);
            total += count;
            bars.add(new BarChart.Bar(WEEKDAYS[day.getValue() - 1], count));
        }
        weekdayChart.setData(total == 0 ? List.of() : bars);
    }

    private void showIncome(Map<YearMonth, Long> byMonth) {
        List<LineChart.Point> points = new ArrayList<>();
        long total = 0;
        for (Map.Entry<YearMonth, Long> entry : byMonth.entrySet()) {
            total += entry.getValue();
            points.add(new LineChart.Point(Formats.monthShort(entry.getKey()), entry.getValue() / 1_000_000.0));
        }
        incomeChart.setData(total == 0 ? List.of() : points);
    }
}

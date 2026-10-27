package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.util.List;

/**
 * Small window to look at {@link StatCard}, {@link DonutChart} and
 * {@link BarChart} without starting the whole application. Run its main
 * method from the IDE.
 */
public final class DashboardComponentsPreview {

    private DashboardComponentsPreview() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();

            StatCard members = new StatCard("Socios activos", "members", Theme.ACCENT, v -> String.format("%,d", v));
            members.setCaption("con membresía vigente");
            members.setValue(128);
            StatCard income = new StatCard("Ingresos del mes", "payments", Theme.SUCCESS, v -> "$ " + String.format("%,d", v));
            income.setCaption("octubre 2026");
            income.setValue(6_840_000);
            StatCard expiring = new StatCard("Por vencer", "shield", Theme.WARNING, v -> String.valueOf(v));
            expiring.setCaption("próximos 7 días");
            expiring.setValue(9);
            StatCard today = new StatCard("Asistencia de hoy", "chart", Theme.ACCENT_ALT, v -> String.valueOf(v));
            today.setCaption("entradas registradas");
            today.setValue(47);

            DonutChart donut = new DonutChart("Estado de las membresías");
            donut.setCenterLabel("membresías");
            donut.setData(List.of(
                    new DonutChart.Slice("Activas", 128, Theme.ACCENT),
                    new DonutChart.Slice("Por vencer", 9, Theme.WARNING),
                    new DonutChart.Slice("Vencidas", 31, Theme.DANGER),
                    new DonutChart.Slice("Canceladas", 4, Theme.TEXT_MUTED)));

            BarChart bars = new BarChart("Asistencia por día");
            bars.setData(List.of(
                    new BarChart.Bar("Lun", 62), new BarChart.Bar("Mar", 55), new BarChart.Bar("Mié", 71),
                    new BarChart.Bar("Jue", 48), new BarChart.Bar("Vie", 66), new BarChart.Bar("Sáb", 39),
                    new BarChart.Bar("Dom", 12)));

            JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
            stats.setOpaque(false);
            stats.add(members);
            stats.add(income);
            stats.add(expiring);
            stats.add(today);
            JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
            charts.setOpaque(false);
            charts.add(donut);
            charts.add(bars);

            JPanel content = new JPanel(new java.awt.BorderLayout(0, 16));
            content.setBackground(Theme.BACKGROUND);
            content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            content.add(stats, java.awt.BorderLayout.NORTH);
            content.add(charts, java.awt.BorderLayout.CENTER);

            JFrame frame = new JFrame("Vista previa de los componentes del panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(content);
            frame.setSize(1100, 520);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

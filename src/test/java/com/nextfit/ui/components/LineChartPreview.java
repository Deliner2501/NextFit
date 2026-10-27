package com.nextfit.ui.components;

import com.nextfit.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.util.List;

/**
 * Small window to look at {@link LineChart} without starting the whole
 * application. Run its main method from the IDE.
 */
public final class LineChartPreview {

    private LineChartPreview() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            LineChart weight = new LineChart("Peso", "kg");
            weight.setData(List.of(
                    new LineChart.Point("01 sep", 84.2), new LineChart.Point("15 sep", 83.0),
                    new LineChart.Point("01 oct", 81.6), new LineChart.Point("15 oct", 80.9),
                    new LineChart.Point("24 oct", 79.8)));
            LineChart bmi = new LineChart("IMC", "");
            bmi.setLineColor(Theme.ACCENT_ALT);
            bmi.setEmptyMessage("Aún no hay mediciones");

            JPanel content = new JPanel(new GridLayout(2, 1, 0, 16));
            content.setBackground(Theme.BACKGROUND);
            content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            content.add(weight);
            content.add(bmi);

            JFrame frame = new JFrame("Vista previa de LineChart");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(content);
            frame.setSize(640, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
package visuals;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;

public class LineChart {
    private final String title;
    private final String categoryAxisLabel;
    private final String valueAxisLabel;
    private final DefaultCategoryDataset dataset;
    private final String frameTitle;

    public LineChart(String title, String categoryAxisLabel, String valueAxisLabel, DefaultCategoryDataset dataset, String frameTitle){
        this.title = title;
        this.categoryAxisLabel = categoryAxisLabel;
        this.valueAxisLabel = valueAxisLabel;
        this.dataset = dataset;
        this.frameTitle = frameTitle;
    }
    public void show() {
        JFreeChart chart = ChartFactory.createLineChart(
                this.title,
                this.categoryAxisLabel,
                this.valueAxisLabel,
                this.dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame frame = new JFrame(this.frameTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}


import csv_handler.CsvHandler;

import java.util.ArrayList;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class Main {

    public static void main(String[] args) {
        System.out.println("Sistemas Distribuidos y Paralelos     GPO: 135      HORA: N5");
        System.out.println("Maestro: Carlos Adrián Pérez Cortés\n");

        String[] nombres = { "Hiram Jair Ramírez Sánchez", "Steven Antonio Luna Guel", "Fátima Aglae Castillo Reyes", "Jesús Ángel Cornejo Tamez" };
        int[] matriculas = {  1903589, 1953782, 1966038, 2077825 };

        for(int i = 0; i < 4; i++){
            System.out.printf("%-30s | %-1d | ITS\n", nombres[i], matriculas[i]);
        }
        System.out.println();

        int numberOfIntegersInCSV = 100000;
        CsvHandler csvHandler = new CsvHandler(numberOfIntegersInCSV);
        csvHandler.generateFile();

        boolean showThreadDetail = false;
        boolean showAsTable = true;

        int maxNumberOfThreadsToBenchmark = 5;
        if(showAsTable){
            System.out.printf("%-17s | %-17s | %-17s\n", "Average", "Time", "# Threads");
        }

   	 	DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < maxNumberOfThreadsToBenchmark; i++) {
            ThreadsController threadController = new ThreadsController();
            threadController.setNumberOfThreads(i+1);
            threadController.setShowThreadDetail(showThreadDetail);
            threadController.setShowAsTable(showAsTable);
            threadController.start();
            dataset.setValue(
        		threadController.totalThreadTime,
        		"Total thread time",
        		String.format("%d", threadController.getNumberOfThreads())
           );
            
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
         	    "Tarea 03 - SDyP - Equipo #8",
         	    "Number of threads",
         	    "Threads time (Nanoseconds)",
         	    dataset,
         	    PlotOrientation.VERTICAL,
         	    true, 
         	    true,
         	    false
         	);
         
         ChartPanel chartPanel = new ChartPanel(chart);
         JFrame frame = new JFrame("Grafica Hilos vs Tiempo - EQ8");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.getContentPane().add(chartPanel);
         frame.pack();
         frame.setVisible(true);
    }
}
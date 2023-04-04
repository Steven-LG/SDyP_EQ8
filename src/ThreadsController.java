import csv_handler.CsvHandler;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;

public class ThreadsController {
    private String[] data = {};
    private int numberOfThreads = 8;

    private boolean showThreadDetail = false;

    public void setShowThreadDetail(boolean showThreadDetail) {
        this.showThreadDetail = showThreadDetail;
    }

    private boolean showAsTable = false;

    public void setShowAsTable(boolean showAsTable) {
        this.showAsTable = showAsTable;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
    
    public int getNumberOfThreads() {
		return numberOfThreads;
	}
    
    public double totalThreadTime = 0;
    public double threadsAverage = 0;

    public void start(){
        try {
            this.data = CsvHandler.readFile("randomNumbers.csv");

            ArrayList<ThreadAverageCounter> counters = new ArrayList<ThreadAverageCounter>();

            ArrayList<String[]> slicedData = new ArrayList<String[]>();
            int partitionSize = (this.data.length / this.numberOfThreads) + 1;

            for(int i=0; i<this.data.length; i+=partitionSize){
                slicedData.add(Arrays.copyOfRange(this.data, i, Math.min(this.data.length, i+partitionSize)));
            }

            for(int i = 0; i < slicedData.size(); i++){
                String[] dataSet = slicedData.get(i);
                ThreadAverageCounter tc = new ThreadAverageCounter(i, dataSet);
                tc.setShowThreadDetail(this.showThreadDetail);
                tc.start();
                counters.add(tc);
            }

            boolean allDone;
            do {
                allDone = true;
                for (ThreadAverageCounter t : counters) {
                    if (t.isAlive()) {
                        allDone = false;
                        break;
                    }
                }
            } while (!allDone);
            
            for(ThreadAverageCounter tc: counters){
                this.threadsAverage += tc.avg / this.numberOfThreads;
                this.totalThreadTime += tc.timeOfThread;
            }

            if(!showAsTable){
                System.out.printf("\nAverage -> %f", this.threadsAverage);
                System.out.printf("\nTime of threads -> %f", this.totalThreadTime);
                System.out.printf("\nNumber of threads given -> %d", this.numberOfThreads);
                System.out.printf("\nNumber of Thread Counters -> %d\n", counters.size());
            } else {
                System.out.printf("%-17f | %-17f | %-17d\n", this.threadsAverage, this.totalThreadTime, counters.size());
            }


        }catch (Error err){
            throw new Error(err);
        }
    }
}
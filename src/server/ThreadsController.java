package server;
import java.util.ArrayList;
import java.util.Arrays;

public class ThreadsController {
    private final String[] DATA;
    private final int NUMBER_OF_THREADS;

    public ThreadsController(int NUMBER_OF_THREADS, String[] DATA){
        this.NUMBER_OF_THREADS = NUMBER_OF_THREADS;
        this.DATA = DATA;
    }
    
    public int getNumberOfThreads() {
		return this.NUMBER_OF_THREADS;
	}
    
    public double totalThreadTime = 0;
    public double threadsAverage = 0;

    public void start(){
        try {
            ArrayList<ThreadAverageCounter> counters = new ArrayList<ThreadAverageCounter>();

            ArrayList<String[]> slicedData = new ArrayList<String[]>();
            int partitionSize = (this.DATA.length / this.NUMBER_OF_THREADS) + 1;

            for(int i=0; i<this.DATA.length; i+=partitionSize){
                slicedData.add(Arrays.copyOfRange(this.DATA, i, Math.min(this.DATA.length, i+partitionSize)));
            }

            for(int i = 0; i < slicedData.size(); i++){
                String[] dataSet = slicedData.get(i);
                ThreadAverageCounter tc = new ThreadAverageCounter(i, dataSet);
//                tc.setShowThreadDetail(this.showThreadDetail);
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
                this.threadsAverage += tc.avg / this.NUMBER_OF_THREADS;
                this.totalThreadTime += tc.timeOfThread;
            }
        }catch (Error err){
            throw new Error(err);
        }
    }
}
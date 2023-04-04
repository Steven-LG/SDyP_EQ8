public class ThreadAverageCounter extends Thread{
    private int id;
    private String[] data;
    public double avg = 0;

    private boolean showThreadDetail = false;

    public void setShowThreadDetail(boolean showThreadDetail) {
        this.showThreadDetail = showThreadDetail;
    }

    public double timeOfThread = 0;
    public ThreadAverageCounter(int id, String[] data){
        this.id = id;
        this.data = data;
    }

    public void run(){
        double sum = 0;

        double initialTime = System.nanoTime();
        for(int i = 0; i<this.data.length; i++) {
            sum += Double.parseDouble(this.data[i]);
        }
        double finalTime = System.nanoTime();

        this.avg = sum / this.data.length;
        this.timeOfThread = finalTime - initialTime;

        if(showThreadDetail){
            System.out.println("Time that took Thead #"+id+" to complete -> "+timeOfThread);
            System.out.printf("Average in Thread[%d] -> %f\n", id, avg);
        }
    }
}

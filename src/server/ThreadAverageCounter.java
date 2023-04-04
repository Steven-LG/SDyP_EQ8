package server;

public class ThreadAverageCounter extends Thread{
    private final int ID;
    private final String[] DATA;
    public double avg = 0;

    public double timeOfThread = 0;
    public ThreadAverageCounter(int ID, String[] DATA){
        this.ID = ID;
        this.DATA = DATA;
    }

    public void run(){
        double sum = 0;

        double initialTime = System.nanoTime();
        for(int i = 0; i<this.DATA.length; i++) {
            sum += Double.parseDouble(this.DATA[i]);
        }
        double finalTime = System.nanoTime();

        this.avg = sum / this.DATA.length;
        this.timeOfThread = finalTime - initialTime;
    }
}

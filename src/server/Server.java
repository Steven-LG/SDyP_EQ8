package server;

import csvhandler.CsvHandler;
import org.jfree.data.category.DefaultCategoryDataset;
import visuals.LineChart;

import java.io.*;
import java.net.*;
import java.util.Base64;

public class Server {
    private static final int PORT = 5555;
    private static int connectedClients = 0;
    private static final boolean RECEIVE_BY_NETWORK = true;

    public static void main(String[] args) {
        showTeamMembers();

        if(!RECEIVE_BY_NETWORK){
            (new threadTask()).start();
            return;
        }

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Waiting for connection...");
                Socket clientSocket = serverSocket.accept();
                connectedClients++;
                System.out.println(clientSocket.getInetAddress() + " address just got connected to server!");
                System.out.println("Currently connected devices: "+connectedClients);

                (new threadTask(clientSocket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Client disconnected");
                connectedClients--;
                System.out.println("Currently connected devices: "+connectedClients);
            }
        }
    }

    private static void showTeamMembers(){
        System.out.println("Sistemas Distribuidos y Paralelos     GPO: 135      HORA: N5");
        System.out.println("Maestro: Carlos Adrián Pérez Cortés\n");

        String[] nombres = { "Hiram Jair Ramírez Sánchez", "Steven Antonio Luna Guel", "Fátima Aglae Castillo Reyes", "Jesús Ángel Cornejo Tamez" };
        int[] matriculas = {  1903589, 1953782, 1966038, 2077825 };

        for(int i = 0; i < 4; i++){
            System.out.printf("%-30s | %-1d | ITS\n", nombres[i], matriculas[i]);
        }
        System.out.println();
    }

    static class threadTask extends Thread {
        private Socket clientSocket = null;

        public threadTask(){}
        public threadTask(Socket socket){
            this.clientSocket = socket;
        }

        @Override
        public void run(){
            final boolean RECEIVE_BY_NETWORK = (this.clientSocket == null) ? false : true;

            try{
                String[] data;
                if(RECEIVE_BY_NETWORK){
                    InputStream inputStream = clientSocket.getInputStream();
                    byte[] fileBytes = readCompleteFile(inputStream);
                    byte[] decodedBytes = Base64.getDecoder().decode(fileBytes);
                    String[] decodedCsv = new String(decodedBytes).split(", ");

                    System.out.println("Received CSV file content");
                    data = decodedCsv;
                } else {
                    final String filePath = "src/server/randomNumbers.csv";
                    CsvHandler csvHandler = new CsvHandler(filePath);
                    csvHandler.generateFile();

                    data = CsvHandler.readFile(filePath);
                }


                final int MAX_THREADS_TO_BENCHMARK = 5;

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();

                for (int i = 0; i < MAX_THREADS_TO_BENCHMARK; i++) {
                    ThreadsController threadController = new ThreadsController(i+1, data);
                    threadController.start();
                    dataset.setValue(
                            threadController.totalThreadTime,
                            "Total thread time",
                            String.format("%d", threadController.getNumberOfThreads())
                    );

                }

                LineChart lChart = new LineChart(
                        "Tarea 03 - SDyP - Equipo #8",
                        "Number of threads",
                        "Threads time (Nanoseconds)",
                        dataset,
                        "Grafica Hilos vs Tiempo - EQ8"
                );

                lChart.show();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static byte[] readCompleteFile(InputStream inputStream) throws IOException {
        final int BUFFER_LENGTH = 1024;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_LENGTH];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}

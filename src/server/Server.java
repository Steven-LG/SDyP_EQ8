package server;

import shared.ClientConnection;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Objects;

public class Server {
    private static final int PORT = 5555;
    private static int connectedClients = 0;
    private static final boolean RECEIVE_BY_NETWORK = true;

    private static DynamicTable dynamicTable;
    private static ArrayList<String> activeAddresses;

    public static void main(String[] args) throws IOException {
        showTeamMembers();
        activeAddresses = new ArrayList<String>(){};
        if(!RECEIVE_BY_NETWORK){
            (new ThreadTask()).start();
            return;
        }

        dynamicTable = new DynamicTable();

        dynamicTable.columns.addColumn("Host");
        dynamicTable.columns.addColumn("Procesador");
        dynamicTable.columns.addColumn("Velocidad de Procesador");
        dynamicTable.columns.addColumn("# Nucleos");
        dynamicTable.columns.addColumn("Uso de procesador");
        dynamicTable.columns.addColumn("Capacidad de disco");
        dynamicTable.columns.addColumn("Uso de RAM");
        dynamicTable.columns.addColumn("OS");
        dynamicTable.columns.addColumn("Rank");
        dynamicTable.columns.addColumn("Fecha");

        dynamicTable.initializeTable();

        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            try{
                System.out.println("Waiting for connection...");
                Socket clientSocket = serverSocket.accept();
                connectedClients++;
                System.out.println(clientSocket.getInetAddress() + " address just got connected to server!");
                System.out.println("Currently connected devices: "+connectedClients);

                ThreadTask tTask = new ThreadTask(clientSocket);
                tTask.start();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private static void showTeamMembers(){

        System.out.println();
    }

    static class ThreadTask extends Thread {
        private Socket clientSocket = null;

        public ThreadTask(){}
        public ThreadTask(Socket socket){
            this.clientSocket = socket;
        }

        @Override
        public void run(){
            final boolean RECEIVE_BY_NETWORK = this.clientSocket != null;

            try{
                String[] data;
                if(RECEIVE_BY_NETWORK){
                    ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                    while(true){
                        try{

                            ClientConnection clientConnection = (ClientConnection) objInputStream.readObject();

                            System.out.println("Time of response " + clientConnection.timer);
                            System.out.println(clientConnection.toString());

                            System.out.print("Ram used ");
                            System.out.println(clientConnection.RAMUsed);

                            if(!activeAddresses.contains(clientConnection.ipAddress)){
                                dynamicTable.registers.add(clientConnection);
                                activeAddresses.add(clientConnection.ipAddress);
                            } else {
                                for(ClientConnection cConnection : dynamicTable.registers){
                                    if(Objects.equals(cConnection.ipAddress, clientSocket.getInetAddress().getHostAddress())){
                                        int registerIndex = dynamicTable.registers.indexOf(cConnection);
                                        dynamicTable.registers.set(registerIndex, clientConnection);
                                    }
                                }
                            }

                            if(!clientSocket.isConnected()){
                                System.out.println("Client disconnected");
                                activeAddresses.remove(clientConnection.ipAddress);
                                connectedClients--;
                                break;
                            }

                        } catch (ClassNotFoundException e) {
                            System.out.println("Error receiving object: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
                connectedClients--;
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

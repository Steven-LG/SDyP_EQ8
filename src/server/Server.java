package server;

import client.Client;
import shared.ClientConnection;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.util.*;

import static shared.ClientConnection.getRank;

public class Server {
    public static int PORT = 5555;
    private static int connectedClients = 0;
    private static final boolean RECEIVE_BY_NETWORK = true;
    private static DynamicTable dynamicTable;
    public static HashMap<String, Double> hostsInfo;
    public static String mostUsableHost;

    public static List<Socket> allClientSockets;

    public static ServerSocket serverSocket;

    public static boolean isServerMostUsableHost;

    public static ClientConnection serverSpecs;

    public static void initialize() throws IOException, InterruptedException {
        showTeamMembers();
        hostsInfo = new HashMap<String, Double>(){};
        allClientSockets = new ArrayList<Socket>(){};

        if(!RECEIVE_BY_NETWORK){
            (new ThreadTask()).start();
            return;
        }
        showDynamicTable();
        

        serverSocket = new ServerSocket(PORT);
        while (true) {
            try{
                System.out.println("Waiting for connection...");
                Socket clientSocket = serverSocket.accept();
                allClientSockets.add(clientSocket);
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
            ClientConnection clientAssignedToThread = new ClientConnection();
            try{
                String[] data;
                if(RECEIVE_BY_NETWORK){
                    ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                    while(true){
                        try{
                            ClientConnection clientConnection = (ClientConnection) objInputStream.readObject();
                            clientAssignedToThread = clientConnection;





                            if(hostsInfo.get(clientConnection.ipAddress) == null){
                                dynamicTable.registers.add(clientConnection);
                                hostsInfo.put(clientConnection.ipAddress, clientConnection.rank);
                            } else {
                                for(ClientConnection cConnection : dynamicTable.registers){
                                    if(Objects.equals(cConnection.ipAddress, clientConnection.ipAddress)){
                                        int registerIndex = dynamicTable.registers.indexOf(cConnection);
                                        dynamicTable.registers.set(registerIndex, clientConnection);

                                        mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
                                        for (Socket cSocket : allClientSockets) {
                                            try {
                                                OutputStream outputStream = cSocket.getOutputStream();
                                                PrintWriter writer = new PrintWriter(outputStream, true);
                                                writer.println(mostUsableHost);
                                            } catch (IOException e) {
                                                System.out.println("Error sending message to client: " + e.getMessage());
                                            }
                                        }

                                        System.out.println("MOST USABLE HOST: " + mostUsableHost);

                                        if(!Objects.equals(mostUsableHost, InetAddress.getLocalHost().getHostAddress())){
                                            System.out.println("NOT MOST USABLE HOST "+InetAddress.getLocalHost().getHostAddress());
                                            isServerMostUsableHost = false;


                                            Client.PORT = PORT;
                                            Client.SERVER_ADDRESS = InetAddress.getLocalHost().getHostAddress();
                                            Client.initialize();

                                            Thread.currentThread().interrupt();
                                            System.exit(0);
                                            break;

                                        } else {
                                            isServerMostUsableHost = true;
                                        }
                                    }
                                }
                            }

                            if(!clientSocket.isConnected()){
                                System.out.println("Client disconnected");
                                hostsInfo.remove(clientConnection.ipAddress);
                                dynamicTable.registers.remove(clientConnection);
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
                hostsInfo.remove(clientAssignedToThread.ipAddress);
                dynamicTable.registers.remove(clientAssignedToThread);
                connectedClients--;
                throw new RuntimeException(e);
            }



        }
    }

    private static void showDynamicTable() throws InterruptedException, UnknownHostException {
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

        serverSpecs = new ClientConnection();
        serverSpecs.ipAddress = InetAddress.getLocalHost().getHostAddress();
        serverSpecs.getClientStaticInfo();
        serverSpecs.getCurrentUsage();
        serverSpecs.rank = getRank(serverSpecs);
        dynamicTable.registers.add(serverSpecs);

        UpdateServerInTable updateServerInTable = new UpdateServerInTable();
        updateServerInTable.start();
    }

    static class UpdateServerInTable extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    for (int i = 0; i < dynamicTable.registers.size(); i++) {
                        ClientConnection cConnection = dynamicTable.registers.get(i);
                        if (Objects.equals(cConnection.ipAddress, serverSpecs.ipAddress)) {
                            ClientConnection updatedServerSpecs = new ClientConnection();
                            updatedServerSpecs.ipAddress = InetAddress.getLocalHost().getHostAddress();
                            updatedServerSpecs.getClientStaticInfo();
                            updatedServerSpecs.getCurrentUsage();
                            updatedServerSpecs.rank = getRank(updatedServerSpecs);

                            dynamicTable.registers.set(i, updatedServerSpecs);
                            break;
                        }
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void killServer() throws InterruptedException {
        Thread.sleep(2000);
        Thread.currentThread().interrupt();
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

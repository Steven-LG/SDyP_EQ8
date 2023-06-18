import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.util.*;

public class LauncherV2__2 {

    // Server attributes
    private static ServerSocket serverSocket;
    private static final int SERVER_PORT = 5555;
    private static Object serverLock = new Object();
    private static ArrayList<ServerThread> currentServerHandlers = new ArrayList<ServerThread>();
    private static Thread serverThread;
    private static HostSpecs serverHostSpecs;

    private static DynamicTable dynamicTable;

    // Client attributes
    private static ObjectOutputStream clientObjectOutputStream;
    private static Socket cSocket;
    private static volatile InetAddress mostUsableServer;
    static {
        try {
            mostUsableServer = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    private static Thread clientThread;
    private static Object clientLock = new Object();

    // Launcher attributes
    private static HashMap<String, Integer> hostsInfo;
    private static HashMap<String, Integer> localAddressAndRank = new HashMap<>();
    private static String hostIP;
    private static Integer rank = 10;
    private static HashMap<String, Integer> hosts;

    private static final int UDP_COMMUNICATION_PORT = 1234;
    private static HashMap<String, Integer> mostUsableOne = new HashMap<>();
    private static HashMap<String, Integer> lastOptimalOne = new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        hosts = new HashMap<>();
        serverHostSpecs = new HostSpecs();
        hostsInfo = new HashMap<String, Integer>(){};
        hostIP = InetAddress.getLocalHost().getHostAddress();
        localAddressAndRank.put(hostIP, rank);

        showDynamicTable();
        dynamicTable.frame.setVisible(false);

        Thread UDPEmitterThread = new Thread(() -> {
            try {
                UDPEmitter();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        UDPEmitterThread.start();

        Thread UDPListenerThread = new Thread(()->{
            try {
                UDPListener();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        UDPListenerThread.start();

        serverThread = new Thread(()->{
            while(true){
                try {
                    startServer();
                } catch (IOException e) {
                    System.out.println(Thread.currentThread().getName()+" - "+e.getMessage().toUpperCase());
                }
                if(Thread.currentThread().isInterrupted()){
                    break;
                }
            }
        });
//        serverThread.start();

        Thread serverStartEmitter = new Thread(()->{
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            synchronized (serverLock){
                serverLock.notifyAll();
                System.out.println("SERVER EMITTER UNLOCKED SERVER");
            }
        });
//        serverStartEmitter.start();

        Thread stopTimerThread = new Thread(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                stopServer();
                System.out.println("STOP FUNCTION CALLED");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            createNewServer();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (serverLock){
                serverLock.notifyAll();
                System.out.println("SERVER EMITTER UNLOCKED SERVER");
            }
        });
        //stopTimerThread.start();

        Thread startClientEmitter = new Thread(()->{
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (clientLock){
                clientLock.notifyAll();
            }
        });
        startClientEmitter.start();

        Thread changeToServerListenerThread = new Thread(()->{
            try {
                Thread.sleep(6000);

                clientThread.interrupt();
                if(cSocket.isConnected()){
                    clientObjectOutputStream.close();
                    cSocket.close();
                }
                System.out.println("NEW SERVER BOUT TO BE LAUNCHED");
                Thread.sleep(2000);
                changeClientSocket(mostUsableServer, 5555);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        //changeToServerListenerThread.start();

        System.out.println("Waiting for client unlock");
        synchronized (clientLock){
            clientLock.wait();
        }
        System.out.println("Client socket unlocked");
        changeClientSocket(mostUsableServer, 5555);

        /*
        * To stop server
        * stopServer();
        *
        * To create new server
        * createNewServer();
        *  synchronized (serverLock){
                    serverLock.notifyAll();
                    System.out.println("SERVER EMITTER UNLOCKED SERVER");
                }
        */
    }

    // Launcher Methods
    public static void UDPEmitter() throws IOException, InterruptedException {
        hosts.put(hostIP, rank);

        ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputObjectStream = new ObjectOutputStream(outputByteStream);

        outputObjectStream.writeObject(hosts);
        outputObjectStream.flush();
        byte[] dataToSend = outputByteStream.toByteArray();

        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
        int destinationPort = UDP_COMMUNICATION_PORT;

        // Socket creation
        DatagramSocket UDPSocket = new DatagramSocket();
        DatagramPacket UDPPacket = new DatagramPacket(
                dataToSend,
                dataToSend.length,
                broadcastAddress,
                destinationPort
        );

        while(true){
            UDPSocket.send(UDPPacket);
            System.out.println("UDP Emitter : Packet sent. ");
            Thread.sleep(2000);
        }
    }
    public static void UDPListener() throws IOException, ClassNotFoundException, InterruptedException {
        hosts.put(hostIP, rank);

        byte[] receiveDataBuffer = new byte[1024];

        int UDPReceiverPort = UDP_COMMUNICATION_PORT;

        DatagramSocket UDPSocket = new DatagramSocket(UDPReceiverPort);
        DatagramPacket UDPPacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

        while(true){
            UDPSocket.receive(UDPPacket);

            byte[] receivedBytes = UDPPacket.getData();

            // Deserialize into a hashmap
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(receivedBytes);
            ObjectInputStream inputObjectStream = new ObjectInputStream(inputByteStream);
            HashMap<String, Integer> receivedHashMap = (HashMap<String, Integer>) inputObjectStream.readObject();

            hosts.putAll(receivedHashMap);

            ArrayList<Map.Entry<String, Integer>> entryList = hashMapToArrayList(hosts);

            String mostUsableHost = "";
            int highiestRank = 0;
            for (Map.Entry<String, Integer> entry : entryList) {
                if(entry.getValue() > highiestRank){
                    highiestRank = entry.getValue();
                    mostUsableHost = entry.getKey();
                }
            }

            System.out.println("Received hosts info by UDP");
            System.out.println(hosts);
            System.out.println();

            mostUsableOne.clear();
            mostUsableOne.put(mostUsableHost, highiestRank);

            if(lastOptimalOne.isEmpty()){
                lastOptimalOne.put(mostUsableHost, highiestRank);
            }

            if(!lastOptimalOne.equals(mostUsableOne)){
                lastOptimalOne = mostUsableOne;
                System.out.println("LAST OPTIMAL ONE CHANGED");

                clientThread.interrupt();
                if(cSocket.isConnected()){
                    clientObjectOutputStream.close();
                    cSocket.close();
                }
                System.out.println("NEW SERVER BOUT TO BE LAUNCHED");
                Thread.sleep(2000);

                // CHANGE HERE
                changeClientSocket(InetAddress.getByName(mostUsableOne.keySet().iterator().next()), SERVER_PORT);
            }

            // as Server
            // Change to client
            if(serverThread.isAlive() && !mostUsableOne.equals(localAddressAndRank)){
                stopServer();

                // CHANGE
                changeClientSocket(InetAddress.getByName(mostUsableOne.keySet().iterator().next()), SERVER_PORT);
                Thread.sleep(2000);
                synchronized (clientLock){
                    clientLock.notifyAll();
                }
            }

            // Change from client to server
            if(clientThread.isAlive() && mostUsableOne.equals(localAddressAndRank)){
                System.out.println("CLIENT TO SERVER TRIGGERED");

                //Stop client and launch server
                clientThread.interrupt();
                if(cSocket.isConnected()){
                    clientObjectOutputStream.close();
                    cSocket.close();
                }

                createNewServer();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (serverLock){
                    serverLock.notifyAll();
                    System.out.println("SERVER EMITTER UNLOCKED SERVER");
                }
            }

//            if(!isServer && !lastOptimalOne.equals(mostUsableOne)){
//                // Change socket
//                changeServer = true;
//                System.out.println("CHANGE SERVER AS A CLIENT DONE");
//            }
        }
    }
    public static ArrayList<Map.Entry<String, Integer>> hashMapToArrayList(HashMap<String, Integer> receivedHashMap){
        // HashMap sorting
        ArrayList<Map.Entry<String, Integer>> entryList = new ArrayList<>(receivedHashMap.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> entry1, Map.Entry<String, Integer> entry2) {
                // Comparar los valores de las entradas en orden descendente
                return entry2.getValue().compareTo(entry1.getValue());
            }
        });

        return entryList;
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

        serverHostSpecs.getCurrentUsage();
        dynamicTable.registers.add(serverHostSpecs);

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
                        HostSpecs cConnection = dynamicTable.registers.get(i);
                        if (Objects.equals(cConnection.ipAddress, serverHostSpecs.ipAddress)) {
                            HostSpecs updatedServerSpecs = new HostSpecs();
                            updatedServerSpecs.ipAddress = InetAddress.getLocalHost().getHostAddress();
                            updatedServerSpecs.getCurrentUsage();
                            dynamicTable.registers.set(i, updatedServerSpecs);
                            break;
                        }
                    }

                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    //throw new RuntimeException(e);
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage());
                    //throw new RuntimeException(e);
                }
            }
        }
    }

    // Server Methods
    public static void startServer() throws IOException {
        System.out.println("Server waiting for unlock");
        synchronized (serverLock){
            try {
                serverLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("I'M STILL ALIVE");
        dynamicTable.frame.setVisible(true);

        if(serverSocket == null || serverSocket.isClosed()){
            serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setSoTimeout(0);
        }

        while(true){
            System.out.println("Server unlocked");

            Socket clientSocket = serverSocket.accept();
            System.out.println("EN EFECTO SE BLOQUEA");

            ServerThread newClientHandler = new ServerThread(clientSocket);
            currentServerHandlers.add(newClientHandler);
            newClientHandler.start();

            if(Thread.currentThread().isInterrupted()){
                System.out.println("SERVER WERE INTERRUMPTED");
                break;
            }
        }
    }
    public static class ServerThread extends Thread {
        public Socket clientSocket;
        public ServerThread(Socket gSocket){
            clientSocket = gSocket;
        }
        @Override
        public void run() {
            System.out.println("Administrando a cliente "+this.clientSocket);
            HostSpecs clientAssignedToThread = null;

            try{
                clientAssignedToThread = new HostSpecs();
                ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                while(true){
                    System.out.println("CLIENT HANDLER ALIVE");
                    try{
                        HostSpecs receivedClientSpecs = (HostSpecs) objInputStream.readObject();
                        clientAssignedToThread = receivedClientSpecs;
                        System.out.print("Received client specs: ");
                        System.out.println(receivedClientSpecs);

                        // If the host is new connection
                        if (receivedClientSpecs.ipAddress != null && !hostsInfo.containsKey(receivedClientSpecs.ipAddress)) {

                            System.out.print("HOSTS INFO");
                            System.out.println(hostsInfo);

                            System.out.print("RECEIVED SPECS");
                            System.out.println(receivedClientSpecs);

                            hostsInfo.put(receivedClientSpecs.ipAddress, receivedClientSpecs.rank);
                            dynamicTable.registers.add(receivedClientSpecs);
                        } else {
                            // update host info in table
                            for (HostSpecs hSpecs : dynamicTable.registers) {
                                if (Objects.equals(hSpecs.ipAddress, receivedClientSpecs.ipAddress)) {
                                    int registerIndex = dynamicTable.registers.indexOf(hSpecs);
                                    dynamicTable.registers.set(registerIndex, receivedClientSpecs);
                                }
                            }
                        }

                        if (!clientSocket.isConnected()) {
                            System.out.println("Client disconnected");

                            clientSocket.close();
                            break;
                        }
                    } catch (Exception e){
                        System.out.println("ERROR INTERNO DE EXPECIÓN");
                        System.out.println(e.getMessage());

                        System.out.println("Client disconnected");
                        if(e.getMessage() != null){
                            System.out.println(e.getMessage().toUpperCase() + " - " + clientAssignedToThread.ipAddress);
                        }
                        clientSocket.close();
                        break;

                    }

                }
            } catch (IOException e) {
                System.out.println("ERROR EXTERNO DE EXPECIÓN");
                System.out.println(e.getMessage());
            }
        }
    }
    public static void createNewServer(){
        Thread newServer = new Thread(()->{
            try {
                startServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        newServer.start();
    }
    public static void stopServer() throws IOException {
        if(serverThread == null){
            return;
        }
        serverThread.interrupt();
        for(ServerThread serverHandler : currentServerHandlers){
            try {
                serverHandler.clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            serverHandler.interrupt();
        }
        serverSocket.close();
        dynamicTable.frame.setVisible(false);
    }

    // Client Methods
    static void changeClientSocket(InetAddress NEW_ADDRESS, int NEW_PORT) throws IOException, InterruptedException {
        try{
            if(cSocket != null && cSocket.isConnected()){
                clientObjectOutputStream.close();
                cSocket.close();
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        cSocket = new Socket(NEW_ADDRESS, NEW_PORT);
        clientObjectOutputStream = new ObjectOutputStream(cSocket.getOutputStream());

        System.out.println("CLIENT SOCKET CHANGED DONE");
        clientThread = new Thread(()->{
            try {
                makeConstantRequests();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
                System.out.println(Thread.currentThread().getName()+" - "+e.getMessage().toUpperCase());
            }
        });
        clientThread.start();


    }
    static synchronized void makeConstantRequests() throws IOException, InterruptedException {
        while(true){
            if(Thread.currentThread().isInterrupted()){
                System.out.println("CLIENT THREAD WERE INTERRUPTED");
                break;
            }

            HostSpecs clientMessage = new HostSpecs();
            clientMessage.getCurrentUsage();

            clientObjectOutputStream.writeObject(clientMessage);
            System.out.println("Object sent");
            clientObjectOutputStream.flush();
            Thread.sleep(1000);

//            if(changeServer.get()){
//                break;
//            }


        }
    }

}

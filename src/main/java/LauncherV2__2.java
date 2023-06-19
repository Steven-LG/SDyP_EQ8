import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static Integer rank = 0;
    private static ConcurrentHashMap<String, Integer> hosts;

    private static final int UDP_COMMUNICATION_PORT = 1234;
    private static HashMap<String, Integer> mostUsableOne = new HashMap<>();
    private static HashMap<String, Integer> lastOptimalOne = new HashMap<>();

    private static HashMap<String, LocalTime> hostsTimeRegister = new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        hosts = new ConcurrentHashMap<>();
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
//                throw new RuntimeException(e);
                System.out.println(Thread.currentThread().getName()+" "+e.getMessage().toUpperCase());
            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
                System.out.println(Thread.currentThread().getName()+" "+e.getMessage().toUpperCase());

            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
                System.out.println(Thread.currentThread().getName()+" "+e.getMessage().toUpperCase());

            }
        });
        UDPListenerThread.start();

        serverThread = serverThreadSingleton.getInstance();
//        serverThread = new Thread(()->{
//            while(true){
//                try {
//                    startServer();
//                } catch (IOException e) {
//                    System.out.println(Thread.currentThread().getName()+" - "+e.getMessage().toUpperCase());
//                }
//                if(Thread.currentThread().isInterrupted()){
//                    break;
//                }
//            }
//        });
//        serverThread.start();

        Thread serverStartEmitter = new Thread(()->{
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
//        startClientEmitter.start();

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

//        System.out.println("Waiting for client unlock");
//        synchronized (clientLock){
//            clientLock.wait();
//        }
//        System.out.println("Client socket unlocked");
//        changeClientSocket(InetAddress.getByName("localhost"), 5555);

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

        Thread changeRankThread = new Thread(()->{
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            hosts.put(hostIP, -1);
            System.out.println("NEGATIVE RANK UPDATED");
            System.out.println(hosts);
        });
        //changeRankThread.start();

        while(true){
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputObjectStream = new ObjectOutputStream(outputByteStream);

            HashMap<String, Integer> localhostHashMap = new HashMap<>();
            localhostHashMap.put(hostIP, rank);
            outputObjectStream.writeObject(localhostHashMap);
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

            UDPSocket.send(UDPPacket);
            System.out.println("UDP Emitter : Packet sent. ");
            Thread.sleep(2000);
        }
    }
    public static void UDPListener() throws IOException, ClassNotFoundException, InterruptedException {
        //hosts.put(hostIP, rank);

        byte[] receiveDataBuffer = new byte[1024];

        int UDPReceiverPort = UDP_COMMUNICATION_PORT;
        DatagramSocket UDPSocket = new DatagramSocket(UDPReceiverPort);

        AtomicBoolean clientToServerDone = new AtomicBoolean(false);
        AtomicBoolean serverToClientDone = new AtomicBoolean(false);

        AtomicBoolean clientThreadLaunched = new AtomicBoolean(false);
        AtomicBoolean serverThreadLaunched = new AtomicBoolean(false);


        while(true){
            DatagramPacket UDPPacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

            UDPSocket.receive(UDPPacket);

            byte[] receivedBytes = UDPPacket.getData();

            // Deserialize into a hashmap
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(receivedBytes);
            ObjectInputStream inputObjectStream = new ObjectInputStream(inputByteStream);
            HashMap<String, Integer> receivedHashMap = (HashMap<String, Integer>) inputObjectStream.readObject();
            hostsTimeRegister.put(hostIP, LocalTime.now());

            System.out.println("RECEIVED ONES");
            for (Map.Entry<String, Integer> entry : receivedHashMap.entrySet()) {
//                String key = entry.getKey();
                hostsTimeRegister.put(entry.getKey(), LocalTime.now());
                System.out.println(entry.getKey() + " / " + entry.getValue());
            }


            hosts.putAll(receivedHashMap);


            //PERA
            LocalTime currentTime = LocalTime.now();
            try{
                for (Map.Entry<String, LocalTime> entry : hostsTimeRegister.entrySet()) {
                    Duration difference = Duration.between(currentTime, entry.getValue()).abs();
                    System.out.println(entry.getKey() + " -> " + difference.toSeconds());

                    if(difference.toSeconds() >= 7){
                        hosts.remove(entry.getKey());
                        hostsTimeRegister.remove(entry.getKey());
                    }
                }
            }catch (Exception e){
                System.out.println(e.getMessage().toUpperCase() + " - " + "hostsTimeRegister loop");
            }

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

            System.out.println("REGISTER OF HASHMAPS");
            System.out.println(hostsTimeRegister);
            System.out.println();

            mostUsableOne.clear();
            mostUsableOne.put(mostUsableHost, highiestRank);

            if(lastOptimalOne.isEmpty()){
                lastOptimalOne.put(mostUsableHost, highiestRank);
            }

            System.out.println("MOST USABLE ONE");
            System.out.println(mostUsableOne.keySet().iterator().next());
            System.out.println();

            System.out.println("LAST OPTIMAL ONE");
            System.out.println(lastOptimalOne.keySet().iterator().next());
            System.out.println();

            if(!lastOptimalOne.equals(mostUsableOne)){
//                lastOptimalOne = mostUsableOne.keySet().iterator().next();

                for (HashMap.Entry<String, Integer> entry : mostUsableOne.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    lastOptimalOne.put(key, value);
                }
                System.out.println("LAST OPTIMAL ONE CHANGED");


                // Server to Client - WORKS
//            if(serverThread.isAlive() && !mostUsableOne.equals(localAddressAndRank)){
                if(!serverToClientDone.get() && !mostUsableOne.equals(localAddressAndRank)){
                    clientToServerDone.set(false);
                    serverThreadLaunched.set(false);
                    System.out.println("Server to Client - USE CASE");
                    Thread useCase1 = new Thread(()->{
                        System.out.println("USE CASE 1 THREAD LAUNCHED");
                        try {
                            stopServer();


                            // CHANGE - HERE GOES TO INFINITE
                            if(!clientThreadLaunched.get()){
                                changeClientSocket(InetAddress.getByName(mostUsableOne.keySet().iterator().next()), SERVER_PORT);
                            }

                            Thread.sleep(2000);
                            synchronized (clientLock){
                                clientLock.notifyAll();
                            }
                            serverToClientDone.set(true);
                            clientThreadLaunched.set(true);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    useCase1.start();

                }
            }

            // Client to Server - WORKS
//            if((clientThread != (null) && clientThread.isAlive()) && mostUsableOne.equals(localAddressAndRank)){
            if(!clientToServerDone.get() && mostUsableOne.equals(localAddressAndRank)){
                System.out.println("CLIENT TO SERVER TRIGGERED");
                serverToClientDone.set(false);
                clientThreadLaunched.set(false);
                Thread useCase2 = new Thread(()->{
                    System.out.println(Thread.currentThread().getName() + " // " + " NEW SERVER");
                    if(clientThread != null){
                        clientThread.interrupt();
                    }

                    if((cSocket != null) && cSocket.isConnected()){
                        try {
                            clientObjectOutputStream.close();
                            cSocket.close();
                        } catch (IOException e) {
                            //throw new RuntimeException(e);
                        }
                    }

                    if(!serverThreadLaunched.get()){
                        createNewServer();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized (serverLock){
                        serverLock.notifyAll();
                        System.out.println("SERVER EMITTER UNLOCKED SERVER");
                    }
                    clientToServerDone.set(true);
                    serverThreadLaunched.set(true);
                });
                useCase2.start();
            }

        }
    }

    public static ArrayList<Map.Entry<String, Integer>> hashMapToArrayList(ConcurrentHashMap<String, Integer> receivedHashMap){
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
        Thread newServer = serverThreadSingleton.getInstance();
        newServer.start();
    }
    public static void stopServer() throws IOException {
        if(serverThread == null || serverSocket == null){
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

        clientThread = clientThreadSingleton.getInstance();
        clientThread.start();
    }
    private static class serverThreadSingleton extends Thread{
        private static final ThreadLocal<serverThreadSingleton> threadLocalInstance = new ThreadLocal<>();
        private serverThreadSingleton(){}
        public static serverThreadSingleton getInstance(){
            if(threadLocalInstance.get() == null){
                threadLocalInstance.set(new serverThreadSingleton());
            }
            return threadLocalInstance.get();
        }

        @Override
        public void run() {
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
        }
    }
    private static class clientThreadSingleton extends Thread{
        private static final ThreadLocal<clientThreadSingleton> threadLocalInstance = new ThreadLocal<>();
        private clientThreadSingleton(){}
        public static clientThreadSingleton getInstance(){
            if(threadLocalInstance.get() == null){
                threadLocalInstance.set(new clientThreadSingleton());
            }
            return threadLocalInstance.get();
        }
        @Override
        public void run() {
            try {
                makeConstantRequests();
            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
                System.out.println(Thread.currentThread().getName()+" - "+e.getMessage().toUpperCase());
            }
        }
    }

    static synchronized void makeConstantRequests() throws InterruptedException {
        while(true){
            if(Thread.currentThread().isInterrupted()){
                System.out.println("CLIENT THREAD WERE INTERRUPTED");
                break;
            }

            HostSpecs clientMessage = null;
            try {
                clientMessage = new HostSpecs();
                clientMessage.getCurrentUsage();

                clientObjectOutputStream.writeObject(clientMessage);
                System.out.println("Object sent");
                clientObjectOutputStream.flush();
                Thread.sleep(1000);

            } catch (UnknownHostException e) {
                System.out.println("makeConstantRequests - UnknownHostException");
                System.out.println(Thread.currentThread().getName() + " - " + e.getMessage().toUpperCase());
                break;

            } catch (IOException e) {
                System.out.println("makeConstantRequests - IOException");
                System.out.println(Thread.currentThread().getName() + " - " + e.getMessage().toUpperCase());


                // REDIRECTION TO MOST USABLE NOW

                break;
            }
        }
    }
}

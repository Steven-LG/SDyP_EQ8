import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Launcher3 {

    private static String hostIP;

    static {
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static Integer rank = 9;
    private static final int PORT = 1234;
    private static HashMap<String, Integer> localAddressAndRank = new HashMap<>();;
    private static HashMap<String, Integer> lastOptimalOne = new HashMap<>();;
    private static HashMap<String, Integer> mostUsableOne = new HashMap<>();;
    private static boolean changeServer = false;
    private static boolean isServer = false;

    private static HashMap<String, Integer> hosts;





    // Last modification
    private static final int SERVER_PORT = 5555;
    private static Object lock = new Object();
    private static HashMap<String, Integer> hostsInfo;
    private static HostSpecs hostSpecs;
    private static DynamicTable dynamicTable;
    public static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException, InterruptedException {
        hostSpecs = new HostSpecs();
        hostsInfo = new HashMap<String, Integer>(){};

        System.out.println("Hello world!");
        localAddressAndRank.put(hostIP, rank);

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
            }
        });
        UDPListenerThread.start();

        showDynamicTable();
        dynamicTable.frame.setVisible(false);

        


        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(isServer){
            startServer();
        } else {
            startClient();
        }
//        Thread serverThread = new Thread(() -> {
//
//        });

//        Thread clientThread = new Thread(() -> {
//
//        });

//        serverThread.start();
//        clientThread.start();

        // Iniciar receptor UDP
//        Thread UDPThreadListener = new Thread(() -> {
//            while(true){
//                try {
//                    DatagramSocket socket = new DatagramSocket(12345);
//                    showDynamicTable();
//                    dynamicTable.frame.setVisible(true);
//
//                    byte[] receiveBuffer = new byte[1024];
//
//                    while (true) {
//                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
//                        socket.receive(receivePacket);
//                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
//                        System.out.println("Message from server: " + receivedMessage);
//
//                        // Server and most usable one
//                        if(startServer && hostSpecs.ipAddress.equals(receivedMessage)){
//                            System.out.println("SERVER IS MOST USABLE ONE ");
//                        }
//
//                        // Server and is not the most usable one
//                        if(startServer && !hostSpecs.ipAddress.equals(receivedMessage)){
//                            synchronized (lock) {
//                                // Should be changed to client and connect to the most usable one
//                                startServer = false;
//                                lock.notifyAll();
//                            }
//                        }
//
//                        // Client and need to be server
//                        if(!startServer && hostSpecs.ipAddress.equals(receivedMessage)){
//                            synchronized (lock) {
//                                // Change to server and close socket that were from client
//                                startServer = true;
//                                changeServer = false;
//                                lock.notifyAll();
//                            }
//                        }
//
//                        // Client and dont need to be server
//                        if(!startServer && !hostSpecs.ipAddress.equals(receivedMessage)){
//                            synchronized (lock) {
//                                // If the most usable one don't correspond to the received one, then make a change in socket
//                                startServer = false;
//                                changeServer = true;
//                                lock.notifyAll();
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//        });
//
//        UDPThreadListener.start();
    }

    public static void UDPEmitter() throws IOException, InterruptedException {
        hosts = new HashMap<>();
        hosts.put(hostIP, rank);

        ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputObjectStream = new ObjectOutputStream(outputByteStream);

        outputObjectStream.writeObject(hosts);
        outputObjectStream.flush();
        byte[] dataToSend = outputByteStream.toByteArray();

        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
        int destinationPort = PORT;

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

    public static void UDPListener() throws IOException, ClassNotFoundException {
        hosts.put(hostIP, rank);

        byte[] receiveDataBuffer = new byte[1024];

        int UDPReceiverPort = PORT;

        DatagramSocket UDPSocket = new DatagramSocket(UDPReceiverPort);
        DatagramPacket UDPPacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

        while(true){
            UDPSocket.receive(UDPPacket);

            byte[] receivedBytes = UDPPacket.getData();

            // Deserialize into a hashmap
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(receivedBytes);
            ObjectInputStream inputObjectStream = new ObjectInputStream(inputByteStream);
            HashMap<String, Integer> receivedHashMap = (HashMap<String, Integer>) inputObjectStream.readObject();


//            System.out.println("Before merge");
//            System.out.println(hosts);

            hosts.putAll(receivedHashMap);

//            System.out.println("Merged HashMap");
//            System.out.println(hosts);


            ArrayList<Map.Entry<String, Integer>> entryList = hashMapToArrayList(hosts);

//            System.out.println("Received data:");
            String mostUsableHost = "";
            int highiestRank = 0;
            for (Map.Entry<String, Integer> entry : entryList) {
                //System.out.println(entry.getKey() + ": " + entry.getValue());

                if(entry.getValue() > highiestRank){
                    highiestRank = entry.getValue();
                    mostUsableHost = entry.getKey();
                }
            }

//            System.out.println("Most usable one");
//            System.out.println(mostUsableHost);

            mostUsableOne.put(mostUsableHost, highiestRank);

            if(lastOptimalOne.isEmpty()){
                lastOptimalOne.put(mostUsableHost, highiestRank);
            }

            if(!lastOptimalOne.equals(mostUsableOne)){
                lastOptimalOne = mostUsableOne;

                System.out.println("LAST OPTIMAL ONE CHANGED");

//                synchronized (lock) {
//                    // boolean
//                    lock.notifyAll();
//                }
            }

            // As server
            if(isServer && !mostUsableOne.equals(localAddressAndRank)){
                // Change to client


                System.out.println("SERVER TO CLIENT DONE");

                synchronized (lock) {
                    isServer = false;
                    // boolean
                    lock.notifyAll();
                }
            }

            // As client
            if(!isServer && mostUsableOne.equals(localAddressAndRank)){


                System.out.println("CLIENT TO SERVER DONE");

                synchronized (lock) {
                    // boolean
                    // Change to server
                    isServer = true;
                    changeServer = false;

                    lock.notifyAll();
                }
            }

            if(!isServer && !lastOptimalOne.equals(mostUsableOne)){
                // Change socket
                changeServer = true;
                System.out.println("CHANGE SERVER AS A CLIENT DONE");
            }
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
//
    public static class ServerThread extends Thread {
        public Socket clientSocket;
        public ServerThread(Socket gSocket){
            clientSocket = gSocket;
        }
        @Override
        public void run() {
            System.out.println("Administrando a cliente "+this.clientSocket);
            HostSpecs clientAssignedToThread = null;
            try {
                clientAssignedToThread = new HostSpecs();
                ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                while (isServer) {
                    try {
                        HostSpecs receivedClientSpecs = (HostSpecs) objInputStream.readObject();
                        clientAssignedToThread = receivedClientSpecs;
                        System.out.print("Received client specs: ");
                        System.out.println(receivedClientSpecs);

//                        // If the host is new connection
                        if (hostsInfo.get(receivedClientSpecs.ipAddress) == null) {
                            dynamicTable.registers.add(receivedClientSpecs);
                            hostsInfo.put(receivedClientSpecs.ipAddress, receivedClientSpecs.rank);
                        } else {
//                            // update host info in table
                            for (HostSpecs hSpecs : dynamicTable.registers) {
                                if (Objects.equals(hSpecs.ipAddress, receivedClientSpecs.ipAddress)) {
                                    int registerIndex = dynamicTable.registers.indexOf(hSpecs);
                                    dynamicTable.registers.set(registerIndex, receivedClientSpecs);
                                }
                            }
                        }

                        if (!clientSocket.isConnected()) {
                            System.out.println("Client disconnected");
                            hostsInfo.remove(receivedClientSpecs.ipAddress);
                            dynamicTable.registers.remove(receivedClientSpecs);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
//
                }
//
            } catch (IOException e) {
                System.out.println("Client disconnected");
                System.out.println(e.getMessage().toUpperCase() + " - " + clientAssignedToThread.ipAddress);

                hostsInfo.remove(clientAssignedToThread.ipAddress);
                dynamicTable.registers.remove(clientAssignedToThread);
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

        hostSpecs.getCurrentUsage();
        dynamicTable.registers.add(hostSpecs);

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
                        if (Objects.equals(cConnection.ipAddress, hostSpecs.ipAddress)) {
                            HostSpecs updatedServerSpecs = new HostSpecs();
                            updatedServerSpecs.ipAddress = InetAddress.getLocalHost().getHostAddress();
                            updatedServerSpecs.getCurrentUsage();
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


    private static void startServer(){
        while (true) {
//            synchronized (lock) {
//                while (!isServer) {
//                    try {
//                        lock.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                if(!isServer){
                    dynamicTable.frame.setVisible(false);
                    break;
                }

                dynamicTable.frame.setVisible(true);
                System.out.println("Running as server");
                System.out.println("Waiting for connection...");
                try {
                    Socket clientSocket = serverSocket.accept();
                    if(isServer){
                        throw new Exception("SERVER CANCELED");
                    }
                    ServerThread newClientHandler = new ServerThread(clientSocket);
                    newClientHandler.start();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

//                lock.notifyAll();
//                try {
//                    lock.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
//        }
    }
    private static void startClient(){
        while (true) {
//            synchronized (lock) {
//                while (isServer) {
//                    try {
//                        lock.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            if(isServer){
                dynamicTable.frame.setVisible(true);
                break;
            }



            dynamicTable.frame.setVisible(false);


                System.out.println("Running as client");
//                Thread threadTask = new Thread(() -> {
                    //String mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
                    //String mostUsableHost = "localhost";

                    try {
//                        while(hosts.size() == 0){
//                            System.out.println("Waiting for other host to join...");
//                        }
                        Socket cSocket = new Socket("25.3.224.138", SERVER_PORT);
                        System.out.println("SOCKET IS CONNECTED "+cSocket.isConnected());
                        AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(cSocket.getOutputStream()));

                        while(!isServer){
                            HostSpecs clientMessage = new HostSpecs();
                            clientMessage.getCurrentUsage();

                            objOutputStream.get().writeObject(clientMessage);
                            System.out.println("Object sent");
                            objOutputStream.get().flush();
                            Thread.sleep(2000);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
//                });
//                threadTask.start();

//                lock.notifyAll();
//                try {
//                    lock.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
//        }
    }

}
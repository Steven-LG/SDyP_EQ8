import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Launcher {
    private static String hostIP;

    static {
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static Integer rank = 10;
    private static final int PORT = 1234;
    private static HashMap<String, Integer> localAddressAndRank = new HashMap<>();;
    private static HashMap<String, Integer> lastOptimalOne = new HashMap<>();;
    private static HashMap<String, Integer> mostUsableOne = new HashMap<>();;
    private static boolean changeServer = false;
    private static boolean isServer = false;
    private static HashMap<String, Integer> hosts;
    private static final int SERVER_PORT = 5555;
    private static Object lock = new Object();
    private static HashMap<String, Integer> hostsInfo;
    private static HostSpecs hostSpecs;
    private static DynamicTable dynamicTable;
    public static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException, InterruptedException {
        hosts = new HashMap<>();
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

        Thread serverThread = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    while (!isServer) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    dynamicTable.frame.setVisible(true);
                    System.out.println("Running as server");

                    System.out.println("Waiting for connection...");
                    try {
                        serverSocket = new ServerSocket(SERVER_PORT);

                        while(!changeServer){
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("EN EFECTO SE BLOQUEA");
                            ServerThread newClientHandler = new ServerThread(clientSocket);
                            newClientHandler.start();
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    lock.notifyAll();
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Thread clientThread = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    while (isServer) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    dynamicTable.frame.setVisible(false);

                    System.out.println("Running as client");
                        try {
                            System.out.print("most usable one");
                            System.out.println(mostUsableOne.keySet().iterator().next());

                            Socket cSocket = new Socket(mostUsableOne.keySet().iterator().next(), SERVER_PORT);

                            System.out.println("SOCKET IS CONNECTED "+cSocket.isConnected());
                            AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(cSocket.getOutputStream()));

                            while(!isServer){
                                HostSpecs clientMessage = new HostSpecs();
                                clientMessage.getCurrentUsage();

                                objOutputStream.get().writeObject(clientMessage);
                                System.out.println("Object sent");
                                objOutputStream.get().flush();
                                Thread.sleep(1000);

                                if(changeServer){
                                    break;
                                }
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                    lock.notifyAll();
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        serverThread.start();
        clientThread.start();
    }

    public static void UDPEmitter() throws IOException, InterruptedException {

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

            mostUsableOne.clear();
            mostUsableOne.put(mostUsableHost, highiestRank);

            if(lastOptimalOne.isEmpty()){
                lastOptimalOne.put(mostUsableHost, highiestRank);
            }

            if(!lastOptimalOne.equals(mostUsableOne)){
                lastOptimalOne = mostUsableOne;

                System.out.println("LAST OPTIMAL ONE CHANGED");

                serverSocket.close();

                synchronized (lock) {
                    // boolean
                    //changeServer = true;
                    lock.notifyAll();
                }
            }

            // As server - WORKS
            if(isServer && !mostUsableOne.equals(localAddressAndRank)){
                // Change to client
                System.out.println("SERVER TO CLIENT DONE");

                synchronized (lock) {
                    isServer = false;
                    // boolean
                    lock.notifyAll();
                }
            }

            // As client - WORKS
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

                        System.out.print("SOCKET CLIENT IS CONNECTED? ");
                        System.out.println(clientSocket.isConnected());

                        if (!clientSocket.isConnected()) {
                            System.out.println("Client disconnected");

                            hostsInfo.remove(receivedClientSpecs.ipAddress);
                            dynamicTable.registers.remove(receivedClientSpecs);
                            clientSocket.close();
                        }

//
                    } catch (Exception e){
                        System.out.println("ERROR INTERNO DE EXPECIÓN");
                        System.out.println(e.getMessage());

                        System.out.println("Client disconnected");
                        if(e.getMessage() != null){
                            System.out.println(e.getMessage().toUpperCase() + " - " + clientAssignedToThread.ipAddress);
                        }
                        clientSocket.close();
                        hostsInfo.remove(clientAssignedToThread.ipAddress);
                        dynamicTable.registers.remove(clientAssignedToThread);
                        break;

                    }

                }
            } catch (IOException e) {
                System.out.println("ERROR EXTERNO DE EXPECIÓN");
                System.out.println(e.getMessage());
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
                    System.out.println(e.getMessage());
                    //throw new RuntimeException(e);
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage());
                    //throw new RuntimeException(e);
                }
            }
        }
    }
}
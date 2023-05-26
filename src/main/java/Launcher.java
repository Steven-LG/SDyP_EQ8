import shared.ClientConnection;
import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Launcher {
    private static final int SERVER_PORT = 5555;
    private static Object lock = new Object();
    private static boolean startServer = true;
    private static boolean changeServer = false;
    private static HashMap<String, Integer> hostsInfo;
    private static HostSpecs hostSpecs;
    private static DynamicTable dynamicTable;


    public static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        hostSpecs = new HostSpecs();
        hostsInfo = new HashMap<String, Integer>(){};

        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread serverThread = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    while (!startServer) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.println("Running as server");
                    System.out.println("Waiting for connection...");
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ServerThread newClientHandler = new ServerThread(clientSocket);
                        newClientHandler.start();
                    } catch (IOException e) {
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
                    while (startServer) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.println("Running as server");
                    Thread threadTask = new Thread(() -> {
                        //String mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
                        String mostUsableHost = "localhost";

                        try {
                            Socket cSocket = new Socket(mostUsableHost, 5555);
                            AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(cSocket.getOutputStream()));

                            while(!startServer){
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
                    });
                    threadTask.start();

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

        // Iniciar receptor UDP
        Thread UDPThreadListener = new Thread(() -> {
            while(true){
                try {
                    DatagramSocket socket = new DatagramSocket(12345);
                    showDynamicTable();
                    dynamicTable.frame.setVisible(true);

                    byte[] receiveBuffer = new byte[1024];

                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);
                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Message from server: " + receivedMessage);

                        // Server and most usable one
                        if(startServer && hostSpecs.ipAddress.equals(receivedMessage)){
                            System.out.println("SERVER IS MOST USABLE ONE ");
                        }

                        // Server and is not the most usable one
                        if(startServer && !hostSpecs.ipAddress.equals(receivedMessage)){
                            synchronized (lock) {
                                startServer = false;
                                lock.notifyAll();
                            }
                        }

                        // Client and need to be server
                        if(!startServer && hostSpecs.ipAddress.equals(receivedMessage)){
                            synchronized (lock) {
                                startServer = true;
                                changeServer = false;
                                lock.notifyAll();
                            }
                        }

                        // Client and dont need to be server
                        if(!startServer && !hostSpecs.ipAddress.equals(receivedMessage)){
                            synchronized (lock) {
                                startServer = false;
                                changeServer = true;
                                lock.notifyAll();
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        UDPThreadListener.start();
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

    public static class ServerThread extends Thread {
        public Socket clientSocket;
        public ServerThread(Socket gSocket){
            clientSocket = gSocket;
        }

        @Override
        public void run() {
            HostSpecs clientAssignedToThread = null;
            try {
                clientAssignedToThread = new HostSpecs();
                ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                while (startServer) {
                    try {
                        HostSpecs receivedClientSpecs = (HostSpecs) objInputStream.readObject();
                        clientAssignedToThread = receivedClientSpecs;
                        System.out.print("Received client specs: ");
                        System.out.println(receivedClientSpecs);
                        // If the host is new connection
                        if (hostsInfo.get(receivedClientSpecs.ipAddress) == null) {
                            dynamicTable.registers.add(receivedClientSpecs);
                            hostsInfo.put(receivedClientSpecs.ipAddress, receivedClientSpecs.rank);
                        } else {
                            // Check for the most usable host
                            // update host info in table
                            String mostUsableHost = "";
                            for (HostSpecs hSpecs : dynamicTable.registers) {
                                if (Objects.equals(hSpecs.ipAddress, receivedClientSpecs.ipAddress)) {
                                    int registerIndex = dynamicTable.registers.indexOf(hSpecs);
                                    dynamicTable.registers.set(registerIndex, receivedClientSpecs);

                                    // Gets most usable host, and then it should pass it through UDP to clients
                                    mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
                                    try{
                                        DatagramSocket UDPBroadcastSocket = new DatagramSocket();
                                        UDPBroadcastSocket.setBroadcast(true);
                                        byte[] buffer = mostUsableHost.getBytes();
                                        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                                        int clientPort = 12345;
                                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, clientPort);
                                        UDPBroadcastSocket.send(packet);

                                    } catch (SocketException e) {
                                        e.printStackTrace();
                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            System.out.print("Most usable host:");
                            System.out.println(mostUsableHost);
                        }


                        if (!clientSocket.isConnected()) {
                            System.out.println("Client disconnected");
                            hostsInfo.remove(receivedClientSpecs.ipAddress);
                            dynamicTable.registers.remove(receivedClientSpecs);
//                                        break;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }

            } catch (IOException e) {
                System.out.println("Client disconnected");
                System.out.println(e.getMessage().toUpperCase() + " - " + clientAssignedToThread.ipAddress);

                hostsInfo.remove(clientAssignedToThread.ipAddress);
                dynamicTable.registers.remove(clientAssignedToThread);
            }
//                        }
        }
    }

}



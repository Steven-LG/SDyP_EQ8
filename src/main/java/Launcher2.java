import client.Client;
import server.Server;
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

public class Launcher2 {
    private static final int SERVER_PORT = 5556;
    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static Object lock = new Object();
    private static boolean startServer = false;
    private static HashMap<String, Integer> hostsInfo;
    private static HostSpecs hostSpecs;
    private static DynamicTable dynamicTable;

    public static void main(String[] args) throws InterruptedException, IOException {
        hostSpecs = new HostSpecs();

        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(SERVER_PORT));

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

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

                    System.out.println("Ejecutando Hilo A");
                    hostsInfo = new HashMap<String, Integer>(){};

                    Thread threadTask = new Thread(() -> {
                        HostSpecs clientAssignedToThread = null;

                        try {
                            System.out.println("Waiting for connection...");
                            SocketChannel clientSocketChannel = null;
                            while(clientSocketChannel == null){
                                clientSocketChannel = serverChannel.accept();
                                Thread.sleep(10);
                            }

                            clientSocketChannel.configureBlocking(false);
                            clientSocketChannel.register(selector, SelectionKey.OP_READ);
                            clientAssignedToThread = new HostSpecs();
                            ObjectInputStream objInputStream = new ObjectInputStream(clientSocketChannel.socket().getInputStream());
                            while(true){
                                try{
                                    HostSpecs clientConnection = (HostSpecs) objInputStream.readObject();
                                    clientAssignedToThread = clientConnection;

                                    // If the host is new connection
                                    if(hostsInfo.get(clientConnection.ipAddress) == null){
                                        dynamicTable.registers.add(clientConnection);
                                        hostsInfo.put(clientConnection.ipAddress, clientConnection.rank);
                                    } else {
                                        // Check for the most usable host
                                    }


                                    if(!clientSocketChannel.isConnected()){
                                        System.out.println("Client disconnected");
                                        hostsInfo.remove(clientConnection.ipAddress);
                                        dynamicTable.registers.remove(clientConnection);
                                        break;
                                    }
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }

                            }

                        } catch (IOException | InterruptedException e) {
                            System.out.println("Client disconnected");
                            hostsInfo.remove(clientAssignedToThread.ipAddress);
                            dynamicTable.registers.remove(clientAssignedToThread);
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


                    System.out.println("Ejecutando Hilo B");

                    Thread threadTask = new Thread(() -> {


                        //String mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
                        String mostUsableHost = "localhost";

                        try {
                            Socket cSocket = new Socket(mostUsableHost, 5555);
                            AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(cSocket.getOutputStream()));
//                        byte[] fileBytes = getBase64EncodedFile(filePath);

                            while(!startServer){
                                HostSpecs clientMessage = new HostSpecs();
                                clientMessage.getCurrentUsage();

                                objOutputStream.get().writeObject(clientMessage);
                                System.out.println("Object sent");
//                                System.out.println(clientMessage.toString());
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

                    // Código de ejecución del hilo B
//                    try {
//                        Thread.sleep(100); // Simulación de tiempo de ejecución
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }


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
            DatagramChannel datagramChannel;
            try {
                datagramChannel = DatagramChannel.open(); // Puerto de recepción UDP
                datagramChannel.configureBlocking(false);
                datagramChannel.socket().bind(new InetSocketAddress(12346));
                ByteBuffer buffer = ByteBuffer.allocate(1);

                try {
                    showDynamicTable();
                    dynamicTable.frame.setVisible(true);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }

                while (true) {
                    try {
                        SocketAddress clientAddress = datagramChannel.receive(buffer);
                        if(clientAddress != null){
                            buffer.flip();
                            boolean startServerThread = buffer.get() != 0;
                            System.out.println("Valor de booleano por UDP: " + startServerThread);

                            if(startServerThread){
                                dynamicTable.frame.setVisible(true);
                            } else {
                                dynamicTable.frame.setVisible(false);
                            }

                            synchronized (lock) {
                                startServer = startServerThread;
                                lock.notifyAll();
                            }

                            buffer.clear();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        UDPThreadListener.start();

//        try{
//            while (true) {
//                selector.select();
//
//                Set<SelectionKey> selectedKeys = selector.selectedKeys();
//                Iterator<SelectionKey> iterator = selectedKeys.iterator();
//
//                while(iterator.hasNext()){
//                    SelectionKey key = iterator.next();
//                    iterator.remove();
//
//                    if(key.isAcceptable()){
//                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
//                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
//                        clientSocketChannel.configureBlocking(false);
//                        clientSocketChannel.register(selector, SelectionKey.OP_READ);
//                        System.out.println("Cliente conectado: " + clientSocketChannel.getRemoteAddress());
//                    } else if(key.isReadable()){
//                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
//                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
//
//                        try {
//                            int bytesRead = clientSocketChannel.read(readBuffer);
//                            if (bytesRead == -1) {
//                                // Cliente desconectado
//                                System.out.println("Cliente desconectado: " + clientSocketChannel.getRemoteAddress());
//                                clientSocketChannel.close();
//                            } else {
//                                readBuffer.flip();
//                                // Procesar los datos leídos del cliente
//                                processClientData(readBuffer);
//                                readBuffer.clear();
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        } catch (IOException e){
//            e.printStackTrace();
//        }
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

}

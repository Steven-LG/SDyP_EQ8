import server.Server;
import shared.ClientConnection;
import shared.HostSpecs;
import visuals.DynamicTable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static shared.ClientConnection.getRank;

public class Launcher {
    private static final int SERVER_PORT = 5555;
    private static ServerSocket serverSocket;
    private static Object lock = new Object();
    private static boolean startServer = true;
    private static HashMap<String, Double> hostsInfo;
    private static HostSpecs hostSpecs;

    private static DynamicTable dynamicTable;


    public static void main(String[] args) throws InterruptedException {
        hostSpecs = new HostSpecs();

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


                    showDynamicTable();

                    System.out.println("Ejecutando Hilo A");
                    hostsInfo = new HashMap<String, Double>(){};
                    try {
                        serverSocket = new ServerSocket(SERVER_PORT);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }



                    try {
                        System.out.println("Waiting for connection...");
                        Socket clientSocket = serverSocket.accept();

                        Thread threadTask = new Thread(() -> {
                            ClientConnection clientAssignedToThread = new ClientConnection();
                            try {
                                ObjectInputStream objInputStream = new ObjectInputStream(clientSocket.getInputStream());
                                while(true){
                                    try{
                                        ClientConnection clientConnection = (ClientConnection) objInputStream.readObject();
                                        clientAssignedToThread = clientConnection;

                                        if(hostsInfo.get(clientConnection.ipAddress) == null){
                                            dynamicTable.registers.add(clientConnection); // VISUALS
                                            hostsInfo.put(clientConnection.ipAddress, clientConnection.rank);
                                        } else {
                                            for(HostSpecs cConnection : dynamicTable.registers){

                                            }
                                        }
                                    }

                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        threadTask.start();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // Código de ejecución del hilo A
                    try {
                        Thread.sleep(100); // Simulación de tiempo de ejecución


                    } catch (InterruptedException e) {
                        e.printStackTrace();
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


                    System.out.println("Ejecutando Hilo B");
                    // Código de ejecución del hilo B
                    try {
                        Thread.sleep(100); // Simulación de tiempo de ejecución
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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

        // Iniciar receptor UDP
//        Thread UDPThreadListener = new Thread(() -> {
//            DatagramSocket socket;
//            try {
//                socket = new DatagramSocket(12345); // Puerto de recepción UDP
//                byte[] buffer = new byte[1];
//                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                while (true) {
//                    try {
//                        socket.receive(packet);
//                        boolean startServerThread = buffer[0] != 0;
//                        System.out.println("Valor de booleano por UDP: "+startServerThread);
//
//                        synchronized (lock) {
//                            startServer = startServerThread;
//                            lock.notifyAll();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (SocketException e) {
//                e.printStackTrace();
//            }
//        });
//
//        UDPThreadListener.start();

        while (true) {
            System.out.print("\033\143");
            System.out.println(hostSpecs.toString());
            Thread.sleep(2000);
        }
    }


    private void showDynamicTable() throws InterruptedException, UnknownHostException {
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

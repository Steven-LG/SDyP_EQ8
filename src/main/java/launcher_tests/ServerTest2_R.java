package launcher_tests;

import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerTest2_R {
    private static ServerSocket serverSocket;
    private static final int SERVER_PORT = 5556;
    private static Object serverLock = new Object();
    private static ArrayList<ServerThread> currentServerHandlers = new ArrayList<ServerThread>();
    private static Thread serverThread;
    public static void main(String[] args) throws IOException, InterruptedException {
        serverThread = new Thread(()->{
            while(true){
                try {
                    startServer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if(Thread.currentThread().isInterrupted()){
                    break;
                }
            }
        });
        serverThread.start();

        Thread serverStartEmitter = new Thread(()->{
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            synchronized (serverLock){
                serverLock.notifyAll();
                System.out.println("SERVER EMITTER UNLOCKED SERVER");
            }
        });
        serverStartEmitter.start();

        Thread stopServerEmitter = new Thread(()->{
//            while(true){
                try {
                    Thread.sleep(12000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                try {
                    stopServer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("SERVER INTERRUPTED FROM STOP EMITTER");

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


//            }

        });
        stopServerEmitter.start();
    }


    public static void startServer() throws IOException {
        System.out.println("Server waiting for unlock");
        synchronized (serverLock){
            try {
                serverLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(serverSocket == null){
            serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setSoTimeout(3000);
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
}
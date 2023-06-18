import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class LauncherV2_2 {

    // Server attributes
    private static ServerSocket serverSocket;
    private static final int SERVER_PORT = 5555;
    private static Object serverLock = new Object();
    private static ArrayList<ServerThread> currentServerHandlers = new ArrayList<ServerThread>();
    private static Thread serverThread;

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
    public static void main(String[] args) throws InterruptedException, IOException {
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
        serverThread.start();

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
        serverStartEmitter.start();

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
        //startClientEmitter.start();

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

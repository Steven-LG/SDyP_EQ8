package launcher_tests;

import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientTest2 {
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

    public static void main(String[] args) throws IOException, InterruptedException {
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
        changeToServerListenerThread.start();

//        Thread t2 = new Thread(() -> {
//            System.out.println("T2 INITIALIZED");
//            int currentPort = 5555;
//            while(true){
//                try {
//                    Thread.sleep(4000);
//                    if(currentPort == 5555){
//                        currentPort = 5556;
//                    } else {
//                        currentPort = 5555;
//                    }
//                    System.out.println("CURRENT PORT "+currentPort);
//                    changeServer.set(true);
//                    System.out.println("CHANGESERVER SET TO TRUE");
//                    changeServer.set(false);
//                    changeClientSocket(mostUsableServer, currentPort);
//
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        t2.start();
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

        System.out.println("Waiting for client unlock");
        synchronized (clientLock){
            clientLock.wait();
        }
        System.out.println("Client socket unlocked");
        changeClientSocket(mostUsableServer, 5555);

    }

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

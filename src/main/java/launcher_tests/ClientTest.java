package launcher_tests;

import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientTest {
    private static ObjectOutputStream clientObjectOutputStream;
    private static Socket cSocket;
    private static AtomicBoolean changeServer = new AtomicBoolean(true);
    private static volatile InetAddress mostUsableServer;
    static {
        try {
            mostUsableServer = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static AtomicBoolean isServer = new AtomicBoolean(false);

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread changeToServerListenerThread = new Thread(()->{
            try {
                Thread.sleep(5000);
                isServer.set(true);

                Thread.sleep(3000);
                changeServer.set(true);
                changeClientSocket(mostUsableServer, 5555);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        //changeToServerListenerThread.start();
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

        changeClientSocket(mostUsableServer, 5555);
    }

    static void changeClientSocket(InetAddress NEW_ADDRESS, int NEW_PORT) throws IOException, InterruptedException {
        try{
            if(cSocket.isConnected()){
                clientObjectOutputStream.close();
                cSocket.close();
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        cSocket = new Socket(NEW_ADDRESS, NEW_PORT);
        clientObjectOutputStream = new ObjectOutputStream(cSocket.getOutputStream());
        isServer.set(false);
        changeServer.set(false);

        System.out.println("CLIENT SOCKET CHANGED DONE");
        Thread t = new Thread(()->{
            try {
                makeConstantRequests();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();


    }

    static synchronized void makeConstantRequests() throws IOException, InterruptedException {
        while(!isServer.get()){
            HostSpecs clientMessage = new HostSpecs();
            clientMessage.getCurrentUsage();

            clientObjectOutputStream.writeObject(clientMessage);
            System.out.println("Object sent");
            clientObjectOutputStream.flush();
            Thread.sleep(1000);

            if(changeServer.get()){
                break;
            }
        }
    }
}

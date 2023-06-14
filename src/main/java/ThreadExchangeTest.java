import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadExchangeTest {
    private static AtomicBoolean isServer = new AtomicBoolean(false);
    private static AtomicBoolean serverChange = new AtomicBoolean(false);
    private static InetAddress ADDRESS;
    private static final int PORT = 5555;
    private static Socket clientSocket;
    private static ObjectOutputStream clientObjOutputStream;

    public static void main(String[] args) throws InterruptedException, IOException {
        ADDRESS = InetAddress.getByName("25.3.224.138");
        clientSocket = new Socket(ADDRESS, PORT);
        while(clientSocket.isConnected()){
            clientObjOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            System.out.println("Conectado");
        }

//        Thread sThread = new ServerThread();
//        Thread cThread = new ClientThread();
//
//        sThread.start();
//        cThread.start();
//
//        Thread changeServerTimer = new Thread(() -> {
//            while(true){
////                if(!isServer.get() && !serverChange.get()){
////                    try {
////                        Thread.sleep(2000);
////                    } catch (InterruptedException e) {
////                        throw new RuntimeException(e);
////                    }
////                    serverChange.set(true);
////                } else if(!serverChange.get() && serverChange.get()){
////                    try {
////                        Thread.sleep(2000);
////                    } catch (InterruptedException e) {
////                        throw new RuntimeException(e);
////                    }
////                    serverChange.set(false);
////                }
//            }
//        });
//        changeServerTimer.start();
//
//        while(true){
//            Thread.sleep(4000);
//            if(!isServer.get()){
//                isServer.set(true);
//                Thread newSThread = new ServerThread();
//                newSThread.start();
//
//            } else {
//                isServer.set(false);
//                Thread newCThread = new ClientThread();
//                newCThread.start();
//
//            }
//        }



    }

    static class ServerThread extends Thread{
        @Override
        public void run() {
            super.run();
            while(true){
                System.out.println("Server now running...");

                if(!isServer.get()){
                    System.out.println("Server stopped...");
                    break;
                }
            }
        }
    }

    static class ClientThread extends Thread{
        @Override
        public void run() {
            super.run();

            try {
                changeClientSocket(InetAddress.getByName("localhost"), 5556);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

//            while(true){
//                System.out.println("Client now running...");
//
//
//                if(serverChange.get()){
//                    try {
//                        changeClientSocket(InetAddress.getByName("localhost"), 5556);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//
//                if(isServer.get()){
//                    System.out.println("Client stopped...");
//                    break;
//                }
//            }
        }
    }

    static void changeClientSocket(InetAddress NEW_ADDRESS, int NEW_PORT) throws IOException, InterruptedException {
        clientObjOutputStream.close();
        if(clientSocket.isConnected()){
            clientSocket.close();
        }

        clientSocket = new Socket(NEW_ADDRESS, NEW_PORT);
        clientObjOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        System.out.println("CLIENT SOCKET CHANGED DONE");

        serverChange.set(false);
        makeConstantRequests();
    }

    static void makeConstantRequests() throws IOException, InterruptedException {
        Object myObject = new Object();

        while(true){
            clientObjOutputStream.writeObject(myObject);
            System.out.println("Object sent");
            clientObjOutputStream.flush();
            Thread.sleep(1000);

            if(serverChange.get()){
                break;
            }
        }
    }

}





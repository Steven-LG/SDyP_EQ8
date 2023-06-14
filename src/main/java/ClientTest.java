import org.checkerframework.checker.units.qual.A;
import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientTest {
    private static ObjectOutputStream ous;
    private static Socket cSocket;
    private static AtomicBoolean changeServer = new AtomicBoolean(true);
    private static volatile InetAddress mostUsableServer;
    static {
        try {

            mostUsableServer = InetAddress.getByName("localhost");
//            mostUsableServer = InetAddress.getByName("25.3.224.138");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread t2 = new Thread(() -> {
            System.out.println("T2 INITIALIZED");
            int currentPort = 5555;
            while(true){
                try {
                    Thread.sleep(4000);
                    if(currentPort == 5555){
                        currentPort = 5556;
                    } else {
                        currentPort = 5555;
                    }
                    System.out.println("CURRENT PORT "+currentPort);
                    changeServer.set(true);
                    System.out.println("CHANGESERVER SET TO TRUE");
                    changeServer.set(false);
                    changeClientSocket(mostUsableServer, currentPort);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
//            boolean changeInInterval = true;
//            while(true) {
//                if (changeInInterval) {
//                    while (true) {
//                        if (changeServer.get()) {
//                            try {
//                                changeClientSocket(mostUsableServer, 5556);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                            break;
//                        }
//                    }
//                }
        });
        t2.start();

        changeClientSocket(mostUsableServer, 5555);



//        while(true){
//            cSocket = new Socket();
//            ous = new ObjectOutputStream(cSocket.getOutputStream());

//            changeClientSocket(mostUsableServer, 5555);
//            while(true){
//                HostSpecs clientMessage = new HostSpecs();
//                clientMessage.getCurrentUsage();
//
//                ous.writeObject(clientMessage);
//                System.out.println("Object sent");
//                ous.flush();
//                Thread.sleep(1000);
//            }
//        }
    }

    static void changeClientSocket(InetAddress NEW_ADDRESS, int NEW_PORT) throws IOException, InterruptedException {
        try{
            if(cSocket.isConnected()){
                ous.close();
                cSocket.close();
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        cSocket = new Socket(NEW_ADDRESS, NEW_PORT);
        ous = new ObjectOutputStream(cSocket.getOutputStream());
        changeServer.set(false);

        System.out.println("CLIENT SOCKET CHANGED DONE");
        //makeConstantRequests();
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
        while(true){
            HostSpecs clientMessage = new HostSpecs();
            clientMessage.getCurrentUsage();

            ous.writeObject(clientMessage);
            System.out.println("Object sent");
            ous.flush();
            Thread.sleep(1000);

            if(changeServer.get()){
                break;
            }
        }
    }
}

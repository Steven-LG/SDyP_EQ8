import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class ClientTest {
    public static boolean isServer = false;
    public static void main(String[] args) {
        System.out.println("Running as client");
        Thread threadTask = new Thread(() -> {
            //String mostUsableHost = Collections.max(hostsInfo.entrySet(), Map.Entry.comparingByValue()).getKey();
            //String mostUsableHost = "localhost";

            try {
                Socket cSocket = new Socket("25.3.224.138", 5555);
                AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(cSocket.getOutputStream()));

                while(!isServer){
                    HostSpecs clientMessage = new HostSpecs();
                    clientMessage.getCurrentUsage();

                    objOutputStream.get().writeObject(clientMessage);
                    System.out.println("Object sent");
                    objOutputStream.get().flush();
                    Thread.sleep(2000);
                }

            } catch (IOException e) {
                System.out.println(e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
        });
        threadTask.start();
    }
}

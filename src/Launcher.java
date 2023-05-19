import client.Client;
import server.Server;

import java.io.IOException;
import java.net.InetAddress;
public class Launcher {
    private static final int PORT = 5555;
    private static String SERVER_ADDRESS;

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean startAsClient = false;
        SERVER_ADDRESS = InetAddress.getLocalHost().getHostAddress();

        SwitchListenerThread tListenerThread = new SwitchListenerThread();
        tListenerThread.start();

        if(startAsClient){
            initializeClient();
        } else {
            initializeServer();
        }
    }

    static class SwitchListenerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);

                    if (Server.isServerMostUsableHost) {
                        try {
                            Server.killServer();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        initializeClient();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void initializeClient(){
        Client.PORT = PORT;
        Client.SERVER_ADDRESS = SERVER_ADDRESS;
        Client.initialize();
    }

    public static void initializeServer() throws IOException, InterruptedException {
        Server.PORT = PORT;
        Server.initialize();
    }

}


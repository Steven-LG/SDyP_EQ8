package launcher_tests;

import shared.HostSpecs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTest2 {
    private static ServerSocket serverSocket;
    private static final int SERVER_PORT = 5556;
    public static void main(String[] args) throws IOException {

        System.out.println("Waiting for connection...");
        try {
            serverSocket = new ServerSocket(SERVER_PORT);

            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("EN EFECTO SE BLOQUEA");
                ServerThread newClientHandler = new ServerThread(clientSocket);
                newClientHandler.start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                    try{
                        HostSpecs receivedClientSpecs = (HostSpecs) objInputStream.readObject();
                        clientAssignedToThread = receivedClientSpecs;
                        System.out.print("Received client specs: ");
                        System.out.println(receivedClientSpecs);

                        if (!clientSocket.isConnected()) {
                            System.out.println("Client disconnected");

                            clientSocket.close();
                        }

//
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
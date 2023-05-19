package client;

import shared.CsvHandler;
import shared.ClientConnection;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static shared.ClientConnection.getRank;

public class Client {
    public static int PORT;
    public static String SERVER_ADDRESS;
    public static Socket socket;
    public static String mostUsableServerAddress;

    public static void initialize(){
        String filePath = "src/client/randomNumbers.csv";
        CsvHandler csvHandler = new CsvHandler(filePath);
        csvHandler.generateFile();

        try {
            socket = new Socket(Client.SERVER_ADDRESS, PORT);
            AtomicReference<ObjectOutputStream> objOutputStream = new AtomicReference<>(new ObjectOutputStream(socket.getOutputStream()));
            byte[] fileBytes = getBase64EncodedFile(filePath);

            Thread receiveThread = new Thread(() -> {
                while(true){
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        String receivedServerAddress;
                        while ((receivedServerAddress = reader.readLine()) != null) {
                            System.out.println("Message from server: " + receivedServerAddress);
                            System.out.println("CURRENT MOST USABLE SERVER ADDRESS "+mostUsableServerAddress);
                            if(!receivedServerAddress.equals(mostUsableServerAddress)){
                                System.out.println("LAST MOST USABLE SERVER ADDRESS "+mostUsableServerAddress);
                                System.out.println("RECEIVED SERVER ADDRESS "+receivedServerAddress);

                                mostUsableServerAddress = receivedServerAddress;
                                socket.close();
                                socket = new Socket(mostUsableServerAddress, PORT);
                                objOutputStream.set(new ObjectOutputStream(socket.getOutputStream()));
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error receiving message from server: " + e.getMessage());
                    }
                }
            });
            receiveThread.start();

            while(true){
                ClientConnection clientConnection = new ClientConnection(fileBytes);
                clientConnection.ipAddress = InetAddress.getLocalHost().getHostAddress();
                clientConnection.rank = getRank(clientConnection);
                clientConnection.getCurrentUsage();

                if(!clientConnection.firstConnection){
                    clientConnection.fileBytes = null;
                }

                System.out.println("RAM used: "+clientConnection.RAMUsed);
                System.out.println("Timer " + clientConnection.timer);

                objOutputStream.get().writeObject(clientConnection);
                System.out.println("Object sent");

                objOutputStream.get().flush();

                Thread.sleep(2000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static byte[] getBase64EncodedFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = new byte[(int) file.length()];
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            inputStream.read(fileBytes, 0, fileBytes.length);
        }
        return Base64.getEncoder().encode(fileBytes);
    }

}


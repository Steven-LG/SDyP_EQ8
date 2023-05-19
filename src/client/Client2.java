package client;
        import shared.ClientConnection;
        import shared.CsvHandler;

        import java.io.*;
        import java.net.Socket;
        import java.util.Base64;

public class Client2 {
    private static final int PORT = 5555;
    private static final String SERVER_NAME = "localhost";

    //    private static HostSpecs clientSpecs;
    private static Socket socket;

    public static void main(String[] args) throws IOException {
//        clientSpecs = new HostSpecs();
        String filePath = "src/client/randomNumbers.csv";
        CsvHandler csvHandler = new CsvHandler(filePath);
        csvHandler.generateFile();

        try {
            socket = new Socket(SERVER_NAME, PORT);
            ObjectOutputStream objOutputStream = new ObjectOutputStream(socket.getOutputStream());
            byte[] fileBytes = getBase64EncodedFile(filePath);

            while(true){
                ClientConnection clientConnection = new ClientConnection(fileBytes);
                clientConnection.ipAddress = "127.0.0.3";
                clientConnection.rank = 999;
                clientConnection.getCurrentUsage();

                if(!clientConnection.firstConnection){
                    clientConnection.fileBytes = null;
                }

                System.out.println("RAM used: "+clientConnection.RAMUsed);
                System.out.println("Timer " + clientConnection.timer);


                objOutputStream.writeObject(clientConnection);

                System.out.println("Object sent");
                System.out.println(socket.isConnected());
                objOutputStream.flush();

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


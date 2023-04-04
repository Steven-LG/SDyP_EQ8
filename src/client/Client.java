package client;

import csvhandler.CsvHandler;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class Client {
    private static final int PORT = 5555;
    private static final String SERVER_NAME = "localhost";

    public static void main(String[] args) {
        String filePath = "src/client/randomNumbers.csv";
        CsvHandler csvHandler = new CsvHandler(filePath);
        csvHandler.generateFile();


        try (Socket socket = new Socket(SERVER_NAME, PORT)) {
            OutputStream outputStream = socket.getOutputStream();
            byte[] fileBytes = getBase64EncodedFile(filePath);
            outputStream.write(fileBytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
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


package client;

import csvhandler.CsvHandler;
import visuals.DynamicTable;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class Client {
    private static final int PORT = 5555;
    private static final String SERVER_NAME = "localhost";

    private static HostSpecs clientSpecs;

    public static void main(String[] args) {
        clientSpecs = new HostSpecs();
        String filePath = "src/client/randomNumbers.csv";
        CsvHandler csvHandler = new CsvHandler(filePath);
        csvHandler.generateFile();
        boolean firstResponseSent = false;
        
        System.out.println("Procesador " + clientSpecs.processorModel);
        System.out.println("Velocidad procesador " + clientSpecs.processorSpeed);
        System.out.println("Nucleos " + clientSpecs.numCores);
        System.out.println("Uso de procesador " + clientSpecs.processorUsage);
        System.out.println("Capacidad de disco " + clientSpecs.diskCapacity);
        for (HostSpecs.Disk dis: clientSpecs.disks) {
            System.out.println("" + dis.name);
            System.out.println("" + dis.freeSpace);
            System.out.println("" + dis.size);
            System.out.println("" + dis.usage);
        }
        System.out.println("RAM Usada " + clientSpecs.RAMUsed);
        System.out.println("OS " + clientSpecs.osVersion);

        try (Socket socket = new Socket(SERVER_NAME, PORT)) {
            OutputStream outputStream = socket.getOutputStream();
            byte[] fileBytes = getBase64EncodedFile(filePath);
            outputStream.write(fileBytes);
            outputStream.flush();

            firstResponseSent = true;

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


import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class ImplementationTest {
    public static String serverIP = "localhost";
    public static int serverPort = 5555;
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(serverIP, serverPort);

// Enviar el HashMap
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("clave1", "valor1");
        hashMap.put("clave2", "valor2");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(hashMap);
        objectOutputStream.flush();

// Enviar el archivo MP4
        String filePath = "D:\\Development\\PersonalCourseProjects\\SDyP_EQ8\\src\\main\\java\\test_video.mp4";
        File file = new File(filePath);
        byte[] buffer = new byte[4096];
        int bytesRead;
        InputStream fileInputStream = new FileInputStream(file);
        OutputStream fileOutputStream = socket.getOutputStream();
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        fileOutputStream.flush();

        fileInputStream.close();
        fileOutputStream.close();
        socket.close();
    }
}

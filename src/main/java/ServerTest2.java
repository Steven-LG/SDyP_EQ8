import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTest2 {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5556);
        System.out.println("SERVER ACTIVO? "+!serverSocket.isClosed());

        while(true){
            Socket clientSocket = serverSocket.accept();
            System.out.println("EN EFECTO SE BLOQUEA");
            Thread sThread = new Thread(() -> {
                while(clientSocket.isConnected()){
                    System.out.println(clientSocket.getInetAddress()+" "+clientSocket.isConnected());
                }
            });
            sThread.start();
        }


    }
}
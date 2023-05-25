import java.io.IOException;
import java.net.*;

public class UDPClientBroadcast {
    public static void main(String[] args) {
//        String host = "localhost"; // Dirección IP del servidor

        try {
            DatagramSocket socket = new DatagramSocket();
//            InetAddress direccionServidor = InetAddress.getByName(host);
            socket.setBroadcast(true);

            while (true) {
                boolean activarHiloA = obtenerValorBooleano(); // Obtener el nuevo valor del booleano

                byte[] buffer = new byte[1];
                buffer[0] = (byte) (activarHiloA ? 1 : 0);

                System.out.println("Valor de booleano enviado por UDP: "+activarHiloA);

                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                int clientPort = 12345; // Puerto del cliente

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, clientPort);
                socket.send(packet);

                Thread.sleep(5000); // Esperar 5 segundos antes de enviar el siguiente booleano
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean counter = true;

    private static boolean obtenerValorBooleano() {
        if(counter == true){
            counter = !counter;
        } else {
            counter = true;
        }
        // Lógica para obtener el nuevo valor del booleano
        // Puedes implementar aquí la lógica que cambia el valor del booleano según tus necesidades
        return counter; // Ejemplo: Generar un valor aleatorio
    }
}
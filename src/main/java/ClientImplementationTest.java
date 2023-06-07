import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ClientImplementationTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(5555);
        Socket socket = serverSocket.accept();

// Recibir el HashMap
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        HashMap<String, String> receivedHashMap = (HashMap<String, String>) objectInputStream.readObject();
        System.out.println("HashMap recibido: " + receivedHashMap);

// Recibir el archivo MP4
        String outputFilePath = "D:\\Development\\PersonalCourseProjects\\SDyP_EQ8\\src\\main\\java\\test\\test_video.mp4";
        FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);
        byte[] buffer = new byte[4096];
        int bytesRead;
        InputStream fileInputStream = socket.getInputStream();
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        fileOutputStream.close();

        objectInputStream.close();
        socket.close();
        serverSocket.close();

        FFmpeg ffmpeg = new FFmpeg("src/main/java/ffmpeg.exe");
        FFprobe ffprobe = new FFprobe("src/main/java/ffprobe.exe");

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput("src/main/java/test_video.mp4/test_video.mp4")
                .overrideOutputFiles(true)
                .addOutput("src/main/java/test/test_video.m4a")
                .disableSubtitle()
                .setAudioChannels(1)
                .setAudioCodec("aac")
                .setAudioSampleRate(96_000)
                .setAudioBitRate(32768)
                .setVideoCodec("libx264")
                .setVideoFrameRate(30, 1)
                .setVideoResolution(1920, 1080)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        executor.createJob(builder).run();
    }
}

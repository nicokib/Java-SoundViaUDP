import javax.sound.sampled.*;
import java.net.*;

public class UdpAudioSender {
    public static void main(String[] args) {
        String host = "localhost"; // Remplacez par l'IP du récepteur si besoin
        int port = 5002;

        // 1. Définir le format audio (CD Quality: 44.1kHz, 16-bit, Mono)
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        TargetDataLine microphone;

        try {
            // 2. Initialiser et ouvrir le microphone
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            // 3. Initialiser le Socket UDP
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);

            System.out.println("Microphone ouvert. Envoi du flux audio par UDP...");

            // Tampon pour lire les données du micro
            byte[] buffer = new byte[1024];

            while (true) {
                // Lire l'audio du microphone
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                // Envoyer les octets audio par UDP
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, port);
                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

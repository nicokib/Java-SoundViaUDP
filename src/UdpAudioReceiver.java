import javax.sound.sampled.*;
import java.net.*;

public class UdpAudioReceiver {
    public static void main(String[] args) {
        int port = 5002;

        // 1. Définir le même format audio exact qu'à l'émission
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        SourceDataLine speakers;

        try {
            // 2. Initialiser et ouvrir les haut-parleurs
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(format);
            speakers.start();

            // 3. Initialiser le Socket UDP
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("Récepteur audio UDP démarré sur le port " + port);

            // Tampon de réception (doit être au moins aussi grand que celui de l'émetteur)
            byte[] buffer = new byte[1024];

            while (true) {
                // Attendre et recevoir un paquet de données audio
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Écrire les données reçues directement vers les haut-parleurs
                speakers.write(packet.getData(), 0, packet.getLength());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
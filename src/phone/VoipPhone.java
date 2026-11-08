package phone;

import javax.sound.sampled.*;
import java.net.*;

public class VoipPhone {

    // Format audio commun (Qualité CD, Mono pour la légèreté)
    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        // --- CONFIGURATION À ADAPTER ---
        // Port sur lequel cette instance va écouter
        int localPort = 5002;

        // Cible (l'autre téléphone)
        String remoteHost = "localhost";
        int remotePort = 5003;
        // -------------------------------

        // Si vous testez sur la même machine, inversez les ports pour la deuxième instance :
        // Instance 1: localPort = 5002, remotePort = 5003
        // Instance 2: localPort = 5003, remotePort = 5002

        try {
            // Création du socket UDP unique partagé pour l'envoi et la réception
            DatagramSocket socket = new DatagramSocket(localPort);
            System.out.println("Téléphone démarré sur le port local : " + localPort);
            System.out.println("Appel configuré vers " + remoteHost + ":" + remotePort);

            // 1. Démarrage du thread d'ÉMISSION (Capture Micro -> Envoi)
            Thread senderThread = new Thread(() -> startSender(socket, remoteHost, remotePort));
            senderThread.start();

            // 2. Démarrage du thread de RÉCEPTION (Écoute Réseau -> Haut-parleurs)
            Thread receiverThread = new Thread(() -> startReceiver(socket));
            receiverThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère la capture du microphone et l'envoi des paquets audio
     */
    private static void startSender(DatagramSocket socket, String remoteHost, int remotePort) {
        TargetDataLine microphone;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(FORMAT);
            microphone.start();

            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            byte[] buffer = new byte[BUFFER_SIZE];

            System.out.println("[Émetteur] Microphone activé.");

            while (!Thread.currentThread().isInterrupted()) {
                // Lit le son du microphone
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                // Envoie l'audio à l'autre correspondant
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, remotePort);
                socket.send(packet);
            }
        } catch (Exception e) {
            System.err.println("Erreur Émetteur: " + e.getMessage());
        }
    }

    /**
     * Gère la réception des paquets réseau et la lecture sur les haut-parleurs
     */
    private static void startReceiver(DatagramSocket socket) {
        SourceDataLine speakers;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(FORMAT);
            speakers.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            System.out.println("[Récepteur] Haut-parleurs activés, en attente de flux...");

            while (!Thread.currentThread().isInterrupted()) {
                // Attend un paquet audio venant de l'autre correspondant
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Joue le son immédiatement
                speakers.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            System.err.println("Erreur Récepteur: " + e.getMessage());
        }
    }
}
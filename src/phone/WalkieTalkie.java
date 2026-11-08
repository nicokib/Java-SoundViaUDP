package phone;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Scanner;

public class WalkieTalkie {

    // Format audio commun (Qualité CD, Mono pour la légèreté)
    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);
    private static final int BUFFER_SIZE = 1024;

    // Variable volatile pour synchroniser l'état Push-To-Talk entre les threads
    private static volatile boolean isTalking = false;

    public static void main(String[] args) {
        // --- CONFIGURATION À ADAPTER ---
        int localPort = 5002;
        String remoteHost = "localhost";
        int remotePort = 5003;
        // -------------------------------

        try {
            DatagramSocket socket = new DatagramSocket(localPort);
            System.out.println("=== WALKIE-TALKIE DÉMARRÉ ===");
            System.out.println("Écoute locale : " + localPort + " | Cible : " + remoteHost + ":" + remotePort);
            System.out.println("Appuyez sur ENTRÉE pour basculer entre PARLER et ÉCOUTER.\n");

            // 1. Démarrage du thread d'ÉMISSION (Actif uniquement quand isTalking = true)
            Thread senderThread = new Thread(() -> startSender(socket, remoteHost, remotePort));
            senderThread.start();

            // 2. Démarrage du thread de RÉCEPTION (Bloqué/Muet quand isTalking = true)
            Thread receiverThread = new Thread(() -> startReceiver(socket));
            receiverThread.start();

            // 3. Gestionnaire du bouton Push-To-Talk (PTT) via la console
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (!isTalking) {
                    System.out.println("[Mode : ÉCOUTE] Appuyez sur ENTRÉE pour prendre la parole...");
                    scanner.nextLine();
                    isTalking = true;
                    System.out.println("\n>>> [MODE : ÉMISSION] Vous parlez... Appuyez sur ENTRÉE pour terminer. >>>");
                } else {
                    scanner.nextLine();
                    isTalking = false;
                    System.out.println("\n<<< [MODE : ÉCOUTE] Fin de transmission. Retour en écoute. <<<");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Émetteur : Capture le micro en continu mais n'envoie sur le réseau
     * que si l'utilisateur a activé le mode "Talk".
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

            while (!Thread.currentThread().isInterrupted()) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                // Condition Walkie-Talkie : on envoie SEULEMENT si on a le bouton enfoncé
                if (isTalking && bytesRead > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, remotePort);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur Émetteur: " + e.getMessage());
        }
    }

    /**
     * Récepteur : Reçoit les paquets réseau en continu mais ignore/vide le buffer
     * si nous sommes nous-mêmes en train de parler (principe du Half-Duplex).
     */
    private static void startReceiver(DatagramSocket socket) {
        SourceDataLine speakers;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(FORMAT);
            speakers.start();

            byte[] buffer = new byte[BUFFER_SIZE];

            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Attend le paquet

                // Si on est en train de parler, on ignore ce qu'on reçoit pour éviter le Larsen
                // et respecter la règle du "chacun son tour".
                if (!isTalking) {
                    speakers.write(packet.getData(), 0, packet.getLength());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur Récepteur: " + e.getMessage());
        }
    }
}
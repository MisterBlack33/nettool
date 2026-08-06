package main.java.networktool.gui.notification;

import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.storage.NotificationHistory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP-Listener (Port {@link MessageSender#NETTOOL_LISTENER_PORT}) für
 * direkte NetTool-zu-NetTool-Nachrichten im LAN.
 */
final class NotificationTcpServer {

    private static final Logger LOG = Logger.getLogger(NotificationTcpServer.class.getName());

    // Halte Referenz auf den TCP-Server damit Tests sauber stoppen können
    private static volatile ServerSocket tcpServerSocket = null;

    private NotificationTcpServer() {}

    static void start() {
        Thread t = new Thread(() -> {
            try {
                tcpServerSocket = new ServerSocket(MessageSender.NETTOOL_LISTENER_PORT);
                try (ServerSocket ss = tcpServerSocket) {
                    while (!ss.isClosed()) {
                        try { handleClient(ss.accept()); }
                        catch (IOException e) {
                            LOG.log(Level.FINE, "TCP-Client konnte nicht angenommen werden", e);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[NetTool] TCP-Port "
                        + MessageSender.NETTOOL_LISTENER_PORT
                        + " nicht verfügbar: " + e.getMessage());
            } finally {
                tcpServerSocket = null;
            }
        }, "NetTool-TCP-Listener");
        t.setDaemon(true);
        t.start();
    }

    /** Stoppt den laufenden TCP-Server — nützlich für Tests. */
    static void stop() {
        try {
            if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
                tcpServerSocket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "TCP-Server-Socket konnte nicht sauber geschlossen werden", e);
        }
        tcpServerSocket = null;
    }

    private static void handleClient(Socket client) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                String from = client.getInetAddress().getHostAddress();
                String msg  = br.readLine();
                if (msg != null && !msg.isBlank()) {
                    System.out.println("  ✉ Nachricht von " + from + ": " + msg);
                    LocalToast.show("NetTool – von " + from, msg);
                    NotificationHistory.getInstance()
                            .add("TCP [" + from + "]", "NetTool – von " + from, msg);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Eingehende TCP-Nachricht konnte nicht verarbeitet werden", e);
            }
        }, "NetTool-TCP-Handler").start();
    }
}

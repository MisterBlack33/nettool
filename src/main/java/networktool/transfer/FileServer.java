package main.java.networktool.transfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Einfacher TCP-Dateiserver.
 * Akzeptiert eingehende Verbindungen und delegiert
 * jede an {@link FileReceiver} in einem eigenen Thread.
 */

public final class FileServer {

    private final int port;
    private volatile ServerSocket serverSocket;

    public FileServer(int port) {
        this.port = port;
    }

    /** Bindet den Port synchron – nach Rückkehr ist der Server verbindungsbereit. */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("FileServer: Bind fehlgeschlagen: " + e.getMessage());
            return;
        }
        new Thread(this::acceptLoop, "FileServer-" + port).start();
    }

    private void acceptLoop() {
        System.out.println("File-Server läuft auf Port " + port);
        try (ServerSocket ss = serverSocket) {
            while (!ss.isClosed()) {
                Socket client = ss.accept();
                FileReceiver.receive(client);
            }
        } catch (IOException e) {
            System.err.println("FileServer: Fehler: " + e.getMessage());
        }
    }
}

package main.java.networktool_v3.transfer;

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

    public FileServer(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(this::acceptLoop, "FileServer-" + port).start();
    }

    private void acceptLoop() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("File-Server läuft auf Port " + port);
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                FileReceiver.receive(client);
            }
        } catch (IOException e) {
            System.err.println("FileServer: Fehler: " + e.getMessage());
        }
    }
}

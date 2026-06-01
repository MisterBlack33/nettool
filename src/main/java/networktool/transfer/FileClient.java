package main.java.networktool.transfer;

import java.io.*;
import java.net.Socket;

/**
 * Sendet eine Datei über TCP an einen laufenden {@link FileServer}.
 *
 * Protokoll:
 *   1. Dateiname als UTF-String
 *   2. Dateigröße als Long
 *   3. Rohdaten
 */
public final class FileClient {

    private static final int BUFFER_SIZE = 4096;

    private final String host;
    private final int    port;

    public FileClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Datei nicht gefunden: " + filePath);
            return;
        }
        try (Socket socket           = new Socket(host, port);
             DataOutputStream dos    = new DataOutputStream(socket.getOutputStream());
             FileInputStream   fis   = new FileInputStream(file)) {

            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            System.out.println("Datei erfolgreich gesendet: " + file.getName());

        } catch (IOException e) {
            System.err.println("FileClient: Fehler beim Senden: " + e.getMessage());
        }
    }
}

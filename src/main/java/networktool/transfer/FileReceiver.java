package main.java.networktool.transfer;

import java.io.*;
import java.net.Socket;

/**
 * Empfängt eine Datei von einem verbundenen {@link FileClient}.
 * Läuft in einem eigenen Thread, um den Server nicht zu blockieren.
 *
 * Die empfangene Datei wird mit dem Präfix "empfangen_" gespeichert.
 */
public final class FileReceiver {

    private static final int    BUFFER_SIZE    = 4096;
    private static final String FILE_PREFIX    = "empfangen_";

    private FileReceiver() {}

    public static void receive(Socket socket) {
        new Thread(() -> handleConnection(socket)).start();
    }

    private static void handleConnection(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            System.out.println("Client verbunden: " + socket.getInetAddress());
            String fileName = dis.readUTF();
            long   fileSize = dis.readLong();
            saveFile(fileName, fileSize, dis);
            System.out.println("Datei empfangen: " + fileName);
        } catch (IOException e) {
            System.err.println("FileReceiver: Fehler beim Empfangen: " + e.getMessage());
        }
    }

    private static void saveFile(String fileName, long fileSize, DataInputStream dis)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(FILE_PREFIX + fileName)) {
            byte[] buffer    = new byte[BUFFER_SIZE];
            long totalRead   = 0;
            int  read;
            while (totalRead < fileSize
                    && (read = dis.read(buffer, 0,
                            (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
        }
    }
}

package main.java.networktool.gui.components;

import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static main.java.networktool.theme.GuiTheme.*;
import static main.java.networktool.gui.components.TerminalChrome.appendTerm;

/**
 * Baut eine rohe TCP-Verbindung zu Port 22 auf, liest den SSH-Banner
 * und streamt danach rohe Antworten ins Terminal-Dokument.
 * Kein vollständiger SSH-Client (dafür wäre JSch nötig).
 */
final class SshConnectionWorker {

    private SshConnectionWorker() {}

    static void connect(String ip, StyledDocument doc, Color termFg,
                         Consumer<PrintWriter> onWriter, Consumer<Socket> onSocket) {
        new Thread(() -> {
            try (Socket sock = new Socket(ip, 22)) {
                onSocket.accept(sock);
                sock.setSoTimeout(0);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                        sock.getOutputStream(), StandardCharsets.UTF_8), true);
                onWriter.accept(writer);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        sock.getInputStream(), StandardCharsets.UTF_8));

                // SSH-Banner lesen
                String banner = reader.readLine();
                if (banner != null) {
                    appendTerm(doc, banner + "\n", new Color(0x80, 0xE0, 0x80));
                }
                appendTerm(doc, "\n[SSH-Banner empfangen. Für vollen SSH-Support wird JSch benoetigt.]\n"
                                + "[Download: mvnrepository.com/artifact/com.jcraft/jsch]\n"
                                + "[JAR in classpath legen → SSH-Auth wird dann aktiviert.]\n\n",
                        new Color(0xD0, 0xC0, 0x60));

                // Stream weiter lesen (für rohe Antworten)
                char[] buf = new char[1024];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    String chunk = new String(buf, 0, read);
                    appendTerm(doc, chunk, termFg);
                }
            } catch (Exception e) {
                appendTerm(doc, "\n[Verbindungsfehler: " + e.getMessage() + "]\n", WARN);
                appendTerm(doc, "[SSH (Port 22) erreichbar? Firewall prüfen.]\n",
                        new Color(0xD0, 0xC0, 0x60));
            }
        }, "SSH-" + ip).start();
    }
}

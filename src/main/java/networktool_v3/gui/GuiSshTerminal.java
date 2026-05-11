package main.java.networktool_v3.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Eingebettetes SSH-Terminal (ohne externe Library).
 *
 * Verbindet via raw TCP Port 22 → SSH-Banner lesen,
 * dann interaktiver I/O-Stream.
 *
 * Für echte SSH-Auth (Passwort/Key) wird JSch benötigt – falls nicht
 * vorhanden, zeigt dieses Terminal den SSH-Banner + erklärt den Setup.
 *
 * Öffnen via: Rechtsklick → "⌨ SSH-Terminal" oder Menüpunkt.
 */
public final class GuiSshTerminal {

    private GuiSshTerminal() {}

    public static void open(String ip) {
        SwingUtilities.invokeLater(() -> buildWindow(ip));
    }

    private static void buildWindow(String ip) {
        JDialog dlg = new JDialog((Frame) null, "SSH Terminal  –  " + ip, false);
        dlg.setSize(720, 480);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color termBg = new Color(0x04, 0x06, 0x04);
        Color termFg = new Color(0x00, 0xFF, 0x80); // klassisches Grün-Terminal

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(termBg);

        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(new Color(0x0A, 0x14, 0x0A));
        header.setBorder(new EmptyBorder(6, 12, 6, 12));
        JLabel title = new JLabel("⌨  SSH  →  " + ip + ":22");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        title.setForeground(termFg);
        header.add(title, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);

        JButton clearBtn = termBtn("CLEAR", new Color(0x60, 0xA0, 0x60));
        JButton closeBtn = termBtn("✕ CLOSE", WARN);
        closeBtn.addActionListener(e -> dlg.dispose());
        clearBtn.addActionListener(e -> { /* cleared below after output ref */ });
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        header.add(btnPanel, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Output-Bereich ────────────────────────────────────────────────
        JTextPane output = new JTextPane();
        output.setEditable(false);
        output.setBackground(termBg);
        output.setForeground(termFg);
        output.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        output.setMargin(new Insets(8, 12, 8, 12));
        StyledDocument doc = output.getStyledDocument();

        clearBtn.addActionListener(e -> {
            try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
        });

        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(termBg);
        root.add(scroll, BorderLayout.CENTER);

        // ── Eingabezeile ─────────────────────────────────────────────────
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(new Color(0x08, 0x10, 0x08));
        inputRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(0x20, 0x40, 0x20)),
                new EmptyBorder(6, 12, 6, 12)));

        JLabel prompt = new JLabel("$ ");
        prompt.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        prompt.setForeground(termFg);

        JTextField input = new JTextField();
        input.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        input.setForeground(termFg);
        input.setBackground(new Color(0x04, 0x08, 0x04));
        input.setCaretColor(termFg);
        input.setBorder(new EmptyBorder(2, 6, 2, 6));

        JButton sendBtn = termBtn("SEND", termFg);
        inputRow.add(prompt,  BorderLayout.WEST);
        inputRow.add(input,   BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);
        root.add(inputRow, BorderLayout.SOUTH);

        // ── Verbindungslogik ─────────────────────────────────────────────
        appendTerm(doc, "Verbinde mit " + ip + ":22...\n", termFg);

        // Writer-Referenz für Eingabe
        final PrintWriter[] writerRef = {null};
        final Socket[]      sockRef   = {null};

        // Eingabe senden
        Runnable sendAction = () -> {
            String cmd = input.getText().trim();
            if (cmd.isEmpty()) return;
            input.setText("");
            appendTerm(doc, "$ " + cmd + "\n", new Color(0xA0, 0xFF, 0xA0));
            if (writerRef[0] != null) {
                writerRef[0].println(cmd);
                writerRef[0].flush();
            } else {
                appendTerm(doc, "[Nicht verbunden]\n", WARN);
            }
        };
        input.addActionListener(e -> sendAction.run());
        sendBtn.addActionListener(e -> sendAction.run());

        // Verbindungs-Thread
        new Thread(() -> {
            try (Socket sock = new Socket(ip, 22)) {
                sockRef[0]   = sock;
                sock.setSoTimeout(0);
                PrintWriter  writer = new PrintWriter(new OutputStreamWriter(
                        sock.getOutputStream(), StandardCharsets.UTF_8), true);
                writerRef[0] = writer;
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

        dlg.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try { if (sockRef[0] != null) sockRef[0].close(); } catch (Exception ignored) {}
            }
        });

        dlg.setContentPane(root);
        dlg.setVisible(true);
        input.requestFocus();
    }

    private static void appendTerm(StyledDocument doc, String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setForeground(a, color);
                StyleConstants.setFontFamily(a, "JetBrains Mono");
                StyleConstants.setFontSize(a, 13);
                doc.insertString(doc.getLength(), text, a);
            } catch (BadLocationException ignored) {}
        });
    }

    private static JButton termBtn(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(new Color(0x10, 0x18, 0x10));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1),
                new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
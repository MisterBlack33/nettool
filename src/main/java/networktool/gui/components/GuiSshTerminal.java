package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static networktool.theme.GuiTheme.*;
import static networktool.gui.components.TerminalChrome.*;

/**
 * Eingebettetes SSH-Terminal (ohne externe Library).
 *
 * Verbindet via raw TCP Port 22 → SSH-Banner lesen,
 * dann interaktiver I/O-Stream. Verbindungslogik siehe {@link SshConnectionWorker}.
 *
 * Für echte SSH-Auth (Passwort/Key) wird JSch benötigt – falls nicht
 * vorhanden, zeigt dieses Terminal den SSH-Banner + erklärt den Setup.
 *
 * Öffnen via: Rechtsklick → "⌨ SSH-Terminal" oder Menüpunkt.
 */
public final class GuiSshTerminal {

    private static final Logger LOG = Logger.getLogger(GuiSshTerminal.class.getName());

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
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ex) {
                LOG.log(Level.FINE, "Terminal-Ausgabe konnte nicht geleert werden", ex);
            }
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

        SshConnectionWorker.connect(ip, doc, termFg,
                writer -> writerRef[0] = writer,
                sock   -> sockRef[0]   = sock);

        dlg.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    if (sockRef[0] != null) sockRef[0].close();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "SSH-Socket zu " + ip + " konnte nicht sauber geschlossen werden", ex);
                }
            }
        });

        dlg.setContentPane(root);
        dlg.setVisible(true);
        input.requestFocus();
    }
}

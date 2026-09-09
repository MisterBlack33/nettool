package main.java.networktool.gui.components.terminal;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.java.networktool.theme.GuiTheme.*;
import static main.java.networktool.gui.components.terminal.TerminalChrome.appendTerm;
import static main.java.networktool.gui.components.terminal.TerminalChrome.termBtn;

/** Baut das SSH-Terminal-Fenster zusammen: Header, Ausgabe, Eingabezeile, Verbindung. */
final class SshTerminalWindowBuilder {

    private static final Logger LOG = Logger.getLogger(SshTerminalWindowBuilder.class.getName());

    private static final Color TERM_BG = new Color(0x04, 0x06, 0x04);
    private static final Color TERM_FG = new Color(0x00, 0xFF, 0x80);

    private final String ip;
    private final PrintWriter[] writerRef = {null};
    private final Socket[]      sockRef   = {null};
    private JTextPane output;
    private JTextField input;

    SshTerminalWindowBuilder(String ip) { this.ip = ip; }

    void buildAndShow() {
        JDialog dlg = new JDialog((Frame) null, "SSH Terminal  –  " + ip, false);
        dlg.setSize(720, 480);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(TERM_BG);
        root.add(buildHeader(dlg), BorderLayout.NORTH);
        root.add(buildOutputScroll(), BorderLayout.CENTER);
        root.add(buildInputRow(), BorderLayout.SOUTH);

        appendTerm(output.getStyledDocument(), "Verbinde mit " + ip + ":22...\n", TERM_FG);
        SshConnectionWorker.connect(ip, output.getStyledDocument(), TERM_FG,
                writer -> writerRef[0] = writer, sock -> sockRef[0] = sock);

        dlg.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { closeSocket(); }
        });
        dlg.setContentPane(root);
        dlg.setVisible(true);
        input.requestFocus();
    }

    private void closeSocket() {
        try {
            if (sockRef[0] != null) sockRef[0].close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "SSH-Socket zu " + ip + " konnte nicht sauber geschlossen werden", e);
        }
    }

    private JPanel buildHeader(JDialog dlg) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(new Color(0x0A, 0x14, 0x0A));
        header.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel title = new JLabel("⌨  SSH  →  " + ip + ":22");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        title.setForeground(TERM_FG);
        header.add(title, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);
        JButton clearBtn = termBtn("CLEAR", new Color(0x60, 0xA0, 0x60));
        JButton closeBtn = termBtn("✕ CLOSE", WARN);
        clearBtn.addActionListener(e -> clearOutput());
        closeBtn.addActionListener(e -> dlg.dispose());
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        header.add(btnPanel, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildOutputScroll() {
        output = new JTextPane();
        output.setEditable(false);
        output.setBackground(TERM_BG);
        output.setForeground(TERM_FG);
        output.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        output.setMargin(new Insets(8, 12, 8, 12));

        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(TERM_BG);
        return scroll;
    }

    private void clearOutput() {
        StyledDocument doc = output.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            LOG.log(Level.FINE, "Terminal-Ausgabe konnte nicht geleert werden", e);
        }
    }

    private JPanel buildInputRow() {
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(new Color(0x08, 0x10, 0x08));
        inputRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(0x20, 0x40, 0x20)),
                new EmptyBorder(6, 12, 6, 12)));

        JLabel prompt = new JLabel("$ ");
        prompt.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        prompt.setForeground(TERM_FG);

        input = new JTextField();
        input.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        input.setForeground(TERM_FG);
        input.setBackground(new Color(0x04, 0x08, 0x04));
        input.setCaretColor(TERM_FG);
        input.setBorder(new EmptyBorder(2, 6, 2, 6));

        JButton sendBtn = termBtn("SEND", TERM_FG);
        Runnable sendAction = this::sendCommand;
        input.addActionListener(e -> sendAction.run());
        sendBtn.addActionListener(e -> sendAction.run());

        inputRow.add(prompt,  BorderLayout.WEST);
        inputRow.add(input,   BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);
        return inputRow;
    }

    private void sendCommand() {
        String cmd = input.getText().trim();
        if (cmd.isEmpty()) return;
        input.setText("");
        appendTerm(output.getStyledDocument(), "$ " + cmd + "\n", new Color(0xA0, 0xFF, 0xA0));
        if (writerRef[0] != null) {
            writerRef[0].println(cmd);
            writerRef[0].flush();
        } else {
            appendTerm(output.getStyledDocument(), "[Nicht verbunden]\n", WARN);
        }
    }
}

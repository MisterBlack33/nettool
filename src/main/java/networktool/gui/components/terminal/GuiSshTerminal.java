package main.java.networktool.gui.components.terminal;

import javax.swing.*;

/**
 * Eingebettetes SSH-Terminal (ohne externe Library) — Rechtsklick → "⌨ SSH-Terminal".
 * Fensteraufbau siehe {@link SshTerminalWindowBuilder}, Verbindungslogik siehe
 * {@link SshConnectionWorker}, gemeinsame UI-Bausteine siehe {@link TerminalChrome}.
 */
public final class GuiSshTerminal {

    private GuiSshTerminal() {}

    public static void open(String ip) {
        SwingUtilities.invokeLater(() -> new SshTerminalWindowBuilder(ip).buildAndShow());
    }
}

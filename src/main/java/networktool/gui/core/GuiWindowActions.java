package networktool.gui.core;

import networktool.gui.components.GuiStatusBar;
import networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import networktool.theme.GuiTheme;

/**
 * Fenster-nahe Querschnitts-Funktionen des Hauptfensters:
 * Tastenkürzel, Vollbild auf dem richtigen Monitor, Beenden-Bestätigung,
 * sanfter Theme-Wechsel. Operiert auf einer übergebenen {@link JFrame}-Instanz.
 */
final class GuiWindowActions {

    private GuiWindowActions() {}

    static void installKeyboardShortcuts(JFrame frame, GuiMenuHandler menuHandler,
                                          Runnable onQuit, Runnable onRestart, Runnable onSearchToggle) {
        JRootPane root = frame.getRootPane();
        InputMap  im   = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "quit");
        am.put("quit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onQuit.run(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "restart");
        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onRestart.run(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "cancel");
        am.put("cancel", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { menuHandler.cancel(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "search");
        am.put("search", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onSearchToggle.run(); }
        });
    }

    static void installWindowClose(JFrame frame, Runnable onQuit) {
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onQuit.run(); }
        });
    }

    static void confirmQuit(JFrame frame, GuiMenuHandler menuHandler) {
        if (menuHandler.isRunning()) {
            int c = JOptionPane.showConfirmDialog(frame,
                    "<html><b>Ein Scan läuft gerade.</b><br>Wirklich beenden?</html>",
                    "Beenden bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }
        AuditLogger.getInstance().log("APP_EXIT", UserAuth.getInstance().getCurrentUser());
        System.exit(0);
    }

    static void enterFullscreen(JFrame frame, GraphicsDevice loginMonitor) {
        frame.setUndecorated(true);
        GraphicsDevice target = (loginMonitor != null)
                ? loginMonitor
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        frame.setBounds(target.getDefaultConfiguration().getBounds());
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    static void toggleTheme(JFrame frame, GuiOutputPanel outputPanel, GuiStatusBar statusBar) {
        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        String msg = GuiTheme.isDark() ? "Dark Mode" : "Light Mode";
        frame.getContentPane().setBackground(GuiTheme.BG);

        // 3 Repaints im 16-ms-Takt → smooth, kein hartes Flackern
        int[] count = {0};
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            SwingUtilities.updateComponentTreeUI(frame);
            frame.repaint(); frame.revalidate();
            if (++count[0] >= 3) t.stop();
        });
        t.start();

        outputPanel.appendText("  " + msg + " aktiviert.\n", GuiTheme.ACCENT);
        statusBar.set("Theme: " + msg, GuiTheme.ACCENT2);
        AuditLogger.getInstance().log("THEME_TOGGLE", msg);
    }
}

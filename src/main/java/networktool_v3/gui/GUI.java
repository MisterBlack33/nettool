package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.analysis.OuiUpdater;
import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.security.LoginDialog;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.security.AuditLogger;
import main.java.networktool_v3.security.SecurityMonitor;
import main.java.networktool_v3.security.UserAuth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Haupt-Fenster der Anwendung.
 *
 * Besonderheiten:
 *  - searchBar-Feld MUSS vor savedHostsPanel deklariert/initialisiert werden
 *  - Fenster öffnet sich auf dem Monitor des Login-Dialogs
 *  - Admin-Check für Menü 11 (Fremdnetz) und 23 (Audit-Log) in handleMenuClick
 *  - GuiSearchBar wird ausschließlich über savedHostsPanel.show() aktiviert
 *    und bei jedem anderen Menüklick über searchBar.hide() deaktiviert
 *  - Smooth Theme-Wechsel via Swing-Timer (3 Repaints à 16 ms)
 */
public class GUI extends JFrame {

    private static GUI INSTANCE;
    public static boolean isGuiActive() {
        return INSTANCE != null && INSTANCE.isDisplayable();
    }
    public static GUI     instance()    { return INSTANCE; }

    /** Monitor auf dem der Login-Dialog angezeigt wurde. */
    private static GraphicsDevice loginMonitor = null;
    public static void setLoginMonitor(GraphicsDevice device) { loginMonitor = device; }

    // !! Reihenfolge der Deklaration = Reihenfolge der Initialisierung !!
    // searchBar MUSS vor savedHostsPanel stehen – wird als Parameter übergeben.
    private final GuiSearchBar       searchBar;
    private final GuiOutputPanel     outputPanel;
    private final GuiProgressBar     progressBar;
    private final GuiStatusBar       statusBar;
    private final GuiInputPanel      inputPanel;
    private final GuiTableRenderer   tableRenderer;
    private final GuiMenuHandler     menuHandler;
    private final GuiContextMenu     contextMenu;
    private final GuiSavedHostsPanel savedHostsPanel;

    public GUI() {
        super("NetTool //");
        INSTANCE = this;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 520));
        getContentPane().setBackground(GuiTheme.BG);
        setLayout(new BorderLayout());

        // Initialisierungsreihenfolge beachten!
        searchBar       = new GuiSearchBar();
        outputPanel     = new GuiOutputPanel();
        progressBar     = new GuiProgressBar();
        statusBar       = new GuiStatusBar();
        inputPanel      = new GuiInputPanel(statusBar.getLabel(), outputPanel);
        tableRenderer   = new GuiTableRenderer(outputPanel);
        menuHandler     = new GuiMenuHandler(inputPanel, outputPanel, tableRenderer, statusBar);
        contextMenu     = new GuiContextMenu(menuHandler, outputPanel);
        savedHostsPanel = new GuiSavedHostsPanel(menuHandler, outputPanel, contextMenu, searchBar);

        tableRenderer.setContextMenu(contextMenu);
        menuHandler.setSavedHostsPanel(savedHostsPanel);

        outputPanel.redirectStreams();
        buildLayout();
        installKeyboardShortcuts();
        installWindowClose();

        enterFullscreen();
        setVisible(true);
        outputPanel.printBanner();

        // Benutzer-Info
        String user   = UserAuth.getInstance().getCurrentUser();
        boolean admin = UserAuth.getInstance().isAdmin();
        if (user != null) {
            String roleLabel = admin ? "  [admin]" : "  [user]";
            outputPanel.appendText("  Eingeloggt als: " + user + roleLabel + "\n\n",
                    admin ? GuiTheme.ACCENT : GuiTheme.ACCENT2);
        }

        MessageSender.startListener();
        OuiUpdater.initAsync(NetworkStore.getInstance().txtDir);

        // SecurityMonitor nach 5 s passiv starten
        new Thread(() -> {
            try { Thread.sleep(5_000); } catch (InterruptedException ignored) {}
            if (!SecurityMonitor.getInstance().isActive())
                SecurityMonitor.getInstance().start("");
        }, "SecurityMonitor-Init").start();
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private void buildLayout() {
        add(GuiSidebar.build(
                this::handleMenuClick,
                menuHandler::cancel,
                this::restart,
                this::toggleTheme,
                menuHandler::isRunning
        ), BorderLayout.WEST);
        add(buildMainPanel(),       BorderLayout.CENTER);
        add(statusBar.buildPanel(), BorderLayout.SOUTH);
    }

    /**
     * Zentraler Menü-Dispatch mit Admin-Check und SearchBar-Steuerung.
     *
     * Regeln:
     *  "09" → savedHostsPanel.show() ruft searchBar.show() intern
     *  alle anderen → searchBar.hide()
     *  "11" Fremdnetz → nur Admins
     *  "23" Audit-Log → nur Admins
     *  "30" Privatsphäre → GuiPrivacyPanel
     */
    private void handleMenuClick(String id) {
        // Admin-Only
        if ("11".equals(id) && !UserAuth.getInstance().isAdmin()) {
            outputPanel.appendText("  ✕ Fremdnetz-Scanner: nur für Admins.\n", GuiTheme.WARN);
            return;
        }
        if ("23".equals(id)) {
            if (!UserAuth.getInstance().isAdmin()) {
                outputPanel.appendText("  ✕ Audit-Log: nur für Admins.\n", GuiTheme.WARN);
                return;
            }
            AuditLogger.getInstance().log("MENU", "23-AuditLog");
            GuiAuditPanel.show(outputPanel);
            return;
        }
        if ("30".equals(id)) {
            AuditLogger.getInstance().log("MENU", "30-Privacy");
            GuiPrivacyPanel.show(outputPanel);
            searchBar.hide(); // sicherheitshalber
            return;
        }

        // SearchBar: bei allen Menüpunkten außer 09 ausblenden.
        // Bei 09 übernimmt savedHostsPanel.show() die Aktivierung.
        if (!"09".equals(id)) {
            searchBar.hide();
        }

        menuHandler.handle(id);
    }

    private JPanel buildMainPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(GuiTheme.PANEL_BG);
        bottom.add(progressBar.getPanel(),  BorderLayout.NORTH);
        bottom.add(inputPanel.buildPanel(), BorderLayout.SOUTH);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setBackground(GuiTheme.BG);
        // searchBar ist standardmäßig unsichtbar; liegt trotzdem im Layout
        centerArea.add(searchBar,                     BorderLayout.NORTH);
        centerArea.add(outputPanel.buildScrollPane(), BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(GuiTheme.BG);
        main.add(outputPanel.buildTopBar(), BorderLayout.NORTH);
        main.add(centerArea,                BorderLayout.CENTER);
        main.add(bottom,                    BorderLayout.SOUTH);
        return main;
    }

    // ── Keyboard Shortcuts ────────────────────────────────────────────────

    private void installKeyboardShortcuts() {
        JRootPane root = getRootPane();
        InputMap  im   = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "quit");
        am.put("quit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { confirmQuit(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "restart");
        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { restart(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "cancel");
        am.put("cancel", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { menuHandler.cancel(); }
        });
        // Ctrl+F: SearchBar toggle – nur wenn SavedHosts aktiv (searchBar sichtbar)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "search");
        am.put("search", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (searchBar.isVisible()) {
                    if (searchBar.isSearchVisible()) searchBar.hide();
                    else searchBar.show();
                }
            }
        });
    }

    // ── Window-Close ─────────────────────────────────────────────────────

    private void installWindowClose() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmQuit(); }
        });
    }

    private void confirmQuit() {
        if (menuHandler.isRunning()) {
            int c = JOptionPane.showConfirmDialog(this,
                    "<html><b>Ein Scan läuft gerade.</b><br>Wirklich beenden?</html>",
                    "Beenden bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }
        AuditLogger.getInstance().log("APP_EXIT", UserAuth.getInstance().getCurrentUser());
        System.exit(0);
    }

    // ── Theme Toggle (smooth) ─────────────────────────────────────────────

    private void toggleTheme() {
        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        String msg = GuiTheme.isDark() ? "Dark Mode" : "Light Mode";
        getContentPane().setBackground(GuiTheme.BG);

        // 3 Repaints im 16-ms-Takt → smooth, kein hartes Flackern
        int[] count = {0};
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            SwingUtilities.updateComponentTreeUI(this);
            repaint(); revalidate();
            if (++count[0] >= 3) t.stop();
        });
        t.start();

        outputPanel.appendText("  " + msg + " aktiviert.\n", GuiTheme.ACCENT);
        statusBar.set("Theme: " + msg, GuiTheme.ACCENT2);
        AuditLogger.getInstance().log("THEME_TOGGLE", msg);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public void showProgress(int total)  { progressBar.showProgress(total); }
    public void updateProgress(int done) { progressBar.updateProgress(done); }

    public void showHostTable(List<HostResult> rows, String title) {
        tableRenderer.showHostTable(rows, title);
    }
    public void showScanTable(List<ScanResult> rows) {
        tableRenderer.showScanTable(rows);
    }

    public void appendText(String text, Color color) { outputPanel.appendText(text, color); }
    public void setStatus(String msg, Color color)   { statusBar.set(msg, color); }
    public JTextPane getOutputPane()                 { return outputPanel.getOutputPane(); }

    // ── Neustart ─────────────────────────────────────────────────────────

    private void restart() {
        AuditLogger.getInstance().log("APP_RESTART", UserAuth.getInstance().getCurrentUser());
        SecurityMonitor.getInstance().stop();
        loginMonitor = getGraphicsConfiguration().getDevice();
        dispose();
        INSTANCE = null;
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            boolean ok = LoginDialog.show(UserAuth.getInstance());
            if (!ok) System.exit(0);
            AuditLogger.getInstance().log("LOGIN_AFTER_RESTART",
                    UserAuth.getInstance().getCurrentUser());
            new GUI();
        });
    }

    // ── Fullscreen auf richtigem Monitor ──────────────────────────────────

    private void enterFullscreen() {
        setUndecorated(true);
        GraphicsDevice target = (loginMonitor != null)
                ? loginMonitor
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        setBounds(target.getDefaultConfiguration().getBounds());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new GUI();
        });
    }
}

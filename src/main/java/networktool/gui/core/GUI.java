package main.java.networktool.gui.core;

import main.java.networktool.gui.components.GuiProgressBar;
import main.java.networktool.gui.components.GuiSidebar;
import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.components.actions.GuiContextMenu;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.panels.saved.GuiSavedHostsPanel;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.security.UserAuth;
import main.java.networktool.theme.GuiTheme;
import main.java.networktool.util.AppIcon;

/**
 * Haupt-Fenster der Anwendung.
 *
 * Besonderheiten:
 *  - searchBar-Feld MUSS vor savedHostsPanel deklariert/initialisiert werden
 *  - Fenster öffnet sich auf dem Monitor des Login-Dialogs
 *  - GuiSearchBar wird ausschließlich über savedHostsPanel.show() aktiviert
 *    und bei jedem anderen Menüklick über searchBar.hide() deaktiviert
 *
 * Fenster-Chrome (Shortcuts/Fullscreen/Theme) siehe {@link GuiWindowActions},
 * Start-Hintergrundaufgaben siehe {@link GuiStartupTasks},
 * Menü-Dispatch siehe {@link GuiMenuDispatch}.
 */
public class GUI extends JFrame {

    private static final Logger LOG = Logger.getLogger(GUI.class.getName());

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
    private final GuiOutputPanel outputPanel;
    private final GuiProgressBar progressBar;
    private final GuiStatusBar statusBar;
    private final GuiInputPanel inputPanel;
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
        GuiWindowActions.installKeyboardShortcuts(this, menuHandler,
                () -> GuiWindowActions.confirmQuit(this, menuHandler), this::restart, this::toggleSearchBar);
        GuiWindowActions.installWindowClose(this, () -> GuiWindowActions.confirmQuit(this, menuHandler));

        GuiWindowActions.enterFullscreen(this, loginMonitor);
        AppIcon.apply(this);
        setVisible(true);
        outputPanel.printBanner();

        GuiStartupTasks.run(outputPanel);
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

    private void handleMenuClick(String id) {
        GuiMenuDispatch.handle(id, outputPanel, searchBar, menuHandler);
    }

    private void toggleSearchBar() {
        if (searchBar.isVisible()) {
            if (searchBar.isSearchVisible()) searchBar.hide();
            else searchBar.show();
        }
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

    private void toggleTheme() {
        GuiWindowActions.toggleTheme(this, outputPanel, statusBar);
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
            catch (Exception e) { LOG.log(Level.FINE, "System-Look-and-Feel konnte nicht gesetzt werden", e); }
            boolean ok = LoginDialog.show(UserAuth.getInstance());
            if (!ok) System.exit(0);
            AuditLogger.getInstance().log("LOGIN_AFTER_RESTART",
                    UserAuth.getInstance().getCurrentUser());
            new GUI();
        });
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { LOG.log(Level.FINE, "System-Look-and-Feel konnte nicht gesetzt werden", e); }
            new GUI();
        });
    }
}

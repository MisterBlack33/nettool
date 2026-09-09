package main.java.networktool.gui.core;

import main.java.networktool.filter.OutputRenderer;
import main.java.networktool.filter.OutputRendererRegistry;
import main.java.networktool.gui.components.GuiProgressBar;
import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.components.actions.GuiContextMenu;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.panels.saved.GuiSavedHostsPanel;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.theme.GuiTheme;
import main.java.networktool.util.AppIcon;

/**
 * Haupt-Fenster der Anwendung.
 *
 * Fenster-Chrome (Shortcuts/Fullscreen/Theme) siehe {@link GuiWindowActions},
 * Start-Hintergrundaufgaben siehe {@link GuiStartupTasks},
 * Menü-Dispatch siehe {@link GuiMenuDispatch},
 * Layout-/Shortcut-Aufbau siehe {@link GuiFrameLayout},
 * Neustart-/Launch-Ablauf siehe {@link GuiRestartFlow}.
 *
 * Implementiert {@link OutputRenderer}, damit {@code filter.*} keine
 * Compile-Abhängigkeit auf diese Klasse braucht.
 */
public class GUI extends JFrame implements OutputRenderer {

    private static GUI INSTANCE;
    public static boolean isGuiActive() { return INSTANCE != null && INSTANCE.isDisplayable(); }
    public static GUI     instance()    { return INSTANCE; }
    static void clearInstance()         { INSTANCE = null; }

    /** Monitor auf dem der Login-Dialog angezeigt wurde. */
    private static GraphicsDevice loginMonitor = null;
    public static void setLoginMonitor(GraphicsDevice device) { loginMonitor = device; }

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
        OutputRendererRegistry.register(this);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 520));
        getContentPane().setBackground(GuiTheme.BG);
        setLayout(new BorderLayout());

        // Reihenfolge beachten: searchBar muss vor savedHostsPanel initialisiert sein.
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
        GuiFrameLayout.assemble(this, searchBar, outputPanel, progressBar, statusBar, inputPanel,
                menuHandler, this::handleMenuClick, this::restart, this::toggleTheme, this::toggleSearchBar);

        GuiWindowActions.enterFullscreen(this, loginMonitor);
        AppIcon.apply(this);
        setVisible(true);
        outputPanel.printBanner();

        GuiStartupTasks.run(outputPanel);
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

    private void toggleTheme() {
        GuiWindowActions.toggleTheme(this, outputPanel, statusBar);
    }

    // ── OutputRenderer ────────────────────────────────────────────────────

    @Override public boolean isActive() { return isDisplayable(); }

    @Override public void showHostTable(List<HostResult> rows, String title) {
        tableRenderer.showHostTable(rows, title);
    }

    @Override public void showScanTable(List<ScanResult> rows) {
        tableRenderer.showScanTable(rows);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public void showProgress(int total)  { progressBar.showProgress(total); }
    public void updateProgress(int done) { progressBar.updateProgress(done); }

    public void appendText(String text, Color color) { outputPanel.appendText(text, color); }
    public void setStatus(String msg, Color color)   { statusBar.set(msg, color); }
    public JTextPane getOutputPane()                 { return outputPanel.getOutputPane(); }

    private void restart() { GuiRestartFlow.restart(this); }

    @SuppressWarnings("unused")
    public static void launch() { GuiRestartFlow.launch(); }
}

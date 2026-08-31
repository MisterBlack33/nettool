package main.java.networktool.gui.core;

import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.components.actions.GuiContextMenu;
import main.java.networktool.gui.components.actions.GuiDiagnosticsActions;
import main.java.networktool.gui.components.actions.GuiDataIOActions;
import main.java.networktool.gui.components.scan.GuiScanActions;
import main.java.networktool.gui.components.scan.GuiForeignNetActions;
import main.java.networktool.gui.components.scan.GuiScanProfileActions;
import main.java.networktool.gui.components.scan.GuiScanCompareActions;
import main.java.networktool.gui.components.scan.GuiSchedulerActions;
import main.java.networktool.gui.components.scan.GuiSonifyActions;
import main.java.networktool.gui.components.map.GuiNetworkMap;
import main.java.networktool.gui.notification.NotificationListener;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.panels.saved.GuiSavedHostsPanel;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.logic.scan.host.NetworkInfo;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.network.NetworkStore;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;

import java.util.concurrent.atomic.AtomicReference;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Verarbeitet Sidebar-Klicks und startet Aktionen asynchron.
 * Menü-ID → Handler-Zuordnung liegt in {@link GuiMenuRegistry}; die eigentliche
 * Dialog-/Ausführungslogik pro Menüpunkt lebt in den thematischen
 * {@code Gui*Actions}-Klassen unter {@code networktool.gui.components}.
 */
public class GuiMenuHandler {

    @FunctionalInterface public interface RunnableEx { void run() throws Exception; }

    private final GuiInputPanel input;
    private final GuiOutputPanel output;
    private final GuiTableRenderer tables;
    private final GuiStatusBar status;
    private final GuiMenuRegistry registry = new GuiMenuRegistry();
    private GuiSavedHostsPanel savedHostsPanel;

    private final AtomicReference<Thread> runningThread = new AtomicReference<>();

    public GuiMenuHandler(GuiInputPanel input, GuiOutputPanel output,
                          GuiTableRenderer tables, GuiStatusBar status) {
        this.input = input; this.output = output;
        this.tables = tables; this.status = status;
        registerHandlers();
    }

    public void setSavedHostsPanel(GuiSavedHostsPanel p) { this.savedHostsPanel = p; }

    // ── Registrierung ─────────────────────────────────────────────────────

    private void registerHandlers() {
        registry.register("01", () -> input.ask("Netzwerkinfo starten? [Enter]",
                v -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_MINIMAL",""); NetworkInfo.showMinimalInfo(); })));
        registry.register("02", () -> input.ask("Vollständige Info starten? [Enter]",
                v -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_FULL",""); NetworkInfo.showFullInfo(); })));
        registry.register("03", () -> GuiDiagnosticsActions.handleDiagnose(input, this));
        registry.register("04", () -> input.ask("Port:", p -> runAsync(() -> {
            AuditLogger.getInstance().log("FILE_SERVER","port="+p);
            new FileServer(Integer.parseInt(p)).start();
            output.appendText("  ✔ Server auf Port " + p + "\n", ACCENT2);
        })));
        registry.register("05", () -> input.ask("Ziel-IP:", ip -> input.ask("Port:", p ->
                input.ask("Dateipfad:", path -> runAsync(() -> {
                    AuditLogger.getInstance().log("FILE_SEND", ip+":"+p);
                    new FileClient(ip, Integer.parseInt(p)).sendFile(path);
                })))));
        registry.register("06", () -> GuiScanActions.handleCidrScan(input, output, tables, this));
        registry.register("07", () -> GuiScanActions.handleFilterScan(input, this));
        registry.register("08", this::handleSendMessage);
        registry.register("09", () -> { if (savedHostsPanel != null) savedHostsPanel.show(); });
        registry.register("10", () -> input.ask("Hop-Analyse starten? [Enter]",
                v -> runAsync(() -> { AuditLogger.getInstance().log("HOP_ANALYSE",""); GuiScanActions.runNetworkInfoWithHops(tables); })));
        registry.register("11", () -> GuiForeignNetActions.handleRemoteNetScan(input, output, status, this));
        registry.register("12", () -> GuiScanProfileActions.handleScanProfiles(input, output, tables, status, this));
        registry.register("13", () -> GuiScanCompareActions.handleScanDelta(input, this));
        registry.register("14", () -> GuiSchedulerActions.handleScheduler(input, output, status));
        registry.register("15", () -> GuiDiagnosticsActions.handleBandwidthTest(input, this));
        registry.register("16", () -> GuiDiagnosticsActions.handleDauerping(input, this));
        registry.register("17", () -> GuiDiagnosticsActions.handleSecurityMonitor(input, output));
        registry.register("18", () -> GuiDataIOActions.handleExportImport(input, output, this));
        registry.register("19", () -> GuiDataIOActions.handleNotificationHistory(input, output));
        registry.register("20", () -> { AuditLogger.getInstance().log("NETWORK_MAP",""); GuiNetworkMap.show(); });
        registry.register("21", () -> GuiDataIOActions.handlePortConfig(input, output, status));
        registry.register("22", () -> GuiScanCompareActions.handleScanHistoryDelta(output, this));
        // Test-Suite: Data-to-Sound (Netzwerk-Traffic-Sonifizierung) Toggle
        registry.register("24", () -> GuiSonifyActions.toggle(input, output));
    }

    // ── Dispatch ──────────────────────────────────────────────────────────

    public void handle(String num) {
        AuditLogger.getInstance().log("MENU", num);
        registry.dispatch(num);
    }

    public void cancel() {
        Thread t = runningThread.getAndSet(null);
        if (t != null && t.isAlive()) {
            t.interrupt();
            AuditLogger.getInstance().log("CANCEL", "");
            status.set("Abgebrochen", WARN);
        } else {
            status.set("Bereit", FG_DIM);
        }
    }

    public boolean isRunning() {
        Thread t = runningThread.get();
        return t != null && t.isAlive();
    }

    // ── Nachricht ─────────────────────────────────────────────────────────

    private void handleSendMessage() {
        input.ask("Ziel-IP:", ip ->
                input.ask("Nachricht:", msg -> {
                    String topic = main.java.networktool.gui.components.actions.GuiContextMenu.promptNtfyTopic();
                    if (topic == null) return;
                    final String ft = topic.trim();
                    if (!ft.isEmpty()) {
                        NetworkStore.getInstance().saveNtfyTopic(ft);
                        NotificationListener.subscribeNewTopic(ft);
                    }
                    AuditLogger.getInstance().log("MSG_SEND", ip + " topic=" + ft);
                    runAsync(() -> MessageSender.send(ip, msg, ft));
                }));
    }

    // ── Async-Runner ──────────────────────────────────────────────────────

    public void runAsync(RunnableEx task) {
        status.set("Läuft…", ACCENT);
        Thread t = new Thread(() -> {
            try { task.run(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (Exception e) { output.appendText("  ✕ " + e.getMessage() + "\n", WARN); }
            finally {
                runningThread.compareAndSet(Thread.currentThread(), null);
                if (!Thread.currentThread().isInterrupted()) status.set("Fertig", ACCENT2);
            }
        });
        runningThread.set(t);
        t.start();
    }
}
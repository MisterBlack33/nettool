package main.java.networktool.gui.core;

import main.java.networktool.gui.components.*;
import main.java.networktool.gui.notification.NotificationListener;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.panels.GuiSavedHostsPanel;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.logic.scan.NetworkInfo;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.NetworkStore;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;
import main.java.networktool.gui.components.*;

import java.util.concurrent.atomic.AtomicReference;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Verarbeitet Sidebar-Klicks und startet Aktionen asynchron.
 * Die eigentliche Dialog-/Ausführungslogik pro Menüpunkt lebt in den
 * thematischen {@code Gui*Actions}-Klassen unter {@code networktool.gui.components}.
 */
public class GuiMenuHandler {

    @FunctionalInterface public interface RunnableEx { void run() throws Exception; }

    private final GuiInputPanel input;
    private final GuiOutputPanel output;
    private final GuiTableRenderer tables;
    private final GuiStatusBar     status;
    private GuiSavedHostsPanel savedHostsPanel;

    private final AtomicReference<Thread> runningThread = new AtomicReference<>();

    public GuiMenuHandler(GuiInputPanel input, GuiOutputPanel output,
                          GuiTableRenderer tables, GuiStatusBar status) {
        this.input = input; this.output = output;
        this.tables = tables; this.status = status;
    }

    public void setSavedHostsPanel(GuiSavedHostsPanel p) { this.savedHostsPanel = p; }

    // ── Dispatch ──────────────────────────────────────────────────────────

    public void handle(String num) {
        AuditLogger.getInstance().log("MENU", num);
        switch (num) {
            case "01" -> input.ask("Netzwerkinfo starten? [Enter]",
                    v -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_MINIMAL",""); NetworkInfo.showMinimalInfo(); }));
            case "02" -> input.ask("Vollständige Info starten? [Enter]",
                    v -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_FULL",""); NetworkInfo.showFullInfo(); }));
            case "03" -> GuiDiagnosticsActions.handleDiagnose(input, this);
            case "04" -> input.ask("Port:", p -> runAsync(() -> {
                AuditLogger.getInstance().log("FILE_SERVER","port="+p);
                new FileServer(Integer.parseInt(p)).start();
                output.appendText("  ✔ Server auf Port " + p + "\n", ACCENT2);
            }));
            case "05" -> input.ask("Ziel-IP:", ip -> input.ask("Port:", p ->
                    input.ask("Dateipfad:", path -> runAsync(() -> {
                        AuditLogger.getInstance().log("FILE_SEND", ip+":"+p);
                        new FileClient(ip, Integer.parseInt(p)).sendFile(path);
                    }))));
            case "06" -> GuiScanActions.handleCidrScan(input, output, tables, this);
            case "07" -> GuiScanActions.handleFilterScan(input, this);
            case "08" -> handleSendMessage();
            case "09" -> { if (savedHostsPanel != null) savedHostsPanel.show(); }
            case "10" -> input.ask("Hop-Analyse starten? [Enter]",
                    v -> runAsync(() -> { AuditLogger.getInstance().log("HOP_ANALYSE",""); GuiScanActions.runNetworkInfoWithHops(tables); }));
            case "11" -> GuiForeignNetActions.handleRemoteNetScan(input, output, status, this);
            case "12" -> GuiScanProfileActions.handleScanProfiles(input, output, tables, status, this);
            case "13" -> GuiScanCompareActions.handleScanDelta(input, this);
            case "14" -> GuiSchedulerActions.handleScheduler(input, output, status);
            case "15" -> GuiDiagnosticsActions.handleBandwidthTest(input, this);
            case "16" -> GuiDiagnosticsActions.handleDauerping(input, this);
            case "17" -> GuiDiagnosticsActions.handleSecurityMonitor(input, output);
            case "18" -> GuiDataIOActions.handleExportImport(input, output, this);
            case "19" -> GuiDataIOActions.handleNotificationHistory(input, output);
            case "20" -> { AuditLogger.getInstance().log("NETWORK_MAP",""); GuiNetworkMap.show(); }
            case "21" -> GuiDataIOActions.handlePortConfig(input, output, status);
            case "22" -> GuiScanCompareActions.handleScanHistoryDelta(output, this);
            // Test-Suite: Data-to-Sound (Netzwerk-Traffic-Sonifizierung) Toggle
            case "24" -> GuiSonifyActions.toggle(input, output);
        }
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
                    String topic = GuiContextMenu.promptNtfyTopic();
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
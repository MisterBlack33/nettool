package main.java.networktool_v3.gui;

import main.java.networktool_v3.gui.notification.NotificationListener;
import main.java.networktool_v3.logic.analysis.IpInspector;
import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.logic.scan.NetworkInfo;
import main.java.networktool_v3.logic.scan.NetworkScanner;
import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.logic.scan.ScanDelta;
import main.java.networktool_v3.logic.scan.ScanScheduler;
import main.java.networktool_v3.logic.analysis.PingMonitor;
import main.java.networktool_v3.logic.analysis.ArpMonitor;
import main.java.networktool_v3.logic.scan.PortChangeMonitor;
import main.java.networktool_v3.logic.ports.PortScanner;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.security.SecurityMonitor;
import main.java.networktool_v3.model.ScanProfile;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.security.AuditLogger;
import main.java.networktool_v3.storage.ScanProfileStore;
import main.java.networktool_v3.transfer.BandwidthTester;
import java.util.List;
import java.util.ArrayList;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.storage.DataExportImport;
import main.java.networktool_v3.storage.NotificationHistory;
import main.java.networktool_v3.logic.scan.ScanHistory;
import main.java.networktool_v3.transfer.FileClient;
import main.java.networktool_v3.transfer.FileServer;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Verarbeitet Sidebar-Klicks und startet Aktionen asynchron.
 * Alle Aktionen werden über {@link AuditLogger} protokolliert.
 */
public class GuiMenuHandler {

    @FunctionalInterface public interface RunnableEx { void run() throws Exception; }

    private final GuiInputPanel    input;
    private final GuiOutputPanel   output;
    private final GuiTableRenderer tables;
    private final GuiStatusBar     status;
    private GuiSavedHostsPanel     savedHostsPanel;

    private final AtomicReference<Thread> runningThread = new AtomicReference<>();

    public GuiMenuHandler(GuiInputPanel input, GuiOutputPanel output,
                          GuiTableRenderer tables, GuiStatusBar status) {
        this.input = input; this.output = output;
        this.tables = tables; this.status = status;
    }

    public void setSavedHostsPanel(GuiSavedHostsPanel panel) { this.savedHostsPanel = panel; }

    // ── Dispatch ──────────────────────────────────────────────────────────

    public void handle(String num) {
        AuditLogger.getInstance().log("MENU", num);
        switch (num) {
            case "01" -> input.ask("Minimale Netzwerkinfo starten? [Enter]",
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_MINIMAL",""); NetworkInfo.showMinimalInfo(); }));
            case "02" -> input.ask("Vollständige Netzwerkinfo starten? [Enter]",
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_FULL",""); NetworkInfo.showFullInfo(); }));
            case "03" -> handleDiagnose();
            case "04" -> input.ask("Port für Server:",
                    p -> runAsync(() -> { AuditLogger.getInstance().log("FILE_SERVER","port="+p); new FileServer(Integer.parseInt(p)).start(); }));
            case "05" -> input.ask("Ziel-IP:", ip -> input.ask("Port:", p ->
                    input.ask("Dateipfad:", path -> runAsync(() -> {
                        AuditLogger.getInstance().log("FILE_SEND", ip+":"+p+" "+path);
                        new FileClient(ip, Integer.parseInt(p)).sendFile(path);
                    }))));
            case "06" -> GuiScanActions.handleCidrScan(input, output, tables, this);
            case "07" -> GuiScanActions.handleFilterScan(input, this);
            case "08" -> handleSendMessage();
            case "09" -> { if (savedHostsPanel != null) savedHostsPanel.show(); }
            case "11" -> handleRemoteNetScan();
            case "10" -> input.ask("Netzwerkinfo + Hop-Analyse starten? [Enter]",
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("HOP_ANALYSE",""); GuiScanActions.runNetworkInfoWithHops(tables); }));
            case "12" -> handleScanProfiles();
            case "13" -> handleScanDelta();
            case "14" -> handleScheduler();
            case "15" -> handleBandwidthTest();
            case "16" -> handleDauerping();
            case "17" -> handleSecurityMonitor();
            case "18" -> handleExportImport();
            case "19" -> handleNotificationHistory();
            case "20" -> { AuditLogger.getInstance().log("NETWORK_MAP",""); GuiNetworkMap.show(); }
            case "21" -> handlePortConfig();
            case "22" -> handleScanHistoryDelta();
        }
    }

    public void cancel() {
        Thread t = runningThread.getAndSet(null);
        if (t != null && t.isAlive()) {
            t.interrupt();
            AuditLogger.getInstance().log("CANCEL", "");
            status.set("Abgebrochen", WARN);
            output.appendText("  ✕ Abgebrochen.\n", WARN);
        } else {
            status.set("Bereit", FG_DIM);
        }
    }

    public boolean isRunning() {
        Thread t = runningThread.get();
        return t != null && t.isAlive();
    }

    // ── Diagnose ──────────────────────────────────────────────────────────

    private void handleDiagnose() {
        output.appendText("\n  [ Diagnose & Analyse ]\n"
                + "  1 – Schnelldiagnose  (ICMP + Ports + OS, ~2 s)\n"
                + "  2 – Vollanalyse       (+ ARP/MAC + Traceroute, ~30 s)\n", ACCENT);
        input.ask("Modus (1/2):", modus ->
                input.ask("Ziel-IP oder Hostname:", target -> {
                    if ("2".equals(modus.trim())) {
                        AuditLogger.getInstance().log("DIAGNOSE_FULL", target);
                        runAsync(() -> IpInspector.inspect(target));
                    } else {
                        AuditLogger.getInstance().log("DIAGNOSE_QUICK", target);
                        runAsync(() -> IpInspector.quickScan(target, 5000));
                    }
                }));
    }

    // ── Nachricht ─────────────────────────────────────────────────────────

    private void handleSendMessage() {
        output.appendText("\n  [ Nachricht senden ]\n"
                + "  Lokal: NetTool/WinRM/SSH  |  Handy: ntfy-App + Topic\n", ACCENT);
        List<String> topics = NetworkStore.getInstance().getNtfyTopics();
        if (!topics.isEmpty())
            output.appendText("  Gespeicherte Topics: " + String.join(", ", topics) + "\n\n", FG_DIM);

        input.ask("Ziel-IP:", ip ->
                input.ask("Nachricht:", msg -> {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    if (topic == null) { output.appendText("  Abgebrochen.\n", FG_DIM); return; }
                    final String finalTopic = topic.trim();
                    if (!finalTopic.isEmpty()) {
                        NetworkStore.getInstance().saveNtfyTopic(finalTopic);
                        NotificationListener.subscribeNewTopic(finalTopic);
                    }
                    AuditLogger.getInstance().log("MSG_SEND", ip + " topic=" + finalTopic);
                    runAsync(() -> MessageSender.send(ip, msg, finalTopic));
                }));
    }

    // ── Fremdnetz-Scanner ─────────────────────────────────────────────────

    private void handleRemoteNetScan() {
        String gw = RemoteNetScanner.detectDefaultGateway();
        output.appendText("\n  [ Fremdnetz-Scanner ]\n"
                        + "  Gateway (auto): " + (gw != null ? gw : "nicht erkannt") + "\n\n"
                        + "  1 – Einzelnes Netz\n  2 – Mehrere Netze\n"
                        + "  3 – Erreichbarkeitstest\n  4 – Routing-Hilfe\n  5 – Gateway\n\n",
                ACCENT);

        input.ask("Modus (1–5):", modus -> {
            switch (modus.trim()) {
                case "1" -> input.ask("Netz:", raw -> runAsync(() -> {
                    AuditLogger.getInstance().log("REMOTE_SCAN", raw);
                    RemoteNetScanner.scanCidr(raw.trim());
                }));
                case "2" -> input.ask("Netze (kommagetrennt):", nets -> {
                    List<String> list = java.util.Arrays.stream(nets.split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                    if (list.isEmpty()) { output.appendText("  Keine Eingabe.\n", WARN); return; }
                    AuditLogger.getInstance().log("REMOTE_MULTI_SCAN", String.join(",", list));
                    runAsync(() -> RemoteNetScanner.scanMultiple(list));
                });
                case "3" -> input.ask("Netz für Test:", raw -> runAsync(() -> {
                    String cidr = RemoteNetScanner.normalizeCidr(raw.trim());
                    RemoteNetScanner.ReachResult r = RemoteNetScanner.parallelProbe(cidr);
                    System.out.println(r.reachable
                            ? "  ✔ " + cidr + " erreichbar (~" + r.avgMs + " ms)"
                            : "  ✕ " + cidr + " NICHT erreichbar.");
                    if (!r.reachable) RemoteNetScanner.printRoutingHints(cidr);
                }));
                case "4" -> input.ask("Netz:", raw ->
                        runAsync(() -> RemoteNetScanner.printRoutingHints(raw.trim())));
                case "5" -> runAsync(() -> {
                    String detected = RemoteNetScanner.detectDefaultGateway();
                    System.out.println("  Gateway: " + (detected != null ? detected : "nicht erkannt"));
                });
                default -> output.appendText("  Ungültige Auswahl.\n", WARN);
            }
        });
    }

    // ── Scan-Profile ──────────────────────────────────────────────────────

    private void handleScanProfiles() {
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        output.appendText("\n  [ Scan-Profile ]\n", ACCENT);
        StringBuilder sb = new StringBuilder("  Gespeicherte Profile:\n");
        if (profiles.isEmpty()) sb.append("  (keine)\n");
        else profiles.forEach(p -> sb.append("  · ").append(p.summary())
                .append(p.lastRun.isBlank() ? "" : "  (zuletzt: " + p.lastRun + ")").append("\n"));
        sb.append("\n  1 Ausführen  2 Neu  3 Löschen\n");
        output.appendText(sb.toString(), FG);

        input.ask("Aktion (1/2/3):", action -> {
            switch (action.trim()) {
                case "1" -> {
                    if (profiles.isEmpty()) { output.appendText("  Keine Profile.\n", WARN); return; }
                    String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                    Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Ausführen",
                            JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                    if (chosen == null) return;
                    AuditLogger.getInstance().log("PROFILE_RUN", chosen.toString());
                    ScanProfileStore.getInstance().get(chosen.toString())
                            .ifPresent(p -> runAsync(() -> runProfile(p)));
                }
                case "2" -> buildNewProfile();
                case "3" -> {
                    if (profiles.isEmpty()) return;
                    String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                    Object chosen = JOptionPane.showInputDialog(null, "Löschen:", "Löschen",
                            JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                    if (chosen != null) {
                        AuditLogger.getInstance().log("PROFILE_DELETE", chosen.toString());
                        ScanProfileStore.getInstance().delete(chosen.toString());
                        output.appendText("  ✔ Gelöscht: " + chosen + "\n", ACCENT2);
                    }
                }
                default -> output.appendText("  Ungültige Auswahl.\n", WARN);
            }
        });
    }

    private void buildNewProfile() {
        input.ask("Profilname:", name -> {
            if (name.isBlank()) return;
            ScanProfile p = new ScanProfile(name.trim());
            input.ask("CIDRs (leer = lokal):", cidrs -> {
                if (!cidrs.isBlank())
                    java.util.Arrays.stream(cidrs.split(",")).map(String::trim)
                            .filter(s -> !s.isBlank()).forEach(p.cidrs::add);
                input.ask("OS-Filter (leer = alle):", os -> {
                    p.osFilter = os.trim();
                    input.ask("Hostname-Filter (leer = alle):", hn -> {
                        p.hnFilter = hn.trim();
                        input.ask("Auto-Save in Kategorie (leer = nein):", cat -> {
                            if (!cat.isBlank()) { p.autoSave = true; p.category = cat.trim(); }
                            ScanProfileStore.getInstance().save(p);
                            AuditLogger.getInstance().log("PROFILE_CREATE", p.summary());
                            output.appendText("  ✔ Profil: " + p.summary() + "\n", ACCENT2);
                        });
                    });
                });
            });
        });
    }

    private void runProfile(ScanProfile profile) throws Exception {
        output.appendText("\n  ▶ Profil: " + profile.summary() + "\n", ACCENT);
        if (!profile.ports.isEmpty()) PortScanner.setActivePorts(profile.ports);
        if (profile.cidrs.isEmpty()) {
            NetworkInfo.showMinimalInfo();
        } else {
            List<ScanResult> all = new ArrayList<>();
            for (String cidr : profile.cidrs) {
                if (Thread.currentThread().isInterrupted()) break;
                all.addAll(NetworkScanner.scanCIDR(cidr));
            }
            tables.showScanTable(all);
        }
        if (!profile.ports.isEmpty()) PortScanner.setActivePorts(null);
    }

    // ── Scan-Delta ────────────────────────────────────────────────────────

    private void handleScanDelta() {
        output.appendText("\n  [ Scan-Vergleich (Δ) ]\n"
                + "  1 Jetzt scannen + letzten vergleichen\n"
                + "  2 Zwei CIDRs vergleichen\n", ACCENT);

        input.ask("Modus (1/2):", m -> {
            if ("2".equals(m.trim())) {
                input.ask("CIDR A (alt):", cidrA ->
                        input.ask("CIDR B (neu):", cidrB -> runAsync(() -> {
                            AuditLogger.getInstance().log("SCAN_DELTA", cidrA + " vs " + cidrB);
                            List<ScanResult> before = NetworkScanner.scanCIDR(cidrA);
                            List<ScanResult> after  = NetworkScanner.scanCIDR(cidrB);
                            ScanDelta.compare(before, after, cidrA, cidrB);
                        })));
            } else {
                input.ask("CIDR:", cidr -> runAsync(() -> {
                    AuditLogger.getInstance().log("SCAN_DELTA_LIVE", cidr);
                    List<ScanResult> fresh = NetworkScanner.scanCIDR(cidr);
                    List<HostResult> saved =
                            NetworkStore.getInstance().getAllHosts();
                    ScanDelta.compareHosts(saved, fresh.stream().map(r ->
                                    new HostResult(r.getIp(),
                                            r.getHostname(), r.getOsGuess())).toList(),
                            "Gespeicherte Hosts", "Aktueller Scan");
                }));
            }
        });
    }

    // ── Scheduler ─────────────────────────────────────────────────────────

    private void handleScheduler() {
        ScanScheduler sched = ScanScheduler.getInstance();
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        output.appendText("\n  [ Scheduler ]\n", ACCENT);
        if (sched.getRunning().isEmpty()) output.appendText("  Keine Scans geplant.\n", FG_DIM);
        else output.appendText("  Laufend: " + sched.getRunning() + "\n", ACCENT2);
        output.appendText("\n  1 Planen  2 Stoppen  3 Alle stoppen\n", FG);

        input.ask("Aktion (1/2/3):", action -> {
            switch (action.trim()) {
                case "1" -> {
                    if (profiles.isEmpty()) {
                        output.appendText("  Erst Scan-Profil anlegen (Menü 12).\n", WARN); return;
                    }
                    String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                    Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Scheduler",
                            JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                    if (chosen == null) return;
                    input.ask("Intervall (Minuten):", minStr -> {
                        try {
                            int min = Integer.parseInt(minStr.trim());
                            String topic = GuiContextMenu.promptNtfyTopic();
                            if (topic == null) topic = "";
                            final String t = topic;
                            AuditLogger.getInstance().log("SCHEDULER_START", chosen + " every=" + min + "min");
                            sched.start(chosen.toString(), min, t);
                            output.appendText("  ✔ Geplant: " + chosen + " alle " + min + " min\n", ACCENT2);
                        } catch (NumberFormatException e) {
                            output.appendText("  Ungültige Zahl.\n", WARN);
                        }
                    });
                }
                case "2" -> {
                    if (sched.getRunning().isEmpty()) { output.appendText("  Keine aktiven Scans.\n", WARN); return; }
                    String[] running = sched.getRunning().toArray(new String[0]);
                    Object chosen = JOptionPane.showInputDialog(null, "Stoppen:", "Stoppen",
                            JOptionPane.QUESTION_MESSAGE, null, running, running[0]);
                    if (chosen != null) {
                        AuditLogger.getInstance().log("SCHEDULER_STOP", chosen.toString());
                        sched.stop(chosen.toString());
                    }
                }
                case "3" -> {
                    AuditLogger.getInstance().log("SCHEDULER_STOP_ALL", "");
                    sched.stopAll();
                    output.appendText("  Alle gestoppt.\n", ACCENT2);
                }
                default -> output.appendText("  Ungültige Auswahl.\n", WARN);
            }
        });
    }

    // ── Bandwidth-Test ────────────────────────────────────────────────────

    private void handleBandwidthTest() {
        output.appendText("\n  [ Bandwidth-Test ]\n"
                + "  1 Server starten\n  2 Test zu Ziel-IP\n", ACCENT);
        input.ask("Modus (1/2):", m -> {
            if ("1".equals(m.trim())) {
                AuditLogger.getInstance().log("BW_SERVER_START", "");
                runAsync(() -> { BandwidthTester.startServer(); Thread.currentThread().join(); });
                output.appendText("  ✔ BW-Server auf Port " + BandwidthTester.TEST_PORT + "\n", ACCENT2);
            } else {
                input.ask("Ziel-IP:", ip -> runAsync(() -> {
                    AuditLogger.getInstance().log("BW_TEST", ip);
                    BandwidthTester.testBoth(ip);
                }));
            }
        });
    }

    // ── Dauerping ─────────────────────────────────────────────────────────

    private void handleDauerping() {
        output.appendText("\n  [ Dauerping ]\n  Abbrechen: Sidebar → ✕\n\n", ACCENT);
        input.ask("Ziel-IP oder Hostname:", host ->
                input.ask("Max. Dauer in Sek. (0 = unbegrenzt):", secStr -> {
                    int sec = 0;
                    try { sec = Integer.parseInt(secStr.trim()); } catch (NumberFormatException ignored) {}
                    final int maxSec = sec;
                    AuditLogger.getInstance().log("DAUERPING", host + " max=" + maxSec + "s");
                    runAsync(() -> PingMonitor.start(host.trim(), maxSec));
                }));
    }

    // ── Sicherheits-Monitor ───────────────────────────────────────────────

    private void handleSecurityMonitor() {
        SecurityMonitor secMon =
                SecurityMonitor.getInstance();
        ArpMonitor      arpMon  = ArpMonitor.getInstance();
        PortChangeMonitor portMon = PortChangeMonitor.getInstance();

        output.appendText("\n  [ Sicherheits-Monitor ]\n", ACCENT);
        output.appendText("  SecurityMonitor: " + (secMon.isActive() ? "✔ aktiv" : "✕ inaktiv") + "\n"
                + "  ARP-Monitor:     " + (arpMon.isActive()  ? "✔ aktiv" : "✕ inaktiv") + "\n"
                + "  Port-Monitor:    " + (portMon.isActive() ? "✔ aktiv (" + portMon.getInterval() + " min)" : "✕ inaktiv") + "\n\n"
                + "  1 SecurityMonitor starten/stoppen\n"
                + "  2 ARP-Monitor starten/stoppen\n"
                + "  3 Port-Monitor starten/stoppen\n", FG);

        input.ask("Aktion (1/2/3):", action -> {
            switch (action.trim()) {
                case "1" -> {
                    if (secMon.isActive()) {
                        secMon.stop();
                        output.appendText("  SecurityMonitor gestoppt.\n", WARN);
                    } else {
                        String topic = GuiContextMenu.promptNtfyTopic();
                        secMon.start(topic != null ? topic : "");
                        output.appendText("  ✔ SecurityMonitor gestartet.\n", ACCENT2);
                    }
                }
                case "2" -> {
                    if (arpMon.isActive()) {
                        AuditLogger.getInstance().log("ARP_MONITOR_STOP", "");
                        arpMon.stop();
                        output.appendText("  ARP-Monitor gestoppt.\n", WARN);
                    } else {
                        String topic = GuiContextMenu.promptNtfyTopic();
                        AuditLogger.getInstance().log("ARP_MONITOR_START", "");
                        arpMon.start(topic != null ? topic : "");
                        output.appendText("  ✔ ARP-Monitor gestartet.\n", ACCENT2);
                    }
                }
                case "3" -> {
                    if (portMon.isActive()) {
                        AuditLogger.getInstance().log("PORT_MONITOR_STOP", "");
                        portMon.stop();
                        output.appendText("  Port-Monitor gestoppt.\n", WARN);
                    } else {
                        input.ask("Intervall (Minuten):", minStr -> {
                            try {
                                int min = Integer.parseInt(minStr.trim());
                                String topic = GuiContextMenu.promptNtfyTopic();
                                AuditLogger.getInstance().log("PORT_MONITOR_START", min + "min");
                                portMon.start(min, topic != null ? topic : "");
                                output.appendText("  ✔ Port-Monitor (" + min + " min)\n", ACCENT2);
                            } catch (NumberFormatException e) {
                                output.appendText("  Ungültige Zahl.\n", WARN);
                            }
                        });
                    }
                }
                default -> output.appendText("  Ungültige Auswahl.\n", WARN);
            }
        });
    }

    // ── Export & Import ───────────────────────────────────────────────────

    private void handleExportImport() {
        output.appendText("\n  [ Export & Import ]\n"
                        + "  1 CSV  2 JSON  3 HTML  4 ZIP  5 CSV imp.  6 JSON imp.  7 ZIP restore\n\n",
                ACCENT);
        input.ask("Aktion (1–7):", action -> {
            java.nio.file.Path outDir = java.nio.file.Paths.get(
                    System.getProperty("user.home"), "NetTool-Export");
            switch (action.trim()) {
                case "1" -> runAsync(() -> {
                    java.nio.file.Path f = DataExportImport.exportCsv(outDir);
                    AuditLogger.getInstance().log("EXPORT_CSV", f.toString());
                    System.out.println("  ✔ CSV: " + f);
                });
                case "2" -> runAsync(() -> {
                    java.nio.file.Path f = DataExportImport.exportJson(outDir);
                    AuditLogger.getInstance().log("EXPORT_JSON", f.toString());
                    System.out.println("  ✔ JSON: " + f);
                });
                case "3" -> runAsync(() -> {
                    java.nio.file.Path f = DataExportImport.exportHtml(outDir);
                    AuditLogger.getInstance().log("EXPORT_HTML", f.toString());
                    System.out.println("  ✔ HTML: " + f);
                    try { java.awt.Desktop.getDesktop().browse(f.toUri()); } catch (Exception ignored) {}
                });
                case "4" -> runAsync(() -> {
                    java.nio.file.Path f = DataExportImport.exportBackup(outDir);
                    AuditLogger.getInstance().log("EXPORT_ZIP", f.toString());
                    System.out.println("  ✔ Backup: " + f);
                });
                case "5" -> input.ask("CSV-Pfad:", path -> runAsync(() -> {
                    int n = DataExportImport.importCsv(java.nio.file.Paths.get(path.trim()));
                    AuditLogger.getInstance().log("IMPORT_CSV", path + " n=" + n);
                    System.out.println("  ✔ " + n + " importiert.");
                }));
                case "6" -> input.ask("JSON-Pfad:", path -> runAsync(() -> {
                    int n = DataExportImport.importJson(java.nio.file.Paths.get(path.trim()));
                    AuditLogger.getInstance().log("IMPORT_JSON", path + " n=" + n);
                    System.out.println("  ✔ " + n + " importiert.");
                }));
                case "7" -> input.ask("ZIP-Pfad:", path -> runAsync(() -> {
                    int n = DataExportImport.restoreBackup(java.nio.file.Paths.get(path.trim()));
                    AuditLogger.getInstance().log("RESTORE_ZIP", path + " n=" + n);
                    System.out.println("  ✔ " + n + " Dateien wiederhergestellt.");
                }));
                default -> output.appendText("  Ungültige Auswahl.\n", WARN);
            }
        });
    }

    // ── Notification-History ──────────────────────────────────────────────

    private void handleNotificationHistory() {
        NotificationHistory hist = NotificationHistory.getInstance();
        output.appendText("\n  [ Nachrichten-Verlauf ]  (" + hist.size() + " Einträge)\n\n", ACCENT);
        if (hist.size() == 0) { output.appendText("  Noch keine Nachrichten.\n", FG_DIM); return; }

        String[][] data = hist.getAll().stream()
                .map(e -> new String[]{e.time, e.source, e.title, e.message})
                .toArray(String[][]::new);
        String[] cols   = {"Zeit", "Quelle", "Titel", "Nachricht"};
        int[]    widths = {120, 130, 150, 250};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(data, cols) {
                    public boolean isCellEditable(int r, int c) { return false; }
                };
        javax.swing.JTable table = TableConfig.buildTable(model, widths);
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(table);
        sp.setPreferredSize(new java.awt.Dimension(0, Math.min(data.length * 26 + 30, 350)));
        sp.setBorder(new javax.swing.border.LineBorder(BORDER, 1));
        sp.getViewport().setBackground(TableConfig.ROW_BG_EVEN);
        SwingUtilities.invokeLater(() -> {
            GUI.instance().appendText("\n", FG);
            javax.swing.JTextPane pane = GUI.instance().getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(sp);
            GUI.instance().appendText("\n", FG);
        });
        input.ask("'clear' = Löschen, Enter = weiter:", v -> {
            if ("clear".equalsIgnoreCase(v.trim())) {
                AuditLogger.getInstance().log("NOTIFICATION_HISTORY_CLEAR", "");
                hist.clear();
                output.appendText("  ✔ Verlauf gelöscht.\n", ACCENT2);
            }
        });
    }

    // ── Port-Konfiguration ────────────────────────────────────────────────

    private void handlePortConfig() {
        List<Integer> current = PortScanner.getActivePorts();
        output.appendText("\n  [ Port-Scanner Konfiguration ]\n"
                + "  Aktuelle Ports: " + current + "\n\n"
                + "  Ports kommagetrennt (z.B. 22,80,443,3389)\n"
                + "  'reset' = Standard-Ports\n\n", ACCENT);
        input.ask("Ports:", value -> {
            if ("reset".equalsIgnoreCase(value.trim())) {
                PortScanner.setActivePorts(null);
                AuditLogger.getInstance().log("PORT_CONFIG_RESET", "");
                output.appendText("  ✔ Standard-Ports: " + PortScanner.getActivePorts() + "\n", ACCENT2);
                return;
            }
            List<Integer> ports = new java.util.ArrayList<>();
            for (String p : value.split(",")) {
                try { ports.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
            }
            if (ports.isEmpty()) { output.appendText("  Keine gültigen Ports.\n", WARN); return; }
            PortScanner.setActivePorts(ports);
            AuditLogger.getInstance().log("PORT_CONFIG_SET", ports.toString());
            output.appendText("  ✔ Port-Liste: " + ports + "\n", ACCENT2);
        });
    }

    // ── Scan-History Delta ────────────────────────────────────────────────

    private void handleScanHistoryDelta() {
        ScanHistory hist = ScanHistory.getInstance();
        output.appendText("\n  [ Scan-Verlauf & Delta ]\n", ACCENT);
        if (hist.size() == 0) { output.appendText("  Kein Scan in dieser Session.\n", FG_DIM); return; }
        StringBuilder sb = new StringBuilder("  Gespeicherte Scans:\n");
        for (int i = 0; i < hist.size(); i++)
            sb.append("  ").append(i).append(" – ")
                    .append(hist.get(i).map(e -> e.display()).orElse("?")).append("\n");
        sb.append("\n  Delta: z.B. 0,1  oder 'last'\n");
        output.appendText(sb.toString(), FG);
        input.ask("Auswahl:", v -> {
            v = v.trim();
            int a = 0, b = 1;
            if (!"last".equalsIgnoreCase(v)) {
                String[] parts = v.split(",");
                try { a = Integer.parseInt(parts[0].trim()); b = Integer.parseInt(parts[1].trim()); }
                catch (Exception e) { output.appendText("  Ungültige Eingabe.\n", WARN); return; }
            }
            var ea = hist.get(a); var eb = hist.get(b);
            if (ea.isEmpty() || eb.isEmpty()) { output.appendText("  Index nicht vorhanden.\n", WARN); return; }
            AuditLogger.getInstance().log("SCAN_HISTORY_DELTA", a + " vs " + b);
            runAsync(() -> ScanDelta.compare(eb.get().results, ea.get().results,
                    eb.get().display(), ea.get().display()));
        });
    }

    // ── Async-Runner ──────────────────────────────────────────────────────

    public void runAsync(RunnableEx task) {
        status.set("Läuft...", ACCENT);
        Thread t = new Thread(() -> {
            try { task.run(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (Exception e) { output.appendText("Fehler: " + e.getMessage() + "\n", WARN); }
            finally {
                runningThread.compareAndSet(Thread.currentThread(), null);
                if (!Thread.currentThread().isInterrupted()) status.set("Fertig", ACCENT2);
            }
        });
        runningThread.set(t);
        t.start();
    }
}
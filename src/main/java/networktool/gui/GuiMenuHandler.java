package main.java.networktool.gui;

import main.java.networktool.gui.notification.NotificationListener;
import main.java.networktool.logic.analysis.IpInspector;
import main.java.networktool.logic.analysis.PingMonitor;
import main.java.networktool.logic.analysis.ArpMonitor;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.logic.scan.*;
import main.java.networktool.model.HostResult;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.storage.DataExporter;
import main.java.networktool.storage.DataImporter;
import main.java.networktool.logic.scan.*;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.NetworkStore;
import main.java.networktool.storage.NotificationHistory;
import main.java.networktool.storage.ScanProfileStore;
import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Verarbeitet Sidebar-Klicks und startet Aktionen asynchron.
 * Ausgabe reduziert: Dialoge statt langer Textblöcke im Output-Panel.
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

    public void setSavedHostsPanel(GuiSavedHostsPanel p) { this.savedHostsPanel = p; }

    // ── Dispatch ──────────────────────────────────────────────────────────

    public void handle(String num) {
        AuditLogger.getInstance().log("MENU", num);
        switch (num) {
            case "01" -> input.ask("Netzwerkinfo starten? [Enter]",
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_MINIMAL",""); NetworkInfo.showMinimalInfo(); }));
            case "02" -> input.ask("Vollständige Info starten? [Enter]",
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("SCAN_FULL",""); NetworkInfo.showFullInfo(); }));
            case "03" -> handleDiagnose();
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
                    _ -> runAsync(() -> { AuditLogger.getInstance().log("HOP_ANALYSE",""); GuiScanActions.runNetworkInfoWithHops(tables); }));
            case "11" -> handleRemoteNetScan();
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
        String[] options = {"Schnell  (ICMP + Ports + OS)", "Voll  (+ ARP + Traceroute)"};
        int choice = JOptionPane.showOptionDialog(null,
                "Diagnose-Modus:", "IP-Analyse",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0) return;
        input.ask("Ziel-IP / Hostname:", target -> {
            if (choice == 1) {
                AuditLogger.getInstance().log("DIAGNOSE_FULL", target);
                runAsync(() -> IpInspector.inspect(target));
            } else {
                AuditLogger.getInstance().log("DIAGNOSE_QUICK", target);
                runAsync(() -> IpInspector.quickScan(target, 5000));
            }
        });
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

    // ── Fremdnetz ────────────────────────────────────────────────────────

    private void handleRemoteNetScan() {
        String gw = RemoteNetScanner.detectDefaultGateway();
        status.set("Gateway: " + (gw != null ? gw : "–"), FG_DIM);
        String[] options = {"Einzelnes Netz", "Mehrere Netze", "Erreichbarkeitstest", "Routing-Hilfe"};
        int choice = JOptionPane.showOptionDialog(null,
                "Fremdnetz-Modus:", "Fremdnetz-Scanner",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0) return;
        switch (choice) {
            case 0 -> input.ask("CIDR:", raw -> runAsync(() -> {
                AuditLogger.getInstance().log("REMOTE_SCAN", raw);
                RemoteNetScanner.scanCidr(raw.trim());
            }));
            case 1 -> input.ask("Netze (kommagetrennt):", nets -> {
                List<String> list = Arrays.stream(nets.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (list.isEmpty()) return;
                AuditLogger.getInstance().log("REMOTE_MULTI_SCAN", String.join(",", list));
                runAsync(() -> RemoteNetScanner.scanMultiple(list));
            });
            case 2 -> input.ask("CIDR:", raw -> runAsync(() -> {
                String cidr = RemoteNetScanner.normalizeCidr(raw.trim());
                RemoteNetScanner.ReachResult r = RemoteNetScanner.parallelProbe(cidr);
                output.appendText(r.reachable
                                ? "  ✔ " + cidr + " erreichbar (~" + r.avgMs + " ms)\n"
                                : "  ✕ " + cidr + " nicht erreichbar\n",
                        r.reachable ? ACCENT2 : WARN);
            }));
            case 3 -> input.ask("CIDR:", raw ->
                    runAsync(() -> RemoteNetScanner.printRoutingHints(raw.trim())));
        }
    }

    // ── Scan-Profile ──────────────────────────────────────────────────────

    private void handleScanProfiles() {
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        String[] actions = {"Ausführen", "Neu anlegen", "Löschen"};
        int action = JOptionPane.showOptionDialog(null,
                profiles.isEmpty() ? "Keine Profile vorhanden." : profiles.size() + " Profile",
                "Scan-Profile", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, actions, actions[0]);
        if (action < 0) return;
        switch (action) {
            case 0 -> {
                if (profiles.isEmpty()) { output.appendText("  Keine Profile.\n", WARN); return; }
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Ausführen",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen == null) return;
                AuditLogger.getInstance().log("PROFILE_RUN", chosen.toString());
                ScanProfileStore.getInstance().get(chosen.toString())
                        .ifPresent(p -> runAsync(() -> runProfile(p)));
            }
            case 1 -> buildNewProfile();
            case 2 -> {
                if (profiles.isEmpty()) return;
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Löschen:", "Profil löschen",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen != null) {
                    AuditLogger.getInstance().log("PROFILE_DELETE", chosen.toString());
                    ScanProfileStore.getInstance().delete(chosen.toString());
                    output.appendText("  ✔ Gelöscht: " + chosen + "\n", ACCENT2);
                }
            }
        }
    }

    private void buildNewProfile() {
        input.ask("Profilname:", name -> {
            if (name.isBlank()) return;
            ScanProfile p = new ScanProfile(name.trim());
            input.ask("CIDRs (leer = lokal):", cidrs -> {
                if (!cidrs.isBlank())
                    Arrays.stream(cidrs.split(",")).map(String::trim)
                            .filter(s -> !s.isBlank()).forEach(p.cidrs::add);
                input.ask("OS-Filter (leer = alle):", os -> {
                    p.osFilter = os.trim();
                    input.ask("Hostname-Filter (leer = alle):", hn -> {
                        p.hnFilter = hn.trim();
                        input.ask("Auto-Save Kategorie (leer = nein):", cat -> {
                            if (!cat.isBlank()) { p.autoSave = true; p.category = cat.trim(); }
                            ScanProfileStore.getInstance().save(p);
                            AuditLogger.getInstance().log("PROFILE_CREATE", p.summary());
                            output.appendText("  ✔ Profil gespeichert: " + p.name + "\n", ACCENT2);
                        });
                    });
                });
            });
        });
    }

    private void runProfile(ScanProfile profile) throws Exception {
        status.set("Profil: " + profile.name, ACCENT);
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
        String[] options = {"Aktuell vs. letzten Scan", "Zwei CIDRs vergleichen"};
        int choice = JOptionPane.showOptionDialog(null, "Scan-Vergleich:",
                "Scan-Δ", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0) return;
        if (choice == 1) {
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
                List<HostResult> saved = NetworkStore.getInstance().getAllHosts();
                ScanDelta.compareHosts(saved, fresh.stream().map(r ->
                                new HostResult(r.getIp(),
                                        r.getHostname(), r.getOsGuess())).toList(),
                        "Gespeichert", "Aktuell");
            }));
        }
    }

    // ── Scheduler ─────────────────────────────────────────────────────────

    private void handleScheduler() {
        ScanScheduler sched = ScanScheduler.getInstance();
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        String running = sched.getRunning().isEmpty() ? "–" : String.join(", ", sched.getRunning());
        status.set("Scheduler: " + running, sched.getRunning().isEmpty() ? FG_DIM : ACCENT2);

        String[] actions = {"Planen", "Stoppen", "Alle stoppen"};
        int action = JOptionPane.showOptionDialog(null,
                "Aktive Scans: " + running, "Scheduler",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, actions, actions[0]);
        if (action < 0) return;
        switch (action) {
            case 0 -> {
                if (profiles.isEmpty()) { output.appendText("  Zuerst Scan-Profil anlegen.\n", WARN); return; }
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Scheduler",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen == null) return;
                input.ask("Intervall (min):", minStr -> {
                    try {
                        int min = Integer.parseInt(minStr.trim());
                        String topic = GuiContextMenu.promptNtfyTopic();
                        if (topic == null) topic = "";
                        final String t = topic;
                        AuditLogger.getInstance().log("SCHEDULER_START", chosen + " every=" + min + "min");
                        sched.start(chosen.toString(), min, t);
                        output.appendText("  ✔ " + chosen + " alle " + min + " min\n", ACCENT2);
                    } catch (NumberFormatException e) {
                        output.appendText("  ✕ Ungültige Zahl\n", WARN);
                    }
                });
            }
            case 1 -> {
                if (sched.getRunning().isEmpty()) return;
                String[] r = sched.getRunning().toArray(new String[0]);
                Object chosen = JOptionPane.showInputDialog(null, "Stoppen:", "Stoppen",
                        JOptionPane.QUESTION_MESSAGE, null, r, r[0]);
                if (chosen != null) {
                    AuditLogger.getInstance().log("SCHEDULER_STOP", chosen.toString());
                    sched.stop(chosen.toString());
                }
            }
            case 2 -> {
                AuditLogger.getInstance().log("SCHEDULER_STOP_ALL", "");
                sched.stopAll();
                output.appendText("  ✔ Alle gestoppt\n", ACCENT2);
            }
        }
    }

    // ── Bandwidth ─────────────────────────────────────────────────────────

    private void handleBandwidthTest() {
        String[] options = {"Server starten", "Test zu Ziel-IP"};
        int choice = JOptionPane.showOptionDialog(null, "Bandwidth-Test:",
                "Bandwidth", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if (choice < 0) return;
        if (choice == 0) {
            AuditLogger.getInstance().log("BW_SERVER_START", "");
            runAsync(() -> { BandwidthTester.startServer(); Thread.currentThread().join(); });
            output.appendText("  ✔ BW-Server Port " + BandwidthTester.TEST_PORT + "\n", ACCENT2);
        } else {
            input.ask("Ziel-IP:", ip -> runAsync(() -> {
                AuditLogger.getInstance().log("BW_TEST", ip);
                BandwidthTester.testBoth(ip);
            }));
        }
    }

    // ── Dauerping ─────────────────────────────────────────────────────────

    private void handleDauerping() {
        input.ask("Ziel-IP:", host ->
                input.ask("Max. Sekunden (0 = ∞):", secStr -> {
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
        ArpMonitor       arpMon  = ArpMonitor.getInstance();
        PortChangeMonitor portMon = PortChangeMonitor.getInstance();

        String state = "SecMon: " + (secMon.isActive() ? "✔" : "✕")
                + "  ARP: " + (arpMon.isActive() ? "✔" : "✕")
                + "  Port: " + (portMon.isActive() ? "✔ (" + portMon.getInterval() + "min)" : "✕");
        String[] options = {"SecurityMonitor", "ARP-Monitor", "Port-Monitor"};
        int choice = JOptionPane.showOptionDialog(null, state, "Sicherheits-Monitor",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        switch (choice) {
            case 0 -> {
                if (secMon.isActive()) { secMon.stop(); output.appendText("  SecurityMonitor gestoppt\n", WARN); }
                else {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    secMon.start(topic != null ? topic : "");
                    output.appendText("  ✔ SecurityMonitor aktiv\n", ACCENT2);
                }
            }
            case 1 -> {
                if (arpMon.isActive()) { AuditLogger.getInstance().log("ARP_MONITOR_STOP",""); arpMon.stop(); output.appendText("  ARP-Monitor gestoppt\n", WARN); }
                else {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    AuditLogger.getInstance().log("ARP_MONITOR_START","");
                    arpMon.start(topic != null ? topic : "");
                    output.appendText("  ✔ ARP-Monitor aktiv\n", ACCENT2);
                }
            }
            case 2 -> {
                if (portMon.isActive()) { AuditLogger.getInstance().log("PORT_MONITOR_STOP",""); portMon.stop(); output.appendText("  Port-Monitor gestoppt\n", WARN); }
                else {
                    input.ask("Intervall (min):", minStr -> {
                        try {
                            int min = Integer.parseInt(minStr.trim());
                            String topic = GuiContextMenu.promptNtfyTopic();
                            AuditLogger.getInstance().log("PORT_MONITOR_START", min + "min");
                            portMon.start(min, topic != null ? topic : "");
                            output.appendText("  ✔ Port-Monitor aktiv (" + min + " min)\n", ACCENT2);
                        } catch (NumberFormatException e) { output.appendText("  ✕ Ungültige Zahl\n", WARN); }
                    });
                }
            }
        }
    }

    // ── Export & Import ───────────────────────────────────────────────────

    private void handleExportImport() {
        String[] options = {"CSV", "JSON", "HTML", "ZIP-Backup", "CSV imp.", "JSON imp.", "ZIP restore"};
        int choice = JOptionPane.showOptionDialog(null, "Export / Import:",
                "Daten", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0) return;
        java.nio.file.Path outDir = java.nio.file.Paths.get(
                System.getProperty("user.home"), "NetTool-Export");
        switch (choice) {
            case 0 -> runAsync(() -> { java.nio.file.Path f = DataExporter.exportCsv(outDir);    AuditLogger.getInstance().log("EXPORT_CSV",  f.toString()); output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2); });
            case 1 -> runAsync(() -> { java.nio.file.Path f = DataExporter.exportJson(outDir);   AuditLogger.getInstance().log("EXPORT_JSON", f.toString()); output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2); });
            case 2 -> runAsync(() -> { java.nio.file.Path f = DataExporter.exportHtml(outDir);   AuditLogger.getInstance().log("EXPORT_HTML", f.toString()); output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2); try { java.awt.Desktop.getDesktop().browse(f.toUri()); } catch (Exception ignored) {} });
            case 3 -> runAsync(() -> { java.nio.file.Path f = DataExporter.exportBackup(outDir); AuditLogger.getInstance().log("EXPORT_ZIP",  f.toString()); output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2); });
            case 4 -> input.ask("CSV-Pfad:",  path -> runAsync(() -> { int n = DataImporter.importCsv(java.nio.file.Paths.get(path.trim()));         AuditLogger.getInstance().log("IMPORT_CSV",  "n="+n); output.appendText("  ✔ " + n + " importiert\n", ACCENT2); }));
            case 5 -> input.ask("JSON-Pfad:", path -> runAsync(() -> { int n = DataImporter.importJson(java.nio.file.Paths.get(path.trim()));        AuditLogger.getInstance().log("IMPORT_JSON", "n="+n); output.appendText("  ✔ " + n + " importiert\n", ACCENT2); }));
            case 6 -> input.ask("ZIP-Pfad:",  path -> runAsync(() -> { int n = DataImporter.restoreBackup(java.nio.file.Paths.get(path.trim())); AuditLogger.getInstance().log("RESTORE_ZIP", "n="+n); output.appendText("  ✔ " + n + " Dateien wiederhergestellt\n", ACCENT2); }));
        }
    }

    // ── Notification-History ──────────────────────────────────────────────

    private void handleNotificationHistory() {
        NotificationHistory hist = NotificationHistory.getInstance();
        if (hist.size() == 0) { output.appendText("  Keine Nachrichten.\n", FG_DIM); return; }

        String[][] data = hist.getAll().stream()
                .map(e -> new String[]{e.time, e.source, e.title, e.message})
                .toArray(String[][]::new);
        String[] cols = {"Zeit", "Quelle", "Titel", "Nachricht"};
        int[]    widths = {120, 130, 150, 250};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(data, cols) {
                    public boolean isCellEditable(int r, int c) { return false; }
                };
        JTable table = TableConfig.buildTable(model, widths);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new java.awt.Dimension(0, Math.min(data.length * 26 + 30, 300)));
        sp.setBorder(new javax.swing.border.LineBorder(BORDER, 1));
        sp.getViewport().setBackground(TableConfig.ROW_BG_EVEN);

        SwingUtilities.invokeLater(() -> {
            GUI.instance().appendText("\n", FG);
            JTextPane pane = GUI.instance().getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(sp);
            GUI.instance().appendText("\n", FG);
        });

        input.ask("'clear' = Löschen, Enter = weiter:", v -> {
            if ("clear".equalsIgnoreCase(v.trim())) {
                AuditLogger.getInstance().log("NOTIFICATION_HISTORY_CLEAR", "");
                hist.clear();
                output.appendText("  ✔ Verlauf geleert\n", ACCENT2);
            }
        });
    }

    // ── Port-Konfiguration ────────────────────────────────────────────────

    private void handlePortConfig() {
        List<Integer> current = PortScanner.getActivePorts();
        status.set("Ports: " + current.size(), FG_DIM);
        input.ask("Ports kommagetrennt (z.B. 22,80,443) oder 'reset':", value -> {
            if ("reset".equalsIgnoreCase(value.trim())) {
                PortScanner.setActivePorts(null);
                AuditLogger.getInstance().log("PORT_CONFIG_RESET", "");
                output.appendText("  ✔ Standard-Ports (" + PortScanner.getActivePorts().size() + ")\n", ACCENT2);
                return;
            }
            List<Integer> ports = new ArrayList<>();
            for (String p : value.split(",")) {
                try { ports.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
            }
            if (ports.isEmpty()) { output.appendText("  ✕ Keine gültigen Ports\n", WARN); return; }
            PortScanner.setActivePorts(ports);
            AuditLogger.getInstance().log("PORT_CONFIG_SET", ports.toString());
            output.appendText("  ✔ " + ports.size() + " Ports konfiguriert\n", ACCENT2);
        });
    }

    // ── Scan-History Delta ────────────────────────────────────────────────

    private void handleScanHistoryDelta() {
        ScanHistory hist = ScanHistory.getInstance();
        if (hist.size() == 0) { output.appendText("  Kein Scan in dieser Session.\n", FG_DIM); return; }
        String[] entries = new String[hist.size()];
        for (int i = 0; i < hist.size(); i++)
            entries[i] = hist.get(i).map(e -> e.display()).orElse("?");

        Object a = JOptionPane.showInputDialog(null, "Scan A (älter):", "Scan-Δ",
                JOptionPane.QUESTION_MESSAGE, null, entries, entries[Math.min(1, entries.length-1)]);
        if (a == null) return;
        Object b = JOptionPane.showInputDialog(null, "Scan B (neuer):", "Scan-Δ",
                JOptionPane.QUESTION_MESSAGE, null, entries, entries[0]);
        if (b == null) return;
        int idxA = Arrays.asList(entries).indexOf(a.toString());
        int idxB = Arrays.asList(entries).indexOf(b.toString());
        var ea = hist.get(idxA); var eb = hist.get(idxB);
        if (ea.isEmpty() || eb.isEmpty()) return;
        AuditLogger.getInstance().log("SCAN_HISTORY_DELTA", idxA + " vs " + idxB);
        runAsync(() -> ScanDelta.compare(eb.get().results, ea.get().results,
                eb.get().display(), ea.get().display()));
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
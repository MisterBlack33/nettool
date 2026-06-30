package main.java.networktool.cli;

import main.java.networktool.logic.analysis.ArpMonitor;
import main.java.networktool.logic.analysis.PingMonitor;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.NetworkInfo;
import main.java.networktool.logic.scan.RemoteNetScanner;
import main.java.networktool.logic.scan.ScanScheduler;
import main.java.networktool.logic.scan.PortChangeMonitor;
import main.java.networktool.storage.DataExportImport;
import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.filter.JsonExporter;
import main.java.networktool.filter.ScanFilter;
import main.java.networktool.filter.TablePrinter;
import main.java.networktool.logic.analysis.IpInspector;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.model.ScanResult;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;

import java.util.List;
import java.util.Scanner;

/**
 * Verarbeitet Benutzereingaben im CLI-Modus.
 *
 * Menü:
 *  1  Minimale Netzwerkinfo
 *  2  Vollständige Netzwerkinfo
 *  3  Diagnose & Analyse  (Submenu: 1=Schnell, 2=Voll)
 *  4  File-Server starten
 *  5  Datei senden
 *  6  CIDR-Scan
 *  7  Scan mit Filter
 *  8  Nachricht an IP
 *  0  Beenden
 */
public class MenuHandler {

    private final Scanner scanner;

    public MenuHandler(Scanner scanner) { this.scanner = scanner; }

    public void handle(int choice) {
        switch (choice) {
            case 1  -> runSafely(NetworkInfo::showMinimalInfo);
            case 2  -> runSafely(NetworkInfo::showFullInfo);
            case 3  -> handleDiagnose();
            case 4  -> handleFileServer();
            case 5  -> handleFileSend();
            case 6  -> handleCidrScan();
            case 7  -> handleFilterScan();
            case 8  -> handleSendMessage();
            case 9  -> handleRemoteNetScan();
            case 10 -> handleSchedulerCli();
            case 11 -> handleSecurityMonitorCli();
            case 12 -> handleDauerpingCli();
            case 13 -> handleBandwidthTestCli();
            case 14 -> handleExportImportCli();
            case 0  -> System.exit(0);
            default -> System.out.println("Ungueltiger Auswahl");
        }
    }

    // ── Menüaktionen ──────────────────────────────────────────────────────

    private void handleDiagnose() {
        System.out.println("\n  [ Diagnose & Analyse ]");
        System.out.println("  1 – Schnelldiagnose  (ICMP + Ports + OS)");
        System.out.println("  2 – Vollanalyse       (+ ARP/MAC + Traceroute)");
        System.out.print("  Modus (1/2): ");
        String modus = scanner.nextLine().trim();
        System.out.print("  Ziel-IP oder Hostname: ");
        String target = scanner.nextLine().trim();
        if ("2".equals(modus)) IpInspector.inspect(target);
        else                   IpInspector.quickScan(target, 5000);
    }

    private void handleFileServer() {
        System.out.print("Port für Server: ");
        int port = Integer.parseInt(scanner.nextLine().trim());
        new FileServer(port).start();
    }

    private void handleFileSend() {
        System.out.print("Ziel-IP: ");   String ip   = scanner.nextLine();
        System.out.print("Port: ");      int port    = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Dateipfad: "); String path = scanner.nextLine();
        new FileClient(ip, port).sendFile(path);
    }

    private void handleCidrScan() {
        System.out.print("CIDR (z.B. 192.168.1.0/24): ");
        List<ScanResult> results = NetworkScanner.scanCIDR(scanner.nextLine().trim());
        TablePrinter.print(results);
        results = applyRegexFilter(results);
        results = applyOsPortFilter(results);
        offerJsonExport(results);
    }

    private void handleFilterScan() {
        System.out.print("OS-Filter (Windows / Linux / Android / Apple / alle): ");
        String osFilter = scanner.nextLine().trim().toLowerCase();
        System.out.print("Hostname-Filter (Teilstring, leer = alle): ");
        String hostnameFilter = scanner.nextLine().trim().toLowerCase();
        runSafely(() -> NetworkInfo.scanWithFilter(osFilter, hostnameFilter));
    }

    private void handleSendMessage() {
        System.out.print("Ziel-IP oder Hostname: ");
        String ip    = scanner.nextLine().trim();
        System.out.print("Nachricht: ");
        String msg   = scanner.nextLine().trim();
        System.out.println("ntfy-Topic (leer = nur lokal/WinRM/SSH, kein Handy-Push):");
        System.out.print("Topic: ");
        String topic = scanner.nextLine().trim();
        MessageSender.send(ip, msg, topic);
    }

    // ── Filter-Hilfsmethoden ──────────────────────────────────────────────

    private List<ScanResult> applyRegexFilter(List<ScanResult> results) {
        System.out.print("\nRegex Hostname (optional, Enter = überspringen): ");
        String regex = scanner.nextLine().trim();
        if (regex.isEmpty()) return results;
        List<ScanResult> filtered = ScanFilter.filterByHostnameRegex(results, regex);
        TablePrinter.print(filtered);
        return filtered;
    }

    private List<ScanResult> applyOsPortFilter(List<ScanResult> results) {
        System.out.print("\nFilter OS + Port (z.B. linux 22) oder leer: ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.isEmpty()) return results;
        String[] parts = input.split(" ");
        if (parts.length < 2) { System.out.println("Ungültiges Format."); return results; }
        try {
            List<ScanResult> filtered = ScanFilter.filterCombined(
                    results, parts[0], Integer.parseInt(parts[1]));
            TablePrinter.print(filtered);
            return filtered;
        } catch (NumberFormatException e) {
            System.out.println("Ungültige Portnummer: " + parts[1]);
            return results;
        }
    }

    private void offerJsonExport(List<ScanResult> results) {
        System.out.print("\nAls JSON speichern? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y"))
            JsonExporter.save(results, "scan_result.json");
    }

    private void handleRemoteNetScan() {
        System.out.println("\n  [ Fremdnetz-Scanner ]");
        System.out.println("  1 – Einzelnes CIDR   (z.B. 10.16.5.0/24)");
        System.out.println("  2 – Mehrere Netze    (kommagetrennt)");
        System.out.println("  3 – Erreichbarkeitstest");
        System.out.println("  4 – Routing-Hilfe anzeigen");
        System.out.print("  Modus (1–4): ");
        String modus = scanner.nextLine().trim();
        switch (modus) {
            case "1" -> { System.out.print("  CIDR: "); RemoteNetScanner.scanCidr(scanner.nextLine().trim()); }
            case "2" -> {
                System.out.print("  Netze (kommagetrennt): ");
                java.util.List<String> nets = java.util.Arrays.stream(
                                scanner.nextLine().split(",")).map(String::trim)
                        .filter(s -> !s.isEmpty()).toList();
                System.out.print("  Tiefe: 1=schnell, 2=voll: ");
                boolean full = "2".equals(scanner.nextLine().trim());
                if (full) nets.forEach(RemoteNetScanner::scanCidr);
                else      RemoteNetScanner.scanMultiple(nets);
            }
            case "3" -> {
                System.out.print("  CIDR: ");
                String cidr = scanner.nextLine().trim();
                RemoteNetScanner.ReachResult r = RemoteNetScanner.parallelProbe(RemoteNetScanner.normalizeCidr(cidr));
                if (r.reachable)
                    System.out.printf("  ✔ Erreichbar (%d/3 Probes, ~%d ms)%n", r.respondedProbes, r.avgMs);
                else { System.out.println("  ✕ Nicht erreichbar."); RemoteNetScanner.printRoutingHints(cidr); }
            }
            case "4" -> { System.out.print("  CIDR für Routing-Hilfe: "); RemoteNetScanner.printRoutingHints(scanner.nextLine().trim()); }
            default  -> System.out.println("  Ungültige Auswahl.");
        }
    }

    private void handleSchedulerCli() {
        ScanScheduler sched = ScanScheduler.getInstance();
        System.out.println("\n  [ Scheduler CLI ]");
        System.out.println("  Laufend: " + sched.getRunning());
        System.out.println("  1 Scan planen  2 Scan stoppen  3 Alle stoppen");
        System.out.print("  Aktion: ");
        String a = scanner.nextLine().trim();
        switch (a) {
            case "1" -> {
                System.out.print("  Profil-Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("  Intervall (min): ");
                int min = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  ntfy-Topic (leer = kein Push): ");
                String t = scanner.nextLine().trim();
                sched.start(name, min, t);
            }
            case "2" -> { System.out.print("  Profil-Name: "); sched.stop(scanner.nextLine().trim()); }
            case "3" -> sched.stopAll();
            default  -> System.out.println("  Ungueltiger Auswahl.");
        }
    }

    private void handleSecurityMonitorCli() {
        ArpMonitor arp   = ArpMonitor.getInstance();
        PortChangeMonitor port = PortChangeMonitor.getInstance();
        System.out.println("\n  [ Sicherheitsmonitor CLI ]");
        System.out.println("  ARP-Monitor:  " + (arp.isActive()  ? "aktiv"  : "inaktiv"));
        System.out.println("  Port-Monitor: " + (port.isActive() ? "aktiv (" + port.getInterval() + " min)" : "inaktiv"));
        System.out.println("  1 ARP starten/stoppen  2 Port starten/stoppen");
        System.out.print("  Aktion: ");
        String a = scanner.nextLine().trim();
        switch (a) {
            case "1" -> { if (arp.isActive()) arp.stop(); else { System.out.print("  ntfy-Topic: "); arp.start(scanner.nextLine().trim()); } }
            case "2" -> { if (port.isActive()) port.stop(); else {
                System.out.print("  Intervall (min): ");
                int m = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  ntfy-Topic: ");
                port.start(m, scanner.nextLine().trim()); } }
            default  -> System.out.println("  Ungueltiger Auswahl.");
        }
    }

    private void handleDauerpingCli() {
        System.out.print("  Ziel-IP: ");
        String host = scanner.nextLine().trim();
        System.out.print("  Max. Dauer in Sek. (0 = unbegrenzt): ");
        int sec = 0;
        try { sec = Integer.parseInt(scanner.nextLine().trim()); } catch (NumberFormatException ignored) {}
        final int maxSec = sec;
        runSafely(() -> PingMonitor.start(host, maxSec));
    }

    private void handleBandwidthTestCli() {
        System.out.println("\n  [ Bandwidth-Test CLI ]");
        System.out.print("  Ziel-IP: ");
        String ip = scanner.nextLine().trim();
        runSafely(() -> BandwidthTester.testBoth(ip));
    }

    private void handleExportImportCli() {
        System.out.println("\n  [ Export & Import CLI ]");
        System.out.println("  1 CSV  2 JSON  3 HTML  4 ZIP-Backup  5 CSV imp.  6 JSON imp.  7 ZIP restore");
        System.out.print("  Aktion: ");
        String a = scanner.nextLine().trim();
        java.nio.file.Path outDir = java.nio.file.Paths.get(System.getProperty("user.home"), "NetTool-Export");
        runSafely(() -> {
            switch (a) {
                case "1" -> System.out.println("  CSV: "  + DataExportImport.exportCsv(outDir));
                case "2" -> System.out.println("  JSON: " + DataExportImport.exportJson(outDir));
                case "3" -> System.out.println("  HTML: " + DataExportImport.exportHtml(outDir));
                case "4" -> System.out.println("  ZIP: "  + DataExportImport.exportBackup(outDir));
                case "5" -> { System.out.print("  Pfad: "); int n = DataExportImport.importCsv(java.nio.file.Paths.get(scanner.nextLine().trim()));  System.out.println("  " + n + " importiert."); }
                case "6" -> { System.out.print("  Pfad: "); int n = DataExportImport.importJson(java.nio.file.Paths.get(scanner.nextLine().trim())); System.out.println("  " + n + " importiert."); }
                case "7" -> { System.out.print("  Pfad: "); int n = DataExportImport.restoreBackup(java.nio.file.Paths.get(scanner.nextLine().trim())); System.out.println("  " + n + " Dateien wiederhergestellt."); }
                default  -> System.out.println("  Ungueltiger Auswahl.");
            }
        });
    }

    private void runSafely(ThrowingRunnable task) {
        try { task.run(); }
        catch (Exception e) { System.err.println("Fehler: " + e.getMessage()); }
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
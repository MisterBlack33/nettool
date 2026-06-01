package main.java.networktool.logic.analysis;

import main.java.networktool.gui.GUI;
import main.java.networktool.logic.ports.PortScanner;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * Netzwerk-Diagnose und IP-Detailanalyse.
 *
 *  quickScan()      – ICMP + Ports + OS  (~2 s, ehem. PingUtil)
 *  inspect()        – Vollanalyse: + ARP/MAC + Traceroute  (~30 s)
 *  inspectHopsOnly()– nur Traceroute (für Menüpunkt 10)
 *
 * Traceroute-Logik:   {@link TracerouteRunner}
 * Traceroute-Ausgabe: {@link TracerouteRenderer}
 */
public final class IpInspector {

    private IpInspector() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public static void quickScan(String target, int timeoutMs) {
        try {
            InetAddress inet = InetAddress.getByName(target);
            String ip = inet.getHostAddress();
            printBanner("Schnelldiagnose");
            System.out.println("  IP       : " + ip);
            long t = System.currentTimeMillis();
            boolean alive = inet.isReachable(timeoutMs);
            long ms = System.currentTimeMillis() - t;
            if (!alive) { System.out.println("  Status   : NICHT erreichbar\n═══"); return; }
            System.out.println("  Status   : erreichbar (" + ms + " ms)");
            System.out.println("  Hostname : " + inet.getCanonicalHostName());
            System.out.println("  OS       : " + OsDetector.detect(ip));
            printPorts(ip, timeoutMs);
            System.out.println("\n═══════════════════════════════════════════════");
        } catch (Exception e) { System.err.println("Fehler: " + e.getMessage()); }
    }

    public static void inspect(String target) {
        try {
            InetAddress inet = InetAddress.getByName(target);
            String ip = inet.getHostAddress();
            printBanner("IP-Detailanalyse");
            printBasicInfo(target, ip, inet);
            printReachability(inet);
            printArpInfo(ip);
            printOsInfo(ip, inet);
            printPorts(ip, 1000);
            printTraceroute(ip, 0);
            System.out.println("\n═══════════════════════════════════════════════");
        } catch (Exception e) { System.err.println("Fehler: " + e.getMessage()); }
    }

    /** Nur Traceroute – für Menüpunkt 10 (Netzwerkinfo + Hop-Analyse). */
    public static void inspectHopsOnly(String target) {
        try { printTraceroute(InetAddress.getByName(target).getHostAddress(), 0); }
        catch (Exception e) { System.err.println("Traceroute: " + e.getMessage()); }
    }

    // ── Ausgabe-Abschnitte ────────────────────────────────────────────────

    private static void printBanner(String title) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf( "║  %-44s║%n", "  " + title);
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private static void printBasicInfo(String target, String ip, InetAddress inet) {
        System.out.println("\n[ Basis ]");
        System.out.println("  Eingabe   : " + target);
        System.out.println("  IP        : " + ip);
        System.out.println("  Hostname  : " + inet.getCanonicalHostName());
        System.out.println("  Loopback  : " + inet.isLoopbackAddress());
        System.out.println("  Link-Local: " + inet.isLinkLocalAddress());
        System.out.println("  Site-Local: " + inet.isSiteLocalAddress());
    }

    private static void printReachability(InetAddress inet) throws Exception {
        System.out.println("\n[ Erreichbarkeit ]");
        long t = System.currentTimeMillis();
        boolean alive = inet.isReachable(2000);
        System.out.println("  ICMP: " + (alive
                ? "erreichbar (" + (System.currentTimeMillis() - t) + " ms)"
                : "nicht erreichbar"));
    }

    private static void printArpInfo(String ip) {
        System.out.println("\n[ ARP / MAC ]");
        String mac = OsDetector.getMacFromArp(ip);
        if (mac != null) {
            System.out.println("  MAC       : " + mac);
            String v = OuiDatabase.lookup(mac);
            System.out.println("  Hersteller: " + (v != null ? v : "unbekannt"));
        } else {
            System.out.println("  MAC: nicht im ARP-Cache");
        }
    }

    private static void printOsInfo(String ip, InetAddress inet) {
        System.out.println("\n[ OS-Erkennung ]");
        String os   = OsDetector.detect(ip);
        String hint = OsDetector.detectFromHostname(inet.getCanonicalHostName(), ip);
        System.out.println("  Erkannt: " + os);
        if (hint != null && !hint.equals(os)) System.out.println("  Hostname: " + hint);
    }

    private static void printPorts(String ip, int timeout) throws InterruptedException {
        System.out.println("\n[ Offene Ports & Banner ]");
        Map<Integer, String> ports = PortScanner.scanParallel(ip, timeout);
        if (ports.isEmpty()) { System.out.println("  Keine."); return; }
        ports.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  Port %-5d -> %s%n", e.getKey(), e.getValue()));
    }

    private static void printTraceroute(String ip, int maxHops) {
        System.out.println("\n[ Traceroute"
                + (maxHops > 0 ? " (max. " + maxHops + ")" : " (alle Hops)") + " ]");
        try {
            List<TracerouteRunner.HopInfo> hops = TracerouteRunner.run(ip, maxHops);
            if (hops.isEmpty()) { System.out.println("  Keine Antworten."); return; }
            if (GUI.isGuiActive()) TracerouteRenderer.renderGui(hops);
            else                   TracerouteRenderer.renderCli(hops);
        } catch (Exception e) {
            System.out.println("  Nicht verfügbar: " + e.getMessage());
        }
    }
}

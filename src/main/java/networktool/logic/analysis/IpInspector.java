package main.java.networktool.logic.analysis;

import main.java.networktool.gui.GUI;
import main.java.networktool.logic.ports.PortScanner;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * IP-Detailanalyse. inspect() nutzt ExtendedOsDetector für maximale Präzision.
 * quickScan() bleibt bei OsDetector für Geschwindigkeit.
 */
public final class IpInspector {

    private IpInspector() {}

    public static volatile boolean testMode = false;

    public static void quickScan(String target, int timeoutMs) {
        if (testMode) return;
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
        if (testMode) return;
        try {
            InetAddress inet = InetAddress.getByName(target);
            String ip = inet.getHostAddress();
            printBanner("IP-Detailanalyse");
            printBasicInfo(target, ip, inet);
            printReachability(inet);
            printArpInfo(ip);
            printExtendedOs(ip);
            printIcmpTiming(ip);
            printDhcpInfo(ip);
            printUpnpInfo(ip);
            printMdnsInfo(ip);
            printPorts(ip, 1000);
            printTraceroute(ip, 0);
            System.out.println("\n═══════════════════════════════════════════════");
        } catch (Exception e) { System.err.println("Fehler: " + e.getMessage()); }
    }

    public static void inspectHopsOnly(String target) {
        if (testMode) return;
        try { printTraceroute(InetAddress.getByName(target).getHostAddress(), 0); }
        catch (Exception e) { System.err.println("Traceroute: " + e.getMessage()); }
    }

    // ── Print-Methoden ────────────────────────────────────────────────────

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
            String vendor = OuiDatabase.lookup(mac);
            System.out.println("  Hersteller: " + (vendor != null ? vendor : "unbekannt"));
        } else {
            System.out.println("  MAC: nicht im ARP-Cache");
        }
    }

    /** Nutzt ExtendedOsDetector für maximale Erkennungstiefe. */
    private static void printExtendedOs(String ip) {
        System.out.println("\n[ OS-Erkennung (erweitert) ]");
        OsDetector.OsResult r = ExtendedOsDetector.detect(ip);
        System.out.println("  OS       : " + r.os);
        System.out.println("  Konfidenz: " + r.confidence);
        System.out.println("  Methode  : " + r.method);
    }

    private static void printIcmpTiming(String ip) {
        System.out.println("\n[ ICMP-Timing ]");
        IcmpAnalyzer.Result r = IcmpAnalyzer.analyze(ip);
        if (r == null) { System.out.println("  Nicht erreichbar."); return; }
        System.out.println("  " + r);
        if (r.isHighJitter())  System.out.println("  ⚠ Hoher Jitter – instabile Verbindung");
        if (r.isUnstable())    System.out.println("  ⚠ Paketverlust > 20%");
    }

    private static void printDhcpInfo(String ip) {
        System.out.println("\n[ DHCP Option 60 ]");
        DhcpOptionAnalyzer.Result r = DhcpOptionAnalyzer.analyze(ip);
        if (r == null) { System.out.println("  Kein DHCP-Server oder keine Antwort."); return; }
        System.out.println("  Vendor Class : " + r.vendorClass());
        System.out.println("  OS-Hinweis   : " + r.detectedOs());
    }

    private static void printUpnpInfo(String ip) {
        System.out.println("\n[ UPnP / SSDP ]");
        List<UpnpDiscovery.Device> devices = UpnpDiscovery.discover();
        devices.stream()
                .filter(d -> ip.equals(d.ip()))
                .forEach(d -> {
                    System.out.println("  Server  : " + d.server());
                    System.out.println("  USN     : " + d.usn());
                    System.out.println("  Location: " + d.location());
                    String os = d.guessOs();
                    if (os != null) System.out.println("  OS      : " + os);
                });
        if (devices.stream().noneMatch(d -> ip.equals(d.ip())))
            System.out.println("  Kein UPnP-Gerät gefunden.");
    }

    private static void printMdnsInfo(String ip) {
        System.out.println("\n[ mDNS / Bonjour ]");
        List<MdnsDiscovery.ServiceRecord> records = MdnsDiscovery.queryHost(ip);
        if (records.isEmpty()) { System.out.println("  Keine mDNS-Services gefunden."); return; }
        records.forEach(r ->
                System.out.println("  " + r.serviceType() + "  →  " + r.name()));
    }

    private static void printPorts(String ip, int timeout) throws InterruptedException {
        System.out.println("\n[ Offene Ports & Banner ]");
        Map<Integer, String> ports = PortScanner.scanParallel(ip, timeout);
        if (ports.isEmpty()) { System.out.println("  Keine."); return; }
        ports.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  Port %-5d -> %s%n", e.getKey(), e.getValue()));
    }

    private static void printTraceroute(String ip, int maxHops) {
        System.out.println("\n[ Traceroute ]");
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
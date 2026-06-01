package main.java.networktool.logic.analysis;

import main.java.networktool.logic.ports.PortScanner;

import java.net.InetAddress;
import java.util.Map;

/**
 * Führt eine erweiterte Netzwerkdiagnose für ein Ziel durch:
 * ICMP-Erreichbarkeit, Latenz, Hostname, offene Ports und OS-Heuristik.
 */
public final class PingUtil {

    private PingUtil() {}

    /**
     * Pingt das Ziel und gibt eine vollständige Diagnose aus.
     *
     * @param host          Ziel-IP oder Hostname
     * @param timeoutMillis Timeout in Millisekunden
     */
    public static void pingWithDetails(String host, int timeoutMillis) {
        try {
            InetAddress inet = InetAddress.getByName(host);
            System.out.println("\n=== Erweiterte Netzwerkdiagnose ===");
            System.out.println("Ziel:      " + host);
            System.out.println("IP:        " + inet.getHostAddress());

            long start      = System.currentTimeMillis();
            boolean reachable = inet.isReachable(timeoutMillis);
            long ms         = System.currentTimeMillis() - start;

            if (!reachable) {
                System.out.println("Status:    NICHT erreichbar");
                return;
            }

            System.out.println("Status:    erreichbar (" + ms + " ms)");
            System.out.println("Hostname:  " + inet.getCanonicalHostName());

            Map<Integer, String> ports = PortScanner.scanParallel(inet.getHostAddress(), timeoutMillis);
            printPorts(ports);

            System.out.println("\nOS-Heuristik: " + OsDetector.detect(inet.getHostAddress()));

        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
        }
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private static void printPorts(Map<Integer, String> ports) {
        System.out.println("\n=== Offene Ports ===");
        if (ports.isEmpty()) {
            System.out.println("Keine offenen Ports.");
            return;
        }
        ports.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("Port " + e.getKey() + "  -> " + e.getValue()));
    }
}

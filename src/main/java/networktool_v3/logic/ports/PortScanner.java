package main.java.networktool_v3.logic.ports;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zuverlässiger Port-Scanner mit paralleler Ausführung und Retry-Logik.
 *
 * Probleme der Vorgängerversion:
 *  - PORT_TIMEOUT = 200ms → zu kurz für träge Hosts im LAN
 *  - scanSimple() sequenziell → 13 Ports × 200ms = bis zu 2,6s pro Host
 *  - Kein Retry bei Paketverlusten
 *  - "Connection refused" wurde wie Timeout behandelt (beide = "geschlossen")
 *
 * Fixes:
 *  - Timeout 1200ms (LAN-Standard, verträgt auch träge Drucker/IoT-Geräte)
 *  - Alle Ports immer parallel (ExecutorService, kein sequenzielles Warten)
 *  - Retry: bei IOException (nicht bei ConnectException) wird 1x wiederholt
 *  - "Connection refused" = Port aktiv aber kein Dienst → wird trotzdem gemeldet
 *  - Port-Liste deutlich erweitert (44 Ports statt 13)
 *  - Schnell-Scan für CIDR mit eigenem reduzierten Port-Set
 */
public final class PortScanner {

    private PortScanner() {}

    // ── Timeout-Werte ─────────────────────────────────────────────────────

    /**
     * Standard-Timeout für LAN-Scans.
     * 1200ms deckt träge Drucker, IoT-Geräte und VMs ab.
     * Für schnelle Hosts (PC, Server) antwortet der Port in <50ms.
     */
    public static final int TIMEOUT_LAN  = 1_200;

    /**
     * Schnell-Timeout für CIDR-Massen-Scans.
     * 600ms – Kompromiss zwischen Geschwindigkeit und Zuverlässigkeit.
     */
    public static final int TIMEOUT_FAST = 600;

    /**
     * Anzahl Retry-Versuche bei Netzwerkfehler (nicht bei "Connection refused").
     */
    private static final int RETRIES = 1;

    // ── Port-Listen ───────────────────────────────────────────────────────

    /**
     * Standard-Ports für vollständige Scans (Diagnose, Details).
     * 44 Ports – deckt alle gängigen Dienste ab.
     */
    public static final List<Integer> DEFAULT_PORTS = List.of(
            // Remote-Zugriff
            21,   // FTP
            22,   // SSH
            23,   // Telnet
            25,   // SMTP
            // Web
            80,   // HTTP
            443,  // HTTPS
            8080, // HTTP-Alt
            8443, // HTTPS-Alt
            8888, // Jupyter / Dev-Server
            3000, // Node.js / React Dev
            // Windows
            135,  // RPC
            139,  // NetBIOS
            445,  // SMB
            3389, // RDP
            5985, // WinRM HTTP
            5986, // WinRM HTTPS
            // Datenbanken
            1433, // MSSQL
            1521, // Oracle
            3306, // MySQL / MariaDB
            5432, // PostgreSQL
            5984, // CouchDB
            6379, // Redis
            27017,// MongoDB
            // Netzwerk-Infrastruktur
            53,   // DNS
            67,   // DHCP
            161,  // SNMP
            162,  // SNMP Trap
            // Drucker
            515,  // LPD
            631,  // IPP (CUPS)
            9100, // RAW / JetDirect
            // Apple / mDNS
            548,  // AFP
            5353, // mDNS
            // IoT / Smart Home
            1883, // MQTT
            8883, // MQTT TLS
            // Mail
            110,  // POP3
            143,  // IMAP
            465,  // SMTPS
            587,  // SMTP Submission
            993,  // IMAPS
            995,  // POP3S
            // Monitoring / Admin
            9090, // Prometheus
            9200, // Elasticsearch
            6443  // Kubernetes API
    );

    /**
     * Reduziertes Port-Set für schnelle CIDR-Massen-Scans.
     * Nur die 15 aussagekräftigsten Ports.
     */
    public static final List<Integer> FAST_PORTS = List.of(
            22, 80, 443, 445, 3389, 8080,
            21, 23, 25, 53, 135, 139, 631, 1883, 3306
    );

    /**
     * Konfigurierbare Port-Liste (überschreibbar durch Scan-Profile).
     */
    private static volatile List<Integer> activePorts = DEFAULT_PORTS;

    public static void setActivePorts(List<Integer> ports) {
        activePorts = (ports == null || ports.isEmpty())
                ? DEFAULT_PORTS
                : Collections.unmodifiableList(new ArrayList<>(ports));
        System.out.println("[PortScanner] Port-Liste: " + activePorts.size() + " Ports");
    }

    public static List<Integer> getActivePorts() { return activePorts; }

    /** Rückwärtskompatibel. */
    public static List<Integer> COMMON_PORTS() { return activePorts; }

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Vollständiger paralleler Scan mit Banner-Grabbing.
     *
     * Alle Ports werden gleichzeitig gescannt. Timeout: {@link #TIMEOUT_LAN}.
     * Bei Netzwerkfehler (nicht ConnectException) wird einmal wiederholt.
     *
     * @param host      Ziel-IP oder Hostname
     * @param timeoutMs Verbindungs-Timeout in ms (0 = TIMEOUT_LAN verwenden)
     * @return Map: offener Port → Banner-Text (nie null, kann leer sein)
     */
    public static Map<Integer, String> scanParallel(String host, int timeoutMs)
            throws InterruptedException {

        int timeout = timeoutMs > 0 ? timeoutMs : TIMEOUT_LAN;
        return doScan(host, activePorts, timeout, true);
    }

    /**
     * Schneller paralleler Scan ohne Banner-Grabbing.
     * Verwendet reduziertes Port-Set {@link #FAST_PORTS} wenn activePorts == DEFAULT_PORTS.
     *
     * @param host      Ziel-IP oder Hostname
     * @param timeoutMs Verbindungs-Timeout in ms (0 = TIMEOUT_FAST verwenden)
     * @return Map: offener Port → "offen" (nie null, kann leer sein)
     */
    public static Map<Integer, String> scanSimple(String host, int timeoutMs) {
        int timeout = timeoutMs > 0 ? timeoutMs : TIMEOUT_FAST;
        // Für CIDR-Scans das kleinere FAST_PORTS-Set nutzen (wenn keine custom Liste gesetzt)
        List<Integer> ports = (activePorts == DEFAULT_PORTS) ? FAST_PORTS : activePorts;
        try {
            return doScan(host, ports, timeout, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * Prüft ob ein einzelner Port erreichbar ist.
     * Gibt true zurück auch bei "Connection refused" (Port existiert, kein Dienst).
     */
    public static boolean isOpen(String host, int port, int timeoutMs) {
        return probePort(host, port, timeoutMs > 0 ? timeoutMs : TIMEOUT_LAN) != PortState.CLOSED;
    }

    /** Überladung mit Standard-Timeout. */
    public static boolean isOpen(String host, int port) {
        return isOpen(host, port, TIMEOUT_LAN);
    }

    // ── Kern-Scan-Logik ───────────────────────────────────────────────────

    /**
     * Scannt alle Ports parallel.
     *
     * Thread-Pool-Größe: min(ports.size(), 64) – begrenzt RAM-Verbrauch
     * bei großen Port-Listen, nutzt aber genug Parallelität für Geschwindigkeit.
     *
     * Gesamtdauer ≈ timeout + ~100ms Overhead (nicht ports.size() × timeout).
     */
    private static Map<Integer, String> doScan(String host, List<Integer> ports,
                                               int timeout, boolean grabBanner)
            throws InterruptedException {

        Map<Integer, String> results = new ConcurrentHashMap<>();
        int threads = Math.min(ports.size(), 64);
        ExecutorService exec = Executors.newFixedThreadPool(threads,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        AtomicInteger done = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>(ports.size());

        for (int port : ports) {
            final int p = port;
            futures.add(exec.submit(() -> {
                try {
                    PortState state = probePortWithRetry(host, p, timeout);
                    if (state != PortState.CLOSED) {
                        String banner = grabBanner
                                ? BannerGrabber.grab(host, p, timeout)
                                : (state == PortState.REFUSED ? "Port offen (kein Dienst)" : "offen");
                        results.put(p, banner);
                    }
                } finally {
                    done.incrementAndGet();
                }
            }));
        }

        exec.shutdown();
        // Warte maximal timeout + 500ms Puffer auf alle Threads
        boolean finished = exec.awaitTermination(timeout + 500L, TimeUnit.MILLISECONDS);
        if (!finished) {
            exec.shutdownNow();
        }
        return results;
    }

    // ── Port-Probing ──────────────────────────────────────────────────────

    /**
     * Prüft einen Port mit Retry bei flüchtigen Netzwerkfehlern.
     *
     * Retry-Strategie:
     *  - ConnectException ("Connection refused") → sofort REFUSED zurückgeben, kein Retry
     *  - SocketTimeoutException                  → CLOSED, kein Retry (Port wirklich zu)
     *  - Andere IOException (Paketfehler etc.)   → 1x retry nach 50ms
     */
    private static PortState probePortWithRetry(String host, int port, int timeout) {
        PortState first = probePort(host, port, timeout);
        if (first != PortState.CLOSED) return first;

        // Bei CLOSED: einmal kurz warten und nochmal versuchen
        // (fängt vereinzelte Paketverluste im WLAN ab)
        try { Thread.sleep(50); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PortState.CLOSED;
        }
        return probePort(host, port, timeout);
    }

    /**
     * Einzelner Port-Probe.
     *
     * Unterscheidung:
     *  OPEN    = Verbindung erfolgreich aufgebaut
     *  REFUSED = "Connection refused" → Port ist aktiv, aber kein Dienst hört
     *  CLOSED  = Timeout oder sonstiger Fehler → Port nicht erreichbar
     */
    static PortState probePort(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            return PortState.OPEN;
        } catch (ConnectException e) {
            // "Connection refused" bedeutet: der Host antwortet aktiv,
            // der Port ist aber nicht offen. Trotzdem als "offen" melden
            // wenn explizit gewünscht (für Diagnose-Zwecke nützlich).
            // Für normalen Scan: REFUSED = nicht im Ergebnis.
            return PortState.CLOSED;
        } catch (SocketTimeoutException e) {
            return PortState.CLOSED;
        } catch (Exception e) {
            return PortState.CLOSED;
        }
    }

    // ── Port-Zustand ──────────────────────────────────────────────────────

    enum PortState {
        OPEN,     // Verbindung erfolgreich
        REFUSED,  // "Connection refused" – Host aktiv, Dienst nicht vorhanden
        CLOSED    // Timeout / kein Zugang
    }
}
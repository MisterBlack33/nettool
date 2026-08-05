package main.java.networktool.logic;

/**
 * Zentrale Timeout-Konstanten für {@code logic.scan} und {@code logic.analysis}.
 * Ersetzt die zuvor über beide Packages verstreuten einzelnen TIMEOUT-Felder.
 *
 * Die meisten Werte sind reine Konstanten. Wo Tests bislang zur Laufzeit
 * kürzere Timeouts brauchten (ICMP/TCP-Erreichbarkeit, Port-Scan), bleiben
 * die Felder {@code volatile} und über die jeweilige Klasse (z.B.
 * {@code HostAliveChecker.setTestTimeouts}) veränderbar — das Verhalten
 * ist unverändert, nur der Speicherort ist jetzt zentral.
 */
public final class TimeoutConfig {

    private TimeoutConfig() {}

    // ── logic.scan ──────────────────────────────────────────────────────────

    /** HostAliveChecker: ICMP isReachable(). Zur Laufzeit für Tests änderbar. */
    public static volatile int ICMP_REACHABLE_MS = 500;
    /** HostAliveChecker: TCP-Connect-Probe. Zur Laufzeit für Tests änderbar. */
    public static volatile int TCP_PROBE_MS = 400;
    /** NetworkHostnameResolver: DNS-Reverse-Lookup (Thread-Join-Deadline). */
    public static final int DNS_LOOKUP_MS = 600;
    /** NetworkScanner: ICMP isReachable() im einfachen Fallback-Scanner. */
    public static final int NETWORK_SCANNER_REACH_MS = 1000;
    /** PortChangeMonitor: Port-Scan pro überwachtem Host. */
    public static final int PORT_CHANGE_SCAN_MS = 500;
    /** RemoteNetProbe / RemoteNetGateway: Erreichbarkeitsprüfung entfernter Netze. */
    public static final int REMOTE_REACH_MS = 1200;
    /** PingSweep: ICMP isReachable() im schnellen Sweep. */
    public static final int PING_SWEEP_MS = 800;
    /** NetworkDiscoverySweep: Gesamt-Timeout der Discovery (mDNS/UPnP/2nd-Ping). */
    public static final int DISCOVERY_SWEEP_SEC = 35;
    /** MapTrafficObserver: Socket-Read-Timeout für Traffic-Beobachtung. */
    public static final int MAP_TRAFFIC_MS = 600;

    // ── logic.analysis ──────────────────────────────────────────────────────

    /** OsDetectorPorts: Port-Scan pro Host während OS-Erkennung. Zur Laufzeit änderbar. */
    public static volatile int OS_DETECT_PORT_SCAN_MS = 600;
    /** OsBannerAnalyzer: TCP-Connect/Read für Banner-Grabbing (SSH/HTTP/SMB/FTP/HTTPS). */
    public static final int BANNER_GRAB_MS = 700;
    /** OsProbeUdp: UDP-Socket-Timeout für NetBIOS/mDNS/SNMP-Sonden. */
    public static final int UDP_PROBE_MS = 800;
    /** DhcpOptionAnalyzer: Socket-Timeout für DHCP-Optionsauswertung. */
    public static final int DHCP_ANALYSIS_MS = 1500;
    /** IcmpAnalyzer: ICMP isReachable() innerhalb der Analyse-Pipeline. */
    public static final int ICMP_ANALYSIS_MS = 1500;
    /** PingMonitor: ICMP isReachable() für Dauer-Monitoring. */
    public static final int PING_MONITOR_MS = 2000;
    /** MdnsDiscovery: Socket-Timeout/Deadline für mDNS-Antworten. */
    public static final int MDNS_DISCOVERY_MS = 2000;
    /** UpnpDiscovery: Socket-Timeout/Deadline für SSDP/UPnP-Antworten. */
    public static final int UPNP_DISCOVERY_MS = 3000;
    /** OuiUpdater: HTTP-Connect-Timeout beim Nachladen der OUI-Datenbank. */
    public static final int OUI_UPDATE_MS = 10_000;
}

package main.java.networktool.logic.analysis;

public final class OsDetector {

    private OsDetector() {}

    public enum Confidence { HOCH, MITTEL, NIEDRIG }

    public static final class OsResult {
        public final String     os;
        public final Confidence confidence;
        public final String     method;

        public OsResult(String os, Confidence c, String m) {
            this.os = os; this.confidence = c; this.method = m;
        }

        public String display() { return os + " [" + confidence.name().charAt(0) + "]"; }
    }

    // ── Public API ────────────────────────────────────────────────────────

    public static OsResult detectWithConfidence(String ip) {
        String byPort = OsDetectorPorts.detectByPorts(ip);
        if (!byPort.equals("Unbekannt"))
            return new OsResult(byPort, Confidence.HOCH, "Port");

        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac != null) {
            String vendor = OuiDatabase.lookup(mac);
            if (vendor != null) return new OsResult(vendor, Confidence.MITTEL, "OUI/MAC");
        }

        String fromHostname = resolveFromHostname(ip);
        if (fromHostname != null)
            return new OsResult(fromHostname, Confidence.MITTEL, "Hostname");

        int ttl = OsDetectorArp.getTtl(ip);
        String fingerprint = OsFingerprint.resolve(ip, ttl, mac);
        if (fingerprint != null)
            return new OsResult(fingerprint, Confidence.NIEDRIG, ttl > 0 ? "TTL=" + ttl : "MAC");

        return new OsResult("Unbekannt", Confidence.NIEDRIG, "—");
    }

    public static String detect(String ip) {
        return detectWithConfidence(ip).os;
    }

    /** Erkennt OS nur aus Hostname – kein Netzwerk-Zugriff. */
    public static String detectFromHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip)) return null;
        if (hostname.startsWith("host-"))             return null;
        return OsDetectorHostname.classify(hostname.toLowerCase());
    }

    public static boolean isOpen(String ip, int port) {
        return OsDetectorPorts.isOpen(ip, port);
    }

    public static String getMacFromArp(String ip) {
        return OsDetectorArp.getMacFromArp(ip);
    }

    /** Delegiert an OsDetectorHostname. */
    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static String resolveFromHostname(String ip) {
        String[] result = {null};
        Thread t = new Thread(() -> {
            try {
                String h = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!h.equals(ip)) result[0] = h;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (result[0] == null) return null;
        return OsDetectorHostname.classify(result[0].toLowerCase());
    }
}
package main.java.networktool.logic.ports;

/** Textbereinigung und Fallback-Dienstnamen für {@link BannerGrabber}. Package-private. */
final class BannerText {

    private BannerText() {}

    /** Maximale Banner-Länge in der Ausgabe. */
    private static final int MAX_BANNER_LEN = 200;

    /**
     * Bereinigt einen Banner-String:
     * - Steuerzeichen entfernen (außer Leerzeichen)
     * - Zeilenumbrüche → " | "
     * - Auf MAX_BANNER_LEN kürzen
     */
    static String clean(String raw) {
        if (raw == null) return "offen";
        String result = raw
                .replace("\r\n", " | ").replace("\n", " | ").replace("\r", "")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // Steuerzeichen
                .replaceAll("\\s+", " ")
                .replaceAll("( \\| )+", " | ")
                .trim();
        // Trailing " |" entfernen
        if (result.endsWith(" |")) result = result.substring(0, result.length() - 2).trim();
        // Kürzen
        if (result.length() > MAX_BANNER_LEN)
            result = result.substring(0, MAX_BANNER_LEN - 1) + "…";
        return result.isBlank() ? "offen" : result;
    }

    /**
     * Gibt den bekannten Dienstnamen für einen Port zurück.
     * Fallback wenn kein Banner gelesen werden konnte.
     */
    static String serviceName(int port) {
        return switch (port) {
            case 21    -> "FTP";
            case 22    -> "SSH";
            case 23    -> "Telnet";
            case 25    -> "SMTP";
            case 53    -> "DNS";
            case 67    -> "DHCP";
            case 80    -> "HTTP";
            case 110   -> "POP3";
            case 135   -> "RPC";
            case 139   -> "NetBIOS";
            case 143   -> "IMAP";
            case 161   -> "SNMP";
            case 443   -> "HTTPS";
            case 445   -> "SMB";
            case 465   -> "SMTPS";
            case 515   -> "LPD";
            case 548   -> "AFP";
            case 587   -> "SMTP-Submission";
            case 631   -> "IPP/CUPS";
            case 993   -> "IMAPS";
            case 995   -> "POP3S";
            case 1433  -> "MSSQL";
            case 1521  -> "Oracle DB";
            case 1883  -> "MQTT";
            case 3000  -> "HTTP (Dev)";
            case 3306  -> "MySQL";
            case 3389  -> "RDP";
            case 5353  -> "mDNS";
            case 5432  -> "PostgreSQL";
            case 5984  -> "CouchDB";
            case 5985  -> "WinRM";
            case 5986  -> "WinRM TLS";
            case 6379  -> "Redis";
            case 6443  -> "Kubernetes API";
            case 8080  -> "HTTP-Alt";
            case 8443  -> "HTTPS-Alt";
            case 8883  -> "MQTT TLS";
            case 8888  -> "HTTP (Jupyter)";
            case 9090  -> "Prometheus";
            case 9100  -> "JetDirect";
            case 9200  -> "Elasticsearch";
            case 27017 -> "MongoDB";
            default    -> "offen";
        };
    }
}

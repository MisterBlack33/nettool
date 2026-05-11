package main.java.networktool_v3.logic.ports;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Liest den Server-Banner eines offenen Ports.
 *
 * Verbesserungen gegenüber der Vorgängerversion:
 *  - Timeout-Parameter statt hartkodierter 1000ms
 *  - Mehr Protokolle: HTTP, HTTPS, FTP, SSH, SMTP, POP3, IMAP, MySQL, Redis,
 *    RDP-Banner, MQTT, Telnet, SMB-Banner, SIP
 *  - Saubere Ausgabe: Steuerzeichen werden entfernt, Zeilenumbrüche → Leerzeichen
 *  - Banner-Länge auf 200 Zeichen begrenzt (kein Überlauf in der Tabelle)
 *  - Nie null – gibt immer einen sinnvollen String zurück
 */
public final class BannerGrabber {

    private BannerGrabber() {}

    /** Maximale Banner-Länge in der Ausgabe. */
    private static final int MAX_BANNER_LEN = 200;

    /** Standard-Timeout wenn kein Wert übergeben wird. */
    private static final int DEFAULT_TIMEOUT = 1_200;

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Liest den Banner eines offenen Ports.
     *
     * @param host      Ziel-IP oder Hostname
     * @param port      Ziel-Port
     * @param timeoutMs Verbindungs- und Lese-Timeout in ms
     * @return Banner-Text (nie null, nie leer – Fallback "offen")
     */
    public static String grab(String host, int port, int timeoutMs) {
        int to = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT;
        try {
            String banner = grabBanner(host, port, to);
            return banner != null && !banner.isBlank() ? clean(banner) : serviceName(port);
        } catch (Exception e) {
            return serviceName(port);
        }
    }

    /** Rückwärtskompatible Überladung mit Standard-Timeout. */
    public static String grab(String host, int port) {
        return grab(host, port, DEFAULT_TIMEOUT);
    }

    // ── Banner-Erkennung nach Port ────────────────────────────────────────

    private static String grabBanner(String host, int port, int timeout) throws Exception {
        return switch (port) {
            case 80, 8080, 8888, 3000 -> grabHttp(host, port, timeout, false);
            case 443, 8443             -> grabHttps(host, port, timeout);
            case 21                    -> grabPassive(host, port, timeout);   // FTP
            case 22                    -> grabPassive(host, port, timeout);   // SSH
            case 23                    -> grabPassive(host, port, timeout);   // Telnet
            case 25, 465, 587          -> grabSmtp(host, port, timeout);
            case 110, 995              -> grabPassive(host, port, timeout);   // POP3
            case 143, 993              -> grabPassive(host, port, timeout);   // IMAP
            case 3306                  -> grabMysql(host, port, timeout);
            case 6379                  -> grabRedis(host, port, timeout);
            case 5432                  -> grabPassive(host, port, timeout);   // PostgreSQL
            case 27017                 -> grabMongo(host, port, timeout);
            case 1883, 8883            -> "MQTT";
            case 5353                  -> "mDNS";
            case 631                   -> grabHttp(host, port, timeout, false);
            case 9100                  -> "RAW/JetDirect (Drucker)";
            case 515                   -> "LPD (Drucker)";
            case 548                   -> "AFP (Apple File Sharing)";
            case 161, 162              -> "SNMP";
            case 53                    -> "DNS";
            case 3389                  -> "RDP (Remote Desktop)";
            case 5985                  -> grabHttp(host, port, timeout, true);
            case 5986                  -> "WinRM HTTPS";
            case 9200                  -> grabHttp(host, port, timeout, false);  // Elasticsearch
            case 9090                  -> grabHttp(host, port, timeout, false);  // Prometheus
            default                    -> grabPassive(host, port, timeout);
        };
    }

    // ── Protokoll-spezifische Grabber ─────────────────────────────────────

    /**
     * Passiver Grabber: Verbindet und liest die ersten Bytes die der Server sendet.
     * Funktioniert für FTP, SSH, SMTP, POP3, IMAP, Telnet, PostgreSQL etc.
     */
    private static String grabPassive(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
            byte[] buf = new byte[256];
            int read = s.getInputStream().read(buf);
            if (read > 0) {
                return new String(buf, 0, read, StandardCharsets.UTF_8);
            }
        } catch (SocketTimeoutException ignored) {
            // Kein Banner gesendet – trotzdem offen
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * HTTP-Grabber: sendet HEAD-Request, liest Server-Header.
     */
    private static String grabHttp(String host, int port, int timeout, boolean winrm) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));

            String request = winrm
                    ? "GET / HTTP/1.0\r\nHost: " + host + "\r\n\r\n"
                    : "HEAD / HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";
            s.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = br.readLine();
            if (statusLine == null) return "HTTP";

            // Server-Header suchen
            String server = null;
            String powered = null;
            String line;
            int headerCount = 0;
            while ((line = br.readLine()) != null && headerCount < 30) {
                headerCount++;
                if (line.isBlank()) break;
                String lower = line.toLowerCase();
                if (lower.startsWith("server:"))       server  = line.substring(7).trim();
                if (lower.startsWith("x-powered-by:")) powered = line.substring(14).trim();
            }

            // Ergebnis zusammenbauen
            String status = statusLine.length() > 12 ? statusLine.substring(9, 12) : "";
            List<String> parts = new ArrayList<>();
            parts.add("HTTP " + status);
            if (server  != null) parts.add(server);
            if (powered != null) parts.add(powered);
            return String.join(" | ", parts);

        } catch (Exception ignored) {}
        return "HTTP";
    }

    /**
     * HTTPS-Grabber: versucht TLS-Verbindung für Server-Info.
     */
    private static String grabHttps(String host, int port, int timeout) {
        try {
            javax.net.ssl.SSLSocketFactory factory =
                    (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
            try (javax.net.ssl.SSLSocket s =
                         (javax.net.ssl.SSLSocket) factory.createSocket()) {
                s.connect(new InetSocketAddress(host, port), timeout);
                s.setSoTimeout(Math.min(timeout, 800));
                s.startHandshake();

                String request = "HEAD / HTTP/1.1\r\nHost: " + host
                        + "\r\nConnection: close\r\n\r\n";
                s.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
                s.getOutputStream().flush();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String statusLine = br.readLine();
                String server = null;
                String line;
                int count = 0;
                while ((line = br.readLine()) != null && count++ < 20) {
                    if (line.isBlank()) break;
                    if (line.toLowerCase().startsWith("server:"))
                        server = line.substring(7).trim();
                }
                String status = (statusLine != null && statusLine.length() > 12)
                        ? statusLine.substring(9, 12) : "";
                return "HTTPS " + status + (server != null ? " | " + server : "");
            }
        } catch (Exception ignored) {}
        return "HTTPS";
    }

    /**
     * SMTP-Grabber: liest 220-Greeting.
     */
    private static String grabSmtp(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count++ < 5) {
                if (line.startsWith("220")) {
                    sb.append(line.substring(3).trim());
                    if (!line.startsWith("220-")) break; // Mehrzeiliges Greeting fertig
                }
            }
            return sb.length() > 0 ? "SMTP: " + sb : "SMTP";
        } catch (Exception ignored) {}
        return "SMTP";
    }

    /**
     * MySQL-Grabber: liest Handshake-Paket für Version.
     */
    private static String grabMysql(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
            byte[] buf = new byte[64];
            int read = s.getInputStream().read(buf);
            if (read > 5) {
                // MySQL Handshake: Bytes 5+ sind die Server-Version als C-String
                int start = 5;
                int end   = start;
                while (end < read && buf[end] != 0) end++;
                if (end > start) {
                    String version = new String(buf, start, end - start, StandardCharsets.UTF_8);
                    if (version.matches("[0-9]+\\.[0-9]+.*"))
                        return "MySQL " + version;
                }
            }
        } catch (Exception ignored) {}
        return "MySQL";
    }

    /**
     * Redis-Grabber: sendet PING, erwartet +PONG.
     */
    private static String grabRedis(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 600));
            s.getOutputStream().write("PING\r\n".getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            byte[] buf = new byte[32];
            int read = s.getInputStream().read(buf);
            if (read > 0) {
                String resp = new String(buf, 0, read, StandardCharsets.UTF_8).trim();
                if (resp.contains("PONG")) return "Redis (PONG)";
                if (resp.startsWith("-")) return "Redis (Auth required)";
                return "Redis";
            }
        } catch (Exception ignored) {}
        return "Redis";
    }

    /**
     * MongoDB-Grabber: sendet isMaster-Kommando.
     */
    private static String grabMongo(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
            // MongoDB Wire Protocol: isMaster-Kommando
            byte[] msg = {
                    0x3f,0x00,0x00,0x00, // messageLength = 63
                    0x01,0x00,0x00,0x00, // requestID
                    0x00,0x00,0x00,0x00, // responseTo
                    (byte)0xd4,0x07,0x00,0x00, // opCode = OP_QUERY (2004)
                    0x00,0x00,0x00,0x00, // flags
                    0x61,0x64,0x6d,0x69,0x6e,0x2e,0x24,0x63,0x6d,0x64,0x00, // "admin.$cmd\0"
                    0x00,0x00,0x00,0x00, // numberToSkip
                    0x01,0x00,0x00,0x00, // numberToReturn
                    // BSON: {isMaster:1}
                    0x13,0x00,0x00,0x00,(byte)0x10,0x69,0x73,0x4d,0x61,0x73,0x74,0x65,0x72,0x00,
                    0x01,0x00,0x00,0x00,0x00
            };
            s.getOutputStream().write(msg);
            s.getOutputStream().flush();
            byte[] buf = new byte[128];
            int read = s.getInputStream().read(buf);
            if (read > 0) return "MongoDB";
        } catch (Exception ignored) {}
        return "MongoDB";
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    /**
     * Bereinigt einen Banner-String:
     * - Steuerzeichen entfernen (außer Leerzeichen)
     * - Zeilenumbrüche → " | "
     * - Auf MAX_BANNER_LEN kürzen
     */
    private static String clean(String raw) {
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
    private static String serviceName(int port) {
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
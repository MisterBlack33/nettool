package main.java.networktool.logic.ports;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Protokoll-spezifische Banner-Grabber für {@link BannerGrabber}. Package-private. */
final class BannerProtocolProbes {

    private BannerProtocolProbes() {}

    /**
     * Passiver Grabber: Verbindet und liest die ersten Bytes die der Server sendet.
     * Funktioniert für FTP, SSH, SMTP, POP3, IMAP, Telnet, PostgreSQL etc.
     */
    static String grabPassive(String host, int port, int timeout) {
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

    /** HTTP-Grabber: sendet HEAD-Request, liest Server-Header. */
    static String grabHttp(String host, int port, int timeout, boolean winrm) {
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

            String status = statusLine.length() > 12 ? statusLine.substring(9, 12) : "";
            List<String> parts = new ArrayList<>();
            parts.add("HTTP " + status);
            if (server  != null) parts.add(server);
            if (powered != null) parts.add(powered);
            return String.join(" | ", parts);

        } catch (Exception ignored) {}
        return "HTTP";
    }

    /** HTTPS-Grabber: versucht TLS-Verbindung für Server-Info. */
    static String grabHttps(String host, int port, int timeout) {
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

    /** SMTP-Grabber: liest 220-Greeting. */
    static String grabSmtp(String host, int port, int timeout) {
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

    /** MySQL-Grabber: liest Handshake-Paket für Version. */
    static String grabMysql(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
            byte[] buf = new byte[64];
            int read = s.getInputStream().read(buf);
            if (read > 5) {
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

    /** Redis-Grabber: sendet PING, erwartet +PONG. */
    static String grabRedis(String host, int port, int timeout) {
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

    /** MongoDB-Grabber: sendet isMaster-Kommando. */
    static String grabMongo(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(Math.min(timeout, 800));
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
}

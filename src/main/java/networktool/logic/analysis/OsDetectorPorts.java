// src/main/java/networktool/logic/analysis/OsDetectorPorts.java
package main.java.networktool.logic.analysis;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * OS-Erkennung per Port-Kombination.
 * Gewichtete Entscheidung statt erster-Treffer-gewinnt.
 */
final class OsDetectorPorts {

    private OsDetectorPorts() {}

    private static final int TIMEOUT_MS   = 400;
    private static final int THREAD_COUNT =
            Math.min(16, Runtime.getRuntime().availableProcessors() * 2);

    private static final int[] PROBE_PORTS = {
            445, 139, 135, 3389, 5985, 5986,  // Windows-spezifisch
            548, 5000, 7000, 5353,             // Apple
            22, 80, 443, 8080, 8443,           // Universal
            631, 9100, 515,                    // Drucker
            53, 161, 23, 67,                   // Netzwerkgeräte
            3306, 5432, 1433, 6379, 27017,     // Datenbanken
            25, 110, 143,                       // Mail
            1883, 8883                          // IoT/MQTT
    };

    static String detectByPorts(String ip) {
        Map<Integer, Boolean> open = probeAllPorts(ip);
        OsSignature signature = classify(open, ip);
        return signature != null ? signature.os : "Unbekannt";
    }

    static OsSignature detectWithSignature(String ip) {
        Map<Integer, Boolean> open = probeAllPorts(ip);
        return classify(open, ip);
    }

    private static Map<Integer, Boolean> probeAllPorts(String ip) {
        Map<Integer, Boolean> open = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        try {
            for (int port : PROBE_PORTS) {
                final int p = port;
                exec.submit(() -> open.put(p, isOpen(ip, p)));
            }
            exec.shutdown();
            exec.awaitTermination(TIMEOUT_MS + 200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return open;
    }

    private static OsSignature classify(Map<Integer, Boolean> open, String ip) {
        // Windows: eindeutige Port-Kombination
        if (is(open, 445) && is(open, 3389))  return OsSignature.of("Windows",  95, "Port-Kombination");
        if (is(open, 445) && is(open, 5985))  return OsSignature.of("Windows",  95, "Port-Kombination");
        if (is(open, 3389) && is(open, 135))  return OsSignature.of("Windows",  90, "Port-Kombination");
        if (is(open, 445) && !is(open, 22))   return OsSignature.of("Windows",  80, "SMB");
        if (is(open, 135) && !is(open, 22))   return OsSignature.of("Windows",  75, "RPC");

        // Apple
        if (is(open, 548) && is(open, 5353))  return OsSignature.of("macOS",    90, "AFP+mDNS");
        if (is(open, 548))                    return OsSignature.of("macOS",    80, "AFP");
        if (is(open, 5000) && !is(open, 445)) return OsSignature.of("macOS",    70, "AirPlay");

        // Drucker — vor Linux prüfen (hat oft Port 22 via Embedded)
        if (is(open, 9100))                   return OsSignature.of("Drucker (JetDirect)", 90, "Port");
        if (is(open, 631) && is(open, 515))   return OsSignature.of("Drucker (IPP/LPD)",  85, "Port-Kombination");
        if (is(open, 631) && !is(open, 22))   return OsSignature.of("Drucker (IPP/CUPS)", 80, "Port");
        if (is(open, 515) && !is(open, 22))   return OsSignature.of("Drucker (LPD)",      75, "Port");

        // IoT
        if (is(open, 1883) || is(open, 8883)) return OsSignature.of("IoT-Gerät (MQTT)",   85, "MQTT-Port");

        // Netzwerkgeräte
        if (is(open, 161) && !is(open, 22) && !is(open, 80))
            return OsSignature.of("Router / Switch (SNMP)", 85, "SNMP");
        if (is(open, 67)  && !is(open, 22) && !is(open, 80))
            return OsSignature.of("DHCP-Server",             80, "Port");
        if (is(open, 53)  && !is(open, 22) && !is(open, 80))
            return OsSignature.of("DNS-Server",              80, "Port");
        if (is(open, 23)  && !is(open, 22))
            return OsSignature.of("Router / Netzwerkgerät",  75, "Telnet");

        // Linux/Unix: Port 22 ohne Windows-Ports
        if (is(open, 22)) return classifyLinux(open, ip);

        // Web ohne SSH
        if (is(open, 80) || is(open, 8080))   return OsSignature.of(detectWebServer(ip), 50, "HTTP");
        if (is(open, 443) || is(open, 8443))  return OsSignature.of("Web-Server (HTTPS)", 45, "HTTPS");

        // Datenbanken
        if (is(open, 3306))  return OsSignature.of("Datenbankserver (MySQL)",      70, "Port");
        if (is(open, 5432))  return OsSignature.of("Datenbankserver (PostgreSQL)", 70, "Port");
        if (is(open, 6379))  return OsSignature.of("Datenbankserver (Redis)",      70, "Port");
        if (is(open, 27017)) return OsSignature.of("Datenbankserver (MongoDB)",    70, "Port");
        if (is(open, 1433))  return OsSignature.of("Datenbankserver (MSSQL)",      70, "Port");

        // Mail
        if (is(open, 25) || is(open, 110) || is(open, 143))
            return OsSignature.of("Mail-Server", 65, "Port");

        return null;
    }

    private static OsSignature classifyLinux(Map<Integer, Boolean> open, String ip) {
        if (is(open, 80)  && is(open, 443)) return OsSignature.of(detectWebServer(ip), 65, "Web+SSH");
        if (is(open, 80))                   return OsSignature.of("Web-Server (HTTP)",  60, "HTTP+SSH");
        if (is(open, 443))                  return OsSignature.of("Web-Server (HTTPS)", 60, "HTTPS+SSH");
        if (is(open, 25)  && (is(open, 143) || is(open, 993)))
            return OsSignature.of("Mail-Server (Linux)", 70, "Mail+SSH");
        if (is(open, 3306)) return OsSignature.of("Datenbankserver (MySQL/Linux)", 70, "MySQL+SSH");
        if (is(open, 5432)) return OsSignature.of("Datenbankserver (PostgreSQL)",  70, "PG+SSH");
        if (is(open, 6379)) return OsSignature.of("Datenbankserver (Redis)",       70, "Redis+SSH");
        if (is(open, 9090)) return OsSignature.of("Monitoring-Server",             65, "Prometheus+SSH");
        if (is(open, 9200)) return OsSignature.of("Suchserver (Elasticsearch)",    65, "ES+SSH");
        return OsSignature.of("Linux/Unix", 55, "SSH");
    }

    static String detectWebServer(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 80), 400);
            s.setSoTimeout(400);
            s.getOutputStream().write(
                    ("HEAD / HTTP/1.1\r\nHost: " + ip + "\r\nConnection: close\r\n\r\n").getBytes());
            s.getOutputStream().flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.toLowerCase().startsWith("server:")) continue;
                String srv = line.substring(7).trim().toLowerCase();
                if (srv.contains("nginx"))    return "Web-Server (nginx)";
                if (srv.contains("apache"))   return "Web-Server (Apache)";
                if (srv.contains("iis"))      return "Web-Server (IIS/Windows)";
                if (srv.contains("caddy"))    return "Web-Server (Caddy)";
                if (srv.contains("lighttpd")) return "Web-Server (lighttpd)";
                if (!srv.isBlank())           return "Web-Server (" + srv.split("/")[0] + ")";
            }
        } catch (Exception ignored) {}
        return "Web-Server";
    }

    static boolean isOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            return true;
        } catch (Exception e) { return false; }
    }

    private static boolean is(Map<Integer, Boolean> map, int port) {
        return Boolean.TRUE.equals(map.get(port));
    }
}
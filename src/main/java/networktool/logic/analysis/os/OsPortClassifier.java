package main.java.networktool.logic.analysis.os;

import java.util.Map;

/** Gewichtete OS-Klassifizierung anhand offener Ports. Package-private. */
final class OsPortClassifier {

    private OsPortClassifier() {}

    static OsSignature classify(Map<Integer, Boolean> open, String ip) {
        // Windows: eindeutige Port-Kombination (sehr zuverlässig)
        if (is(open, 445) && is(open, 3389))  return OsSignature.of("Windows",  95, "Port-Kombination");
        if (is(open, 445) && is(open, 5985))  return OsSignature.of("Windows",  95, "Port-Kombination");
        if (is(open, 3389) && is(open, 135))  return OsSignature.of("Windows",  90, "Port-Kombination");
        if (is(open, 445) && !is(open, 22))   return OsSignature.of("Windows",  80, "SMB");
        if (is(open, 135) && !is(open, 22))   return OsSignature.of("Windows",  75, "RPC");
        if (is(open, 139) && !is(open, 22))   return OsSignature.of("Windows",  70, "NetBIOS");

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

        // Web-Server — besserer Fallback wenn Ports blockiert
        if (is(open, 80) || is(open, 8080) || is(open, 8000) || is(open, 8888))
            return OsSignature.of(OsDetectorPorts.detectWebServer(ip), 50, "HTTP");
        if (is(open, 443) || is(open, 8443))
            return OsSignature.of("Web-Server (HTTPS)", 45, "HTTPS");

        // Datenbanken - auch wenn andere Services blockiert sind
        if (is(open, 3306) && !is(open, 22))
            return OsSignature.of("Linux/Unix (MySQL)",      40, "Port");
        if (is(open, 5432) && !is(open, 22))
            return OsSignature.of("Linux/Unix (PostgreSQL)", 40, "Port");
        if (is(open, 1433) && !is(open, 22))
            return OsSignature.of("Windows (MSSQL)",         50, "Port");

        return null;
    }

    private static OsSignature classifyLinux(Map<Integer, Boolean> open, String ip) {
        if (is(open, 80)  && is(open, 443)) return OsSignature.of(OsDetectorPorts.detectWebServer(ip), 65, "Web+SSH");
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

    private static boolean is(Map<Integer, Boolean> map, int port) {
        return Boolean.TRUE.equals(map.get(port));
    }
}
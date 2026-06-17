package main.java.networktool.logic.analysis;

import java.net.*;

/**
 * UDP-basierte OS-Erkennung: mDNS, NetBIOS, SNMP.
 * Funktioniert auch wenn TCP-Ports durch Firewalls blockiert sind.
 */
final class OsProbeUdp {

    private OsProbeUdp() {}

    private static final int TIMEOUT_MS = 800;

    /** Kombiniertes UDP-Probing. Gibt beste OsSignature oder null zurück. */
    static OsSignature probe(String ip) {
        OsSignature best = null;
        best = OsSignature.best(best, probeNetBios(ip));
        best = OsSignature.best(best, probeMdns(ip));
        best = OsSignature.best(best, probeSnmp(ip));
        return best;
    }

    // ── NetBIOS Name Query (UDP 137) ──────────────────────────────────────
    // Funktioniert zuverlässig bei Windows-Rechnern ohne offene TCP-Ports

    static OsSignature probeNetBios(String ip) {
        byte[] query = buildNetBiosQuery();
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(TIMEOUT_MS);
            InetAddress addr = InetAddress.getByName(ip);
            sock.send(new DatagramPacket(query, query.length, addr, 137));
            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            sock.receive(resp);
            if (resp.getLength() > 56) {
                // Antwort erhalten → Windows oder Samba
                String name = extractNetBiosName(buf, resp.getLength());
                if (name != null && name.toLowerCase().contains("samba")) {
                    return OsSignature.of("Linux/Unix (Samba)", 72, "NetBIOS");
                }
                return OsSignature.of("Windows", 75, "NetBIOS");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] buildNetBiosQuery() {
        // Standard NetBIOS Name Service Query für "*"
        return new byte[]{
                0x00, 0x01,             // Transaction ID
                0x00, 0x00,             // Flags: Query
                0x00, 0x01,             // Questions: 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x20,                   // Name length
                0x43, 0x4B, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x00,                   // End of name
                0x00, 0x21,             // Type: NBSTAT
                0x00, 0x01              // Class: IN
        };
    }

    private static String extractNetBiosName(byte[] buf, int len) {
        // Namen beginnen ab Offset 56 (nach Header + Fragen-Section)
        if (len < 58) return null;
        int numNames = buf[56] & 0xFF;
        if (numNames == 0 || len < 57 + numNames * 18) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numNames && i < 3; i++) {
            int off = 57 + i * 18;
            String name = new String(buf, off, 15).trim();
            if (!name.isBlank()) sb.append(name).append(" ");
        }
        return sb.toString().trim();
    }

    // ── mDNS Query (UDP 5353) ─────────────────────────────────────────────
    // Apple-Geräte antworten fast immer; auch viele Linux/IoT-Geräte

    static OsSignature probeMdns(String ip) {
        byte[] query = buildMdnsQuery();
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(TIMEOUT_MS);
            InetAddress addr = InetAddress.getByName(ip);
            // Unicast mDNS (direkt an Host, nicht Multicast)
            sock.send(new DatagramPacket(query, query.length, addr, 5353));
            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            sock.receive(resp);
            if (resp.getLength() > 12) {
                String response = new String(buf, 0, resp.getLength());
                return classifyMdnsResponse(response);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] buildMdnsQuery() {
        return new byte[]{
                0x00, 0x00,             // Transaction ID: 0
                0x00, 0x00,             // Flags: Standard Query
                0x00, 0x01,             // Questions: 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                // Query: _services._dns-sd._udp.local PTR
                0x09, 0x5f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x73,
                0x07, 0x5f, 0x64, 0x6e, 0x73, 0x2d, 0x73, 0x64,
                0x04, 0x5f, 0x75, 0x64, 0x70,
                0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x00,
                0x00, 0x0c,             // Type: PTR
                0x00, 0x01              // Class: IN
        };
    }

    private static OsSignature classifyMdnsResponse(String response) {
        String r = response.toLowerCase();
        if (r.contains("apple") || r.contains("iphone") || r.contains("ipad")
                || r.contains("macbook") || r.contains("imac")) {
            return OsSignature.of("Apple-Gerät (mDNS)", 78, "mDNS");
        }
        if (r.contains("android") || r.contains("samsung") || r.contains("pixel")) {
            return OsSignature.of("Android", 72, "mDNS");
        }
        if (r.contains("printer") || r.contains("print") || r.contains("ipp")) {
            return OsSignature.of("Drucker (mDNS)", 75, "mDNS");
        }
        if (r.contains("chromecast") || r.contains("googlecast")) {
            return OsSignature.of("Chromecast", 85, "mDNS");
        }
        if (r.contains("sonos"))   return OsSignature.of("Sonos",        85, "mDNS");
        if (r.contains("shelly"))  return OsSignature.of("Shelly (IoT)", 85, "mDNS");
        // Irgendeine mDNS-Antwort → mindestens Linux/macOS/IoT
        return OsSignature.of("Linux/Unix oder Apple", 45, "mDNS");
    }

    // ── SNMP Community-Probe (UDP 161) ────────────────────────────────────
    // Netzwerkgeräte (Router, Switches) antworten auf "public"

    static OsSignature probeSnmp(String ip) {
        byte[] getRequest = buildSnmpGetRequest();
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(TIMEOUT_MS);
            InetAddress addr = InetAddress.getByName(ip);
            sock.send(new DatagramPacket(getRequest, getRequest.length, addr, 161));
            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            sock.receive(resp);
            if (resp.getLength() > 0) {
                String content = extractSnmpString(buf, resp.getLength());
                return classifySnmpResponse(content);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] buildSnmpGetRequest() {
        // SNMPv1 GetRequest für sysDescr (OID 1.3.6.1.2.1.1.1.0)
        return new byte[]{
                0x30, 0x26,                         // SEQUENCE
                0x02, 0x01, 0x00,                   // Version: 1
                0x04, 0x06, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, // Community: "public"
                0xa0, 0x19,                         // GetRequest PDU
                0x02, 0x01, 0x01,                   // Request ID: 1
                0x02, 0x01, 0x00,                   // Error status: 0
                0x02, 0x01, 0x00,                   // Error index: 0
                0x30, 0x0e,                         // VarBindList
                0x30, 0x0c,                         // VarBind
                0x06, 0x08, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00, // sysDescr
                0x05, 0x00                          // NULL
        };
    }

    private static String extractSnmpString(byte[] buf, int len) {
        // Suche nach OctetString-Wert in der Antwort (vereinfacht)
        for (int i = 0; i < len - 2; i++) {
            if ((buf[i] & 0xFF) == 0x04) {   // OctetString tag
                int strLen = buf[i + 1] & 0xFF;
                if (i + 2 + strLen <= len) {
                    return new String(buf, i + 2, strLen);
                }
            }
        }
        return "";
    }

    private static OsSignature classifySnmpResponse(String sysDescr) {
        String d = sysDescr.toLowerCase();
        if (d.contains("routeros") || d.contains("mikrotik")) return OsSignature.of("Router (MikroTik)", 90, "SNMP");
        if (d.contains("cisco"))                               return OsSignature.of("Cisco-Gerät",       88, "SNMP");
        if (d.contains("juniper"))                             return OsSignature.of("Juniper-Gerät",     88, "SNMP");
        if (d.contains("ubiquiti") || d.contains("unifi"))     return OsSignature.of("Access Point (Ubiquiti)", 88, "SNMP");
        if (d.contains("fritz"))                               return OsSignature.of("Router (FRITZ!Box)", 90, "SNMP");
        if (d.contains("synology"))                            return OsSignature.of("NAS (Synology)",    88, "SNMP");
        if (d.contains("qnap"))                                return OsSignature.of("NAS (QNAP)",        88, "SNMP");
        if (d.contains("linux"))                               return OsSignature.of("Linux/Unix",        70, "SNMP");
        if (d.contains("windows"))                             return OsSignature.of("Windows",           70, "SNMP");
        if (!d.isBlank())                                      return OsSignature.of("Netzwerkgerät",     60, "SNMP");
        return null;
    }
}
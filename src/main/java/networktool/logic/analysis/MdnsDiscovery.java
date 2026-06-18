package main.java.networktool.logic.analysis;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vollständige mDNS/Bonjour Service-Discovery.
 * Fragt bekannte Service-Typen ab und wertet Antworten aus.
 *
 * Abgefragte Service-Typen:
 *  _http._tcp.local, _ftp._tcp.local, _ssh._tcp.local,
 *  _printer._tcp.local, _ipp._tcp.local, _smb._tcp.local,
 *  _airplay._tcp.local, _googlecast._tcp.local
 */
public final class MdnsDiscovery {

    private MdnsDiscovery() {}

    private static final String MDNS_GROUP  = "224.0.0.251";
    private static final int    MDNS_PORT   = 5353;
    private static final int    TIMEOUT_MS  = 2000;

    private static final String[] SERVICE_TYPES = {
            "_http._tcp.local",
            "_ftp._tcp.local",
            "_ssh._tcp.local",
            "_printer._tcp.local",
            "_ipp._tcp.local",
            "_smb._tcp.local",
            "_airplay._tcp.local",
            "_googlecast._tcp.local",
            "_raop._tcp.local",
            "_daap._tcp.local"
    };

    public record ServiceRecord(
            String ip,
            String serviceType,
            String name,
            int    port
    ) {
        public String guessOs() {
            String st = serviceType.toLowerCase();
            if (st.contains("airplay") || st.contains("raop") || st.contains("daap"))
                return "Apple-Gerät";
            if (st.contains("googlecast"))
                return "Chromecast";
            if (st.contains("printer") || st.contains("ipp"))
                return "Drucker";
            if (st.contains("smb"))
                return "Windows oder Samba";
            return null;
        }
    }

    /**
     * Discovert mDNS-Services im lokalen Netz.
     */
    public static List<ServiceRecord> discover() {
        List<ServiceRecord> results = Collections.synchronizedList(new ArrayList<>());
        Set<String> seen = ConcurrentHashMap.newKeySet();

        try (MulticastSocket socket = new MulticastSocket(MDNS_PORT)) {
            InetAddress group = InetAddress.getByName(MDNS_GROUP);
            socket.joinGroup(group);
            socket.setSoTimeout(TIMEOUT_MS);

            for (String serviceType : SERVICE_TYPES) {
                byte[] query = buildQuery(serviceType);
                socket.send(new DatagramPacket(query, query.length, group, MDNS_PORT));
            }

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    byte[] buf = new byte[4096];
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    socket.receive(pkt);

                    String ip  = pkt.getAddress().getHostAddress();
                    String key = ip + ":" + pkt.getLength();
                    if (!seen.add(key)) continue;

                    parseResponse(ip, buf, pkt.getLength(), results);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            socket.leaveGroup(group);
        } catch (Exception ignored) {}

        return Collections.unmodifiableList(results);
    }

    /**
     * Fragt einen spezifischen Host direkt per Unicast-mDNS ab.
     */
    public static List<ServiceRecord> queryHost(String ip) {
        List<ServiceRecord> results = new ArrayList<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress addr = InetAddress.getByName(ip);

            for (String serviceType : SERVICE_TYPES) {
                byte[] query = buildQuery(serviceType);
                socket.send(new DatagramPacket(query, query.length, addr, MDNS_PORT));
            }

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    byte[] buf = new byte[4096];
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    socket.receive(pkt);
                    parseResponse(ip, buf, pkt.getLength(), results);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        } catch (Exception ignored) {}
        return Collections.unmodifiableList(results);
    }

    // ── DNS-Message Builder ───────────────────────────────────────────────

    private static byte[] buildQuery(String serviceType) {
        List<Byte> msg = new ArrayList<>();

        // Header: ID=0, QR=Query, QDCOUNT=1
        msg.addAll(bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00));

        // QNAME: encode labels
        for (String label : serviceType.split("\\.")) {
            byte[] lb = label.getBytes(StandardCharsets.UTF_8);
            msg.add((byte) lb.length);
            for (byte b : lb) msg.add(b);
        }
        msg.add((byte) 0x00); // root

        // QTYPE=PTR(12), QCLASS=IN(1) with unicast bit
        msg.addAll(bytes(0x00, 0x0C, 0x80, 0x01));

        byte[] result = new byte[msg.size()];
        for (int i = 0; i < msg.size(); i++) result[i] = msg.get(i);
        return result;
    }

    private static List<Byte> bytes(int... values) {
        List<Byte> list = new ArrayList<>();
        for (int v : values) list.add((byte) v);
        return list;
    }

    /** Minimaler DNS-Response-Parser: extrahiert lesbare Namen und Ports. */
    private static void parseResponse(String ip, byte[] buf, int len,
                                      List<ServiceRecord> results) {
        try {
            // ANCOUNT bei Offset 6
            int anCount = ((buf[6] & 0xFF) << 8) | (buf[7] & 0xFF);
            if (anCount == 0) return;

            // Einfache Heuristik: suche TXT-ähnliche Strings im Payload
            String raw = new String(buf, 0, len, StandardCharsets.UTF_8);

            for (String svcType : SERVICE_TYPES) {
                String shortType = svcType.replace(".local", "");
                if (raw.contains(shortType.replace("_", ""))) {
                    results.add(new ServiceRecord(ip, svcType, extractName(raw), 0));
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static String extractName(String raw) {
        // Extrahiert erste lesbare Zeichenkette (Geräte-Name)
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c >= 0x20 && c < 0x7F && c != '.') sb.append(c);
            else if (sb.length() > 3) break;
        }
        return sb.length() > 0 ? sb.toString().trim() : "unbekannt";
    }
}
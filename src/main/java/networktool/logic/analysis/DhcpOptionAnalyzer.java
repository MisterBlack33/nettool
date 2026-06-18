package main.java.networktool.logic.analysis;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Wertet DHCP Option 60 (Vendor Class Identifier) aus.
 * Sendet ein DHCP-Discover-Paket und liest die Option-60-Antwort.
 *
 * Bekannte Vendor-Class-Strings:
 *  "MSFT 5.0"          → Windows
 *  "android-dhcp-*"    → Android
 *  "dhcpcd-*"          → Linux (dhcpcd)
 *  "udhcp *"           → Embedded Linux / Router
 *  "AAPLBSDPC"         → Apple BSDP (macOS)
 */
public final class DhcpOptionAnalyzer {

    private DhcpOptionAnalyzer() {}

    private static final int DHCP_SERVER_PORT = 67;
    private static final int DHCP_CLIENT_PORT = 68;
    private static final int TIMEOUT_MS       = 1500;

    public record Result(String vendorClass, String detectedOs) {}

    /**
     * Sendet DHCP-Discover an den Host und wertet Option 60 der Antwort aus.
     * Gibt null zurück wenn kein DHCP-Server antwortet.
     */
    public static Result analyze(String ip) {
        try (DatagramSocket socket = new DatagramSocket(DHCP_CLIENT_PORT)) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setBroadcast(true);

            byte[] discover = buildDiscover();
            InetAddress target = InetAddress.getByName(ip);
            socket.send(new DatagramPacket(discover, discover.length, target, DHCP_SERVER_PORT));

            byte[] buf = new byte[1024];
            DatagramPacket response = new DatagramPacket(buf, buf.length);
            socket.receive(response);

            String vendorClass = extractOption60(buf, response.getLength());
            if (vendorClass == null) return null;

            return new Result(vendorClass, classify(vendorClass));
        } catch (Exception e) {
            return null;
        }
    }

    private static String classify(String vendorClass) {
        String v = vendorClass.toLowerCase();
        if (v.contains("msft"))           return "Windows";
        if (v.contains("android-dhcp"))   return "Android";
        if (v.contains("aaplbsdpc"))      return "macOS";
        if (v.contains("dhcpcd"))         return "Linux";
        if (v.contains("udhcp"))          return "Embedded Linux / Router";
        if (v.contains("cisco"))          return "Cisco-Gerät";
        if (v.contains("aruba"))          return "Access Point (Aruba)";
        return "Unbekannt (" + vendorClass + ")";
    }

    private static byte[] buildDiscover() {
        byte[] pkt = new byte[300];
        pkt[0]  = 0x01; // BOOTREQUEST
        pkt[1]  = 0x01; // Ethernet
        pkt[2]  = 0x06; // HW addr length

        // XID (random)
        byte[] xid = new byte[4];
        new Random().nextBytes(xid);
        System.arraycopy(xid, 0, pkt, 4, 4);

        // Magic cookie
        pkt[236] = 0x63; pkt[237] = (byte) 0x82;
        pkt[238] = 0x53; pkt[239] = 0x63;

        // Option 53: DHCP Discover
        pkt[240] = 0x35; pkt[241] = 0x01; pkt[242] = 0x01;

        // Option 60: Vendor Class "NetTool"
        byte[] vc = "NetTool".getBytes(StandardCharsets.US_ASCII);
        pkt[243] = 0x3C;
        pkt[244] = (byte) vc.length;
        System.arraycopy(vc, 0, pkt, 245, vc.length);

        pkt[245 + vc.length] = (byte) 0xFF; // End
        return pkt;
    }

    private static String extractOption60(byte[] buf, int len) {
        int i = 240; // nach Magic Cookie
        while (i < len - 1) {
            int code = buf[i] & 0xFF;
            if (code == 255) break; // End
            if (code == 0)  { i++; continue; } // Pad

            int optLen = buf[i + 1] & 0xFF;
            if (code == 60 && i + 2 + optLen <= len) {
                return new String(buf, i + 2, optLen, StandardCharsets.US_ASCII);
            }
            i += 2 + optLen;
        }
        return null;
    }
}
package main.java.networktool.logic.analysis;

import java.net.*;

/**
 * Sendet einen Wake-on-LAN Magic Packet an eine MAC-Adresse.
 *
 * Protokoll: 6 × 0xFF, gefolgt von 16 × Ziel-MAC (= 102 Bytes)
 * Übertragung via UDP-Broadcast auf Port 9 oder 7.
 *
 * Voraussetzungen im Zielnetz:
 *  - Ziel-Host hat WoL im BIOS/UEFI aktiviert
 *  - Netzwerkkarte unterstützt WoL
 *  - UDP-Broadcast wird nicht gefiltert (Schulnetz: oft erlaubt in /24)
 */
public final class WakeOnLan {

    private WakeOnLan() {}

    private static final int WOL_PORT    = 9;
    private static final int PACKET_SIZE = 102; // 6 + 16*6

    /**
     * Sendet einen Magic Packet.
     *
     * @param macAddress  MAC im Format "AA:BB:CC:DD:EE:FF" oder "AA-BB-CC-DD-EE-FF"
     * @param broadcast   Broadcast-Adresse des Zielnetzes (z.B. "192.168.1.255")
     *                    oder "255.255.255.255" für globalen Broadcast
     */
    public static boolean send(String macAddress, String broadcast) {
        try {
            byte[] mac    = parseMac(macAddress);
            byte[] packet = buildPacket(mac);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                InetAddress addr = InetAddress.getByName(broadcast);
                DatagramPacket dp = new DatagramPacket(packet, packet.length, addr, WOL_PORT);
                socket.send(dp);
                System.out.println("  ⚡ WoL gesendet an " + macAddress
                        + "  (Broadcast: " + broadcast + ")");
                return true;
            }
        } catch (Exception e) {
            System.err.println("  WoL-Fehler: " + e.getMessage());
            return false;
        }
    }

    /**
     * Leitet die Broadcast-Adresse aus einer Host-IP und einer Netzmaske ab.
     * Fallback: 255.255.255.255
     *
     * @param ip      z.B. "192.168.1.42"
     * @param prefix  Netzpräfix-Länge (z.B. 24 für /24)
     */
    public static String deriveBroadcast(String ip, int prefix) {
        try {
            byte[] addr = InetAddress.getByName(ip).getAddress();
            int ipInt   = ((addr[0] & 0xFF) << 24) | ((addr[1] & 0xFF) << 16)
                        | ((addr[2] & 0xFF) <<  8) |  (addr[3] & 0xFF);
            int mask    = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
            int bcast   = (ipInt & mask) | ~mask;
            return ((bcast >> 24) & 0xFF) + "." + ((bcast >> 16) & 0xFF)
                 + "." + ((bcast >> 8) & 0xFF) + "." + (bcast & 0xFF);
        } catch (Exception e) {
            return "255.255.255.255";
        }
    }

    /**
     * Extrahiert die MAC-Adresse aus einem Hostnamen mit MAC-Anteil
     * (Format wie in HostResult.hostname: "server [AA:BB:CC:DD:EE:FF]")
     */
    public static String extractMacFromHostname(String hostname) {
        if (hostname == null) return null;
        int start = hostname.indexOf('[');
        int end   = hostname.indexOf(']');
        if (start < 0 || end < 0 || end <= start) return null;
        String candidate = hostname.substring(start + 1, end).trim();
        // Prüfen ob es wie eine MAC aussieht
        if (candidate.matches("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}"))
            return candidate;
        return null;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static byte[] parseMac(String mac) {
        String[] parts = mac.split("[:\\-]");
        if (parts.length != 6) throw new IllegalArgumentException("Ungültige MAC: " + mac);
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++)
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        return bytes;
    }

    private static byte[] buildPacket(byte[] mac) {
        byte[] packet = new byte[PACKET_SIZE];
        // 6 × 0xFF
        for (int i = 0; i < 6; i++) packet[i] = (byte) 0xFF;
        // 16 × MAC
        for (int i = 1; i <= 16; i++)
            System.arraycopy(mac, 0, packet, i * 6, 6);
        return packet;
    }
}

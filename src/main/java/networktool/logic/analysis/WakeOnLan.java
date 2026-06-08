package main.java.networktool.logic.analysis;

import java.net.*;
import java.util.regex.Pattern;

public final class WakeOnLan {

    private WakeOnLan() {}

    private static final int    WOL_PORT    = 9;
    private static final int    PACKET_SIZE = 102;
    private static final String BROADCAST_FALLBACK = "255.255.255.255";

    private static final Pattern IPV4 =
            Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

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
     * Leitet Broadcast-Adresse aus IP + Prefix ab.
     * Gibt Fallback zurück wenn IP kein gültiges IPv4-Format hat –
     * ohne DNS-Lookup (verhindert Hänger bei ungültigen Eingaben).
     */
    public static String deriveBroadcast(String ip, int prefix) {
        if (ip == null || !IPV4.matcher(ip).matches())
            return BROADCAST_FALLBACK;
        try {
            byte[] addr = InetAddress.getByName(ip).getAddress();
            int ipInt = ((addr[0] & 0xFF) << 24) | ((addr[1] & 0xFF) << 16)
                    | ((addr[2] & 0xFF) <<  8) |  (addr[3] & 0xFF);
            int mask  = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
            int bcast = (ipInt & mask) | ~mask;
            return ((bcast >> 24) & 0xFF) + "." + ((bcast >> 16) & 0xFF)
                    + "." + ((bcast >> 8) & 0xFF) + "." + (bcast & 0xFF);
        } catch (Exception e) {
            return BROADCAST_FALLBACK;
        }
    }

    public static String extractMacFromHostname(String hostname) {
        if (hostname == null) return null;
        int start = hostname.indexOf('[');
        int end   = hostname.indexOf(']');
        if (start < 0 || end < 0 || end <= start) return null;
        String candidate = hostname.substring(start + 1, end).trim();
        if (candidate.matches("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}"))
            return candidate;
        return null;
    }

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
        for (int i = 0; i < 6; i++) packet[i] = (byte) 0xFF;
        for (int i = 1; i <= 16; i++) System.arraycopy(mac, 0, packet, i * 6, 6);
        return packet;
    }
}
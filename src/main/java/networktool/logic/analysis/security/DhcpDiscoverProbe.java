package main.java.networktool.logic.analysis.security;

import main.java.networktool.logging.DebugLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/** Sendet ein DHCP-Discover per Broadcast und sammelt die Absender der Offers. */
public final class DhcpDiscoverProbe {

    private static final int  SERVER_PORT    = 67;
    private static final int  CLIENT_PORT    = 68;
    private static final int  PACKET_LEN     = 300;
    private static final int  RECEIVE_BUFFER = 1024;
    private static final int  XID_OFFSET     = 4;
    private static final int  XID_LEN        = 4;
    private static final int  FLAGS_OFFSET   = 10;
    private static final int  MAGIC_OFFSET   = 236;
    private static final int  OPTIONS_OFFSET = 240;
    private static final int  NO_TYPE        = -1;
    private static final int  OPT_PAD        = 0;
    private static final int  OPT_MSG_TYPE   = 53;
    private static final int  OPT_END        = 255;
    private static final int  MSG_OFFER      = 2;
    private static final byte BOOT_REQUEST   = 1;
    private static final byte BOOT_REPLY     = 2;
    private static final byte HTYPE_ETHERNET = 1;
    private static final byte HLEN_ETHERNET  = 6;
    private static final byte MSG_DISCOVER   = 1;
    private static final byte BROADCAST_FLAG = (byte) 0x80;
    private static final byte[] MAGIC = {0x63, (byte) 0x82, 0x53, 0x63};

    private DhcpDiscoverProbe() {}

    /** Leeres Set bei Fehlern (z.B. Port 68 belegt) — best effort. */
    public static Set<String> collectServers(int timeoutMs) {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(CLIENT_PORT));
            socket.setBroadcast(true);
            byte[] discover = buildDiscover();
            socket.send(new DatagramPacket(discover, discover.length,
                    InetAddress.getByName("255.255.255.255"), SERVER_PORT));
            return readOfferSenders(socket, timeoutMs);
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[DhcpDiscoverProbe] Probe fehlgeschlagen: " + e);
            return Set.of();
        }
    }

    static Set<String> readOfferSenders(DatagramSocket socket, int timeoutMs) throws IOException {
        Set<String> servers = new TreeSet<>();
        byte[] buf = new byte[RECEIVE_BUFFER];
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            int remaining = (int) (deadline - System.currentTimeMillis());
            if (remaining <= 0) return servers;
            socket.setSoTimeout(remaining);
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException e) {
                return servers;
            }
            if (isOffer(packet.getData(), packet.getLength()))
                servers.add(packet.getAddress().getHostAddress());
        }
    }

    static byte[] buildDiscover() {
        byte[] pkt = new byte[PACKET_LEN];
        pkt[0] = BOOT_REQUEST;
        pkt[1] = HTYPE_ETHERNET;
        pkt[2] = HLEN_ETHERNET;
        byte[] xid = new byte[XID_LEN];
        new SecureRandom().nextBytes(xid);
        System.arraycopy(xid, 0, pkt, XID_OFFSET, XID_LEN);
        pkt[FLAGS_OFFSET] = BROADCAST_FLAG;
        System.arraycopy(MAGIC, 0, pkt, MAGIC_OFFSET, MAGIC.length);
        pkt[OPTIONS_OFFSET]     = (byte) OPT_MSG_TYPE;
        pkt[OPTIONS_OFFSET + 1] = 1;
        pkt[OPTIONS_OFFSET + 2] = MSG_DISCOVER;
        pkt[OPTIONS_OFFSET + 3] = (byte) OPT_END;
        return pkt;
    }

    static boolean isOffer(byte[] buf, int len) {
        return len > OPTIONS_OFFSET && buf[0] == BOOT_REPLY && messageType(buf, len) == MSG_OFFER;
    }

    private static int messageType(byte[] buf, int len) {
        if (!Arrays.equals(buf, MAGIC_OFFSET, MAGIC_OFFSET + MAGIC.length, MAGIC, 0, MAGIC.length))
            return NO_TYPE;
        int i = OPTIONS_OFFSET;
        while (i + 1 < len) {
            int code = buf[i] & 0xFF;
            if (code == OPT_END) break;
            if (code == OPT_PAD) { i++; continue; }
            if (code == OPT_MSG_TYPE && i + 2 < len) return buf[i + 2] & 0xFF;
            i += 2 + (buf[i + 1] & 0xFF);
        }
        return NO_TYPE;
    }
}

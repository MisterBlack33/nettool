package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DhcpDiscoverProbeTest {

    private static final int OPTIONS = 240;

    private static byte[] offer() {
        byte[] p = DhcpDiscoverProbe.buildDiscover();
        p[0] = 2;
        p[OPTIONS + 2] = 2;
        return p;
    }

    private static Set<String> sendAndRead(byte[] payload, int timeoutMs) throws Exception {
        try (DatagramSocket receiver = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             DatagramSocket sender = new DatagramSocket()) {
            sender.send(new DatagramPacket(payload, payload.length,
                    InetAddress.getLoopbackAddress(), receiver.getLocalPort()));
            return DhcpDiscoverProbe.readOfferSenders(receiver, timeoutMs);
        }
    }

    @Test void buildDiscover_hasMagicCookieAndDiscoverType() {
        byte[] p = DhcpDiscoverProbe.buildDiscover();
        assertEquals(1, p[0]);
        assertEquals(0x63, p[236]);
        assertEquals(53, p[OPTIONS]);
        assertEquals(1, p[OPTIONS + 2]);
    }

    @Test void isOffer_validOffer_true()   { assertTrue(DhcpDiscoverProbe.isOffer(offer(), 300)); }
    @Test void isOffer_discover_false() {
        byte[] p = DhcpDiscoverProbe.buildDiscover();
        assertFalse(DhcpDiscoverProbe.isOffer(p, p.length));
    }
    @Test void isOffer_tooShort_false()    { assertFalse(DhcpDiscoverProbe.isOffer(offer(), 100)); }
    @Test void isOffer_noMagic_false() {
        byte[] p = offer();
        p[236] = 0;
        assertFalse(DhcpDiscoverProbe.isOffer(p, 300));
    }
    @Test void isOffer_padBeforeType_true() {
        byte[] p = offer();
        p[OPTIONS] = 0;
        p[OPTIONS + 1] = 53;
        p[OPTIONS + 2] = 1;
        p[OPTIONS + 3] = 2;
        assertTrue(DhcpDiscoverProbe.isOffer(p, 300));
    }
    @Test void isOffer_endWithoutType_false() {
        byte[] p = offer();
        p[OPTIONS] = (byte) 255;
        assertFalse(DhcpDiscoverProbe.isOffer(p, 300));
    }
    @Test void isOffer_otherOptionSkipped_true() {
        byte[] p = offer();
        p[OPTIONS] = 51; p[OPTIONS + 1] = 1; p[OPTIONS + 2] = 9;
        p[OPTIONS + 3] = 53; p[OPTIONS + 4] = 1; p[OPTIONS + 5] = 2;
        assertTrue(DhcpDiscoverProbe.isOffer(p, 300));
    }

    @Test void readOfferSenders_offer_returnsSender() throws Exception {
        assertEquals(Set.of("127.0.0.1"), sendAndRead(offer(), 500));
    }

    @Test void readOfferSenders_nonOffer_ignored() throws Exception {
        assertTrue(sendAndRead(DhcpDiscoverProbe.buildDiscover(), 300).isEmpty());
    }

    @Test void readOfferSenders_timeout_returnsEmpty() throws Exception {
        try (DatagramSocket s = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            assertTrue(DhcpDiscoverProbe.readOfferSenders(s, 100).isEmpty());
        }
    }

    @Test @Tag("slow") void collectServers_doesNotThrow() {
        assertNotNull(DhcpDiscoverProbe.collectServers(200));
    }
}

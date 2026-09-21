package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UdpSnmpTransportTest {

    @Test void exchange_returnsServerReply() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            Thread echo = new Thread(() -> reply(server), "test-snmp-server");
            echo.setDaemon(true);
            echo.start();
            Optional<byte[]> result = new UdpSnmpTransport("127.0.0.1", server.getLocalPort())
                    .exchange("ping".getBytes());
            assertArrayEquals("pong".getBytes(), result.orElseThrow());
        }
    }

    @Test void exchange_silentServer_returnsEmptyAfterTimeout() throws Exception {
        try (DatagramSocket silent = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            assertTrue(new UdpSnmpTransport("127.0.0.1", silent.getLocalPort()).exchange(new byte[]{1}).isEmpty());
        }
    }

    @Test void defaultConstructor_usesSnmpPort() {
        assertEquals(161, UdpSnmpTransport.DEFAULT_PORT);
        assertNotNull(new UdpSnmpTransport("127.0.0.1"));
    }

    private static void reply(DatagramSocket server) {
        try {
            DatagramPacket in = new DatagramPacket(new byte[64], 64);
            server.receive(in);
            byte[] pong = "pong".getBytes();
            server.send(new DatagramPacket(pong, pong.length, in.getAddress(), in.getPort()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

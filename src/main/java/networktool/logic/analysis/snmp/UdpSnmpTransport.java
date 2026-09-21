package main.java.networktool.logic.analysis.snmp;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.TimeoutConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;

/** SNMP über UDP; ein Datagramm pro Anfrage, Timeout laut {@link TimeoutConfig}. */
public final class UdpSnmpTransport implements SnmpTransport {

    public static final int DEFAULT_PORT = 161;
    private static final int RESPONSE_BUFFER_BYTES = 4096;

    private final String host;
    private final int port;

    public UdpSnmpTransport(String host) {
        this(host, DEFAULT_PORT);
    }

    public UdpSnmpTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Optional<byte[]> exchange(byte[] request) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TimeoutConfig.SNMP_REQUEST_MS);
            socket.send(new DatagramPacket(request, request.length, InetAddress.getByName(host), port));
            DatagramPacket response = new DatagramPacket(new byte[RESPONSE_BUFFER_BYTES], RESPONSE_BUFFER_BYTES);
            socket.receive(response);
            return Optional.of(Arrays.copyOf(response.getData(), response.getLength()));
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[UdpSnmpTransport] " + host + ": " + e);
            return Optional.empty();
        }
    }
}

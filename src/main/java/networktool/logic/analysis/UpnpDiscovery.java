package main.java.networktool.logic.analysis;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * UPnP-Geräteerkennung via SSDP (Simple Service Discovery Protocol).
 * Sendet M-SEARCH an 239.255.255.250:1900 und wertet Antworten aus.
 *
 * Typische Geräte: Router, Smart-TVs, NAS, Drucker, Spielekonsolen.
 */
public final class UpnpDiscovery {

    private UpnpDiscovery() {}

    private static final String SSDP_MULTICAST = "239.255.255.250";
    private static final int    SSDP_PORT      = 1900;
    private static final int    TIMEOUT_MS     = 3000;

    public record Device(
            String ip,
            String server,
            String usn,
            String location,
            String st
    ) {
        /** Leitet OS-Hinweis aus dem Server-Header ab. */
        public String guessOs() {
            if (server == null) return null;
            String s = server.toLowerCase();
            if (s.contains("windows"))   return "Windows";
            if (s.contains("linux"))     return "Linux";
            if (s.contains("synology"))  return "NAS (Synology)";
            if (s.contains("qnap"))      return "NAS (QNAP)";
            if (s.contains("fritz"))     return "Router (FRITZ!Box)";
            if (s.contains("philips"))   return "Philips Hue";
            if (s.contains("samsung"))   return "Samsung-Gerät";
            if (s.contains("sony"))      return "Sony-Gerät";
            if (s.contains("roku"))      return "Roku";
            return null;
        }
    }

    /**
     * Discovert UPnP-Geräte im lokalen Netz.
     * Gibt leere Liste zurück wenn keine Geräte antworten.
     */
    public static List<Device> discover() {
        List<Device> devices = Collections.synchronizedList(new ArrayList<>());
        Set<String> seen = ConcurrentHashMap.newKeySet();

        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setTimeToLive(4);

            String msearch =
                    "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: ssdp:all\r\n\r\n";

            byte[] data = msearch.getBytes(StandardCharsets.UTF_8);
            InetAddress group = InetAddress.getByName(SSDP_MULTICAST);
            socket.send(new DatagramPacket(data, data.length, group, SSDP_PORT));

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    byte[] buf = new byte[2048];
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    socket.receive(pkt);

                    String ip  = pkt.getAddress().getHostAddress();
                    String msg = new String(buf, 0, pkt.getLength(), StandardCharsets.UTF_8);

                    if (seen.add(ip)) {
                        Device dev = parseResponse(ip, msg);
                        if (dev != null) devices.add(dev);
                    }
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        } catch (Exception ignored) {}

        return Collections.unmodifiableList(devices);
    }

    private static Device parseResponse(String ip, String msg) {
        String server   = extractHeader(msg, "SERVER");
        String usn      = extractHeader(msg, "USN");
        String location = extractHeader(msg, "LOCATION");
        String st       = extractHeader(msg, "ST");
        return new Device(ip, server, usn, location, st);
    }

    private static String extractHeader(String msg, String name) {
        for (String line : msg.split("\r\n")) {
            if (line.toUpperCase().startsWith(name + ":")) {
                return line.substring(name.length() + 1).trim();
            }
        }
        return null;
    }
}
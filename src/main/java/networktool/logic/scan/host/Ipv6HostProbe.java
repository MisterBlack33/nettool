package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.TimeoutConfig;
import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.model.ScanResult;

import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Erreichbarkeit und Detailermittlung eines einzelnen IPv6-Hosts. */
final class Ipv6HostProbe {

    private static final String UNKNOWN_OS = "Unbekannt";

    private Ipv6HostProbe() {}

    static boolean isAlive(String ip) {
        return HostAliveChecker.isAlive(ip);
    }

    static ScanResult describe(String ip) {
        String hostname = resolveHostname(ip);
        Map<Integer, String> ports = PortScanner.scanSimple(ip, 0);
        String os = Optional.ofNullable(OsDetector.detectFromHostname(hostname, ip)).orElse(UNKNOWN_OS);
        return new ScanResult(ip, hostname, ports, os);
    }

    private static String resolveHostname(String ip) {
        try {
            return CompletableFuture.supplyAsync(() -> lookup(ip))
                    .get(TimeoutConfig.DNS_LOOKUP_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ip;
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            DebugLogger.getInstance().log("FINE", "[Ipv6HostProbe] Hostname-Lookup fehlgeschlagen (" + ip + "): " + e);
            return ip;
        }
    }

    private static String lookup(String ip) {
        try {
            return InetAddress.getByName(ip).getCanonicalHostName();
        } catch (java.io.IOException e) {
            return ip;
        }
    }
}

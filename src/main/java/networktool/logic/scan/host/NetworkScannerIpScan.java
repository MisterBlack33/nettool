package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.TimeoutConfig;
import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.model.ScanResult;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/** Einzel-Host-Scanlogik für {@link NetworkScanner}. Package-private. */
final class NetworkScannerIpScan {

    private NetworkScannerIpScan() {}

    static void scanIp(String ip, List<ScanResult> results) {
        try {
            if (!InetAddress.getByName(ip).isReachable(TimeoutConfig.NETWORK_SCANNER_REACH_MS)) return;
            String hostname            = resolveHostname(ip);
            Map<Integer, String> ports = PortScanner.scanSimple(ip, 0);
            String os                  = OsDetector.detect(ip);
            results.add(new ScanResult(ip, hostname, ports, os));
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkScannerIpScan] Scan von " + ip + " fehlgeschlagen: " + e);
        }
    }

    static String resolveHostname(String ip) {
        String[] result = {ip};
        Thread t = new Thread(() -> {
            try {
                String name = InetAddress.getByName(ip).getCanonicalHostName();
                if (name != null && !name.equals(ip)) result[0] = name;
            } catch (Exception e) {
                DebugLogger.getInstance().log("FINE", "[NetworkScannerIpScan] Hostname-Lookup fehlgeschlagen (" + ip + "): " + e);
            }
        });
        t.setDaemon(true);
        t.start();
        try { t.join(800); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }
}
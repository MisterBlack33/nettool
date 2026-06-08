package main.java.networktool.logic.scan;

import main.java.networktool.filter.HostResultFilter;
import main.java.networktool.filter.HostResultPrinter;
import main.java.networktool.model.HostResult;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class NetworkInfo {

    private NetworkInfo() {}

    private static final int SCAN_TIMEOUT_SEC = 120;

    public static volatile boolean testMode = false;

    public static void showMinimalInfo() throws Exception {
        System.out.println("\n=== Minimale Netzwerkinfo ===");
        System.out.println("Gerätename: " + InetAddress.getLocalHost().getHostName());
        runScan(null, null);
    }

    public static void showFullInfo() throws Exception {
        System.out.println("\n=== Vollständige Netzwerkinfo ===");
        printAllInterfaces();
        runScan(null, null);
    }

    public static void scanWithFilter(String osFilter, String hostnameFilter) throws Exception {
        runScan(osFilter, hostnameFilter);
    }

    // ── private ───────────────────────────────────────────────────────────

    private static void printAllInterfaces() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            System.out.println("\nInterface: " + ni.getName()
                    + " | Up: " + ni.isUp() + " | Loopback: " + ni.isLoopback());
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements())
                System.out.println("  IP: " + addrs.nextElement().getHostAddress());
        }
    }

    private static void runScan(String osFilter, String hostnameFilter) throws Exception {
        if (testMode) {
            HostResultPrinter.print(List.of(),
                    HostResultFilter.buildLabel(osFilter, hostnameFilter));
            return;
        }

        // Immer /24-Präfixe — nie rohe CIDR-Expansion (verhindert 65k-Host-Scans)
        List<String> subnets = SubnetDetector.getAllSubnets();
        if (subnets.isEmpty()) {
            System.out.println("Kein gültiges IPv4-Subnetz gefunden.");
            return;
        }

        System.out.printf("Scanne %d /24-Subnetz(e)...%n", subnets.size());

        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NetworkInfo-Scan");
            t.setDaemon(true);
            return t;
        });

        Future<List<HostResult>> future = exec.submit(
                () -> NetworkHostScanner.scan(subnets));
        exec.shutdown();

        List<HostResult> found   = fetchWithTimeout(future);
        List<HostResult> filtered = HostResultFilter.filter(found, osFilter, hostnameFilter);
        HostResultPrinter.print(filtered, HostResultFilter.buildLabel(osFilter, hostnameFilter));
    }

    private static List<HostResult> fetchWithTimeout(Future<List<HostResult>> future) {
        try {
            return future.get(SCAN_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("  [NetworkInfo] Scan-Timeout nach " + SCAN_TIMEOUT_SEC + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("  [NetworkInfo] Scan-Fehler: " + e.getCause().getMessage());
        }
        return Collections.emptyList();
    }
}
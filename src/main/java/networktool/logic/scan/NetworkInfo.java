package networktool.logic.scan;

import networktool.filter.HostResultFilter;
import networktool.filter.HostResultPrinter;
import networktool.model.HostResult;

import java.net.*;
import java.util.*;

public final class NetworkInfo {

    private NetworkInfo() {}

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

        List<String> subnets = SubnetDetector.getAllSubnets();
        if (subnets.isEmpty()) {
            System.out.println("Kein gültiges IPv4-Subnetz gefunden.");
            return;
        }

        System.out.printf("Scanne %d /24-Subnetz(e)...%n", subnets.size());

        // Kein Timeout — scan läuft bis fertig oder Interrupt
        List<HostResult> found = NetworkHostScanner.scan(subnets);
        List<HostResult> filtered = HostResultFilter.filter(found, osFilter, hostnameFilter);
        HostResultPrinter.print(filtered, HostResultFilter.buildLabel(osFilter, hostnameFilter));
    }
}
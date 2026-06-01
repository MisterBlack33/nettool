package main.java.networktool.logic.scan;

import main.java.networktool.filter.HostResultFilter;
import main.java.networktool.filter.HostResultPrinter;
import main.java.networktool.model.HostResult;

import java.net.*;
import java.util.*;

/**
 * Zeigt Informationen über das lokale Netzwerk an und startet Host-Scans.
 *
 * Drei Einstiegspunkte:
 * - {@link #showMinimalInfo()} – Gerätename + Scan aller Hosts
 * - {@link #showFullInfo()}    – vollständige Interface-Infos + Scan
 * - {@link #scanWithFilter()}  – Scan mit OS- und Hostname-Filter
 */
public final class NetworkInfo {

    private NetworkInfo() {}

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

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private static void printAllInterfaces() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            System.out.println("\nInterface: " + ni.getName()
                    + " | Up: " + ni.isUp()
                    + " | Loopback: " + ni.isLoopback());
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                System.out.println("  IP: " + addrs.nextElement().getHostAddress());
            }
        }
    }

    private static void runScan(String osFilter, String hostnameFilter) throws Exception {
        List<String> subnets = SubnetDetector.getAllSubnets();
        if (subnets.isEmpty()) {
            System.out.println("Kein gültiges IPv4-Subnetz gefunden.");
            return;
        }
        System.out.println("Subnetze: " + subnets.size());

        List<HostResult> found    = NetworkHostScanner.scan(subnets);
        List<HostResult> filtered = HostResultFilter.filter(found, osFilter, hostnameFilter);
        String label              = HostResultFilter.buildLabel(osFilter, hostnameFilter);
        HostResultPrinter.print(filtered, label);
    }
}

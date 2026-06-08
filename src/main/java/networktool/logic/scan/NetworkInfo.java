package main.java.networktool.logic.scan;

import main.java.networktool.filter.HostResultFilter;
import main.java.networktool.filter.HostResultPrinter;
import main.java.networktool.model.HostResult;

import java.net.*;
import java.util.*;

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
        List<String> cidrs = SubnetDetector.getAllCidrs();
        if (cidrs.isEmpty()) {
            System.out.println("Kein gültiges IPv4-Subnetz gefunden.");
            return;
        }
        System.out.println("Netze: " + cidrs);

        List<HostResult> found = new ArrayList<>();
        for (String cidr : cidrs)
            found.addAll(NetworkHostScanner.scanCidr(cidr));

        List<HostResult> filtered = HostResultFilter.filter(found, osFilter, hostnameFilter);
        HostResultPrinter.print(filtered, HostResultFilter.buildLabel(osFilter, hostnameFilter));
    }
}
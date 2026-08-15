package main.java.networktool.logic.sonify;

import main.java.networktool.logic.windows.PsInterfaceStatsResolver;
import main.java.networktool.util.PlatformUtils;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class ActiveInterfaceDetector {

    private static final List<String> SKIP_PREFIXES =
            List.of("lo", "docker", "br-", "veth", "virbr", "tun", "tap", "wg", "utun");

    private ActiveInterfaceDetector() {}

    public static String detect() {
        if (PlatformUtils.isWindows()) return detectWindows();
        return detectUnix();
    }

    private static String detectWindows() {
        for (String name : PsInterfaceStatsResolver.listActiveAdapters()) {
            if (PsInterfaceStatsResolver.read(name) != null) return name;
        }
        return "Ethernet"; // Fallback-Adaptername unter Windows
    }

    private static String detectUnix() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(ifaces)) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String name = ni.getName().toLowerCase();
                if (SKIP_PREFIXES.stream().anyMatch(name::startsWith)) continue;
                if (InterfaceStatsReader.read(ni.getName()) != null) return ni.getName();
            }
        } catch (Exception ignored) {}
        return "eth0";
    }
}
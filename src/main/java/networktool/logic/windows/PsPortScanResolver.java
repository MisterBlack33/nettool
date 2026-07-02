package main.java.networktool.logic.windows;

import java.util.*;

/** Port-Check via PowerShell Test-NetConnection. Windows-only, sonst false. */
public final class PsPortScanResolver {

    private PsPortScanResolver() {}

    public static boolean isOpen(String ip, int port) {
        if (!PowerShellRunner.isAvailable() || ip == null || ip.isBlank()) return false;
        String script = "(Test-NetConnection -ComputerName '" + ip
                + "' -Port " + port + " -WarningAction SilentlyContinue).TcpTestSucceeded";
        return PowerShellRunner.run(script, 3_000).stream()
                .anyMatch(l -> l.trim().equalsIgnoreCase("True"));
    }

    public static Map<Integer, Boolean> scanPorts(String ip, List<Integer> ports) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        for (int port : ports) result.put(port, isOpen(ip, port));
        return result;
    }
}
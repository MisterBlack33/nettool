package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.windows.PowerShellRunner;
import main.java.networktool.util.Ipv6AddressUtils;
import main.java.networktool.util.PlatformSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Liest IPv6-Nachbarn aus dem Neighbor-Cache des Systems. Link-Local-Adressen
 * bleiben außen vor: ohne Zonen-ID sind sie nicht ansprechbar.
 */
final class Ipv6NeighborSource {

    private static final String WINDOWS_SCRIPT =
            "Get-NetNeighbor -AddressFamily IPv6 | "
                    + "Where-Object { $_.State -ne 'Unreachable' -and $_.State -ne 'Incomplete' } | "
                    + "Select-Object -ExpandProperty IPAddress";
    private static final String STATE_FAILED = "FAILED";
    private static final long COMMAND_TIMEOUT_SEC = 3;

    private Ipv6NeighborSource() {}

    static Set<String> read() {
        return parse(readLines());
    }

    static Set<String> parse(List<String> lines) {
        Set<String> neighbors = new LinkedHashSet<>();
        for (String line : lines) {
            String[] tokens = line.trim().split("\\s+");
            String candidate = tokens[0];
            if (line.contains(STATE_FAILED) || !Ipv6AddressUtils.isValidIpv6(candidate)
                    || Ipv6AddressUtils.isLinkLocal(candidate)) continue;
            neighbors.add(Ipv6HostRange.canonical(candidate));
        }
        return neighbors;
    }

    private static List<String> readLines() {
        if (PlatformSupport.isWindows()) return PowerShellRunner.run(WINDOWS_SCRIPT);
        return PlatformSupport.isMac() ? runCommand("ndp", "-an") : runCommand("ip", "-6", "neigh", "show");
    }

    private static List<String> runCommand(String... command) {
        List<String> lines = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) lines.add(line);
            }
            process.waitFor(COMMAND_TIMEOUT_SEC, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[Ipv6NeighborSource] " + command[0] + ": " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return lines;
    }
}

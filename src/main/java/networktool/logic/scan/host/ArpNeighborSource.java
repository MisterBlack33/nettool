package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.windows.PsArpResolver;
import main.java.networktool.util.PlatformSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liest die Nachbartabelle des Betriebssystems, ohne selbst Pakete zu senden.
 * Linux nutzt /proc/net/arp, Windows PowerShell, sonst {@code arp -a -n}.
 */
final class ArpNeighborSource {

    private static final Path PROC_NET_ARP = Path.of("/proc/net/arp");
    private static final String INCOMPLETE_FLAGS = "0x0";
    private static final String EMPTY_MAC = "00:00:00:00:00:00";
    private static final int PROC_MIN_COLUMNS = 4;
    private static final int PROC_COL_IP = 0;
    private static final int PROC_COL_FLAGS = 2;
    private static final int PROC_COL_MAC = 3;

    private ArpNeighborSource() {}

    static Map<String, String> read() {
        if (PlatformSupport.isWindows()) return PsArpResolver.table();
        if (Files.isReadable(PROC_NET_ARP)) return readProc();
        return readArpCommand();
    }

    static Map<String, String> parseProcNetArp(List<String> lines) {
        Map<String, String> neighbors = new LinkedHashMap<>();
        for (String line : lines.stream().skip(1).toList()) {
            String[] cols = line.trim().split("\\s+");
            if (cols.length < PROC_MIN_COLUMNS || INCOMPLETE_FLAGS.equals(cols[PROC_COL_FLAGS])) continue;
            String mac = cols[PROC_COL_MAC].toUpperCase();
            if (!EMPTY_MAC.equals(mac)) neighbors.put(cols[PROC_COL_IP], mac);
        }
        return neighbors;
    }

    private static Map<String, String> readProc() {
        try {
            return parseProcNetArp(Files.readAllLines(PROC_NET_ARP));
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[ArpNeighborSource] /proc/net/arp nicht lesbar: " + e);
            return Map.of();
        }
    }

    private static Map<String, String> readArpCommand() {
        Map<String, String> neighbors = new LinkedHashMap<>();
        try {
            Process process = new ProcessBuilder("arp", "-a", "-n").redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] entry = NetworkHostArpResolver.parseArpLine(line);
                    if (entry != null) neighbors.put(entry[0], entry[1]);
                }
            }
            process.destroy();
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[ArpNeighborSource] arp nicht ausführbar: " + e);
        }
        return neighbors;
    }
}

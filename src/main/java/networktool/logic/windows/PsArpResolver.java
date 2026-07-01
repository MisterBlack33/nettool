package main.java.networktool.logic.windows;

import java.util.*;
import java.util.regex.*;

/** ARP via "Get-NetNeighbor" – zuverlässiger als "arp -a" (einheitliches CSV, kein Locale-Parsing). */
public final class PsArpResolver {

    private static final long CACHE_TTL_MS = 10_000;
    private static final Pattern LINE = Pattern.compile(
            "^\"?(\\d{1,3}(?:\\.\\d{1,3}){3})\"?,\"?([0-9A-Fa-f-]{17})\"?,\"?(\\w+)\"?");

    private static volatile Map<String, String> cache = Map.of();
    private static volatile long cacheTime = 0;

    private PsArpResolver() {}

    public static String lookup(String ip) { refreshIfStale(); return cache.get(ip); }

    public static Map<String, String> table() { refreshIfStale(); return cache; }

    private static synchronized void refreshIfStale() {
        if (System.currentTimeMillis() - cacheTime < CACHE_TTL_MS) return;
        cache = fetch();
        cacheTime = System.currentTimeMillis();
    }

    private static Map<String, String> fetch() {
        Map<String, String> result = new LinkedHashMap<>();
        String script =
                "Get-NetNeighbor -AddressFamily IPv4 | " +
                        "Where-Object { $_.State -ne 'Unreachable' -and $_.State -ne 'Incomplete' } | " +
                        "Select-Object IPAddress,LinkLayerAddress,State | ConvertTo-Csv -NoTypeInformation";
        for (String line : PowerShellRunner.run(script)) {
            Matcher m = LINE.matcher(line.trim());
            if (!m.find()) continue;
            String mac = m.group(2).toUpperCase().replace('-', ':');
            if (mac.startsWith("00:00:00:00:00:00") || mac.startsWith("FF:FF")) continue;
            result.put(m.group(1), mac);
        }
        return result;
    }
}
package main.java.networktool_v3.logic.analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Führt einen Traceroute-Prozess aus und parst die Ausgabe.
 *
 * Unterstützt Windows (tracert) und Linux/macOS (traceroute).
 * Gibt eine Liste von {@link HopInfo}-Objekten zurück.
 */
public final class TracerouteRunner {

    private TracerouteRunner() {}

    private static final Pattern MS_PATTERN =
            Pattern.compile("(?:<\\s*1|[0-9]+(?:[.,][0-9]+)?)\\s*ms", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");

    /**
     * @param ip      Ziel-IP
     * @param maxHops 0 = kein Limit (255 Hops, praktisch bis Ziel)
     * @return geparste Hop-Liste
     */
    public static List<HopInfo> run(String ip, int maxHops) throws Exception {
        boolean win   = System.getProperty("os.name", "").toLowerCase().contains("win");
        String  limit = maxHops > 0 ? String.valueOf(maxHops) : "255";
        String[] cmd  = win
                ? new String[]{"tracert", "-h", limit, "-w", "500", ip}
                : new String[]{"traceroute", "-m", limit, "-w", "2", ip};

        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        List<HopInfo> hops = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            HopInfo hop = parseLine(line.trim(), win);
            if (hop != null) hops.add(hop);
        }
        process.destroy();
        return hops;
    }

    // ── Zeilen-Parser ─────────────────────────────────────────────────────

    static HopInfo parseLine(String line, boolean win) {
        if (line.isEmpty()) return null;
        Matcher num = Pattern.compile("^\\s*(\\d{1,3})\\b").matcher(line);
        if (!num.find()) return null;

        HopInfo hop = new HopInfo(Integer.parseInt(num.group(1)));

        if (line.contains("* * *") || (line.contains("*") && !MS_PATTERN.matcher(line).find())) {
            hop.timeout = true; return hop;
        }

        Matcher ipM = IP_PATTERN.matcher(line);
        if (ipM.find()) hop.ip = ipM.group(1);

        if (!win && hop.ip != null) {
            int idx = line.indexOf("(" + hop.ip + ")");
            if (idx > 0) {
                String before = line.substring(num.end(), idx).trim();
                if (!before.isEmpty() && !before.equals(hop.ip)) hop.hostname = before;
            }
        }
        if (hop.hostname == null || hop.hostname.isEmpty())
            hop.hostname = hop.ip != null ? hop.ip : "";

        Matcher ms = MS_PATTERN.matcher(line);
        while (ms.find()) {
            String raw = ms.group().toLowerCase()
                    .replace("ms","").replace(",",".").replace("<","").trim();
            try { hop.msValues.add(Math.round(Double.parseDouble(raw)));; }
            catch (NumberFormatException ignored) {}
        }
        return hop;
    }

    // ── Datenklasse ───────────────────────────────────────────────────────

    /** Enthält die Daten eines einzelnen Traceroute-Hops. */
    public static class HopInfo {
        public final int        number;
        public boolean          timeout  = false;
        public String           ip       = "";
        public String           hostname = "";
        public final List<Long> msValues = new ArrayList<>();

        public HopInfo(int number) { this.number = number; }

        public String latencyFormatted() {
            if (msValues.isEmpty()) return "–";
            if (msValues.size() == 1) return msValues.get(0) + " ms";
            long min = java.util.Collections.min(msValues);
            long max = java.util.Collections.max(msValues);
            long avg = msValues.stream().mapToLong(Long::longValue).sum() / msValues.size();
            return min + " / " + avg + " / " + max + " ms";
        }
    }
}

package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.filter.TablePrinter;

import java.util.*;
import java.util.concurrent.*;

public final class RemoteNetScanner {

    private RemoteNetScanner() {}

    private static final int BATCH_SIZE = 12;

    // ── Public API ────────────────────────────────────────────────────────

    public static String normalizeCidr(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.contains("/")) return input;
        String[] parts = input.split("\\.");
        return switch (parts.length) {
            case 2 -> parts[0] + "." + parts[1] + ".0.0/16";
            case 3 -> parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
            case 4 -> input + "/24";
            default -> input;
        };
    }

    public static List<ScanResult> scanCidr(String rawInput) {
        String cidr = normalizeCidr(rawInput);
        printBox("Fremdnetz-Scan: " + cidr);

        String gw = RemoteNetGateway.detectDefaultGateway();
        System.out.println("  Gateway (auto): " + (gw != null ? gw : "nicht erkannt"));

        ReachResult reach = RemoteNetProbe.parallelProbe(cidr);
        if (!reach.reachable) {
            System.out.println("  ✕ Netz nicht erreichbar.");
            if (gw != null) System.out.println("  Tipp: route add " + cidr + " gw " + gw);
            RemoteNetGateway.printRoutingHints(cidr);
            return Collections.emptyList();
        }
        System.out.printf("  ✔ Erreichbar  (Probes: %d/%d  ~%d ms)%n",
                reach.respondedProbes, RemoteNetProbe.PROBE_COUNT, reach.avgMs);

        List<ScanResult> results = NetworkScanner.scanCIDR(cidr);
        TablePrinter.print(results);
        return results;
    }

    public static List<HostResult> scanMultiple(List<String> rawInputs) {
        List<String> cidrs = rawInputs.stream()
                .map(RemoteNetScanner::normalizeCidr)
                .filter(Objects::nonNull)
                .toList();

        printBox("Multi-Netz-Scan: " + cidrs.size() + " Netzwerk(e)");
        String gw = RemoteNetGateway.detectDefaultGateway();
        System.out.println("  Gateway (auto): " + (gw != null ? gw : "nicht erkannt") + "\n");

        System.out.println("  ▶ Erreichbarkeitstest (parallel)...");
        Map<String, ReachResult> reachMap = RemoteNetProbe.parallelProbeAll(cidrs);
        reachMap.forEach((cidr, r) ->
                System.out.printf("  %s  %-28s  %s%n",
                        r.reachable ? "✔" : "✕", cidr,
                        r.reachable ? "~" + r.avgMs + " ms (" + r.respondedProbes + "/" + RemoteNetProbe.PROBE_COUNT + ")"
                                : "nicht erreichbar"));
        System.out.println();

        if (reachMap.values().stream().noneMatch(r -> r.reachable)) {
            System.out.println("  ✕ Kein Netz erreichbar.");
            cidrs.forEach(RemoteNetGateway::printRoutingHints);
            return Collections.emptyList();
        }

        List<HostResult> allHosts = Collections.synchronizedList(new ArrayList<>());
        for (String cidr : cidrs) {
            if (Thread.currentThread().isInterrupted()) break;
            ReachResult r = reachMap.get(cidr);
            if (r == null || !r.reachable) {
                System.out.println("  ↷ Übersprungen: " + cidr);
                continue;
            }
            List<String> subnets = cidrToSubnetPrefixes(cidr);
            System.out.println("\n── " + cidr + " → " + subnets.size() + " /24-Subnetz(e)");
            for (int i = 0; i < subnets.size(); i += BATCH_SIZE) {
                if (Thread.currentThread().isInterrupted()) break;
                List<String> batch = subnets.subList(i, Math.min(i + BATCH_SIZE, subnets.size()));
                allHosts.addAll(NetworkHostScanner.scan(batch));
            }
        }

        List<HostResult> result = new ArrayList<>(allHosts);
        result.sort(Comparator.comparingInt(h -> ipToInt(h.ip)));
        System.out.printf("%n  Gesamt: %d Host(s) in %d Netz(en)%n", result.size(), cidrs.size());
        return result;
    }

    public static ReachResult parallelProbe(String cidr) {
        return RemoteNetProbe.parallelProbe(cidr);
    }

    public static String detectDefaultGateway() {
        return RemoteNetGateway.detectDefaultGateway();
    }

    public static void printRoutingHints(String cidr) {
        RemoteNetGateway.printRoutingHints(cidr);
    }

    // ── Package-private ───────────────────────────────────────────────────

    static List<String> cidrToSubnetPrefixes(String cidr) {
        List<String> prefixes = new ArrayList<>();
        try {
            String[] parts  = cidr.split("/");
            int      prefix = Integer.parseInt(parts[1]);
            String[] oct    = parts[0].split("\\.");
            if (oct.length < 3) return prefixes;

            if (prefix >= 24) {
                prefixes.add(oct[0] + "." + oct[1] + "." + oct[2]);
            } else if (prefix >= 16) {
                int start = Integer.parseInt(oct[2]);
                int count = 1 << (24 - prefix);
                for (int i = 0; i < count; i++)
                    prefixes.add(oct[0] + "." + oct[1] + "." + (start + i));
            } else if (prefix >= 8) {
                int second = Integer.parseInt(oct[1]);
                int count  = Math.min(256, 1 << (16 - prefix));
                for (int i = 0; i < count; i++)
                    prefixes.add(oct[0] + "." + (second + i / 256) + "." + (i % 256));
            }
        } catch (Exception ignored) {}
        return prefixes;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int ipToInt(String ip) {
        if (ip == null) return 0;
        String[] p = ip.split("\\.");
        int r = 0;
        for (String s : p) {
            try { r = (r << 8) | Integer.parseInt(s.trim()); } catch (Exception ignored) {}
        }
        return r;
    }

    private static void printBox(String title) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.printf( "║  %-48s║%n", title);
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    // ── Result type ───────────────────────────────────────────────────────

    public static final class ReachResult {
        public final boolean reachable;
        public final int     respondedProbes;
        public final long    avgMs;

        public ReachResult(boolean reachable, int respondedProbes, long avgMs) {
            this.reachable       = reachable;
            this.respondedProbes = respondedProbes;
            this.avgMs           = avgMs;
        }
    }
}
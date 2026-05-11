package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.filter.HostResultPrinter;
import main.java.networktool_v3.gui.GUI;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.filter.TablePrinter;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.util.CIDRUtils;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scannt Fremdnetzwerke, die über Routing erreichbar sind.
 *
 * Verbesserungen v3:
 *  - normalizeCidr()      : "10.16.5" → "10.16.5.0/24" automatisch
 *  - Gateway-Hint         : Auto-Erkennung + Hinweis bei unerreichbarem Netz
 *  - parallelProbe()      : Erreichbarkeitstest läuft parallel (schneller)
 *  - scanCidr()           : zeigt Gateway-Tipp wenn Netz nicht erreichbar
 *  - scanMultiple()       : parallele Erreichbarkeitstests, sortierte Ausgabe
 *  - autoSaveResults()    : nach Scan Ergebnisse in Kategorie speicherbar
 *  - cidrToSubnetPrefixes: korrekte /8–/24-Berechnung
 */
public final class RemoteNetScanner {

    private RemoteNetScanner() {}

    private static final int PROBE_COUNT   = 8;
    private static final int REACH_TIMEOUT = 1200;
    private static final int BATCH_SIZE    = 12;

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Normalisiert eine Nutzereingabe zu einem gültigen CIDR-String.
     *   "10.16.5"         → "10.16.5.0/24"
     *   "10.16.0.0"       → "10.16.0.0/24"
     *   "10.16.0.0/16"    → "10.16.0.0/16"  (unverändert)
     *   "10.16"           → "10.16.0.0/16"
     */
    public static String normalizeCidr(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.contains("/")) return input;                       // bereits CIDR

        String[] parts = input.split("\\.");
        return switch (parts.length) {
            case 2  -> parts[0] + "." + parts[1] + ".0.0/16";
            case 3  -> parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
            case 4  -> input + "/24";
            default -> input;
        };
    }

    /**
     * Vollständiger CIDR-Scan eines Fremdnetzwerks.
     * Unterstützt Kurznotation: "10.16.5" wird zu "10.16.5.0/24".
     */
    public static List<ScanResult> scanCidr(String rawInput) {
        String cidr = normalizeCidr(rawInput);

        printBox("Fremdnetz-Scan: " + cidr);

        // Gateway anzeigen
        String gw = detectDefaultGateway();
        System.out.println("  Gateway (auto): " + (gw != null ? gw : "nicht erkannt"));

        // Erreichbarkeitstest
        ReachResult reach = parallelProbe(cidr);
        if (!reach.reachable) {
            System.out.println("  ✕ Netz nicht erreichbar.");
            if (gw != null)
                System.out.println("  Tipp: Route prüfen →  route add " + cidr + " gw " + gw);
            printRoutingHints(cidr);
            return Collections.emptyList();
        }
        System.out.printf("  ✔ Erreichbar  (Probes: %d/%d  ~%d ms)%n",
                reach.respondedProbes, PROBE_COUNT, reach.avgMs);

        List<ScanResult> results = NetworkScanner.scanCIDR(cidr);
        TablePrinter.print(results);

        if (!results.isEmpty()) {
            autoSavePrompt(results);
        }
        return results;
    }

    /**
     * /24-Subnetz-Scan (schneller als CIDR für große Netze).
     * Eingabe "10.16.5" wird akzeptiert.
     */
    public static List<HostResult> scanSubnet(String subnet) {
        subnet = subnet.trim().replaceAll("\\.0$", ""); // "10.16.5.0" → "10.16.5"

        printBox("Fremdnetz-Subnetz: " + subnet + ".0/24");

        if (!isSubnetReachable(subnet)) {
            System.out.println("  ✕ Subnetz nicht erreichbar.");
            printRoutingHints(subnet + ".0/24");
            return Collections.emptyList();
        }

        List<HostResult> found = NetworkHostScanner.scan(List.of(subnet));
        HostResultPrinter.print(found, "Fremdnetz " + subnet + ".x");
        return found;
    }

    /**
     * Scannt mehrere CIDRs/Subnetze.
     * Erreichbarkeitstests laufen parallel.
     * Eingaben werden automatisch normalisiert.
     */
    public static List<HostResult> scanMultiple(List<String> rawInputs) {
        List<String> cidrs = rawInputs.stream()
                .map(RemoteNetScanner::normalizeCidr)
                .filter(Objects::nonNull)
                .toList();

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.printf( "║  Multi-Netz-Scan: %-30s ║%n", cidrs.size() + " Netzwerk(e)");
        System.out.println("╚══════════════════════════════════════════════════╝");

        String gw = detectDefaultGateway();
        System.out.println("  Gateway (auto): " + (gw != null ? gw : "nicht erkannt") + "\n");

        // Parallele Erreichbarkeitstests
        System.out.println("  ▶ Erreichbarkeitstest (parallel)...");
        Map<String, ReachResult> reachMap = parallelProbeAll(cidrs);
        reachMap.forEach((cidr, r) ->
                System.out.printf("  %s  %-28s  %s%n",
                        r.reachable ? "✔" : "✕", cidr,
                        r.reachable ? "~" + r.avgMs + " ms  (" + r.respondedProbes + "/" + PROBE_COUNT + ")"
                                : "nicht erreichbar"));
        System.out.println();

        long reachableCount = reachMap.values().stream().filter(r -> r.reachable).count();
        if (reachableCount == 0) {
            System.out.println("  ✕ Kein Netz erreichbar. Routing-Hinweise:");
            cidrs.forEach(RemoteNetScanner::printRoutingHints);
            return Collections.emptyList();
        }

        List<HostResult> allHosts = Collections.synchronizedList(new ArrayList<>());

        for (String cidr : cidrs) {
            if (Thread.currentThread().isInterrupted()) break;
            ReachResult r = reachMap.get(cidr);
            if (r == null || !r.reachable) {
                System.out.println("  ↷ Übersprungen (nicht erreichbar): " + cidr);
                if (gw != null)
                    System.out.println("    Tipp: route add " + cidr + " gw " + gw);
                continue;
            }

            List<String> subnets = cidrToSubnetPrefixes(cidr);
            System.out.println("\n── " + cidr + " → " + subnets.size()
                    + " /24-Subnetz(e)  [Batches à " + BATCH_SIZE + "]");

            for (int i = 0; i < subnets.size(); i += BATCH_SIZE) {
                if (Thread.currentThread().isInterrupted()) break;
                List<String> batch = subnets.subList(i, Math.min(i + BATCH_SIZE, subnets.size()));
                List<HostResult> found = NetworkHostScanner.scan(batch);
                allHosts.addAll(found);
                if (!found.isEmpty())
                    System.out.printf("  Batch %d/%d: %d Host(s) gefunden%n",
                            i / BATCH_SIZE + 1,
                            (subnets.size() - 1) / BATCH_SIZE + 1,
                            found.size());
            }
        }

        List<HostResult> result = new ArrayList<>(allHosts);
        result.sort(Comparator.comparingInt(h -> ipToInt(h.ip)));

        System.out.println("\n══════════════════════════════════════════════════");
        System.out.printf( "  Gesamt: %d Host(s) in %d Netzwerk(en)%n",
                result.size(), cidrs.size());
        System.out.println("══════════════════════════════════════════════════");
        HostResultPrinter.print(result, "Multi-Netz (" + cidrs.size() + " Netze)");
        return result;
    }

    /**
     * Schneller paralleler Erreichbarkeitstest für ein einzelnes CIDR.
     * Testet PROBE_COUNT gleichmäßig verteilte IPs gleichzeitig.
     */
    public static ReachResult parallelProbe(String cidr) {
        List<String> allIps;
        try { allIps = CIDRUtils.getAllIPs(normalizeCidr(cidr)); }
        catch (Exception e) { return new ReachResult(false, 0, 0); }
        if (allIps.isEmpty()) return new ReachResult(false, 0, 0);

        int step = Math.max(1, allIps.size() / PROBE_COUNT);
        List<String> probes = new ArrayList<>();
        for (int i = 0; i < PROBE_COUNT && i * step < allIps.size(); i++)
            probes.add(allIps.get(i * step));
        // Immer .1 testen (Gateway)
        String base = allIps.get(0);
        String dot1 = base.substring(0, base.lastIndexOf('.') + 1) + "1";
        if (!probes.contains(dot1)) probes.set(0, dot1);

        ExecutorService exec = Executors.newFixedThreadPool(probes.size());
        List<Future<Long>> futures = new ArrayList<>();
        for (String ip : probes) {
            futures.add(exec.submit(() -> {
                long t = System.currentTimeMillis();
                try {
                    return InetAddress.getByName(ip).isReachable(REACH_TIMEOUT)
                            ? System.currentTimeMillis() - t : -1L;
                } catch (Exception e) { return -1L; }
            }));
        }
        exec.shutdown();
        try { exec.awaitTermination(REACH_TIMEOUT + 500L, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        int responded = 0; long totalMs = 0;
        for (Future<Long> f : futures) {
            try {
                long ms = f.get(100, TimeUnit.MILLISECONDS);
                if (ms >= 0) { responded++; totalMs += ms; }
            } catch (Exception ignored) {}
        }

        return new ReachResult(responded > 0, responded,
                responded > 0 ? totalMs / responded : 0);
    }

    /** Testet alle CIDRs parallel → Map cidr→ReachResult. */
    private static Map<String, ReachResult> parallelProbeAll(List<String> cidrs) {
        Map<String, ReachResult> result = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(Math.min(cidrs.size(), 8));
        List<Future<?>> futures = new ArrayList<>(cidrs.stream()
                .map(cidr -> (Future<?>) exec.submit(() -> result.put(cidr, parallelProbe(cidr))))
                .toList());
        exec.shutdown();
        try { exec.awaitTermination(REACH_TIMEOUT * 2L + 1000, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // Fehlende Einträge mit "nicht erreichbar" befüllen
        cidrs.forEach(c -> result.putIfAbsent(c, new ReachResult(false, 0, 0)));
        return result;
    }

    // ── Gateway-Erkennung ─────────────────────────────────────────────────

    /** Ermittelt das Standard-Gateway via route-Tabelle. */
    public static String detectDefaultGateway() {
        try {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            ProcessBuilder pb = win
                    ? new ProcessBuilder("cmd", "/c", "route", "print", "0.0.0.0")
                    : new ProcessBuilder("sh", "-c",
                    "ip route show default 2>/dev/null || netstat -rn 2>/dev/null");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (win) {
                        if (line.trim().startsWith("0.0.0.0")) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 3) return parts[2];
                        }
                    } else {
                        if (line.startsWith("default via ")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 3) return parts[2];
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String detectGatewayForNet(String targetCidr) {
        String defGw = detectDefaultGateway();
        if (defGw != null) {
            try { if (InetAddress.getByName(defGw).isReachable(1000)) return defGw; }
            catch (Exception ignored) {}
        }
        return null;
    }

    // ── Routing-Hinweise ─────────────────────────────────────────────────

    /** Gibt plattformspezifische Routing-Hinweise aus. */
    public static void printRoutingHints(String cidr) {
        cidr = normalizeCidr(cidr);
        String[] parts  = cidr.split("/");
        String network  = parts[0];
        String prefix   = parts.length > 1 ? parts[1] : "24";
        String netmask  = prefixToNetmask(Integer.parseInt(prefix));
        boolean isWin   = System.getProperty("os.name", "").toLowerCase().contains("win");
        String gw       = detectDefaultGateway();
        String gwStr    = gw != null ? gw : "<GATEWAY-IP>";

        System.out.println("\n  ── Routing einrichten ───────────────────────────");
        System.out.println("  Ziel:    " + cidr);
        System.out.println("  Gateway: " + gwStr + (gw != null ? "  (automatisch erkannt)" : "  (manuell setzen)"));
        System.out.println();
        if (isWin) {
            System.out.println("  [Windows] Einmalig:");
            System.out.println("    route add " + network + " mask " + netmask + " " + gwStr);
            System.out.println("  [Windows] Dauerhaft:");
            System.out.println("    route -p add " + network + " mask " + netmask + " " + gwStr);
            System.out.println("  [Windows] Entfernen:");
            System.out.println("    route delete " + network);
        } else {
            System.out.println("  [Linux] Einmalig:");
            System.out.println("    ip route add " + cidr + " via " + gwStr);
            System.out.println("  [Linux] Entfernen:");
            System.out.println("    ip route del " + cidr);
            System.out.println("  [macOS] Einmalig:");
            System.out.println("    sudo route -n add -net " + network + "/" + prefix + " " + gwStr);
        }
        System.out.println("\n  ── Firewall-Hinweise ────────────────────────────");
        System.out.println("  → ICMP muss im Zielnetz erlaubt sein");
        System.out.println("  → TCP-Port-Scan via CIDR-Scan falls kein ICMP");
        System.out.println("  → VPN/Tunnel falls kein direktes Routing möglich");
        System.out.println("  ────────────────────────────────────────────────");
    }

    // ── Auto-Save nach Scan ───────────────────────────────────────────────

    /**
     * Fragt im GUI-Modus ob Scan-Ergebnisse gespeichert werden sollen.
     * Im CLI-Modus: keine Aktion (der CLI-Handler fragt selbst).
     */
    private static void autoSavePrompt(List<ScanResult> results) {
        if (!GUI.isGuiActive()) return;
        List<String> networks = NetworkStore.getInstance().getNetworkNames()
                .stream().filter(n -> !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (networks.isEmpty()) return;

        javax.swing.SwingUtilities.invokeLater(() -> {
            Object chosen = javax.swing.JOptionPane.showInputDialog(null,
                    "<html><b>" + results.size() + " Host(s) gefunden.</b><br>"
                            + "In welches Netzwerk speichern?<br>"
                            + "<small>(Abbrechen = nicht speichern)</small></html>",
                    "Scan-Ergebnisse speichern",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null, networks.toArray(), networks.get(0));
            if (chosen == null) return;
            String cat = chosen.toString();
            int saved = 0;
            for (ScanResult r : results) {
                HostResult h = new HostResult(r.getIp(), r.getHostname(),
                        r.getOsGuess(), null, r.getOpenPorts(), "");
                if (NetworkStore.getInstance().save(h, cat)) saved++;
            }
            System.out.println("  ★ " + saved + " Host(s) gespeichert in \"" + cat + "\"");
        });
    }

    // ── Ergebnis-Klasse ───────────────────────────────────────────────────

    public static class ReachResult {
        public final boolean reachable;
        public final int     respondedProbes;
        public final long    avgMs;

        public ReachResult(boolean reachable, int respondedProbes, long avgMs) {
            this.reachable       = reachable;
            this.respondedProbes = respondedProbes;
            this.avgMs           = avgMs;
        }
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

    private static boolean isSubnetReachable(String subnetPrefix) {
        try { return InetAddress.getByName(subnetPrefix + ".1").isReachable(REACH_TIMEOUT); }
        catch (Exception e) { return false; }
    }

    /**
     * Rechnet CIDR in /24-Präfix-Liste um.
     *   /24 oder kleiner → 1 Präfix
     *   /16              → 256 Präfixe
     *   /8               → bis 256 Präfixe (gesichert)
     */
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

    private static String prefixToNetmask(int prefix) {
        int mask = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        return ((mask >> 24) & 0xFF) + "." + ((mask >> 16) & 0xFF)
                + "." + ((mask >> 8) & 0xFF) + "." + (mask & 0xFF);
    }

    private static int ipToInt(String ip) {
        if (ip == null) return 0;
        String[] p = ip.split("\\.");
        int r = 0;
        for (String s : p) { try { r = (r << 8) | Integer.parseInt(s.trim()); } catch (Exception ignored) {} }
        return r;
    }

    private static void printBox(String title) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.printf( "║  %-48s║%n", title);
        System.out.println("╚══════════════════════════════════════════════════╝");
    }
}
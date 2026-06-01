package main.java.networktool.logic.scan;

import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vergleicht zwei Scan-Ergebnisse und zeigt Unterschiede.
 *
 * Kategorien:
 *   NEU       – Host in B aber nicht in A
 *   WEG       – Host in A aber nicht in B
 *   OS-WECHSEL– OS hat sich verändert
 *   PORT-NEU  – neue offene Ports
 *   PORT-WEG  – Ports geschlossen
 *   GLEICH    – keine Änderung
 */
public final class ScanDelta {

    private ScanDelta() {}

    public enum ChangeType { NEU, WEG, OS_WECHSEL, PORT_AENDERUNG, GLEICH }

    public static final class DeltaEntry {
        public final String     ip;
        public final String     hostname;
        public final ChangeType type;
        public final String     detail;   // beschreibt die Änderung

        DeltaEntry(String ip, String hostname, ChangeType type, String detail) {
            this.ip = ip; this.hostname = hostname;
            this.type = type; this.detail = detail;
        }
    }

    // ── ScanResult-basierter Vergleich ────────────────────────────────────

    /**
     * Vergleicht zwei ScanResult-Listen (z.B. CIDR-Scan von heute vs. gestern).
     * Gibt alle Änderungen aus und liefert die DeltaEntry-Liste zurück.
     */
    public static List<DeltaEntry> compare(List<ScanResult> before,
                                           List<ScanResult> after,
                                           String labelBefore,
                                           String labelAfter) {
        Map<String, ScanResult> mapBefore = index(before);
        Map<String, ScanResult> mapAfter  = index(after);
        List<DeltaEntry> delta = new ArrayList<>();

        // Neue Hosts
        for (ScanResult r : after) {
            if (!mapBefore.containsKey(r.getIp())) {
                delta.add(new DeltaEntry(r.getIp(), r.getHostname(),
                        ChangeType.NEU, "neu entdeckt (" + r.getOsGuess() + ")"));
            }
        }

        // Verschwundene Hosts
        for (ScanResult r : before) {
            if (!mapAfter.containsKey(r.getIp())) {
                delta.add(new DeltaEntry(r.getIp(), r.getHostname(),
                        ChangeType.WEG, "nicht mehr erreichbar"));
            }
        }

        // OS- und Port-Änderungen bei bekannten Hosts
        for (ScanResult a : after) {
            ScanResult b = mapBefore.get(a.getIp());
            if (b == null) continue;

            if (!b.getOsGuess().equals(a.getOsGuess())) {
                delta.add(new DeltaEntry(a.getIp(), a.getHostname(),
                        ChangeType.OS_WECHSEL,
                        b.getOsGuess() + " → " + a.getOsGuess()));
            }

            Set<Integer> portsBefore = b.getOpenPorts().keySet();
            Set<Integer> portsAfter  = a.getOpenPorts().keySet();
            Set<Integer> newPorts    = new HashSet<>(portsAfter);
            newPorts.removeAll(portsBefore);
            Set<Integer> gonePorts   = new HashSet<>(portsBefore);
            gonePorts.removeAll(portsAfter);

            if (!newPorts.isEmpty() || !gonePorts.isEmpty()) {
                String detail = (newPorts.isEmpty()  ? "" : "+Ports " + sorted(newPorts) + "  ")
                              + (gonePorts.isEmpty() ? "" : "-Ports " + sorted(gonePorts));
                delta.add(new DeltaEntry(a.getIp(), a.getHostname(),
                        ChangeType.PORT_AENDERUNG, detail.trim()));
            }
        }

        printDelta(delta, labelBefore, labelAfter);
        return delta;
    }

    // ── HostResult-basierter Vergleich ────────────────────────────────────

    /**
     * Vergleicht zwei HostResult-Listen (z.B. Netzwerk-Scans).
     */
    public static List<DeltaEntry> compareHosts(List<HostResult> before,
                                                List<HostResult> after,
                                                String labelBefore,
                                                String labelAfter) {
        Map<String, HostResult> mapBefore = indexHosts(before);
        Map<String, HostResult> mapAfter  = indexHosts(after);
        List<DeltaEntry> delta = new ArrayList<>();

        after.stream().filter(h -> !mapBefore.containsKey(h.ip))
                .forEach(h -> delta.add(new DeltaEntry(h.ip, h.hostname,
                        ChangeType.NEU, "neu (" + h.os + ")")));

        before.stream().filter(h -> !mapAfter.containsKey(h.ip))
                .forEach(h -> delta.add(new DeltaEntry(h.ip, h.hostname,
                        ChangeType.WEG, "verschwunden")));

        after.stream().filter(h -> mapBefore.containsKey(h.ip))
                .forEach(h -> {
                    HostResult b = mapBefore.get(h.ip);
                    if (!b.os.equals(h.os))
                        delta.add(new DeltaEntry(h.ip, h.hostname,
                                ChangeType.OS_WECHSEL, b.os + " → " + h.os));
                });

        printDelta(delta, labelBefore, labelAfter);
        return delta;
    }

    // ── Ausgabe ───────────────────────────────────────────────────────────

    private static void printDelta(List<DeltaEntry> delta,
                                   String labelBefore, String labelAfter) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║  Scan-Vergleich: Δ                           ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("  Vorher : " + labelBefore);
        System.out.println("  Nachher: " + labelAfter);
        System.out.println();

        if (delta.isEmpty()) {
            System.out.println("  ✔ Keine Änderungen.");
            return;
        }

        long neu  = delta.stream().filter(e -> e.type == ChangeType.NEU).count();
        long weg  = delta.stream().filter(e -> e.type == ChangeType.WEG).count();
        long chg  = delta.stream().filter(e -> e.type != ChangeType.NEU
                                            && e.type != ChangeType.WEG).count();
        System.out.printf("  Neu: %d  |  Weg: %d  |  Geändert: %d%n%n", neu, weg, chg);

        // Gruppiert ausgeben
        for (ChangeType type : ChangeType.values()) {
            if (type == ChangeType.GLEICH) continue;
            List<DeltaEntry> group = delta.stream()
                    .filter(e -> e.type == type).collect(Collectors.toList());
            if (group.isEmpty()) continue;
            String header = switch (type) {
                case NEU          -> "  ++ NEUE HOSTS";
                case WEG          -> "  -- VERSCHWUNDENE HOSTS";
                case OS_WECHSEL   -> "  ~~ OS-ÄNDERUNGEN";
                case PORT_AENDERUNG -> "  ~~ PORT-ÄNDERUNGEN";
                default -> "";
            };
            System.out.println(header);
            group.forEach(e -> System.out.printf("     %-18s %-28s %s%n",
                    e.ip, e.hostname.length() > 28
                            ? e.hostname.substring(0, 27) + "…" : e.hostname,
                    e.detail));
            System.out.println();
        }
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static Map<String, ScanResult> index(List<ScanResult> list) {
        Map<String, ScanResult> map = new LinkedHashMap<>();
        list.forEach(r -> map.put(r.getIp(), r));
        return map;
    }

    private static Map<String, HostResult> indexHosts(List<HostResult> list) {
        Map<String, HostResult> map = new LinkedHashMap<>();
        list.forEach(h -> map.put(h.ip, h));
        return map;
    }

    private static String sorted(Set<Integer> ports) {
        return new TreeSet<>(ports).toString();
    }
}

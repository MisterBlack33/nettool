package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Host-CRUD-Operationen. Paket-privat, nur von NetworkStore verwendet. */
final class NetworkStoreHostOps {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private NetworkStoreHostOps() {}

    /** @return true wenn neuer Host (nicht nur Merge) */
    static boolean addOrMerge(String ip, Map<String, List<HostResult>> networks,
                              String cat, HostResult host) {
        List<HostResult> list = networks.get(cat);
        Optional<HostResult> existing = list.stream().filter(e -> e.ip.equals(ip)).findFirst();
        if (existing.isPresent()) {
            if (host.ports != null && !host.ports.isEmpty())
                existing.get().ports.putAll(host.ports);
            return false;
        }
        host.savedAt = LocalDateTime.now().format(DATE_FMT);
        if (host.notes == null) host.notes = "";
        list.add(host);
        return true;
    }

    static boolean removeFrom(String ip, Map<String, List<HostResult>> networks, String cat) {
        return networks.getOrDefault(cat, Collections.emptyList())
                .removeIf(e -> e.ip.equals(ip));
    }

    static boolean removeFromAll(String ip, Map<String, List<HostResult>> networks) {
        boolean changed = false;
        for (List<HostResult> list : networks.values())
            if (list.removeIf(h -> h.ip.equals(ip))) changed = true;
        return changed;
    }

    static void updateOs(String ip, String os, Map<String, List<HostResult>> networks) {
        allMutable(networks).stream().filter(e -> e.ip.equals(ip)).findFirst()
                .ifPresent(e -> e.os = os != null ? os : "");
    }

    static void updateNotes(String ip, String notes, Map<String, List<HostResult>> networks) {
        allMutable(networks).stream().filter(e -> e.ip.equals(ip)).findFirst()
                .ifPresent(e -> e.notes = notes != null ? notes : "");
    }

    static String findNetwork(String ip, Map<String, List<HostResult>> networks) {
        return networks.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(h -> h.ip.equals(ip)))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    static List<HostResult> allMutable(Map<String, List<HostResult>> networks) {
        Set<String> seen = new LinkedHashSet<>();
        return networks.values().stream().flatMap(Collection::stream)
                .filter(h -> seen.add(h.ip)).collect(Collectors.toList());
    }

    static List<HostResult> sorted(List<HostResult> list,
                                   NetworkStore.SortField sortField, boolean sortAsc) {
        Comparator<HostResult> cmp = switch (sortField) {
            case HOSTNAME -> Comparator.comparing(h -> h.hostname != null ? h.hostname.toLowerCase() : "");
            case OS       -> Comparator.comparing(h -> h.os != null ? h.os.toLowerCase() : "");
            default       -> Comparator.comparingInt(h -> ipToInt(h.ip));
        };
        if (!sortAsc) cmp = cmp.reversed();
        return list.stream().sorted(cmp).collect(Collectors.toUnmodifiableList());
    }

    static int ipToInt(String ip) {
        if (ip == null) return 0;
        String[] p = ip.split("\\.");
        int r = 0;
        for (String s : p) { try { r = (r << 8) | Integer.parseInt(s.trim()); } catch (Exception ignored) {} }
        return r;
    }
}
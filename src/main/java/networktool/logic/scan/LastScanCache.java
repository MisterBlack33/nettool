package networktool.logic.scan;

import networktool.model.HostResult;
import networktool.model.ScanResult;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Cache der zuletzt in der GUI-Tabelle angezeigten Hosts. Wird von GuiTableRenderer befüllt. */
public final class LastScanCache {

    private static final List<CachedHost> hosts = new CopyOnWriteArrayList<>();

    private LastScanCache() {}

    public static void updateFromHostResults(List<HostResult> list) {
        hosts.clear();
        for (HostResult h : list)
            hosts.add(new CachedHost(h.ip, cleanHostname(h.hostname), h.os));
    }

    public static void updateFromScanResults(List<ScanResult> list) {
        hosts.clear();
        for (ScanResult r : list)
            hosts.add(new CachedHost(r.getIp(), r.getHostname(), r.getOsGuess()));
    }

    public static List<CachedHost> getAll()  { return Collections.unmodifiableList(hosts); }
    public static boolean          isEmpty() { return hosts.isEmpty(); }

    private static String cleanHostname(String hostname) {
        if (hostname == null) return "";
        int idx = hostname.indexOf(" [");
        return idx < 0 ? hostname : hostname.substring(0, idx).trim();
    }

    public record CachedHost(String ip, String hostname, String os) {}
}
package main.java.networktool.logic.scan.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Zeitbuchhaltung pro Host (zuletzt gesehen, Alarm bereits gesendet). Zeit wird injiziert. */
final class OfflineTracker {

    private final Map<String, Long> lastSeenMs = new ConcurrentHashMap<>();
    private final Set<String> alerted = ConcurrentHashMap.newKeySet();

    void register(String ip, long nowMs) { lastSeenMs.putIfAbsent(ip, nowMs); }

    void markSeen(String ip, long nowMs) {
        lastSeenMs.put(ip, nowMs);
        alerted.remove(ip);
    }

    long offlineMs(String ip, long nowMs) {
        Long seen = lastSeenMs.get(ip);
        return seen == null ? 0 : nowMs - seen;
    }

    /** Liefert jeden Host genau einmal, sobald er die Schwelle überschreitet. */
    List<String> newlyOffline(long nowMs, long thresholdMs) {
        List<String> result = new ArrayList<>();
        lastSeenMs.forEach((ip, seen) -> {
            if (nowMs - seen >= thresholdMs && alerted.add(ip)) result.add(ip);
        });
        return result;
    }

    void retainOnly(Set<String> ips) {
        lastSeenMs.keySet().retainAll(ips);
        alerted.retainAll(ips);
    }

    Map<String, Long> snapshot() { return Map.copyOf(lastSeenMs); }

    void restore(Map<String, Long> saved) { saved.forEach(lastSeenMs::putIfAbsent); }

    void clear() {
        lastSeenMs.clear();
        alerted.clear();
    }
}

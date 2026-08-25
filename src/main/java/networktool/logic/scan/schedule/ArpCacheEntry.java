package main.java.networktool.logic.scan.schedule;

/** Ein Eintrag im persistenten ARP-Cache: IP↔MAC-Zuordnung mit Zeitstempel. */
public record ArpCacheEntry(String ip, String mac, long timestampMs) {

    public boolean isExpired(long maxAgeMs, long now) {
        return now - timestampMs > maxAgeMs;
    }
}
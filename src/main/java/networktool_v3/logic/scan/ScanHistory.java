package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.model.ScanResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Speichert die letzten N Scan-Ergebnisse automatisch (RAM, kein File).
 * Singleton, thread-sicher.
 *
 * Wird von NetworkScanner und NetworkHostScanner nach jedem Scan befüllt.
 * Für Scan-Vergleich Δ: letzten zwei Scans vergleichen ohne manuell 2x scannen.
 */
public final class ScanHistory {

    private static final class Holder { static final ScanHistory INSTANCE = new ScanHistory(); }
    public static ScanHistory getInstance() { return Holder.INSTANCE; }

    public static final int MAX_HISTORY = 10;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CopyOnWriteArrayList<Entry> history = new CopyOnWriteArrayList<>();

    private ScanHistory() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public void add(String label, List<ScanResult> results) {
        history.add(0, new Entry(label, new ArrayList<>(results),
                LocalDateTime.now().format(FMT)));
        if (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
    }

    /** Alle Einträge, neueste zuerst. */
    public List<Entry> getAll() { return Collections.unmodifiableList(history); }

    /** Letzten Eintrag holen (für Auto-Delta). */
    public Optional<Entry> getLast() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(0));
    }

    /** Zwei spezifische Einträge nach Index holen. */
    public Optional<Entry> get(int index) {
        if (index < 0 || index >= history.size()) return Optional.empty();
        return Optional.of(history.get(index));
    }

    public int size() { return history.size(); }
    public void clear() { history.clear(); }

    // ── Datenklasse ───────────────────────────────────────────────────────

    public static final class Entry {
        public final String          label;
        public final List<ScanResult> results;
        public final String          timestamp;

        public Entry(String label, List<ScanResult> results, String timestamp) {
            this.label     = label;
            this.results   = Collections.unmodifiableList(results);
            this.timestamp = timestamp;
        }

        public String display() {
            return "[" + timestamp + "]  " + label + "  (" + results.size() + " Hosts)";
        }
    }
}
package main.java.networktool_v3.storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Speichert alle empfangenen Nachrichten (ntfy + TCP) im RAM.
 * Max. 500 Einträge (FIFO).
 * Singleton – thread-sicher durch CopyOnWriteArrayList.
 */
public final class NotificationHistory {

    private static final class Holder {
        static final NotificationHistory INSTANCE = new NotificationHistory();
    }
    public static NotificationHistory getInstance() { return Holder.INSTANCE; }

    public static final int MAX_ENTRIES = 500;

    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private NotificationHistory() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public void add(String source, String title, String message) {
        Entry e = new Entry(source, title, message,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss  dd.MM.yyyy")));
        entries.add(0, e); // neueste zuerst
        if (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        listeners.forEach(Runnable::run);
    }

    public List<Entry> getAll() { return Collections.unmodifiableList(entries); }

    public void clear() { entries.clear(); listeners.forEach(Runnable::run); }

    public void addListener(Runnable l) { listeners.add(l); }

    public int size() { return entries.size(); }

    // ── Datenklasse ───────────────────────────────────────────────────────

    public static final class Entry {
        public final String source;   // "ntfy [topic]" oder "TCP [ip]"
        public final String title;
        public final String message;
        public final String time;

        public Entry(String source, String title, String message, String time) {
            this.source  = source;
            this.title   = title;
            this.message = message;
            this.time    = time;
        }
    }
}
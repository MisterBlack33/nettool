package networktool.gui.notification;

import main.java.networktool.storage.NetworkStore;
import main.java.networktool.storage.NotificationHistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Verwaltet Long-Poll-Subscriptions auf ntfy.sh-Topics (Handy/externe Geräte → PC).
 * since=all beim ersten Poll verhindert das Wiederholen alter Nachrichten.
 */
final class NtfySubscriptionManager {

    private static final Map<String, Future<?>> subscriptions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService exec =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "ntfy-sub");
                t.setDaemon(true);
                return t;
            });

    private NtfySubscriptionManager() {}

    static void subscribe(String topic) {
        if (topic == null || topic.isBlank()) return;
        if (subscriptions.containsKey(topic)) return;
        Future<?> f = exec.submit(() -> subscribeLoop(topic));
        subscriptions.put(topic, f);
        // kein System.out – kein Spam
    }

    /**
     * Abonniert gespeicherte Topics still (kein Startup-Listing).
     * Verzögerung 4s damit der GUI-Start abgeschlossen ist.
     */
    static void startSavedTopics() {
        exec.schedule(() -> {
            List<String> topics = NetworkStore.getInstance().getNtfyTopics();
            for (String topic : topics) {
                String t = topic.trim();
                if (!t.isEmpty() && !subscriptions.containsKey(t)) {
                    Future<?> f = exec.submit(() -> subscribeLoop(t));
                    subscriptions.put(t, f);
                }
            }
            // Stille Aktivierung – kein "Abonniert: [...]"-Spam
        }, 4, TimeUnit.SECONDS);
    }

    /** Bricht alle laufenden Subscriptions ab — nützlich für Tests. */
    static void stopAll() {
        for (Future<?> f : subscriptions.values()) f.cancel(true);
        subscriptions.clear();
        // Executor nicht vollständig herunterfahren (kann erneut verwendet werden).
    }

    private static void subscribeLoop(String topic) {
        long   backoffMs = 1_000;
        String lastId    = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                lastId    = connectAndReceive(topic, lastId);
                backoffMs = 1_000;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Fehler nur loggen wenn Verbindung wirklich verloren gegangen ist
                // (kein Dauerspam bei temporären Netzwerkproblemen)
            }
            try {
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, 60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static String connectAndReceive(String topic, String lastId) throws Exception {
        String since  = (lastId == null) ? "all" : lastId;
        String urlStr = "https://ntfy.sh/" + topic + "/json?poll=0&since=" + since;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/x-ndjson");
        conn.setRequestProperty("User-Agent", "NetTool/3.0 Java");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(75_000);

        int status = conn.getResponseCode();
        if (status != 200) throw new IOException("HTTP " + status);

        String newLastId = lastId;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(line);
                if (ev == null) continue;
                if (ev.id != null && !ev.id.isEmpty()) newLastId = ev.id;
                if (!"message".equals(ev.event)) continue;

                String msg   = ev.message != null ? ev.message.trim() : "";
                String title = (ev.title != null && !ev.title.isBlank())
                        ? ev.title : "ntfy [" + topic + "]";

                if (!msg.isEmpty()) {
                    System.out.println("  📱 ntfy [" + topic + "]  " + title + ":  " + msg);
                    LocalToast.show(title, msg);
                    NotificationHistory.getInstance()
                            .add("ntfy [" + topic + "]", title, msg);
                }
            }
        } finally {
            conn.disconnect();
        }
        return newLastId;
    }
}

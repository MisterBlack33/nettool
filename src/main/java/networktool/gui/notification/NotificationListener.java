package main.java.networktool.gui.notification;

import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.storage.NotificationHistory;
import main.java.networktool.storage.NetworkStore;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Empfängt eingehende Nachrichten auf zwei Kanälen:
 *  1. TCP-Listener (Port 9999) – NetTool ↔ NetTool direkt im LAN
 *  2. ntfy.sh NDJSON-Subscription – Handy/externe Geräte → PC
 *
 * Startup-Verhalten:
 *  - Abonnierte Topics werden NICHT beim Start in die Konsole geschrieben
 *  - Erst wenn eine Nachricht eintrifft, wird ausgegeben
 *  - since=all verhindert das Wiederholen alter Nachrichten
 */
public final class NotificationListener {

    private NotificationListener() {}

    private static final Map<String, Future<?>> ntfySubscriptions =
            new ConcurrentHashMap<>();
    private static final ScheduledExecutorService ntfyExec =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "ntfy-sub");
                t.setDaemon(true);
                return t;
            });

    // ── Öffentliche API ───────────────────────────────────────────────────

    public static void start() {
        startTcpListener();
        startNtfySubscriptions();
    }

    public static void subscribeNewTopic(String topic) {
        if (topic == null || topic.isBlank()) return;
        if (ntfySubscriptions.containsKey(topic)) return;
        Future<?> f = ntfyExec.submit(() -> subscribeLoop(topic));
        ntfySubscriptions.put(topic, f);
        // kein System.out – kein Spam
    }

    // ── TCP-Listener ──────────────────────────────────────────────────────

    private static void startTcpListener() {
        Thread t = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(MessageSender.NETTOOL_LISTENER_PORT)) {
                while (!ss.isClosed()) {
                    try { handleTcpClient(ss.accept()); }
                    catch (IOException ignored) {}
                }
            } catch (IOException e) {
                System.out.println("[NetTool] TCP-Port "
                        + MessageSender.NETTOOL_LISTENER_PORT
                        + " nicht verfügbar: " + e.getMessage());
            }
        }, "NetTool-TCP-Listener");
        t.setDaemon(true);
        t.start();
    }

    private static void handleTcpClient(Socket client) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                String from = client.getInetAddress().getHostAddress();
                String msg  = br.readLine();
                if (msg != null && !msg.isBlank()) {
                    System.out.println("  ✉ Nachricht von " + from + ": " + msg);
                    LocalToast.show("NetTool – von " + from, msg);
                    NotificationHistory.getInstance()
                            .add("TCP [" + from + "]", "NetTool – von " + from, msg);
                }
            } catch (Exception ignored) {}
        }, "NetTool-TCP-Handler").start();
    }

    // ── ntfy-Subscription ─────────────────────────────────────────────────

    /**
     * Abonniert gespeicherte Topics still (kein Startup-Listing).
     * Verzögerung 4s damit der GUI-Start abgeschlossen ist.
     */
    private static void startNtfySubscriptions() {
        ntfyExec.schedule(() -> {
            List<String> topics = NetworkStore.getInstance().getNtfyTopics();
            if (topics.isEmpty()) return;
            for (String topic : topics) {
                String t = topic.trim();
                if (!t.isEmpty() && !ntfySubscriptions.containsKey(t)) {
                    Future<?> f = ntfyExec.submit(() -> subscribeLoop(t));
                    ntfySubscriptions.put(t, f);
                }
            }
            // Stille Aktivierung – kein "Abonniert: [...]"-Spam
        }, 4, TimeUnit.SECONDS);
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

                NtfyEvent ev = parseNtfyJson(line);
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

    // ── Minimaler JSON-Parser ─────────────────────────────────────────────

    private static final class NtfyEvent {
        String id, event, topic, title, message;
    }

    static NtfyEvent parseNtfyJson(String json) {
        if (json == null || !json.startsWith("{")) return null;
        NtfyEvent ev = new NtfyEvent();
        ev.id      = extractStr(json, "id");
        ev.event   = extractStr(json, "event");
        ev.topic   = extractStr(json, "topic");
        ev.title   = extractStr(json, "title");
        ev.message = extractStr(json, "message");
        return ev;
    }

    private static String extractStr(String json, String field) {
        String keyPattern1 = ",\"" + field + "\"";
        String keyPattern2 = "{\"" + field + "\"";
        int ki = -1;
        int i1 = json.indexOf(keyPattern1), i2 = json.indexOf(keyPattern2);
        if (i1 < 0 && i2 < 0) return null;
        if (i1 < 0) ki = i2;
        else if (i2 < 0) ki = i1;
        else ki = Math.min(i1, i2);
        if (ki < 0) return null;
        int keyStart = json.indexOf('"', ki);
        if (keyStart < 0) return null;
        int keyEnd = json.indexOf('"', keyStart + 1);
        if (keyEnd < 0) return null;
        int colon = json.indexOf(':', keyEnd + 1);
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length() || json.charAt(s) != '"') return null;
        s++;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append(ch); sb.append(nx); }
                }
            } else if (ch == '"') {
                break;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}

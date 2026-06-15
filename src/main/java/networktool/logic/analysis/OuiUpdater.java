package main.java.networktool.logic.analysis;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Lädt die IEEE OUI-Datenbank von der offiziellen Quelle herunter
 * und cached sie lokal als data/oui_cache.bin (binäres Mapping).
 *
 * Beim ersten Start automatisch ausgeführt (wenn keine Cache-Datei existiert).
 * Format (binär): int count, then count * (UTF oui, UTF vendor)
 */
public final class OuiUpdater {

    private OuiUpdater() {}

    private static final String OUI_URL  =
            "https://standards-oui.ieee.org/oui/oui.txt";
    private static final String CACHE_FILE = "oui_cache.bin";
    private static final int    TIMEOUT_MS = 10_000;

    // Geladene externe OUIs (zusätzlich zu OuiDatabase-Einträgen)
    private static final Map<String, String> EXTENDED = new HashMap<>();
    private static boolean loaded = false;

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Schaut zuerst im Cache nach. Wenn nicht vorhanden → Download starten.
     * Läuft asynchron damit GUI nicht blockiert.
     */
    public static void initAsync(Path dataDir) {
        Thread t = new Thread(() -> init(dataDir), "OUI-Updater");
        t.setDaemon(true);
        t.start();
    }

    /** Synchrones Laden (für Tests). */
    public static void init(Path dataDir) {
        Path cache = dataDir.resolve(CACHE_FILE);
        if (Files.exists(cache)) {
            loadCache(cache);
        } else {
            download(dataDir, cache);
        }
    }

    /** Lookup: erst OuiDatabase, dann EXTENDED. */
    public static String lookup(String oui) {
        String result = OuiDatabase.lookup(oui);
        if (result != null) return result;
        if (!loaded) return null;
        return EXTENDED.get(oui != null ? oui.toUpperCase() : null);
    }

    /** Erzwingt einen Download (ignoriert Cache). */
    public static void forceUpdate(Path dataDir) {
        download(dataDir, dataDir.resolve(CACHE_FILE));
    }

    public static int extendedCount() { return EXTENDED.size(); }
    public static boolean isLoaded()  { return loaded; }

    // ── Download ─────────────────────────────────────────────────────────

    private static void download(Path dataDir, Path cache) {
        System.out.println("[OUI-Updater] Lade IEEE OUI-Datenbank...");
        try {
            Files.createDirectories(dataDir);
            URL url = new URL(OUI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent", "NetTool/3.0");

            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Format: "XX-XX-XX   (hex)\t\tHerstellername"
                    if (line.contains("(hex)")) {
                        String[] parts = line.split("\\(hex\\)", 2);
                        if (parts.length == 2) {
                            String oui = parts[0].trim().replace("-", ":").toUpperCase();
                            String vendor = parts[1].trim();
                            if (oui.length() >= 8 && !vendor.isEmpty()) {
                                lines.add(oui.substring(0, 8) + "\t" + vendor);
                            }
                        }
                    }
                }
            }
            // Schreibe kompakt binär: count + (oui, vendor)
            try (var out = new DataOutputStream(Files.newOutputStream(cache,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeInt(lines.size());
                for (String l : lines) {
                    String[] p = l.split("\t", 2);
                    out.writeUTF(p[0]);
                    out.writeUTF(p.length > 1 ? p[1] : "");
                }
            }
            System.out.println("[OUI-Updater] " + lines.size() + " Eintraege gespeichert (bin).");
            loadCache(cache);
        } catch (Exception e) {
            System.out.println("[OUI-Updater] Download fehlgeschlagen: " + e.getMessage());
            System.out.println("[OUI-Updater] Verwende eingebaute Datenbank (" +
                    OuiDatabase.class.getSimpleName() + ").");
        }
    }

    private static void loadCache(Path cache) {
        try (var in = new DataInputStream(Files.newInputStream(cache))) {
            int count = in.readInt();
            int loadedCount = 0;
            for (int i = 0; i < count; i++) {
                String oui = in.readUTF();
                String vendor = in.readUTF();
                if (oui != null && !oui.isBlank()) {
                    EXTENDED.put(oui.trim().toUpperCase(), vendor != null ? vendor.trim() : "");
                    loadedCount++;
                }
            }
            loaded = true;
            System.out.println("[OUI-Updater] " + loadedCount + " OUI-Eintraege geladen.");
        } catch (IOException e) {
            System.err.println("[OUI-Updater] Cache-Ladefehler: " + e.getMessage());
        }
    }
}
package main.java.networktool.logic.analysis;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Lädt die IEEE OUI-Datenbank von der offiziellen Quelle herunter
 * und cached sie lokal als txt/oui_cache.txt.
 *
 * Beim ersten Start automatisch ausgeführt (wenn keine Cache-Datei existiert).
 * Format: "XX:XX:XX  Herstellername"
 */
public final class OuiUpdater {

    private OuiUpdater() {}

    private static final String OUI_URL  =
            "https://standards-oui.ieee.org/oui/oui.txt";
    private static final String CACHE_FILE = "oui_cache.txt";
    private static final int    TIMEOUT_MS = 10_000;

    // Geladene externe OUIs (zusätzlich zu OuiDatabase-Einträgen)
    private static final Map<String, String> EXTENDED = new HashMap<>();
    private static boolean loaded = false;

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Schaut zuerst im Cache nach. Wenn nicht vorhanden → Download starten.
     * Läuft asynchron damit GUI nicht blockiert.
     */
    public static void initAsync(Path txtDir) {
        Thread t = new Thread(() -> init(txtDir), "OUI-Updater");
        t.setDaemon(true);
        t.start();
    }

    /** Synchrones Laden (für Tests). */
    public static void init(Path txtDir) {
        Path cache = txtDir.resolve(CACHE_FILE);
        if (Files.exists(cache)) {
            loadCache(cache);
        } else {
            download(txtDir, cache);
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
    public static void forceUpdate(Path txtDir) {
        download(txtDir, txtDir.resolve(CACHE_FILE));
    }

    public static int extendedCount() { return EXTENDED.size(); }
    public static boolean isLoaded()  { return loaded; }

    // ── Download ─────────────────────────────────────────────────────────

    private static void download(Path txtDir, Path cache) {
        System.out.println("[OUI-Updater] Lade IEEE OUI-Datenbank...");
        try {
            Files.createDirectories(txtDir);
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
            Files.write(cache, lines, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("[OUI-Updater] " + lines.size() + " Eintraege gespeichert.");
            loadCache(cache);
        } catch (Exception e) {
            System.out.println("[OUI-Updater] Download fehlgeschlagen: " + e.getMessage());
            System.out.println("[OUI-Updater] Verwende eingebaute Datenbank (" +
                    OuiDatabase.class.getSimpleName() + ").");
        }
    }

    private static void loadCache(Path cache) {
        try {
            int count = 0;
            for (String line : Files.readAllLines(cache)) {
                if (line.isBlank()) continue;
                String[] p = line.split("\t", 2);
                if (p.length == 2) {
                    EXTENDED.put(p[0].trim().toUpperCase(), p[1].trim());
                    count++;
                }
            }
            loaded = true;
            System.out.println("[OUI-Updater] " + count + " OUI-Eintraege geladen.");
        } catch (IOException e) {
            System.err.println("[OUI-Updater] Cache-Ladefehler: " + e.getMessage());
        }
    }
}
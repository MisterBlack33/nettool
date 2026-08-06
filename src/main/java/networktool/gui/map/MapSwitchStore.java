package networktool.gui.map;

import main.java.networktool.storage.StorageUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistiert manuell markierte Switch-IPs in data/mapSwitches.json.
 * Lädt beim Start automatisch.
 */
public final class MapSwitchStore {

    private static final Logger LOG = Logger.getLogger(MapSwitchStore.class.getName());

    private MapSwitchStore() {}

    static final Set<String> SWITCHES = Collections.synchronizedSet(new HashSet<>());
    private static final String FILE = "mapSwitches.json";

    /** Wenn true: keine Datei-I/O, nur In-Memory. Von Tests gesetzt, damit produktive Daten unverändert bleiben. */
    static volatile boolean testMode = false;

    static {
        load();
    }

    public static void add(String ip) {
        SWITCHES.add(ip);
        persist();
    }

    static void remove(String ip) {
        SWITCHES.remove(ip);
        persist();
    }

    static void clear() {
        SWITCHES.clear();
        persist();
    }

    static boolean contains(String ip) {
        return SWITCHES.contains(ip);
    }

    private static void load() {
        try {
            Path file = StorageUtils.resolveDataDir().resolve(FILE);
            if (!Files.exists(file)) return;
            String raw = Files.readString(file, StandardCharsets.UTF_8)
                    .trim().replaceAll("^\\[|]$", "");
            for (String part : raw.split(",")) {
                String ip = part.trim().replaceAll("^\"|\"$", "");
                if (!ip.isBlank()) SWITCHES.add(ip);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Switch-Liste konnte nicht geladen werden", e);
        }
    }

    private static void persist() {
        if (testMode) return;
        try {
            Path dir = StorageUtils.resolveDataDir();
            Files.createDirectories(dir);
            List<String> sorted = new ArrayList<>(SWITCHES);
            Collections.sort(sorted);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(sorted.get(i)).append("\"");
            }
            Files.writeString(dir.resolve(FILE), sb.append("]").toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Switch-Liste konnte nicht gespeichert werden", e);
        }
    }
}
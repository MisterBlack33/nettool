package main.java.networktool.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Zentrale Auflösung aller persistenten Speicherorte.
 *
 * Struktur:
 *   saves/
 *     userdata/       – UserAuth (users.json, gehashte Passwörter, kein Klartext)
 *     networkdata/     – gespeicherte Netzwerke/Hosts (NetworkStore)
 *     profiles/        – Scan-Profile
 *     logs/            – Audit-/Debug-Log
 *     cache/           – ARP-Cache, Sonify-Konfiguration, OUI-Cache
 */
public final class StorageLocations {

    private static final String ROOT_DIR_NAME = "saves";

    private StorageLocations() {}

    public static Path root()        { return ensure(projectRoot().resolve(ROOT_DIR_NAME)); }
    public static Path userData()    { return ensure(root().resolve("userdata")); }
    public static Path networkData() { return ensure(root().resolve("networkdata")); }
    public static Path profiles()    { return ensure(root().resolve("profiles")); }
    public static Path logs()        { return ensure(root().resolve("logs")); }
    public static Path cache()       { return ensure(root().resolve("cache")); }

    private static Path projectRoot() {
        try {
            URL url = StorageLocations.class.getProtectionDomain().getCodeSource().getLocation();
            Path base = Paths.get(url.toURI()).toAbsolutePath().normalize();
            if (base.toString().endsWith(".jar")) {
                Path parent = base.getParent();
                return parent != null && parent.getFileName() != null
                        && "target".equals(parent.getFileName().toString())
                        ? parent.getParent()
                        : parent;
            }
            Path p = base;
            while (p != null && !isProjectRoot(p)) p = p.getParent();
            return p != null ? p : Paths.get(System.getProperty("user.dir"));
        } catch (URISyntaxException | SecurityException | NullPointerException ignored) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    private static boolean isProjectRoot(Path p) {
        return Files.exists(p.resolve("pom.xml"));
    }

    private static Path ensure(Path dir) {
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir;
    }
}
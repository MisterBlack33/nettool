package main.java.networktool.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Zentrale Auflösung aller persistenten Speicherorte.
 *
 * Struktur:
 *   ~/.nettool/
 *     userdata/       - UserAuth account data
 *     networkdata/    - saved networks and hosts
 *     profiles/       - scan profiles
 *     logs/           - audit and debug logs
 *     cache/          - local caches and settings
 */
public final class StorageLocations {

    private static final String DATA_DIR_PROPERTY = "nettool.dataDir";
    private static final String SKIP_MIGRATION_PROPERTY = "nettool.skipLegacyMigration";
    private static final String LEGACY_DIR_NAME = "saves";
    private static volatile Path resolvedRoot;

    private StorageLocations() {}

    public static Path root() {
        Path root = resolvedRoot;
        if (root != null) return root;
        synchronized (StorageLocations.class) {
            if (resolvedRoot == null) resolvedRoot = resolveRoot();
            return resolvedRoot;
        }
    }

    public static Path userData()    { return ensure(root().resolve("userdata")); }
    public static Path networkData() { return ensure(root().resolve("networkdata")); }
    public static Path profiles()    { return ensure(root().resolve("profiles")); }
    public static Path logs()        { return ensure(root().resolve("logs")); }
    public static Path cache()       { return ensure(root().resolve("cache")); }

    private static Path resolveRoot() {
        String configured = System.getProperty(DATA_DIR_PROPERTY);
        Path root;
        if (configured != null && !configured.isBlank()) {
            root = Paths.get(configured).toAbsolutePath().normalize();
        } else {
            String userHome = System.getProperty("user.home");
            if (userHome == null || userHome.isBlank()) {
                throw new IllegalStateException("Java-Systemeigenschaft user.home ist nicht gesetzt.");
            }
            root = Paths.get(userHome).resolve(".nettool").toAbsolutePath().normalize();
        }
        ensure(root);
        if (!Boolean.getBoolean(SKIP_MIGRATION_PROPERTY)) {
            migrateLegacyData(projectRoot().resolve(LEGACY_DIR_NAME), root);
        }
        return root;
    }

    static void migrateLegacyData(Path legacyRoot, Path dataRoot) {
        Path legacy = legacyRoot.toAbsolutePath().normalize();
        Path destinationRoot = dataRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(legacy) || destinationRoot.startsWith(legacy)) return;
        try {
            Files.walkFileTree(legacy, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                    Path destination = destinationRoot.resolve(legacy.relativize(file)).normalize();
                    if (!destination.startsWith(destinationRoot)) {
                        throw new IOException("Ungültiger Pfad in Legacy-Daten: " + file);
                    }
                    if (!Files.exists(destination)) {
                        Files.createDirectories(destination.getParent());
                        try {
                            Files.copy(file, destination);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) throw e;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Vorhandene NetTool-Daten konnten nicht migriert werden.", e);
        }
    }

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
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("NetTool-Datenverzeichnis kann nicht angelegt werden: " + dir, e);
        }
        return dir;
    }
}
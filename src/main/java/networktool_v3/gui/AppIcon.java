package main.java.networktool_v3.gui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Lädt das App-Icon und setzt es für Fenster + Taskleiste.
 *
 * Suchpfade (in dieser Reihenfolge):
 *  1. Classpath: /icon.png
 *  2. Classpath: /main/java/networktool_v3/gui/icon.png
 *  3. Dateisystem: src/main/resources/icon.png  (IntelliJ Run ohne Build)
 *  4. Dateisystem: resources/icon.png
 *  5. Neben der JAR / im Arbeitsverzeichnis: icon.png
 */
public final class AppIcon {

    private static final Logger LOG = Logger.getLogger(AppIcon.class.getName());

    private static final String[] CLASSPATH_PATHS = {
            "/icon.png",
            "/images/icon.png",
            "/main/java/networktool_v3/gui/icon.png"
    };

    private static final String[] FS_PATHS = {
            "src/main/resources/icon.png",
            "resources/icon.png",
            "icon.png"
    };

    private static Image cachedIcon;

    private AppIcon() {}

    public static Image get() {
        if (cachedIcon != null) return cachedIcon;

        // 1. Classpath
        for (String path : CLASSPATH_PATHS) {
            Image img = loadFromClasspath(path);
            if (img != null) { cachedIcon = img; return cachedIcon; }
        }

        // 2. Dateisystem (relativ zum Arbeitsverzeichnis)
        String workDir = System.getProperty("user.dir", ".");
        for (String rel : FS_PATHS) {
            Image img = loadFromFile(Paths.get(workDir, rel));
            if (img != null) { cachedIcon = img; return cachedIcon; }
        }

        // 3. Relativ zur JAR/Klassen-Datei
        try {
            Path codeBase = Paths.get(
                    AppIcon.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            Path base = codeBase.toString().endsWith(".jar")
                    ? codeBase.getParent()
                    : codeBase;
            for (String rel : FS_PATHS) {
                Image img = loadFromFile(base.resolve(rel));
                if (img != null) { cachedIcon = img; return cachedIcon; }
            }
        } catch (Exception ignored) {}

        LOG.warning("[AppIcon] Icon nicht gefunden. Lege icon.png unter src/main/resources/icon.png ab.");
        return null;
    }

    public static void apply(Frame frame) {
        Image icon = get();
        if (icon == null) return;
        frame.setIconImage(icon);
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar tb = Taskbar.getTaskbar();
                if (tb.isSupported(Taskbar.Feature.ICON_IMAGE))
                    tb.setIconImage(icon);
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {}
    }

    private static Image loadFromClasspath(String path) {
        try (InputStream is = AppIcon.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) { return null; }
    }

    private static Image loadFromFile(Path path) {
        File f = path.toFile();
        if (!f.exists()) return null;
        try { return ImageIO.read(f); }
        catch (Exception e) { return null; }
    }
}
package main.java.networktool_v3.gui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Lädt das App-Icon (ICO-Format) und setzt es für Fenster + Taskleiste.
 *
 * Suchpfade (in dieser Reihenfolge):
 *  1. Classpath: /icon.ico
 *  2. {workDir}/src/main/resources/icon.ico
 *  3. {workDir}/resources/icon.ico
 *  4. {workDir}/icon.ico
 *  5. Neben der JAR
 */
public final class AppIcon {

    private static final Logger LOG = Logger.getLogger(AppIcon.class.getName());

    private static final String[] CLASSPATH_PATHS = { "/icon.ico", "/images/icon.ico" };
    private static final String[] FS_PATHS        = {
            "src/main/resources/icon.ico",
            "resources/icon.ico",
            "icon.ico"
    };

    private static Image cachedIcon;

    private AppIcon() {}

    public static Image get() {
        if (cachedIcon != null) return cachedIcon;

        for (String p : CLASSPATH_PATHS) {
            Image img = loadFromClasspath(p);
            if (img != null) { cachedIcon = img; return cachedIcon; }
        }

        String workDir = System.getProperty("user.dir", ".");
        for (String rel : FS_PATHS) {
            Image img = loadFromFile(Paths.get(workDir, rel));
            if (img != null) { cachedIcon = img; return cachedIcon; }
        }

        try {
            Path codeBase = Paths.get(
                    AppIcon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path base = codeBase.toString().endsWith(".jar")
                    ? codeBase.getParent() : codeBase;
            for (String rel : FS_PATHS) {
                Image img = loadFromFile(base.resolve(rel));
                if (img != null) { cachedIcon = img; return cachedIcon; }
            }
        } catch (Exception ignored) {}

        LOG.warning("[AppIcon] icon.ico nicht gefunden – lege icon.ico unter src/main/resources/ ab.");
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

    // ── Loader ────────────────────────────────────────────────────────────

    private static Image loadFromClasspath(String path) {
        try (InputStream is = AppIcon.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return readIco(is.readAllBytes());
        } catch (Exception e) { return null; }
    }

    private static Image loadFromFile(Path path) {
        if (!path.toFile().exists()) return null;
        try { return readIco(Files.readAllBytes(path)); }
        catch (Exception e) { return null; }
    }

    /**
     * Liest eine ICO-Datei.
     * ICO-Format: Header + Directory + Image-Data (PNG oder BMP eingebettet).
     * Wählt das größte verfügbare Bild.
     */
    private static Image readIco(byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // ICO Header: reserved(2) + type(2) + count(2)
        buf.position(0);
        int reserved = buf.getShort() & 0xFFFF;
        int type     = buf.getShort() & 0xFFFF;
        if (reserved != 0 || type != 1) {
            // Kein gültiger ICO-Header – direkt als Bild versuchen
            return ImageIO.read(new ByteArrayInputStream(data));
        }

        int count = buf.getShort() & 0xFFFF;
        if (count == 0) return null;

        // Directory lesen – bestes (größtes) Bild wählen
        int bestOffset = -1, bestSize = -1, bestWidth = 0;
        for (int i = 0; i < count; i++) {
            int w       = buf.get() & 0xFF;  // 0 = 256
            int h       = buf.get() & 0xFF;
            buf.get();                         // colorCount
            buf.get();                         // reserved
            buf.getShort();                    // planes
            buf.getShort();                    // bitCount
            int imgSize   = buf.getInt();
            int imgOffset = buf.getInt();
            int realW = (w == 0) ? 256 : w;
            if (realW > bestWidth) {
                bestWidth  = realW;
                bestSize   = imgSize;
                bestOffset = imgOffset;
            }
        }

        if (bestOffset < 0 || bestOffset + bestSize > data.length) return null;

        byte[] imgData = new byte[bestSize];
        System.arraycopy(data, bestOffset, imgData, 0, bestSize);

        // PNG-Signatur prüfen (0x89 50 4E 47)
        if (imgData.length >= 4
                && (imgData[0] & 0xFF) == 0x89
                && imgData[1] == 0x50
                && imgData[2] == 0x4E
                && imgData[3] == 0x47) {
            return ImageIO.read(new ByteArrayInputStream(imgData));
        }

        // BMP-Daten: ICO-BMP hat keinen BITMAPFILEHEADER → direkt als AWT lesen
        return decodeBmpDib(imgData, bestWidth);
    }

    /** Dekodiert ICO-BMP (DIB ohne BITMAPFILEHEADER) über AWT. */
    private static Image decodeBmpDib(byte[] dib, int width) {
        try {
            // BMP-Datei-Header (14 Bytes) voranstellen
            int fileSize = 14 + dib.length;
            ByteBuffer bmp = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);
            bmp.put((byte)'B').put((byte)'M');
            bmp.putInt(fileSize);
            bmp.putInt(0);
            bmp.putInt(14 + 40); // pixel data offset
            bmp.put(dib);
            return ImageIO.read(new ByteArrayInputStream(bmp.array()));
        } catch (Exception e) {
            return null;
        }
    }
}
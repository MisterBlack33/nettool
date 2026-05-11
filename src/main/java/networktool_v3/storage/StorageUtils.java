package main.java.networktool_v3.storage;

import java.nio.file.*;

/**
 * Öffentliche Hilfsmethoden für den Storage-Bereich.
 *
 * Löst das Package-Privacy-Problem: {@link NetworkStorePersistence} ist
 * package-private und kann von {@code networktool_v3.security} nicht
 * direkt aufgerufen werden.
 *
 * Diese Klasse stellt die zwei extern benötigten Methoden öffentlich bereit:
 *   - {@link #resolveTxtDir()}    – txt-Verzeichnis neben der JAR / den .class-Dateien
 *   - {@link #extractJsonStr}     – Einzelnen String-Wert aus JSON-Text lesen
 */
public final class StorageUtils {

    private StorageUtils() {}

    // ── txt-Verzeichnis ───────────────────────────────────────────────────

    /**
     * Gibt das txt/-Verzeichnis zurück (neben JAR oder neben Klassen-Root).
     *
     * Auflösungsreihenfolge:
     *  1. JAR:  &lt;jar-dir&gt;/txt/
     *  2. IDE:  Klassen-Root/networktool_v3/txt/
     *  3. Fallback: user.dir/networktool_v3/txt/
     *
     * Delegiert intern an {@link NetworkStorePersistence#resolveTxtDir()}.
     */
    public static Path resolveTxtDir() {
        return NetworkStorePersistence.resolveTxtDir();
    }

    // ── JSON-Hilfsmethode ─────────────────────────────────────────────────

    /**
     * Extrahiert einen String-Wert aus einem einfachen JSON-Objekt.
     *
     * Beispiel:
     * <pre>
     *   String json = "{\"username\":\"admin\",\"hash\":\"abc\"}";
     *   String user = StorageUtils.extractJsonStr(json, "username"); // → "admin"
     * </pre>
     *
     * Unterstützt einfache Escape-Sequenzen: {@code \"}, {@code \\},
     * {@code \n}, {@code \t}.
     *
     * @param json  JSON-String (oder JSON-Fragment)
     * @param field Feldname (ohne Anführungszeichen)
     * @return Wert als String, oder {@code null} wenn Feld nicht gefunden
     */
    public static String extractJsonStr(String json, String field) {
        return NetworkStorePersistence.extractJsonStr(json, field);
    }

    // ── JSON-Escape ───────────────────────────────────────────────────────

    /**
     * Maskiert Sonderzeichen für JSON-Strings.
     *
     * @param s Eingabe-String (darf null sein)
     * @return JSON-sicherer String ohne Anführungszeichen
     */
    public static String escapeJson(String s) {
        return NetworkStorePersistence.esc(s);
    }
}
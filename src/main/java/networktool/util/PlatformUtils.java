package networktool.util;

import java.util.regex.Pattern;

/**
 * Zentrale Plattform-Erkennung und Validierungshilfen.
 * Ersetzt alle verteilten isWin()-Duplikate im Projekt.
 */
public final class PlatformUtils {

    private PlatformUtils() {}

    private static final String OS = System.getProperty("os.name", "").toLowerCase();

    public static boolean isWindows() { return OS.contains("win"); }
    public static boolean isMac()     { return OS.contains("mac"); }
    public static boolean isLinux()   { return OS.contains("linux") || OS.contains("nix") || OS.contains("nux"); }

    // ── Sicherheits-Validierung ───────────────────────────────────────────

    private static final Pattern SAFE_IP =
            Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern SAFE_IFACE =
            Pattern.compile("^[a-zA-Z0-9@:\\-]{1,32}$");
    private static final Pattern SAFE_MAC =
            Pattern.compile("^([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}$");
    private static final Pattern SAFE_CIDR =
            Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2}$");
    private static final Pattern SAFE_HOSTNAME =
            Pattern.compile("^[a-zA-Z0-9.\\-]{1,253}$");

    public static boolean isSafeIp(String s)       { return s != null && SAFE_IP.matcher(s).matches(); }
    public static boolean isSafeInterface(String s){ return s != null && SAFE_IFACE.matcher(s).matches(); }
    public static boolean isSafeMac(String s)      { return s != null && SAFE_MAC.matcher(s).matches(); }
    public static boolean isSafeCidr(String s)     { return s != null && SAFE_CIDR.matcher(s).matches(); }
    public static boolean isSafeHostname(String s) { return s != null && SAFE_HOSTNAME.matcher(s).matches(); }

    /**
     * Gibt den validierten String zurück oder wirft IllegalArgumentException.
     * Für direkte Verwendung in exec()-Aufrufen.
     */
    public static String requireSafeIp(String ip) {
        if (!isSafeIp(ip)) throw new IllegalArgumentException("Ungültige IP: " + ip);
        return ip;
    }

    public static String requireSafeInterface(String iface) {
        if (!isSafeInterface(iface)) throw new IllegalArgumentException("Ungültiges Interface: " + iface);
        return iface;
    }

    public static String requireSafeMac(String mac) {
        if (!isSafeMac(mac)) throw new IllegalArgumentException("Ungültige MAC: " + mac);
        return mac;
    }

    // ── Shell-Escaping (nur für Logging/Anzeige, nicht für exec) ─────────

    /** Single-Quote-Escape für PowerShell-Strings. */
    public static String escapePowerShell(String s) {
        if (s == null) return "";
        return s.replace("'", "''").replace("\n", " ").replace("\r", "");
    }

    /** Escaping für SSH-Kommandos (nur notify-send / osascript). */
    public static String escapeSshArg(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "");
    }
}
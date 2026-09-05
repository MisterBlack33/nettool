package main.java.networktool.util;

/**
 * Einheitliche Text-Tags für Statusmeldungen (Ausgabefenster, Logs).
 * Ersetzt bisherige Emoji-Präfixe (✔ ✕ ⚠ ℹ) — farbliche Kennzeichnung
 * (GuiTheme.ACCENT2 / WARN / ACCENT / FG_DIM) übernimmt die Signalwirkung.
 */
public final class StatusTags {

    private StatusTags() {}

    public static final String OK     = "[OK]";
    public static final String FEHLER = "[FEHLER]";
    public static final String WARN   = "[WARN]";
    public static final String INFO   = "[INFO]";
}
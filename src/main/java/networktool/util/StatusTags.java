package main.java.networktool.util;

/**
 * Einheitliche Text-Tags für Statusmeldungen (Ausgabefenster, Logs).
 * Ersetzt bisherige Emoji-Präfixe (✔ ✕ ⚠ ℹ) — farbliche Kennzeichnung
 * {@code GuiStatusReporter} übernimmt die Signalwirkung über GuiTheme-Farben.
 */
public final class StatusTags {

    private StatusTags() {}

    public static final String OK     = "[OK]";
    public static final String FEHLER = "[FEHLER]";
    public static final String WARN   = "[WARN]";
    public static final String INFO   = "[INFO]";
}
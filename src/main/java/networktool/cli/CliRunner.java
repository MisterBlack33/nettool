package main.java.networktool.cli;

import java.io.PrintStream;

/**
 * Führt die geparsten CLI-Argumente aus und liefert den Exit-Code.
 * Kein Swing-Import hier, damit dieser Vertrag headless testbar bleibt —
 * den eigentlichen GUI-Start übernimmt {@code Main} bei Rückgabewert
 * {@link #GUI_START}.
 */
public final class CliRunner {

    public static final int EXIT_OK           = 0;
    public static final int EXIT_INVALID_ARGS = 2;
    /** Signalisiert dem Aufrufer (Main): keine CLI-Aktion, GUI starten. */
    public static final int GUI_START = -1;

    static final String USAGE = """
            NetTool – Netzwerk-Analyse-Suite

            Verwendung:
              nettool                 Startet die grafische Oberfläche (Standard)
              nettool --help | -h     Zeigt diese Hilfe
              nettool --version | -v  Zeigt die Programmversion

            Ohne Argumente startet NetTool im GUI-Modus; ein headless
            Scan-Modus ist aktuell nicht Teil dieses CLI-Vertrags.""";

    static final String VERSION = "NetTool v3 (Feature-Version 0.8.12)";

    private CliRunner() {}

    public static int run(CliArgs parsed, PrintStream out) {
        return switch (parsed.command) {
            case HELP    -> printAndOk(out, USAGE);
            case VERSION -> printAndOk(out, VERSION);
            case GUI     -> handleGui(parsed, out);
        };
    }

    private static int handleGui(CliArgs parsed, PrintStream out) {
        if (!parsed.hasUnknownArgs()) return GUI_START;
        out.println("Unbekannte Argumente: " + String.join(" ", parsed.unknown));
        out.println(USAGE);
        return EXIT_INVALID_ARGS;
    }

    private static int printAndOk(PrintStream out, String text) {
        out.println(text);
        return EXIT_OK;
    }
}

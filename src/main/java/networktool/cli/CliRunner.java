package main.java.networktool.cli;

import main.java.networktool.model.ScanProfile;
import main.java.networktool.storage.profile.ScanProfileStore;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;

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
              nettool --profiles      Listet gespeicherte Scan-Profile
              nettool --profiles-json Listet Profile als JSON

            Profilbefehle lesen nur lokale Konfiguration und senden keinen
            Netzwerkverkehr. Ohne Argumente startet NetTool im GUI-Modus.
            Aktive Scans sind nicht Teil dieses CLI-Vertrags.""";

    static final String VERSION = "NetTool v3 (Feature-Version 0.8.12)";

    private CliRunner() {}

    public static int run(CliArgs parsed, PrintStream out) {
        return run(parsed, out, () -> ScanProfileStore.getInstance().getAll());
    }

    static int run(CliArgs parsed, PrintStream out, Supplier<List<ScanProfile>> profiles) {
        return switch (parsed.command) {
            case HELP          -> printAndOk(out, USAGE);
            case VERSION       -> printAndOk(out, VERSION);
            case PROFILES      -> printProfiles(out, profiles.get(), false);
            case PROFILES_JSON -> printProfiles(out, profiles.get(), true);
            case GUI           -> handleGui(parsed, out);
        };
    }

    private static int printProfiles(PrintStream out, List<ScanProfile> profiles, boolean json) {
        if (json) {
            out.println(profilesJson(profiles));
        } else if (profiles.isEmpty()) {
            out.println("Keine gespeicherten Scan-Profile.");
        } else {
            out.println("Gespeicherte Scan-Profile:");
            for (ScanProfile profile : profiles) {
                out.println("- " + profile.name + "  ["
                        + (profile.cidrs.isEmpty() ? "lokal" : String.join(", ", profile.cidrs))
                        + "]");
            }
        }
        return EXIT_OK;
    }

    private static String profilesJson(List<ScanProfile> profiles) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < profiles.size(); i++) {
            if (i > 0) json.append(',');
            ScanProfile profile = profiles.get(i);
            json.append("{\"name\":").append(jsonString(profile.name))
                    .append(",\"cidrs\":[");
            for (int j = 0; j < profile.cidrs.size(); j++) {
                if (j > 0) json.append(',');
                json.append(jsonString(profile.cidrs.get(j)));
            }
            json.append("]}");
        }
        return json.append(']').toString();
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
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

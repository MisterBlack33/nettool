package main.java.networktool.cli;

import java.util.Arrays;
import java.util.List;

/**
 * Parst die produktiven CLI-Argumente. Reines Parsing, keine Seiteneffekte.
 * Vertrag: --help/-h, --version/-v. Ohne (bekannte) Argumente startet die GUI.
 */
public final class CliArgs {

    public enum Command { HELP, VERSION, GUI }

    public final Command command;
    public final List<String> unknown;

    private CliArgs(Command command, List<String> unknown) {
        this.command = command;
        this.unknown = unknown;
    }

    public static CliArgs parse(String[] args) {
        if (args == null || args.length == 0) return new CliArgs(Command.GUI, List.of());
        for (String arg : args) {
            if (isHelp(arg)) return new CliArgs(Command.HELP, List.of());
            if (isVersion(arg)) return new CliArgs(Command.VERSION, List.of());
        }
        return new CliArgs(Command.GUI, List.copyOf(Arrays.asList(args)));
    }

    /** true, wenn im GUI-Fall Argumente übrig blieben, die keinem bekannten Schalter entsprachen. */
    public boolean hasUnknownArgs() {
        return command == Command.GUI && !unknown.isEmpty();
    }

    private static boolean isHelp(String arg)    { return "--help".equals(arg) || "-h".equals(arg); }
    private static boolean isVersion(String arg) { return "--version".equals(arg) || "-v".equals(arg); }
}

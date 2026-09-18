package main.java.networktool.logic.analysis.security;

import java.util.List;

/** Hält die aktive {@link FindingsSource}-Instanz (analog OutputRendererRegistry). */
public final class FindingsSourceRegistry {

    private static volatile FindingsSource source;

    private FindingsSourceRegistry() {}

    public static void register(FindingsSource s)   { source = s; }
    public static void unregister(FindingsSource s) { if (source == s) source = null; }

    /** Nie null — liefert leere Liste solange Workstream A keine Quelle registriert hat. */
    public static List<SecurityFinding> getAll() {
        FindingsSource s = source;
        return s != null ? s.getAll() : List.of();
    }
}

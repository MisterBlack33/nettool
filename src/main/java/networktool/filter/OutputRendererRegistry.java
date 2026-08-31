package main.java.networktool.filter;

/** Hält die aktuell aktive {@link OutputRenderer}-Instanz (falls GUI-Modus aktiv). */
public final class OutputRendererRegistry {

    private static volatile OutputRenderer renderer;

    private OutputRendererRegistry() {}

    public static void register(OutputRenderer r) { renderer = r; }

    public static void unregister(OutputRenderer r) {
        if (renderer == r) renderer = null;
    }

    public static OutputRenderer get() { return renderer; }
}
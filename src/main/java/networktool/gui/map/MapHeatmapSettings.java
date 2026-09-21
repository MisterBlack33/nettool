package main.java.networktool.gui.map;

/** Schalter der Karten-Heatmap (Test-Suite-Menü "38"); wirkt beim nächsten Neuzeichnen der Karte. */
public final class MapHeatmapSettings {

    private static final class Holder { static final MapHeatmapSettings INSTANCE = new MapHeatmapSettings(); }
    public static MapHeatmapSettings getInstance() { return Holder.INSTANCE; }

    private volatile boolean enabled;

    private MapHeatmapSettings() {}

    public boolean isEnabled()          { return enabled; }
    public void    setEnabled(boolean on) { enabled = on; }

    /** @return neuer Zustand */
    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }
}

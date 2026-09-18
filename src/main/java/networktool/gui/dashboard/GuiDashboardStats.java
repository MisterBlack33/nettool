package main.java.networktool.gui.dashboard;

import main.java.networktool.gui.panels.tags.HostTagStore;
import main.java.networktool.logic.analysis.security.FindingsSourceRegistry;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.storage.network.NetworkStore;

/** Sammelt die Kennzahlen für {@link GuiDashboardPanel}. Keine Swing-Abhängigkeit. */
public final class GuiDashboardStats {

    private GuiDashboardStats() {}

    public record Snapshot(int hostCount, String lastScanLabel, int findingsCount, int favoriteCount) {}

    public static Snapshot capture() {
        int hosts = NetworkStore.getInstance().getAllHosts().size();
        String lastScan = ScanHistory.getInstance().getLast()
                .map(ScanHistory.Entry::display)
                .orElse("Noch kein Scan in dieser Session");
        int findings = FindingsSourceRegistry.getAll().size();
        int favorites = HostTagStore.getInstance().favoriteCount();
        return new Snapshot(hosts, lastScan, findings, favorites);
    }
}

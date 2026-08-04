package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.ScanDelta;
import main.java.networktool.logic.scan.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static networktool.theme.GuiTheme.*;

/**
 * Vergleicht Scan-Ergebnisse: CIDR-vs-CIDR, gespeichert-vs-aktuell
 * sowie Session-History (Menüpunkte "13" und "22").
 */
public final class GuiScanCompareActions {

    private GuiScanCompareActions() {}

    public static void handleScanDelta(GuiInputPanel input, GuiMenuHandler handler) {
        String[] options = {"Aktuell vs. letzten Scan", "Zwei CIDRs vergleichen"};
        int choice = JOptionPane.showOptionDialog(null, "Scan-Vergleich:", "Scan-Δ",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        if (choice == 1) {
            input.ask("CIDR A (alt):", cidrA ->
                    input.ask("CIDR B (neu):", cidrB -> handler.runAsync(() -> {
                        AuditLogger.getInstance().log("SCAN_DELTA", cidrA + " vs " + cidrB);
                        ScanDelta.compare(NetworkScanner.scanCIDR(cidrA),
                                NetworkScanner.scanCIDR(cidrB), cidrA, cidrB);
                    })));
        } else {
            input.ask("CIDR:", cidr -> handler.runAsync(() -> {
                AuditLogger.getInstance().log("SCAN_DELTA_LIVE", cidr);
                List<ScanResult> fresh = NetworkScanner.scanCIDR(cidr);
                List<HostResult> saved = NetworkStore.getInstance().getAllHosts();
                ScanDelta.compareHosts(saved, fresh.stream().map(r ->
                                new HostResult(r.getIp(), r.getHostname(), r.getOsGuess())).toList(),
                        "Gespeichert", "Aktuell");
            }));
        }
    }

    public static void handleScanHistoryDelta(GuiOutputPanel output, GuiMenuHandler handler) {
        ScanHistory hist = ScanHistory.getInstance();
        if (hist.size() == 0) {
            output.appendText("  Kein Scan in dieser Session.\n", FG_DIM);
            return;
        }
        String[] entries = new String[hist.size()];
        for (int i = 0; i < hist.size(); i++)
            entries[i] = hist.get(i).map(e -> e.display()).orElse("?");

        Object a = JOptionPane.showInputDialog(null, "Scan A (älter):", "Scan-Δ",
                JOptionPane.QUESTION_MESSAGE, null, entries, entries[Math.min(1, entries.length - 1)]);
        if (a == null) return;
        Object b = JOptionPane.showInputDialog(null, "Scan B (neuer):", "Scan-Δ",
                JOptionPane.QUESTION_MESSAGE, null, entries, entries[0]);
        if (b == null) return;
        int idxA = Arrays.asList(entries).indexOf(a.toString());
        int idxB = Arrays.asList(entries).indexOf(b.toString());
        var ea = hist.get(idxA);
        var eb = hist.get(idxB);
        if (ea.isEmpty() || eb.isEmpty()) return;
        AuditLogger.getInstance().log("SCAN_HISTORY_DELTA", idxA + " vs " + idxB);
        handler.runAsync(() -> ScanDelta.compare(eb.get().results, ea.get().results,
                eb.get().display(), ea.get().display()));
    }
}

package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.ScanOutcome;
import main.java.networktool.logic.scan.host.NetworkScanner;
import main.java.networktool.logic.scan.schedule.ScanDelta;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.network.NetworkStore;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

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
            input.ask("CIDR A (alt):", cidrA -> {
                if (!validateCidr(cidrA)) return;
                input.ask("CIDR B (neu):", cidrB -> {
                    if (!validateCidr(cidrB)) return;
                    handler.runAsync(() -> {
                        AuditLogger.getInstance().log("SCAN_DELTA", cidrA + " vs " + cidrB);
                        ScanDelta.compare(scan(cidrA), scan(cidrB), cidrA, cidrB);
                    });
                });
            });
        } else {
            input.ask("CIDR:", cidr -> {
                if (!validateCidr(cidr)) return;
                handler.runAsync(() -> {
                    AuditLogger.getInstance().log("SCAN_DELTA_LIVE", cidr);
                    List<ScanResult> fresh = scan(cidr);
                    List<HostResult> saved = NetworkStore.getInstance().getAllHosts();
                    ScanDelta.compareHosts(saved, fresh.stream().map(r ->
                                    new HostResult(r.getIp(), r.getHostname(), r.getOsGuess())).toList(),
                            "Gespeichert", "Aktuell");
                });
            });
        }
    }

    private static boolean validateCidr(String cidr) {
        String error = NetworkScanner.validateCidr(cidr);
        if (error == null) return true;
        JOptionPane.showMessageDialog(null, error, "Scanbereich ungültig", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private static List<ScanResult> scan(String cidr) throws InterruptedException {
        ScanOutcome<List<ScanResult>> outcome = NetworkScanner.scanCIDROutcome(cidr);
        if (outcome instanceof ScanOutcome.Success<List<ScanResult>> success) return success.value();
        String reason = ((ScanOutcome.Failure<List<ScanResult>>) outcome).reason();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException(reason);
        throw new IllegalStateException(reason);
    }

    public static void handleScanHistoryDelta(GuiOutputPanel output, GuiMenuHandler handler) {
        ScanHistory hist = ScanHistory.getInstance();
        if (hist.size() == 0) {
            output.appendText("  Keine gespeicherten Scans vorhanden.\n", FG_DIM);
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

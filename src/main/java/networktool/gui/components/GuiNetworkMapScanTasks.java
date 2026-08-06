package main.java.networktool.gui.components;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.gui.map.MapCanvas;
import main.java.networktool.gui.map.*;
import main.java.networktool.logic.scan.LastScanCache;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.SubnetDetector;
import main.java.networktool.model.ScanResult;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Hintergrund-Tasks der Netzwerk-Karte: initialer Quick-Scan des lokalen
 * Netzwerks und Hop-Discovery (Traceroute) inkl. Traffic-Probe.
 */
final class GuiNetworkMapScanTasks {

    private static final Logger LOG = Logger.getLogger(GuiNetworkMapScanTasks.class.getName());
    private static final AtomicBoolean scanRunning = new AtomicBoolean(false);

    private GuiNetworkMapScanTasks() {}

    static boolean isScanRunning() { return scanRunning.get(); }

    /**
     * Führt einen Quick Scan des lokalen Netzwerks durch
     * und aktualisiert LastScanCache mit den Ergebnissen.
     */
    static void startQuickLocalScan() {
        new Thread(() -> {
            try {
                List<String> cidrs = SubnetDetector.getAllCidrs();
                if (cidrs.isEmpty()) {
                    System.out.println("[GuiNetworkMap] Keine lokalen Netze erkannt");
                    return;
                }

                // Scanne das erste lokale Netzwerk (üblicherweise das Hauptnetzwerk)
                String mainCidr = cidrs.getFirst();
                System.out.println("[GuiNetworkMap] Starte Quick Scan für: " + mainCidr);

                List<ScanResult> results = NetworkScanner.scanCIDR(mainCidr);

                if (!results.isEmpty()) {
                    LastScanCache.updateFromScanResults(results);
                    System.out.println("[GuiNetworkMap] Quick Scan abgeschlossen: " + results.size() + " Hosts");

                    if (GUI.isGuiActive()) {
                        GUI.instance().setStatus(
                                "Netzwerk-Karte aktualisiert (" + results.size() + " Hosts)",
                                ACCENT2);
                    }
                }
            } catch (Exception e) {
                System.err.println("[GuiNetworkMap] Quick Scan Fehler: " + e.getMessage());
            }
        }, "GuiNetworkMapQuickScan").start();
    }

    static void startHopDiscovery(MapCanvas canvas, java.util.Map<String, String> hopParent) {
        new Thread(() -> {
            scanRunning.set(true);
            if (GUI.isGuiActive()) GUI.instance().setStatus("Hop-Analyse läuft…", ACCENT);
            try {
                canvas.setStatus("  Traceroute läuft…");
                hopParent.putAll(MapHopDiscovery.discover());
                SwingUtilities.invokeLater(() -> {
                    canvas.reload();
                    // Nach Hop-Discovery Traffic-Probe starten
                    canvas.runTrafficProbing();
                });
            } catch (Exception e) {
                LOG.log(Level.FINE, "Hop-Discovery fehlgeschlagen, Karte bleibt mit vorhandenen Daten nutzbar", e);
            } finally {
                scanRunning.set(false);
                if (GUI.isGuiActive()) GUI.instance().setStatus("Fertig", ACCENT2);
            }
        }, "MapHopDiscovery").start();
    }
}

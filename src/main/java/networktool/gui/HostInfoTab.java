package main.java.networktool.gui;

import main.java.networktool.logic.analysis.OsDetector;
import main.java.networktool.logic.analysis.OuiDatabase;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.net.InetAddress;

import static main.java.networktool.gui.GuiTheme.*;

/** Tab ①: Basis-Info (IP, Hostname, OS, MAC/OUI, Erreichbarkeit). */
final class HostInfoTab {

    private HostInfoTab() {}

    static JPanel build(String ip, String hostname, String os, Color panBg) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(panBg);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        HostResult stored = findStored(ip);
        addStaticRows(panel, ip, hostname, os, stored, panBg);
        addReachabilityRow(panel, ip, panBg);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static HostResult findStored(String ip) {
        return NetworkStore.getInstance().getAllHosts()
                .stream().filter(h -> h.ip.equals(ip)).findFirst().orElse(null);
    }

    private static void addStaticRows(JPanel panel, String ip, String hostname,
                                      String os, HostResult stored, Color panBg) {
        String hn       = stored != null && stored.hostname != null ? stored.hostname : hostname;
        String storedOs = stored != null && stored.os != null       ? stored.os       : os;
        String savedAt  = stored != null && stored.savedAt != null  ? stored.savedAt  : "–";

        OsDetector.OsResult osResult = OsDetector.detectWithConfidence(ip);
        String confLabel = osResult.confidence.name() + "  [" + osResult.method + "]";

        String mac = OsDetector.getMacFromArp(ip);
        String oui = mac != null ? OuiDatabase.lookup(mac) : null;
        String macStr = mac != null
                ? mac + (oui != null ? "  →  " + oui : "  (unbekannt)")
                : "nicht im ARP-Cache";

        String cleanHn = hn != null && hn.contains(" [")
                ? hn.substring(0, hn.indexOf(" [")).trim() : hn;

        HostDetailRows.addRow(panel, "IP-Adresse",     ip,          ACCENT,            panBg);
        HostDetailRows.addRow(panel, "Hostname",       cleanHn,     FG,                panBg);
        HostDetailRows.addRow(panel, "OS (gespeich.)", storedOs,    osColor(storedOs), panBg);
        HostDetailRows.addRow(panel, "OS (aktuell)",   osResult.os, osColor(osResult.os), panBg);
        HostDetailRows.addRow(panel, "OS-Konfidenz",   confLabel,   FG_DIM,            panBg);
        HostDetailRows.addRow(panel, "MAC / OUI",      macStr,      INFO,              panBg);
        HostDetailRows.addRow(panel, "Gespeichert am", savedAt,     FG_DIM,            panBg);
    }

    private static void addReachabilityRow(JPanel panel, String ip, Color panBg) {
        JLabel reachLbl = HostDetailRows.label("Prüfe...", FG_DIM);
        HostDetailRows.addRowWithLabel(panel, "Erreichbar", reachLbl, panBg);

        new Thread(() -> {
            try {
                long start    = System.currentTimeMillis();
                boolean alive = InetAddress.getByName(ip).isReachable(2000);
                long ms       = System.currentTimeMillis() - start;
                SwingUtilities.invokeLater(() -> {
                    reachLbl.setText(alive ? "✔ ja  (" + ms + " ms)" : "✕ nein");
                    reachLbl.setForeground(alive ? ACCENT2 : WARN);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    reachLbl.setText("Fehler");
                    reachLbl.setForeground(WARN);
                });
            }
        }, "HostInfoTab-Reachability").start();
    }
}
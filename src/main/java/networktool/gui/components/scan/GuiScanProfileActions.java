package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.host.NetworkInfo;
import main.java.networktool.logic.scan.host.NetworkScanner;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.profile.ScanProfileStore;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Anlegen, Ausführen und Löschen von Scan-Profilen (Menüpunkt "12").
 */
public final class GuiScanProfileActions {

    private GuiScanProfileActions() {}

    public static void handleScanProfiles(GuiInputPanel input, GuiOutputPanel output,
                                          GuiTableRenderer tables, GuiStatusBar status,
                                          GuiMenuHandler handler) {
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        String[] actions = {"Ausführen", "Neu anlegen", "Löschen"};
        int action = JOptionPane.showOptionDialog(null,
                profiles.isEmpty() ? "Keine Profile vorhanden." : profiles.size() + " Profile",
                "Scan-Profile", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, actions, actions[0]);
        if (action < 0) return;
        switch (action) {
            case 0 -> {
                if (profiles.isEmpty()) {
                    output.appendText("  ✕ Keine Profile.\n", WARN);
                    return;
                }
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Ausführen",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen == null) return;
                AuditLogger.getInstance().log("PROFILE_RUN", chosen.toString());
                ScanProfileStore.getInstance().get(chosen.toString())
                        .ifPresent(p -> handler.runAsync(() -> runProfile(p, status, tables)));
            }
            case 1 -> buildNewProfile(input, output);
            case 2 -> {
                if (profiles.isEmpty()) return;
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Löschen:", "Profil löschen",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen != null) {
                    AuditLogger.getInstance().log("PROFILE_DELETE", chosen.toString());
                    ScanProfileStore.getInstance().delete(chosen.toString());
                    output.appendText("  ✔ Gelöscht: " + chosen + "\n", ACCENT2);
                }
            }
        }
    }

    private static void buildNewProfile(GuiInputPanel input, GuiOutputPanel output) {
        input.ask("Profilname:", name -> {
            if (name.isBlank()) return;
            ScanProfile p = new ScanProfile(name.trim());
            input.ask("CIDRs (leer = lokal):", cidrs -> {
                if (!cidrs.isBlank())
                    Arrays.stream(cidrs.split(",")).map(String::trim)
                            .filter(s -> !s.isBlank()).forEach(p.cidrs::add);
                input.ask("OS-Filter (leer = alle):", os -> {
                    p.osFilter = os.trim();
                    input.ask("Hostname-Filter (leer = alle):", hn -> {
                        p.hnFilter = hn.trim();
                        input.ask("Auto-Save Kategorie (leer = nein):", cat -> {
                            if (!cat.isBlank()) { p.autoSave = true; p.category = cat.trim(); }
                            ScanProfileStore.getInstance().save(p);
                            AuditLogger.getInstance().log("PROFILE_CREATE", p.summary());
                            output.appendText("  ✔ Profil gespeichert: " + p.name + "\n", ACCENT2);
                        });
                    });
                });
            });
        });
    }

    private static void runProfile(ScanProfile profile, GuiStatusBar status,
                                    GuiTableRenderer tables) throws Exception {
        status.set("Profil: " + profile.name, ACCENT);
        if (!profile.ports.isEmpty()) PortScanner.setActivePorts(profile.ports);
        if (profile.cidrs.isEmpty()) {
            NetworkInfo.showMinimalInfo();
        } else {
            List<ScanResult> all = new ArrayList<>();
            for (String cidr : profile.cidrs) {
                if (Thread.currentThread().isInterrupted()) break;
                all.addAll(NetworkScanner.scanCIDR(cidr));
            }
            tables.showScanTable(all);
        }
        if (!profile.ports.isEmpty()) PortScanner.setActivePorts(null);
    }

}

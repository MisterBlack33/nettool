package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.filter.ScanFilter;
import main.java.networktool.logic.ScanOutcome;
import main.java.networktool.logic.scan.host.NetworkInfo;
import main.java.networktool.logic.scan.host.NetworkScanner;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.network.NetworkStore;
import main.java.networktool.util.IpValidator;
import main.java.networktool.storage.profile.ScanProfileStore;
import main.java.networktool.util.StatusTags;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
            case 0 -> runExistingProfile(profiles, status, tables, handler, output);
            case 1 -> buildNewProfile(input, output);
            case 2 -> deleteProfile(profiles, output);
        }
    }

    private static void runExistingProfile(List<ScanProfile> profiles, GuiStatusBar status,
                                           GuiTableRenderer tables, GuiMenuHandler handler,
                                           GuiOutputPanel output) {
        if (profiles.isEmpty()) {
            output.appendText("  " + StatusTags.FEHLER + " Keine Profile.\n", WARN);
            return;
        }
        String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
        Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Ausführen",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (chosen == null) return;
        ScanProfile profile = ScanProfileStore.getInstance().get(chosen.toString()).orElse(null);
        if (profile == null) return;
        String preview = preview(profile);
        if (preview == null) {
            output.appendText("  " + StatusTags.FEHLER
                    + " Profil enthält ungültige Ziele, Ports oder Filter.\n", WARN);
            return;
        }
        if (JOptionPane.showConfirmDialog(null, preview + "\n\nScan starten?",
                "Scan-Vorschau", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        AuditLogger.getInstance().log("PROFILE_RUN", chosen.toString());
        handler.runAsync(() -> runProfile(profile, status, tables, output));
    }

    private static void deleteProfile(List<ScanProfile> profiles, GuiOutputPanel output) {
        if (profiles.isEmpty()) return;
        String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
        Object chosen = JOptionPane.showInputDialog(null, "Löschen:", "Profil löschen",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (chosen == null) return;
        AuditLogger.getInstance().log("PROFILE_DELETE", chosen.toString());
        ScanProfileStore.getInstance().delete(chosen.toString());
        output.appendText("  " + StatusTags.OK + " Gelöscht: " + chosen + "\n", ACCENT2);
    }

    private static void buildNewProfile(GuiInputPanel input, GuiOutputPanel output) {
        input.ask("Profilname:", name -> {
            if (name.isBlank()) return;
            ScanProfile p = new ScanProfile(name.trim());
            input.ask("CIDRs (leer = lokal):", cidrs -> {
                if (!cidrs.isBlank()) {
                    List<String> targets = Arrays.stream(cidrs.split(",")).map(String::trim)
                            .filter(s -> !s.isBlank()).toList();
                    if (!validTargets(targets)) {
                        output.appendText("  " + StatusTags.FEHLER
                                + " Ungültige oder zu große Ziele; Profil nicht gespeichert.\n", WARN);
                        return;
                    }
                    p.cidrs.addAll(targets);
                }
                input.ask("OS-Filter (leer = alle):", os -> {
                    p.osFilter = os.trim();
                    input.ask("Hostname-Filter (leer = alle):", hn -> {
                        p.hnFilter = hn.trim();
                        input.ask("Auto-Save Kategorie (leer = nein):", cat -> {
                            if (!cat.isBlank()) { p.autoSave = true; p.category = cat.trim(); }
                            ScanProfileStore.getInstance().save(p);
                            AuditLogger.getInstance().log("PROFILE_CREATE", p.summary());
                            output.appendText("  " + StatusTags.OK + " Profil gespeichert: " + p.name + "\n", ACCENT2);
                        });
                    });
                });
            });
        });
    }

    private static void runProfile(ScanProfile profile, GuiStatusBar status,
                                   GuiTableRenderer tables, GuiOutputPanel output) throws Exception {
        status.set("Profil: " + profile.name, ACCENT);
        if (profile.cidrs.isEmpty()) {
            List<HostResult> hosts = NetworkInfo.scanWithFilterResults(profile.osFilter, profile.hnFilter);
            if (profile.autoSave && !profile.category.isBlank()) {
                for (HostResult host : hosts)
                    NetworkStore.getInstance().save(host, profile.category);
            }
        } else {
            List<ScanResult> all = new ArrayList<>();
            for (String cidr : profile.cidrs) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                ScanOutcome<List<ScanResult>> outcome = NetworkScanner.scanCIDROutcome(
                        cidr, profile.ports.isEmpty() ? null : profile.ports);
                if (outcome instanceof ScanOutcome.Success<List<ScanResult>> success) {
                    all.addAll(success.value());
                } else {
                    String reason = ((ScanOutcome.Failure<List<ScanResult>>) outcome).reason();
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException(reason);
                    output.appendText("Profil-Scan fehlgeschlagen: " + reason + "\n", WARN);
                    return;
                }
            }
            List<ScanResult> filtered = applyProfileFilters(all, profile);
            tables.showScanTable(filtered);
            if (profile.autoSave && !profile.category.isBlank()) {
                for (ScanResult result : filtered) {
                    NetworkStore.getInstance().save(new HostResult(result.getIp(), result.getHostname(),
                            result.getOsGuess(), null, result.getOpenPorts(), ""), profile.category);
                }
            }
        }
    }

    private static List<ScanResult> applyProfileFilters(List<ScanResult> results, ScanProfile profile) {
        List<ScanResult> filtered = results;
        if (!profile.osFilter.isBlank())
            filtered = ScanFilter.filterByOS(filtered, profile.osFilter);
        if (!profile.hnFilter.isBlank())
            filtered = ScanFilter.filterByHostnameRegex(filtered, profile.hnFilter);
        return filtered;
    }

    static String preview(ScanProfile profile) {
        if (profile.ports.stream().anyMatch(port -> port == null || port < 1 || port > 65_535))
            return null;
        try {
            if (!profile.hnFilter.isBlank()) Pattern.compile(profile.hnFilter);
        } catch (PatternSyntaxException e) {
            return null;
        }
        if (profile.cidrs.isEmpty()) return "Ziel: lokales Netzwerk\nScanbereich: lokale Erkennung";
        if (!validTargets(profile.cidrs)) return null;
        long total = profile.cidrs.stream().mapToLong(NetworkScanner::estimatedHostCount).sum();
        return "Ziel-CIDRs: " + String.join(", ", profile.cidrs)
                + "\nGeschätzte Hosts: " + total
                + "\nAktive Ports: " + (profile.cidrs.isEmpty() ? "lokale Erkennung"
                : profile.ports.isEmpty() ? "Standard" : profile.ports.size())
                + "\nOS-Filter: " + (profile.osFilter.isBlank() ? "keiner" : profile.osFilter)
                + "\nHostname-Filter: " + (profile.hnFilter.isBlank() ? "keiner" : profile.hnFilter)
                + "\nAuto-Save: " + (profile.autoSave ? profile.category : "aus");
    }

    private static boolean validTargets(List<String> cidrs) {
        long total = 0;
        for (String cidr : cidrs) {
            if (!IpValidator.isValidCidr(cidr)) return false;
            total += NetworkScanner.estimatedHostCount(cidr);
            if (total > NetworkScanner.MAX_SCAN_HOSTS) return false;
        }
        return true;
    }
}
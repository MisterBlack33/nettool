package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.schedule.ScanScheduler;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.profile.ScanProfileStore;

import javax.swing.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Planen, Stoppen und Anzeigen wiederkehrender Scans (Menüpunkt "14").
 */
public final class GuiSchedulerActions {

    private static final Logger LOG = Logger.getLogger(GuiSchedulerActions.class.getName());

    private GuiSchedulerActions() {}

    public static void handleScheduler(GuiInputPanel input, GuiOutputPanel output,
                                       GuiStatusBar status) {
        ScanScheduler sched = ScanScheduler.getInstance();
        List<ScanProfile> profiles = ScanProfileStore.getInstance().getAll();
        String running = sched.getRunning().isEmpty() ? "–" : String.join(", ", sched.getRunning());
        status.set("Scheduler: " + running, sched.getRunning().isEmpty() ? FG_DIM : ACCENT2);

        String[] actions = {"Planen", "Stoppen", "Alle stoppen"};
        int action = JOptionPane.showOptionDialog(null,
                "Aktive Scans: " + running, "Scheduler",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, actions, actions[0]);
        if (action < 0) return;
        switch (action) {
            case 0 -> {
                if (profiles.isEmpty()) {
                    output.appendText("  ✕ Zuerst Scan-Profil anlegen.\n", WARN);
                    return;
                }
                String[] names = profiles.stream().map(p -> p.name).toArray(String[]::new);
                Object chosen = JOptionPane.showInputDialog(null, "Profil:", "Scheduler",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (chosen == null) return;
                input.ask("Intervall (min):", minStr -> {
                    try {
                        int min = Integer.parseInt(minStr.trim());
                        String topic = GuiContextMenu.promptNtfyTopic();
                        if (topic == null) topic = "";
                        final String t = topic;
                        AuditLogger.getInstance().log("SCHEDULER_START", chosen + " every=" + min + "min");
                        sched.start(chosen.toString(), min, t);
                        output.appendText("  ✔ " + chosen + " alle " + min + " min\n", ACCENT2);
                    } catch (NumberFormatException e) {
                        LOG.log(Level.FINE, "Ungültiges Scheduler-Intervall \"" + minStr + "\"", e);
                        output.appendText("  ✕ Ungültige Zahl\n", WARN);
                    }
                });
            }
            case 1 -> {
                if (sched.getRunning().isEmpty()) return;
                String[] r = sched.getRunning().toArray(new String[0]);
                Object chosen = JOptionPane.showInputDialog(null, "Stoppen:", "Stoppen",
                        JOptionPane.QUESTION_MESSAGE, null, r, r[0]);
                if (chosen != null) {
                    AuditLogger.getInstance().log("SCHEDULER_STOP", chosen.toString());
                    sched.stop(chosen.toString());
                }
            }
            case 2 -> {
                AuditLogger.getInstance().log("SCHEDULER_STOP_ALL", "");
                sched.stopAll();
                output.appendText("  ✔ Alle gestoppt\n", ACCENT2);
            }
        }
    }
}

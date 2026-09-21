package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.schedule.WolSchedule;
import main.java.networktool.logic.scan.schedule.WolScheduler;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.PlatformSupport;
import main.java.networktool.util.StatusTags;

import javax.swing.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static main.java.networktool.theme.GuiTheme.ACCENT2;
import static main.java.networktool.theme.GuiTheme.WARN;

/** Test-Suite-Aktion "WoL-Scheduler" (Menü-ID "34"): tägliches Wake-on-LAN. */
public final class GuiWolSchedulerActions {

    static final String DEFAULT_BROADCAST = "255.255.255.255";
    private static final String[] ACTIONS = {"Planen", "Stoppen", "Alle stoppen"};

    private GuiWolSchedulerActions() {}

    public static void handle(GuiInputPanel input, GuiOutputPanel output) {
        WolScheduler scheduler = WolScheduler.getInstance();
        String running = scheduler.getRunning().isEmpty() ? "–" : String.join(", ", scheduler.getRunning());
        int action = JOptionPane.showOptionDialog(null, "Aktive Zeitpläne: " + running, "WoL-Scheduler",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, ACTIONS, ACTIONS[0]);
        switch (action) {
            case 0 -> plan(input, output, scheduler);
            case 1 -> stopOne(scheduler);
            case 2 -> stopAll(output, scheduler);
            default -> { }
        }
    }

    static Optional<WolSchedule> parseSchedule(String mac, String time, String broadcast) {
        String cleanMac = mac.trim();
        String cleanBroadcast = broadcast.isBlank() ? DEFAULT_BROADCAST : broadcast.trim();
        if (!PlatformSupport.isSafeMac(cleanMac) || !PlatformSupport.isSafeIp(cleanBroadcast)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new WolSchedule(cleanMac, cleanBroadcast, LocalTime.parse(time.trim())));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static void plan(GuiInputPanel input, GuiOutputPanel output, WolScheduler scheduler) {
        input.ask("MAC-Adresse:", mac ->
                input.ask("Uhrzeit täglich (HH:mm):", time ->
                        input.ask("Broadcast (leer = " + DEFAULT_BROADCAST + "):", broadcast ->
                                start(output, scheduler, parseSchedule(mac, time, broadcast)))));
    }

    private static void start(GuiOutputPanel output, WolScheduler scheduler, Optional<WolSchedule> parsed) {
        if (parsed.isEmpty()) {
            output.appendText("  " + StatusTags.FEHLER + " Ungültige Eingabe (MAC, HH:mm, Broadcast)\n", WARN);
            return;
        }
        WolSchedule schedule = parsed.get();
        scheduler.start(schedule.mac(), schedule);
        AuditLogger.getInstance().log("WOL_SCHEDULE_START", schedule.mac() + " " + schedule.time());
        output.appendText("  " + StatusTags.OK + " WoL " + schedule.mac()
                + " täglich " + schedule.time() + "\n", ACCENT2);
    }

    private static void stopOne(WolScheduler scheduler) {
        if (scheduler.getRunning().isEmpty()) return;
        String[] names = scheduler.getRunning().toArray(new String[0]);
        Object chosen = JOptionPane.showInputDialog(null, "Stoppen:", "WoL-Scheduler",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (chosen == null) return;
        AuditLogger.getInstance().log("WOL_SCHEDULE_STOP", chosen.toString());
        scheduler.stop(chosen.toString());
    }

    private static void stopAll(GuiOutputPanel output, WolScheduler scheduler) {
        AuditLogger.getInstance().log("WOL_SCHEDULE_STOP_ALL", "");
        scheduler.stopAll();
        output.appendText("  " + StatusTags.OK + " Alle WoL-Zeitpläne gestoppt\n", ACCENT2);
    }
}

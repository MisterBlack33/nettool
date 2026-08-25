package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.sonify.SonifyConfig;
import main.java.networktool.logic.sonify.SonifyConfigStore;
import main.java.networktool.logic.sonify.TrafficSonifier;
import main.java.networktool.security.AuditLogger;

import static main.java.networktool.theme.GuiTheme.*;

/** Sidebar-Aktion für den Netzwerk-Sonifier inkl. Ton-Einstellungen (Menü-ID "24"). */
public final class GuiSonifyActions {

    private GuiSonifyActions() {}

    public static void toggle(GuiInputPanel input, GuiOutputPanel output) {
        TrafficSonifier sonifier = TrafficSonifier.getInstance();
        if (sonifier.isActive()) {
            sonifier.stop();
            AuditLogger.getInstance().log("SONIFY_STOP", "");
            output.appendText("  ⏹ Sonify gestoppt\n", WARN);
            return;
        }
        input.ask("Interface (z.B. eth0, leer = eth0):", iface ->
                askConfig(input, output, iface.isBlank() ? "eth0" : iface.trim()));
    }

    private static void askConfig(GuiInputPanel input, GuiOutputPanel output, String iface) {
        SonifyConfig cfg = SonifyConfigStore.load();
        input.ask("Hoher Ton in Hz (Standard " + cfg.highHz + "):", h ->
                input.ask("Tiefer Ton in Hz (Standard " + cfg.lowHz + "):", l ->
                        input.ask("Tonlänge in ms, kurz/lang (Standard " + cfg.toneMs + "):", t ->
                                startWithConfig(output, iface, cfg, h, l, t))));
    }

    private static void startWithConfig(GuiOutputPanel output, String iface, SonifyConfig cfg,
                                        String h, String l, String t) {
        cfg.highHz = parseOr(h, cfg.highHz);
        cfg.lowHz  = parseOr(l, cfg.lowHz);
        cfg.toneMs = parseOr(t, cfg.toneMs);
        SonifyConfigStore.save(cfg);

        TrafficSonifier sonifier = TrafficSonifier.getInstance();
        sonifier.setConfig(cfg);
        sonifier.start(iface);
        AuditLogger.getInstance().log("SONIFY_START", iface + " " + cfg.highHz + "/" + cfg.lowHz + "Hz");
        output.appendText("  🎵 Sonify aktiv auf \"" + iface + "\"  ("
                + cfg.highHz + "Hz / " + cfg.lowHz + "Hz)\n", ACCENT2);
    }

    private static int parseOr(String s, int fallback) {
        try { return s.isBlank() ? fallback : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
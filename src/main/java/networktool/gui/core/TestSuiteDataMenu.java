package main.java.networktool.gui.core;

import main.java.networktool.gui.components.scan.GuiSonifyActions;
import main.java.networktool.gui.components.scan.GuiTrafficSpectrogramActions;
import main.java.networktool.gui.components.scan.GuiTrafficVisualizerActions;
import main.java.networktool.gui.dashboard.GuiDashboardPanel;
import main.java.networktool.gui.panels.bandwidth.GuiBandwidthHistoryPanel;
import main.java.networktool.gui.panels.security.GuiSecurityFindingsPanel;
import main.java.networktool.security.AuditLogger;

/** Test-Suite "Data": Sonify, Visualizer, Findings, Bandbreiten-Verlauf, Dashboard (IDs 24–29). */
final class TestSuiteDataMenu {

    private TestSuiteDataMenu() {}

    static void register(GuiMenuRegistry registry, GuiMenuContext ctx) {
        registry.register("24", () -> GuiSonifyActions.toggle(ctx.input(), ctx.output()));
        registry.register("25", () -> GuiTrafficVisualizerActions.toggle(ctx.input(), ctx.output()));
        registry.register("26", () -> GuiTrafficSpectrogramActions.toggle(ctx.input(), ctx.output()));
        registry.register("27", () -> {
            AuditLogger.getInstance().log("SECURITY_FINDINGS", "");
            GuiSecurityFindingsPanel.show(ctx.output());
        });
        registry.register("28", () -> ctx.input().ask("Ziel-IP:", ip -> ctx.handler().runAsync(() -> {
            AuditLogger.getInstance().log("BANDWIDTH_HISTORY", ip);
            GuiBandwidthHistoryPanel.show(ctx.output(), ip.trim());
        })));
        registry.register("29", () -> {
            AuditLogger.getInstance().log("DASHBOARD", "");
            GuiDashboardPanel.show(ctx.output());
        });
    }
}

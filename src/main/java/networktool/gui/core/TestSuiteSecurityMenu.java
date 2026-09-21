package main.java.networktool.gui.core;

import main.java.networktool.gui.components.scan.GuiSecurityAutomationActions;

/** Test-Suite "Sicherheit & Compliance" (IDs 35–37). */
final class TestSuiteSecurityMenu {

    private TestSuiteSecurityMenu() {}

    static void register(GuiMenuRegistry registry, GuiMenuContext ctx) {
        registry.register("35", () -> GuiSecurityAutomationActions.toggleRogueDhcp(ctx.input(), ctx.output()));
        registry.register("36", () -> GuiSecurityAutomationActions.toggleScanHook(ctx.output()));
        registry.register("37", () -> GuiSecurityAutomationActions.toggleTlsScheduler(ctx.input(), ctx.output()));
    }
}

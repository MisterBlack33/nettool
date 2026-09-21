package main.java.networktool.gui.core;

import main.java.networktool.gui.components.actions.GuiBackupActions;
import main.java.networktool.gui.components.actions.GuiOfflineMonitorActions;
import main.java.networktool.gui.components.actions.GuiTagFilterActions;
import main.java.networktool.gui.components.actions.GuiWebhookActions;

/** Test-Suite "Automatisierung & Integration" (IDs 42–45). */
final class TestSuiteAutomationMenu {

    private TestSuiteAutomationMenu() {}

    static void register(GuiMenuRegistry registry, GuiMenuContext ctx) {
        registry.register("42", () -> GuiTagFilterActions.handle(ctx.input(), ctx.tables()));
        registry.register("43", () -> GuiWebhookActions.handle(ctx.input(), ctx.output(), ctx.handler()));
        registry.register("44", () -> GuiOfflineMonitorActions.handle(ctx.input(), ctx.output()));
        registry.register("45", () -> GuiBackupActions.handle(ctx.input(), ctx.output(), ctx.handler()));
    }
}

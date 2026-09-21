package main.java.networktool.gui.core;

import main.java.networktool.gui.components.scan.GuiArpSnifferActions;
import main.java.networktool.gui.components.scan.GuiIpv6ScanActions;
import main.java.networktool.gui.components.scan.GuiSnmpActions;
import main.java.networktool.gui.components.scan.GuiWolSchedulerActions;

/** Test-Suite "Scanning & Discovery" (IDs 31–34). */
final class TestSuiteScanningMenu {

    private TestSuiteScanningMenu() {}

    static void register(GuiMenuRegistry registry, GuiMenuContext ctx) {
        registry.register("31", () -> GuiSnmpActions.handleWalk(ctx.input(), ctx.output(), ctx.handler()));
        registry.register("32", () -> GuiArpSnifferActions.toggle(ctx.output()));
        registry.register("33", () -> GuiIpv6ScanActions.handleScan(ctx.input(), ctx.output(), ctx.handler()));
        registry.register("34", () -> GuiWolSchedulerActions.handle(ctx.input(), ctx.output()));
    }
}

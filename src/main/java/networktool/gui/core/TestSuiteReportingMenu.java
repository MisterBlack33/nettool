package main.java.networktool.gui.core;

import main.java.networktool.gui.components.scan.GuiMapExportActions;
import main.java.networktool.gui.components.scan.GuiMapHeatmapActions;
import main.java.networktool.gui.components.scan.GuiPdfExportActions;
import main.java.networktool.gui.components.scan.GuiScanTimelineActions;

/** Test-Suite "Visualisierung & Reporting" (IDs 38–41). */
final class TestSuiteReportingMenu {

    private TestSuiteReportingMenu() {}

    static void register(GuiMenuRegistry registry, GuiMenuContext ctx) {
        registry.register("38", () -> GuiMapHeatmapActions.toggle(ctx.output()));
        registry.register("39", () -> GuiMapExportActions.handle(ctx.output(), ctx.handler()));
        registry.register("40", () -> GuiPdfExportActions.handle(ctx.output(), ctx.handler()));
        registry.register("41", () -> GuiScanTimelineActions.show(ctx.output()));
    }
}

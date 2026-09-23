package main.java.networktool.gui.core;

/**
 * Registriert alle Test-Suite-Menüpunkte (IDs 24–29, 31–46), gruppiert nach Thema.
 * Neue user-invoked Features starten hier statt im Produktiv-Menü (siehe Clean-Code-Guide).
 */
final class GuiTestSuiteMenus {

    private GuiTestSuiteMenus() {}

    static void registerAll(GuiMenuRegistry registry, GuiMenuContext ctx) {
        TestSuiteDataMenu.register(registry, ctx);
        TestSuiteScanningMenu.register(registry, ctx);
        TestSuiteSecurityMenu.register(registry, ctx);
        TestSuiteReportingMenu.register(registry, ctx);
        TestSuiteAutomationMenu.register(registry, ctx);
    }
}

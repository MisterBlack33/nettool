package main.java.networktool.theme;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class GuiThemeTest {
    private boolean wasDark;
    @BeforeEach void captureState() { wasDark = GuiTheme.isDark(); }
    @AfterEach void restoreState() { if (GuiTheme.isDark() != wasDark) GuiTheme.toggleTheme(); }
    @Test void getters_returnNonNullColors() {
        assertNotNull(GuiTheme.bg()); assertNotNull(GuiTheme.panelBg()); assertNotNull(GuiTheme.sidebarBg());
        assertNotNull(GuiTheme.btnBg()); assertNotNull(GuiTheme.btnHov()); assertNotNull(GuiTheme.border());
        assertNotNull(GuiTheme.borderLt()); assertNotNull(GuiTheme.fg()); assertNotNull(GuiTheme.fgDim());
        assertNotNull(GuiTheme.rowEven()); assertNotNull(GuiTheme.rowOdd()); assertNotNull(GuiTheme.rowSel());
    }
    @Test void toggleTheme_flipsAndUpdatesStatics() {
        boolean before = GuiTheme.isDark(); GuiTheme.toggleTheme();
        assertNotEquals(before, GuiTheme.isDark()); assertEquals(GuiTheme.bg(), GuiTheme.BG);
    }
    @Test void themeName_matchesIsDark() { assertEquals(GuiTheme.isDark() ? "Dunkel" : "Hell", GuiTheme.themeName()); }
    @Test void applyToStatics_doesNotThrow() { assertDoesNotThrow(GuiTheme::applyToStatics); }
    @Test void osColor_coversCategoriesAndFallback() {
        assertEquals(GuiTheme.WIN_COL, GuiTheme.osColor("Windows 11"));
        assertEquals(GuiTheme.APL_COL, GuiTheme.osColor("macOS"));
        assertEquals(GuiTheme.IOS_COL, GuiTheme.osColor("iPad"));
        assertEquals(GuiTheme.RPI_COL, GuiTheme.osColor("Raspberry Pi"));
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android Samsung"));
        assertEquals(GuiTheme.LIN_COL, GuiTheme.osColor("Linux/Unix"));
        assertEquals(GuiTheme.NET_COL, GuiTheme.osColor("Router"));
        assertEquals(GuiTheme.PRN_COL, GuiTheme.osColor("Printer"));
        assertEquals(GuiTheme.IOT_COL, GuiTheme.osColor("MQTT"));
        assertEquals(GuiTheme.FG_DIM, GuiTheme.osColor("Unbekannt"));
        assertEquals(GuiTheme.FG, GuiTheme.osColor("Xbox"));
        assertEquals(GuiTheme.FG, GuiTheme.osColor(null));
    }
    @Test void brighter_clampsAndPreservesFactorOne() {
        java.awt.Color result = GuiTheme.brighter(new java.awt.Color(200, 100, 50), 2f);
        assertEquals(new java.awt.Color(255, 200, 100), result);
        assertEquals(new java.awt.Color(10, 20, 30), GuiTheme.brighter(new java.awt.Color(10, 20, 30), 1f));
    }
}

package main.java.networktool.theme;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiThemePaletteTest {
    @Test void dark_singleton_isStable() { assertSame(GuiThemeDark.INSTANCE, GuiThemeDark.INSTANCE); }
    @Test void light_singleton_isStable() { assertSame(GuiThemeLight.INSTANCE, GuiThemeLight.INSTANCE); }
    @Test void dark_allChannelsNonNull() { assertPalette(GuiThemeDark.INSTANCE); }
    @Test void light_allChannelsNonNull() { assertPalette(GuiThemeLight.INSTANCE); }
    @Test void dark_and_light_backgroundsDiffer() { assertNotEquals(GuiThemeDark.INSTANCE.bg(), GuiThemeLight.INSTANCE.bg()); }
    private static void assertPalette(GuiColorPalette p) {
        assertNotNull(p.bg()); assertNotNull(p.panelBg()); assertNotNull(p.sidebarBg());
        assertNotNull(p.btnBg()); assertNotNull(p.btnHov()); assertNotNull(p.border());
        assertNotNull(p.borderLt()); assertNotNull(p.fg()); assertNotNull(p.fgDim());
        assertNotNull(p.rowEven()); assertNotNull(p.rowOdd()); assertNotNull(p.rowSel());
        assertNotNull(p.winCol()); assertNotNull(p.linCol()); assertNotNull(p.aplCol());
        assertNotNull(p.iosCol()); assertNotNull(p.andCol()); assertNotNull(p.netCol());
        assertNotNull(p.prnCol()); assertNotNull(p.iotCol()); assertNotNull(p.rpiCol());
    }
}

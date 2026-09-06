package main.java.networktool.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiThemePaletteImplTest {

    @Test void dark_singleton_sameInstance() {
        assertSame(GuiThemeDark.INSTANCE, GuiThemeDark.INSTANCE);
    }

    @Test void light_singleton_sameInstance() {
        assertSame(GuiThemeLight.INSTANCE, GuiThemeLight.INSTANCE);
    }

    @Test void dark_allColors_nonNull() {
        GuiColorPalette p = GuiThemeDark.INSTANCE;
        assertAllNonNull(p);
    }

    @Test void light_allColors_nonNull() {
        GuiColorPalette p = GuiThemeLight.INSTANCE;
        assertAllNonNull(p);
    }

    @Test void dark_and_light_bg_differ() {
        assertNotEquals(GuiThemeDark.INSTANCE.bg(), GuiThemeLight.INSTANCE.bg());
    }

    @Test void dark_and_light_fg_differ() {
        assertNotEquals(GuiThemeDark.INSTANCE.fg(), GuiThemeLight.INSTANCE.fg());
    }

    @Test void dark_and_light_osColors_differ() {
        assertNotEquals(GuiThemeDark.INSTANCE.winCol(), GuiThemeLight.INSTANCE.winCol());
        assertNotEquals(GuiThemeDark.INSTANCE.linCol(), GuiThemeLight.INSTANCE.linCol());
        assertNotEquals(GuiThemeDark.INSTANCE.aplCol(), GuiThemeLight.INSTANCE.aplCol());
        assertNotEquals(GuiThemeDark.INSTANCE.rpiCol(), GuiThemeLight.INSTANCE.rpiCol());
    }

    private void assertAllNonNull(GuiColorPalette p) {
        assertNotNull(p.bg());
        assertNotNull(p.panelBg());
        assertNotNull(p.sidebarBg());
        assertNotNull(p.btnBg());
        assertNotNull(p.btnHov());
        assertNotNull(p.border());
        assertNotNull(p.borderLt());
        assertNotNull(p.fg());
        assertNotNull(p.fgDim());
        assertNotNull(p.rowEven());
        assertNotNull(p.rowOdd());
        assertNotNull(p.rowSel());
        assertNotNull(p.winCol());
        assertNotNull(p.linCol());
        assertNotNull(p.aplCol());
        assertNotNull(p.iosCol());
        assertNotNull(p.andCol());
        assertNotNull(p.netCol());
        assertNotNull(p.prnCol());
        assertNotNull(p.iotCol());
        assertNotNull(p.rpiCol());
    }
}
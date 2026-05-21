package main.java.networktool_v3.gui;

import org.junit.jupiter.api.*;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class GuiThemePackageTest {

    @BeforeEach
    void resetDark() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
    }

    @AfterEach
    void resetAfter() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
    }

    // ── Dynamic accessors both modes ──────────────────────────────

    @Test
    void darkMode_allAccessors_nonNull() {
        assertNotNull(GuiTheme.bg());
        assertNotNull(GuiTheme.panelBg());
        assertNotNull(GuiTheme.sidebarBg());
        assertNotNull(GuiTheme.btnBg());
        assertNotNull(GuiTheme.btnHov());
        assertNotNull(GuiTheme.border());
        assertNotNull(GuiTheme.borderLt());
        assertNotNull(GuiTheme.fg());
        assertNotNull(GuiTheme.fgDim());
        assertNotNull(GuiTheme.rowEven());
        assertNotNull(GuiTheme.rowOdd());
    }

    @Test
    void lightMode_allAccessors_nonNull() {
        GuiTheme.toggleTheme();
        assertNotNull(GuiTheme.bg());
        assertNotNull(GuiTheme.panelBg());
        assertNotNull(GuiTheme.sidebarBg());
        assertNotNull(GuiTheme.btnBg());
        assertNotNull(GuiTheme.btnHov());
        assertNotNull(GuiTheme.border());
        assertNotNull(GuiTheme.borderLt());
        assertNotNull(GuiTheme.fg());
        assertNotNull(GuiTheme.fgDim());
        assertNotNull(GuiTheme.rowEven());
        assertNotNull(GuiTheme.rowOdd());
    }

    @Test
    void applyToStatics_lightMode_updatesStatics() {
        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        Color lightBg = GuiTheme.BG;
        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        Color darkBg = GuiTheme.BG;
        assertNotEquals(lightBg, darkBg);
    }

    @Test
    void rowSel_dark_notNull() {
        assertNotNull(GuiTheme.ROW_SEL);
    }

    @Test
    void fgDead_dark_notNull() {
        assertNotNull(GuiTheme.FG_DEAD);
    }

    @Test
    void btnAct_dark_notNull() {
        assertNotNull(GuiTheme.BTN_ACT);
    }

    // ── osColor full coverage ─────────────────────────────────────

    @Test
    void osColor_ios() {
        assertEquals(GuiTheme.IOS_COL, GuiTheme.osColor("iPad (iPadOS)"));
    }

    @Test
    void osColor_mobilesGeraet() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Mobiles Gerät"));
    }

    @Test
    void osColor_raspberryPi() {
        assertEquals(GuiTheme.RPI_COL, GuiTheme.osColor("Raspberry Pi (Linux)"));
    }

    @Test
    void osColor_iotMqtt() {
        assertEquals(GuiTheme.IOT_COL, GuiTheme.osColor("IoT-Gerät (MQTT)"));
    }

    @Test
    void osColor_switch() {
        assertEquals(GuiTheme.NET_COL, GuiTheme.osColor("Netzwerk-Switch"));
    }

    @Test
    void osColor_drucker() {
        assertEquals(GuiTheme.PRN_COL, GuiTheme.osColor("Drucker (LPD)"));
    }

    @Test
    void osColor_emptyString_returnsFg() {
        assertEquals(GuiTheme.FG, GuiTheme.osColor(""));
    }

    @Test
    void osColor_samsung() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Samsung)"));
    }

    @Test
    void osColor_xiaomi() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Xiaomi)"));
    }

    @Test
    void osColor_huawei() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Huawei)"));
    }

    @Test
    void osColor_pixel() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Google Pixel)"));
    }

    @Test
    void osColor_nothing() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Nothing Phone)"));
    }

    @Test
    void osColor_sony() {
        assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Sony)"));
    }

    @Test
    void brighter_clamps255() {
        Color c = new Color(200, 200, 200);
        Color b = GuiTheme.brighter(c, 2.0f);
        assertEquals(255, b.getRed());
        assertEquals(255, b.getGreen());
        assertEquals(255, b.getBlue());
    }

    @Test
    void brighter_zero_staysZero() {
        Color c = new Color(0, 0, 0);
        Color b = GuiTheme.brighter(c, 2.0f);
        assertEquals(0, b.getRed());
    }

    // ── Static color constants ────────────────────────────────────

    @Test
    void staticColors_allNonNull() {
        assertNotNull(GuiTheme.ACCENT);
        assertNotNull(GuiTheme.ACCENT2);
        assertNotNull(GuiTheme.WARN);
        assertNotNull(GuiTheme.INFO);
        assertNotNull(GuiTheme.WIN_COL);
        assertNotNull(GuiTheme.LIN_COL);
        assertNotNull(GuiTheme.APL_COL);
        assertNotNull(GuiTheme.IOS_COL);
        assertNotNull(GuiTheme.AND_COL);
        assertNotNull(GuiTheme.NET_COL);
        assertNotNull(GuiTheme.PRN_COL);
        assertNotNull(GuiTheme.IOT_COL);
        assertNotNull(GuiTheme.RPI_COL);
    }

    @Test
    void fonts_correct_family() {
        assertEquals("JetBrains Mono", GuiTheme.MONO.getFamily());
        assertEquals("JetBrains Mono", GuiTheme.MONO_S.getFamily());
        assertEquals("JetBrains Mono", GuiTheme.MONO_XS.getFamily());
        assertEquals("JetBrains Mono", GuiTheme.BTN_F.getFamily());
        assertEquals("JetBrains Mono", GuiTheme.BTN_F_S.getFamily());
    }

    @Test
    void themeName_light() {
        GuiTheme.toggleTheme();
        assertTrue(GuiTheme.themeName().contains("Dunkel") || GuiTheme.themeName().contains("🌙"));
    }
}
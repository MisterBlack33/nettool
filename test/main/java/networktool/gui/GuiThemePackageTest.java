package networktool.gui;

import org.junit.jupiter.api.*;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
    void themeName_light() {
        GuiTheme.toggleTheme();
        assertTrue(GuiTheme.themeName().contains("Dunkel") || GuiTheme.themeName().contains("🌙"));
    }

    // ── Volatile statics ──────────────────────────────────────────

    @Test
    void mutableStatics_areVolatile() throws Exception {
        String[] fields = {"BG", "PANEL_BG", "SIDEBAR_BG", "BTN_BG", "BTN_HOV",
                "BORDER", "BORDER_LT", "FG", "FG_DIM", "ROW_EVEN", "ROW_ODD", "ROW_SEL"};
        for (String name : fields) {
            Field f = GuiTheme.class.getDeclaredField(name);
            assertTrue(Modifier.isVolatile(f.getModifiers()),
                    name + " sollte volatile sein");
        }
    }

    @Test
    void btn_act_fieldDoesNotExist() {
        boolean found = false;
        for (Field f : GuiTheme.class.getDeclaredFields()) {
            if (f.getName().equals("BTN_ACT")) { found = true; break; }
        }
        assertFalse(found, "BTN_ACT sollte entfernt worden sein");
    }

    @Test
    void fg_dead_fieldDoesNotExist() {
        boolean found = false;
        for (Field f : GuiTheme.class.getDeclaredFields()) {
            if (f.getName().equals("FG_DEAD")) { found = true; break; }
        }
        assertFalse(found, "FG_DEAD sollte entfernt worden sein");
    }

    @Test
    void applyToStatics_darkVsLight_differentColors() {
        GuiTheme.applyToStatics();
        Color darkBg = GuiTheme.BG;
        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        Color lightBg = GuiTheme.BG;
        assertNotEquals(darkBg, lightBg);
    }

    @Test
    void toggleTheme_threadSafe_noException() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                GuiTheme.toggleTheme();
                GuiTheme.applyToStatics();
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(1000);
        assertNotNull(GuiTheme.BG);
    }

    @Test
    void osColor_empty_returnsFg() {
        assertEquals(GuiTheme.FG, GuiTheme.osColor(""));
    }

    @Test
    void osColor_null_returnsFg() {
        assertEquals(GuiTheme.FG, GuiTheme.osColor(null));
    }

    // ── Dynamic color accessors ───────────────────────────────────

    @Test
    void dynamicColors_darkMode() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        assertNotNull(GuiTheme.bg());
        assertNotNull(GuiTheme.fg());
        assertNotNull(GuiTheme.border());
    }

    @Test
    void dynamicColors_lightMode() {
        if (GuiTheme.isDark()) GuiTheme.toggleTheme();
        assertNotNull(GuiTheme.bg());
        assertNotNull(GuiTheme.fg());
    }

    @Test
    void themeName_dark() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        assertTrue(GuiTheme.themeName().contains("Hell") || GuiTheme.themeName().contains("☀"));
    }

    @Test
    void fonts_notNull() {
        assertNotNull(GuiTheme.MONO);
        assertNotNull(GuiTheme.MONO_S);
        assertNotNull(GuiTheme.BTN_F);
    }
}
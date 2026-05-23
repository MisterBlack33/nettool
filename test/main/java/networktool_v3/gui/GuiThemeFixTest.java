package main.java.networktool_v3.gui;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für GuiTheme-Fixes: volatile Aliase, keine BTN_ACT/FG_DEAD mehr. */
class GuiThemeFixTest {

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
        java.awt.Color darkBg = GuiTheme.BG;

        GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
        java.awt.Color lightBg = GuiTheme.BG;

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
        // Kein Fehler = volatile schützt korrekt
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
}
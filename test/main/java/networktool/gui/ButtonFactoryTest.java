package main.java.networktool.gui;

import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für ButtonFactory — einheitliche Swing-Button-Fabrik.
 */
class ButtonFactoryTest {

    @BeforeEach
    void setupTheme() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        GuiTheme.applyToStatics();
    }

    // ── Action Button ──────────────────────────────────────────────────

    @Test
    void action_createsButton() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertNotNull(b);
        assertEquals("Test", b.getText());
    }

    @Test
    void action_setsCorrectFont() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertNotNull(b.getFont());
        assertTrue(b.getFont().getName().contains("JetBrains") || b.getFont().getName().contains("Mono"));
    }

    @Test
    void action_setsForeground() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertEquals(Color.RED, b.getForeground());
    }

    @Test
    void action_setsBackground() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertNotNull(b.getBackground());
    }

    @Test
    void action_setBorderNotNull() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertNotNull(b.getBorder());
    }

    @Test
    void action_setsFocusPaintedFalse() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertFalse(b.isFocusPainted());
    }

    @Test
    void action_setsCursorToHand() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertEquals(Cursor.HAND_CURSOR, b.getCursor().getType());
    }

    @Test
    void action_hasMouseListener() {
        JButton b = ButtonFactory.action("Test", Color.RED);
        assertTrue(b.getMouseListeners().length > 0, "Button sollte MouseListener haben");
    }

    // ── Icon Button ────────────────────────────────────────────────────

    @Test
    void icon_createsButton() {
        JButton b = ButtonFactory.icon("Click", Color.BLUE, () -> {});
        assertNotNull(b);
        assertEquals("Click", b.getText());
    }

    @Test
    void icon_executesAction() {
        boolean[] executed = {false};
        JButton b = ButtonFactory.icon("Click", Color.BLUE, () -> { executed[0] = true; });
        // Simuliere Click
        for (var listener : b.getActionListeners()) {
            listener.actionPerformed(new javax.swing.event.ChangeEvent(b));
        }
        assertTrue(executed[0]);
    }

    @Test
    void icon_hasMouseListener() {
        JButton b = ButtonFactory.icon("Click", Color.BLUE, () -> {});
        assertTrue(b.getMouseListeners().length > 0);
    }

    // ── Link Button ────────────────────────────────────────────────────

    @Test
    void link_createsButton() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertNotNull(b);
        assertEquals("Link", b.getText());
    }

    @Test
    void link_setsForeground() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertEquals(Color.GREEN, b.getForeground());
    }

    @Test
    void link_noBorderPainted() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertFalse(b.isBorderPainted());
    }

    @Test
    void link_noContentAreaFilled() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertFalse(b.isContentAreaFilled());
    }

    @Test
    void link_noFocusPainted() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertFalse(b.isFocusPainted());
    }

    @Test
    void link_hasHandCursor() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertEquals(Cursor.HAND_CURSOR, b.getCursor().getType());
    }

    @Test
    void link_hasMouseListener() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        assertTrue(b.getMouseListeners().length > 0);
    }

    @Test
    void link_hoverChangesForeground() {
        JButton b = ButtonFactory.link("Link", Color.GREEN);
        Color originalColor = b.getForeground();

        // Simuliere Mouse Enter
        MouseEvent enterEvent = new MouseEvent(b, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false);
        for (var listener : b.getMouseListeners()) {
            listener.mouseEntered(enterEvent);
        }

        // Nach Hover sollte Farbe unterschiedlich sein (zu ACCENT)
        Color hoverColor = b.getForeground();
        assertNotEquals(originalColor, hoverColor, "Foreground sollte sich bei Hover ändern");
    }

    // ── Terminal Button ────────────────────────────────────────────────

    @Test
    void terminal_createsButton() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        assertNotNull(b);
        assertEquals("Term", b.getText());
    }

    @Test
    void terminal_setsForeground() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        assertEquals(Color.YELLOW, b.getForeground());
    }

    @Test
    void terminal_setsDarkBackground() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        Color bg = b.getBackground();
        assertNotNull(bg);
        // Terminal-Hintergrund sollte dunkel sein
        assertTrue(bg.getRed() < 50 && bg.getGreen() < 50 && bg.getBlue() < 50,
                "Terminal-Button sollte dunklen Hintergrund haben");
    }

    @Test
    void terminal_hasBorder() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        assertNotNull(b.getBorder());
    }

    @Test
    void terminal_setsFocusPaintedFalse() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        assertFalse(b.isFocusPainted());
    }

    @Test
    void terminal_hasHandCursor() {
        JButton b = ButtonFactory.terminal("Term", Color.YELLOW);
        assertEquals(Cursor.HAND_CURSOR, b.getCursor().getType());
    }

    // ── Various colors ────────────────────────────────────────────────

    @Test
    void action_multipleColors() {
        JButton r = ButtonFactory.action("R", Color.RED);
        JButton g = ButtonFactory.action("G", Color.GREEN);
        JButton b = ButtonFactory.action("B", Color.BLUE);

        assertEquals(Color.RED, r.getForeground());
        assertEquals(Color.GREEN, g.getForeground());
        assertEquals(Color.BLUE, b.getForeground());
    }

    @Test
    void link_multipleColors() {
        JButton r = ButtonFactory.link("R", Color.RED);
        JButton g = ButtonFactory.link("G", Color.GREEN);

        assertEquals(Color.RED, r.getForeground());
        assertEquals(Color.GREEN, g.getForeground());
    }

    @Test
    void constructor_privateNoInstantiation() {
        // ButtonFactory sollte nicht instanziierbar sein
        assertThrows(Error.class, () -> {
            var constructor = ButtonFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }
}


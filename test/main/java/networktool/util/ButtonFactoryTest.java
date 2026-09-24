package main.java.networktool.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import static org.junit.jupiter.api.Assertions.*;

class ButtonFactoryTest {
    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }
    @Test void action_hasLabelAndColor() {
        JButton b = ButtonFactory.action("Scan", Color.RED);
        assertEquals("Scan", b.getText());
        assertEquals(Color.RED, b.getForeground());
    }
    @Test void action_hoverListener_changesBackground() {
        JButton b = ButtonFactory.action("X", Color.BLUE);
        Color initial = b.getBackground();
        b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false));
        assertNotEquals(initial, b.getBackground());
        b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
        assertEquals(initial, b.getBackground());
    }
    @Test void icon_runsActionOnClick() {
        boolean[] called = {false};
        JButton b = ButtonFactory.icon("I", Color.GREEN, () -> called[0] = true);
        for (var l : b.getActionListeners()) l.actionPerformed(null);
        assertTrue(called[0]);
    }
    @Test void link_hasNoBorderOrFill() {
        JButton b = ButtonFactory.link("L", Color.WHITE);
        assertFalse(b.isBorderPainted());
        assertFalse(b.isContentAreaFilled());
    }
    @Test void link_hoverChangesForeground() {
        JButton b = ButtonFactory.link("L", Color.WHITE);
        b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false));
        assertNotEquals(Color.WHITE, b.getForeground());
        b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
        assertEquals(Color.WHITE, b.getForeground());
    }
    @Test void terminal_hasDarkBackground() {
        JButton b = ButtonFactory.terminal("T", Color.CYAN);
        assertEquals(new Color(0x10, 0x18, 0x10), b.getBackground());
    }
}

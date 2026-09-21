package main.java.networktool.gui.core;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GuiTestSuiteMenusTest {

    private final GuiMenuRegistry registry = new GuiMenuRegistry();

    GuiTestSuiteMenusTest() {
        GuiTestSuiteMenus.registerAll(registry, new GuiMenuContext(null, null, null, null));
    }

    @Test void registersAllTestSuiteIds() {
        IntStream.rangeClosed(24, 45).filter(id -> id != 30)
                .forEach(id -> assertTrue(registry.contains(String.valueOf(id)), "ID " + id));
    }

    @Test void doesNotRegisterPrivacyId30() {
        assertFalse(registry.contains("30"));
    }

    @Test void registersExactlyTwentyOneIds() {
        assertEquals(21, registry.size());
    }
}

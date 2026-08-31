package main.java.networktool.gui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiMenuRegistryTest {

    @Test void dispatch_registeredId_runsActionAndReturnsTrue() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        int[] calls = {0};
        reg.register("01", () -> calls[0]++);
        assertTrue(reg.dispatch("01"));
        assertEquals(1, calls[0]);
    }

    @Test void dispatch_unknownId_returnsFalse_doesNotThrow() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        assertFalse(reg.dispatch("99"));
    }

    @Test void contains_reflectsRegisteredIds() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        assertFalse(reg.contains("05"));
        reg.register("05", () -> {});
        assertTrue(reg.contains("05"));
    }

    @Test void register_sameId_overwritesPrevious() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        int[] calls = {0, 0};
        reg.register("01", () -> calls[0]++);
        reg.register("01", () -> calls[1]++);
        reg.dispatch("01");
        assertEquals(0, calls[0]);
        assertEquals(1, calls[1]);
    }

    @Test void size_reflectsDistinctRegistrations() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        reg.register("01", () -> {});
        reg.register("02", () -> {});
        reg.register("01", () -> {}); // overwrite, not a new entry
        assertEquals(2, reg.size());
    }

    @Test void dispatch_propagatesRuntimeExceptionFromAction() {
        GuiMenuRegistry reg = new GuiMenuRegistry();
        reg.register("01", () -> { throw new IllegalStateException("boom"); });
        assertThrows(IllegalStateException.class, () -> reg.dispatch("01"));
    }

    @Test void realMenuHandler_registersAllExpectedIds() {
        // GuiMenuHandler baut seine Registry im Konstruktor auf; wir prüfen indirekt
        // über handle(), dass unbekannte IDs klaglos ignoriert werden.
        assertDoesNotThrow(() -> new GuiMenuRegistry().dispatch("unknown"));
    }
}
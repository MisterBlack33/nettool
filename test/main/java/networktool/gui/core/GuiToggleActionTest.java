package main.java.networktool.gui.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiToggleActionTest {

    @Test void toggle_inactiveRunsStartOnly() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GuiToggleAction.toggle(new GuiToggleAction.Parameters(() -> false, starts::incrementAndGet, stops::incrementAndGet));
        assertEquals(1, starts.get());
        assertEquals(0, stops.get());
    }

    @Test void toggle_activeRunsStopOnly() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GuiToggleAction.toggle(new GuiToggleAction.Parameters(() -> true, starts::incrementAndGet, stops::incrementAndGet));
        assertEquals(0, starts.get());
        assertEquals(1, stops.get());
    }
}

package main.java.networktool.gui.components.scan;

import main.java.networktool.logic.scan.schedule.WolSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GuiWolSchedulerActionsTest {

    private static final String MAC = "AA:BB:CC:DD:EE:FF";

    @Test void parse_validInput() {
        WolSchedule s = GuiWolSchedulerActions.parseSchedule(MAC, "07:30", "192.168.1.255").orElseThrow();
        assertEquals(MAC, s.mac());
        assertEquals(LocalTime.of(7, 30), s.time());
        assertEquals("192.168.1.255", s.broadcast());
    }

    @Test void parse_blankBroadcast_usesDefault() {
        assertEquals(GuiWolSchedulerActions.DEFAULT_BROADCAST,
                GuiWolSchedulerActions.parseSchedule(MAC, "07:30", "").orElseThrow().broadcast());
    }

    @Test void parse_trimsInput() {
        assertTrue(GuiWolSchedulerActions.parseSchedule(" " + MAC + " ", " 07:30 ", " ").isPresent());
    }

    @Test void parse_invalidMac_empty() {
        assertEquals(Optional.empty(), GuiWolSchedulerActions.parseSchedule("nope", "07:30", ""));
    }

    @Test void parse_invalidTime_empty() {
        assertTrue(GuiWolSchedulerActions.parseSchedule(MAC, "25:99", "").isEmpty());
        assertTrue(GuiWolSchedulerActions.parseSchedule(MAC, "7:30", "").isEmpty());
        assertTrue(GuiWolSchedulerActions.parseSchedule(MAC, "", "").isEmpty());
    }

    @Test void parse_invalidBroadcast_empty() {
        assertTrue(GuiWolSchedulerActions.parseSchedule(MAC, "07:30", "not-an-ip").isEmpty());
    }
}

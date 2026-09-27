package main.java.networktool.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliArgsTest {

    @Test void noArgs_returnsGui() {
        assertEquals(CliArgs.Command.GUI, CliArgs.parse(new String[]{}).command);
    }

    @Test void nullArgs_returnsGui() {
        assertEquals(CliArgs.Command.GUI, CliArgs.parse(null).command);
    }

    @Test void help_longForm() {
        assertEquals(CliArgs.Command.HELP, CliArgs.parse(new String[]{"--help"}).command);
    }

    @Test void help_shortForm() {
        assertEquals(CliArgs.Command.HELP, CliArgs.parse(new String[]{"-h"}).command);
    }

    @Test void version_longForm() {
        assertEquals(CliArgs.Command.VERSION, CliArgs.parse(new String[]{"--version"}).command);
    }

    @Test void version_shortForm() {
        assertEquals(CliArgs.Command.VERSION, CliArgs.parse(new String[]{"-v"}).command);
    }

    @Test void unknownArg_keptAsGuiWithUnknownList() {
        CliArgs r = CliArgs.parse(new String[]{"--bogus"});
        assertEquals(CliArgs.Command.GUI, r.command);
        assertTrue(r.hasUnknownArgs());
        assertEquals(List.of("--bogus"), r.unknown);
    }

    @Test void helpTakesPriorityOverOtherArgs() {
        assertEquals(CliArgs.Command.HELP, CliArgs.parse(new String[]{"--bogus", "--help"}).command);
    }

    @Test void unknown_isImmutable() {
        CliArgs r = CliArgs.parse(new String[]{"--bogus"});
        assertThrows(UnsupportedOperationException.class, () -> r.unknown.add("x"));
    }

    @Test void hasUnknownArgs_falseForHelpAndVersion() {
        assertFalse(CliArgs.parse(new String[]{"--help"}).hasUnknownArgs());
        assertFalse(CliArgs.parse(new String[]{"--version"}).hasUnknownArgs());
    }

    @Test void hasUnknownArgs_falseForEmptyGui() {
        assertFalse(CliArgs.parse(new String[]{}).hasUnknownArgs());
    }
}

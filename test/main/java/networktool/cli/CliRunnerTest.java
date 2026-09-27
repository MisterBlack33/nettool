package main.java.networktool.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class CliRunnerTest {

    @Test void help_printsUsage_exitOk() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int code = CliRunner.run(CliArgs.parse(new String[]{"--help"}), new PrintStream(buf));
        assertEquals(CliRunner.EXIT_OK, code);
        assertTrue(buf.toString().contains("Verwendung"));
    }

    @Test void version_printsVersion_exitOk() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int code = CliRunner.run(CliArgs.parse(new String[]{"--version"}), new PrintStream(buf));
        assertEquals(CliRunner.EXIT_OK, code);
        assertTrue(buf.toString().contains("NetTool"));
    }

    @Test void noArgs_signalsGuiStart() {
        int code = CliRunner.run(CliArgs.parse(new String[]{}), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(CliRunner.GUI_START, code);
    }

    @Test void unknownArgs_exitInvalidArgs() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int code = CliRunner.run(CliArgs.parse(new String[]{"--bogus"}), new PrintStream(buf));
        assertEquals(CliRunner.EXIT_INVALID_ARGS, code);
        assertTrue(buf.toString().contains("--bogus"));
    }

    @Test void unknownArgs_alsoPrintsUsage() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CliRunner.run(CliArgs.parse(new String[]{"--bogus"}), new PrintStream(buf));
        assertTrue(buf.toString().contains("Verwendung"));
    }

    @Test void exitCodes_areDistinct() {
        assertNotEquals(CliRunner.EXIT_OK, CliRunner.EXIT_INVALID_ARGS);
        assertNotEquals(CliRunner.EXIT_OK, CliRunner.GUI_START);
    }
}

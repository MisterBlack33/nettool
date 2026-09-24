package main.java.networktool.gui.panels;

import main.java.networktool.theme.GuiTheme;
import org.junit.jupiter.api.*;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutputStreamRedirectorTest {

    private record Entry(String line, Color color) {}

    private List<Entry> captured;
    private PrintStream out;
    private PrintStream err;

    @BeforeEach
    void setup() {
        if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        captured = new ArrayList<>();
        out = OutputStreamRedirector.build(false, (line, color) -> captured.add(new Entry(line, color)));
        err = OutputStreamRedirector.build(true, (line, color) -> captured.add(new Entry(line, color)));
    }

    @Test void errorStream_alwaysUsesWarnColor() {
        err.println("anything");
        assertEquals(GuiTheme.WARN, captured.get(0).color());
    }

    @Test void successLine_classifiedAccent2() {
        out.println("Verbindung erfolgreich hergestellt");
        assertEquals(GuiTheme.ACCENT2, captured.get(0).color());
    }

    @Test void reachableLine_classifiedAccent2() {
        out.println("Host erreichbar");
        assertEquals(GuiTheme.ACCENT2, captured.get(0).color());
    }

    @Test void headerLine_classifiedAccent() {
        out.println("=== Scan-Ergebnisse ===");
        assertEquals(GuiTheme.ACCENT, captured.get(0).color());
    }

    @Test void errorTextLine_classifiedWarn() {
        out.println("Fehler beim Verbinden");
        assertEquals(GuiTheme.WARN, captured.get(0).color());
    }

    @Test void windowsLine_classifiedWinColor() {
        out.println("Windows 11 gefunden");
        assertEquals(GuiTheme.WIN_COL, captured.get(0).color());
    }

    @Test void linuxLine_classifiedLinColor() {
        out.println("Linux/Unix gefunden");
        assertEquals(GuiTheme.LIN_COL, captured.get(0).color());
    }

    @Test void appleLine_classifiedApplColor() {
        out.println("Apple-Gerät gefunden");
        assertEquals(GuiTheme.APL_COL, captured.get(0).color());
    }

    @Test void plainLine_usesDefaultTerminalColor() {
        out.println("nichts besonderes");
        assertNotNull(captured.get(0).color());
        assertNotEquals(GuiTheme.WARN, captured.get(0).color());
    }

    @Test void blankLine_notAppended() {
        out.println();
        assertTrue(captured.isEmpty());
    }

    @Test void carriageReturn_ignoredNotFlushed() {
        out.print("partial\r");
        assertEquals("partial", captured.get(0).line());
    }

    @Test void multipleLines_eachFlushedSeparately() {
        out.println("erste Zeile");
        out.println("zweite Zeile");
        assertEquals(2, captured.size());
    }
}

package networktool.gui.panels;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import networktool.theme.GuiTheme;
import static networktool.theme.GuiTheme.*;

/**
 * Baut PrintStreams, die {@code System.out}/{@code System.err} zeilenweise
 * einfärben und ans Output-Panel weiterreichen.
 */
final class OutputStreamRedirector {

    private OutputStreamRedirector() {}

    static PrintStream build(boolean isError, BiConsumer<String, Color> appendText) {
        return new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            public void write(int b) {
                if (b == '\r') return;
                buf.write(b);
                if (b == '\n') flush();
            }
            public void flush() {
                if (buf.size() == 0) return;
                String line = buf.toString(StandardCharsets.UTF_8);
                buf.reset();
                if (line.isBlank()) return;
                appendText.accept(line, isError ? WARN : classifyColor(line));
            }
        }, true, StandardCharsets.UTF_8);
    }

    private static Color classifyColor(String line) {
        boolean dark = GuiTheme.isDark();
        if (line.contains("erreichbar")||line.contains("erfolgreich")||line.contains("Aktiv:"))
            return dark ? ACCENT2 : new Color(0x18,0x90,0x38);
        if (line.contains("===")||line.contains("═")||line.contains("╔")||line.contains("╚"))
            return dark ? ACCENT : new Color(0x9A,0x6C,0x08);
        if (line.contains("Fehler")||line.contains("NICHT")||line.contains("ERROR"))
            return WARN;
        if (line.contains("Windows"))  return dark ? WIN_COL : new Color(0x18,0x60,0xB8);
        if (line.contains("Linux")||line.contains("Android"))
            return dark ? LIN_COL : new Color(0x18,0x80,0x28);
        if (line.contains("Apple")||line.contains("iOS")||line.contains("macOS"))
            return dark ? APL_COL : new Color(0x50,0x50,0x60);
        return terminalFg();
    }

    private static Color terminalFg() {
        return GuiTheme.isDark() ? new Color(0xE8,0xE4,0xD8) : new Color(0x10,0x12,0x10);
    }
}

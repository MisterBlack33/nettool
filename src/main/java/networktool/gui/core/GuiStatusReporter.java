package main.java.networktool.gui.core;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.theme.GuiTheme;
import main.java.networktool.util.StatusTags;

import java.awt.Color;
import java.util.Objects;

/** Writes consistently tagged, theme-colored messages to the GUI output. */
public final class GuiStatusReporter {

    private GuiStatusReporter() {}

    public static void ok(GuiOutputPanel output, String message) {
        report(output, StatusTags.OK, message, GuiTheme.ACCENT2);
    }

    public static void error(GuiOutputPanel output, String message) {
        report(output, StatusTags.FEHLER, message, GuiTheme.WARN);
    }

    public static void warning(GuiOutputPanel output, String message) {
        report(output, StatusTags.WARN, message, GuiTheme.WARN);
    }

    public static void info(GuiOutputPanel output, String message) {
        report(output, StatusTags.INFO, message, GuiTheme.INFO);
    }

    public static void report(GuiOutputPanel output, String tag, String message, Color color) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(color, "color");
        output.appendText("  " + tag + " " + message + "\n", color);
    }
}

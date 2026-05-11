package main.java.networktool_v3.filter;

import java.awt.*;
import java.awt.datatransfer.*;

/**
 * Hilfsmethoden für die Zwischenablage.
 */
public final class ClipboardUtil {

    private ClipboardUtil() {}

    /**
     * Kopiert einen Text in die Systemzwischenablage.
     * Gibt eine Bestätigung auf System.out aus.
     */
    public static void copy(String text) {
        if (text == null || text.isBlank()) return;
        try {
            StringSelection sel = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            System.out.println("  ✔ In Zwischenablage kopiert: " + text);
        } catch (Exception e) {
            System.err.println("Zwischenablage: " + e.getMessage());
        }
    }
}

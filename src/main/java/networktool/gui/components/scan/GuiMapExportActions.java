package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.map.MapExporter;
import main.java.networktool.gui.map.MapSnapshot;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.export.ExportFiles;
import main.java.networktool.util.StatusTags;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "Karte: Export" als SVG oder GraphML (Menü-ID "39"). */
public final class GuiMapExportActions {

    private GuiMapExportActions() {}

    public static void handle(GuiOutputPanel output, GuiMenuHandler handler) {
        String[] options = {"SVG", "GraphML"};
        int choice = JOptionPane.showOptionDialog(null, "Topologie exportieren als:", "Karten-Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        Path dir = ExportFiles.defaultDir();
        handler.runAsync(() -> { if (choice == 0) exportSvg(dir, output); else exportGraphml(dir, output); });
    }

    static Path exportSvg(Path dir, GuiOutputPanel output) throws IOException {
        MapSnapshot snap = MapSnapshot.capture();
        return report(ExportFiles.writeUnique(dir, "svg", MapExporter.toSvg(snap.nodes(), snap.edges())),
                "MAP_EXPORT_SVG", output);
    }

    static Path exportGraphml(Path dir, GuiOutputPanel output) throws IOException {
        MapSnapshot snap = MapSnapshot.capture();
        return report(ExportFiles.writeUnique(dir, "graphml", MapExporter.toGraphml(snap.nodes(), snap.edges())),
                "MAP_EXPORT_GRAPHML", output);
    }

    private static Path report(Path file, String action, GuiOutputPanel output) {
        AuditLogger.getInstance().log(action, file.getFileName().toString());
        output.appendText("  " + StatusTags.OK + " " + file + "\n", ACCENT2);
        return file;
    }
}

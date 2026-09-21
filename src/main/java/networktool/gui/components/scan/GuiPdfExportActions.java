package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.model.HostResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.export.ExportFiles;
import main.java.networktool.storage.export.PdfHostLines;
import main.java.networktool.storage.export.PdfReportBuilder;
import main.java.networktool.storage.network.NetworkStore;
import main.java.networktool.util.StatusTags;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "PDF-Export" gespeicherter Hosts (Menü-ID "40"). */
public final class GuiPdfExportActions {

    private static final String TITLE = "NetTool v3 - Host-Report";

    private GuiPdfExportActions() {}

    public static void handle(GuiOutputPanel output, GuiMenuHandler handler) {
        handler.runAsync(() -> export(ExportFiles.defaultDir(),
                NetworkStore.getInstance().getAllHosts(), output));
    }

    static Path export(Path dir, List<HostResult> hosts, GuiOutputPanel output) throws IOException {
        Path file = PdfReportBuilder.save(dir, PdfHostLines.from(hosts, TITLE));
        AuditLogger.getInstance().log("EXPORT_PDF", file.getFileName().toString());
        output.appendText("  " + StatusTags.OK + " " + file + "\n", ACCENT2);
        return file;
    }
}

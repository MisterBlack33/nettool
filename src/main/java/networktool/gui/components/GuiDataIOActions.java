package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.DataExporter;
import main.java.networktool.storage.DataImporter;
import main.java.networktool.storage.NotificationHistory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static networktool.theme.GuiTheme.*;

/**
 * Export/Import von Scan-Daten, Notification-History-Anzeige
 * und Port-Konfiguration (Menüpunkte "18", "19", "21").
 */
public final class GuiDataIOActions {

    private static final Logger LOG = Logger.getLogger(GuiDataIOActions.class.getName());

    private GuiDataIOActions() {}

    public static void handleExportImport(GuiInputPanel input, GuiOutputPanel output, GuiMenuHandler handler) {
        String[] options = {"CSV", "JSON", "HTML", "ZIP-Backup", "CSV imp.", "JSON imp.", "ZIP restore"};
        int choice = JOptionPane.showOptionDialog(null, "Export / Import:", "Daten",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        java.nio.file.Path outDir = java.nio.file.Paths.get(System.getProperty("user.home"), "NetTool-Export");
        switch (choice) {
            case 0 -> handler.runAsync(() -> {
                java.nio.file.Path f = DataExporter.exportCsv(outDir);
                AuditLogger.getInstance().log("EXPORT_CSV", f.toString());
                output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2);
            });
            case 1 -> handler.runAsync(() -> {
                java.nio.file.Path f = DataExporter.exportJson(outDir);
                AuditLogger.getInstance().log("EXPORT_JSON", f.toString());
                output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2);
            });
            case 2 -> handler.runAsync(() -> {
                java.nio.file.Path f = DataExporter.exportHtml(outDir);
                AuditLogger.getInstance().log("EXPORT_HTML", f.toString());
                output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2);
                try {
                    java.awt.Desktop.getDesktop().browse(f.toUri());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Export-Datei konnte nicht automatisch geöffnet werden", e);
                }
            });
            case 3 -> handler.runAsync(() -> {
                java.nio.file.Path f = DataExporter.exportBackup(outDir);
                AuditLogger.getInstance().log("EXPORT_ZIP", f.toString());
                output.appendText("  ✔ " + f.getFileName() + "\n", ACCENT2);
            });
            case 4 -> input.ask("CSV-Pfad:", path -> handler.runAsync(() -> {
                int n = DataImporter.importCsv(java.nio.file.Paths.get(path.trim()));
                AuditLogger.getInstance().log("IMPORT_CSV", "n=" + n);
                output.appendText("  ✔ " + n + " importiert\n", ACCENT2);
            }));
            case 5 -> input.ask("JSON-Pfad:", path -> handler.runAsync(() -> {
                int n = DataImporter.importJson(java.nio.file.Paths.get(path.trim()));
                AuditLogger.getInstance().log("IMPORT_JSON", "n=" + n);
                output.appendText("  ✔ " + n + " importiert\n", ACCENT2);
            }));
            case 6 -> input.ask("ZIP-Pfad:", path -> handler.runAsync(() -> {
                int n = DataImporter.restoreBackup(java.nio.file.Paths.get(path.trim()));
                AuditLogger.getInstance().log("RESTORE_ZIP", "n=" + n);
                output.appendText("  ✔ " + n + " Dateien wiederhergestellt\n", ACCENT2);
            }));
        }
    }

    public static void handleNotificationHistory(GuiInputPanel input, GuiOutputPanel output) {
        NotificationHistory hist = NotificationHistory.getInstance();
        if (hist.size() == 0) {
            output.appendText("  Keine Nachrichten.\n", FG_DIM);
            return;
        }

        String[][] data = hist.getAll().stream()
                .map(e -> new String[]{e.time, e.source, e.title, e.message})
                .toArray(String[][]::new);
        String[] cols = {"Zeit", "Quelle", "Titel", "Nachricht"};
        int[] widths = {120, 130, 150, 250};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(data, cols) {
                    public boolean isCellEditable(int r, int c) { return false; }
                };
        JTable table = TableConfig.buildTable(model, widths);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new java.awt.Dimension(0, Math.min(data.length * 26 + 30, 280)));
        sp.setBorder(new javax.swing.border.LineBorder(BORDER, 1));
        sp.getViewport().setBackground(TableConfig.ROW_BG_EVEN);

        SwingUtilities.invokeLater(() -> {
            GUI.instance().appendText("\n", FG);
            JTextPane pane = GUI.instance().getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(sp);
            GUI.instance().appendText("\n", FG);
        });

        input.ask("'clear' = Löschen, Enter = weiter:", v -> {
            if ("clear".equalsIgnoreCase(v.trim())) {
                AuditLogger.getInstance().log("NOTIFICATION_HISTORY_CLEAR", "");
                hist.clear();
                output.appendText("  ✔ Verlauf geleert\n", ACCENT2);
            }
        });
    }

    public static void handlePortConfig(GuiInputPanel input, GuiOutputPanel output, GuiStatusBar status) {
        List<Integer> current = PortScanner.getActivePorts();
        status.set("Ports: " + current.size(), FG_DIM);
        input.ask("Ports kommagetrennt (z.B. 22,80,443) oder 'reset':", value -> {
            if ("reset".equalsIgnoreCase(value.trim())) {
                PortScanner.setActivePorts(null);
                AuditLogger.getInstance().log("PORT_CONFIG_RESET", "");
                output.appendText("  ✔ Standard-Ports (" + PortScanner.getActivePorts().size() + ")\n", ACCENT2);
                return;
            }
            List<Integer> ports = new ArrayList<>();
            for (String p : value.split(",")) {
                try {
                    ports.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException e) {
                    LOG.log(Level.FINE, "Ungültiger Port-Wert \"" + p + "\" wird ignoriert", e);
                }
            }
            if (ports.isEmpty()) {
                output.appendText("  ✕ Keine gültigen Ports\n", WARN);
                return;
            }
            PortScanner.setActivePorts(ports);
            AuditLogger.getInstance().log("PORT_CONFIG_SET", ports.toString());
            output.appendText("  ✔ " + ports.size() + " Ports konfiguriert\n", ACCENT2);
        });
    }
}

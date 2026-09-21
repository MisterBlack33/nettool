package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.BackupManager;
import main.java.networktool.storage.StorageLocations;
import main.java.networktool.util.StatusTags;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "Backup/Restore" des saves/-Verzeichnisses (Menü-ID "45"). */
public final class GuiBackupActions {

    private static final String BACKUP_DIR = "backups";

    private GuiBackupActions() {}

    public static void handle(GuiInputPanel input, GuiOutputPanel output, GuiMenuHandler handler) {
        String[] options = {"Backup erstellen", "Backup wiederherstellen"};
        int choice = JOptionPane.showOptionDialog(null, "saves/-Verzeichnis:", "Backup",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        Path root = StorageLocations.root();
        Path dir = root.resolve(BACKUP_DIR);
        if (choice == 0) {
            handler.runAsync(() -> runBackup(root, dir, output));
        } else {
            input.ask("Pfad zur Backup-ZIP:", p ->
                    handler.runAsync(() -> runRestore(Path.of(p.trim()), root, dir, output)));
        }
    }

    static void runBackup(Path source, Path targetDir, GuiOutputPanel output) throws IOException {
        Path zip = BackupManager.createBackup(source, targetDir);
        AuditLogger.getInstance().log("BACKUP_CREATE", zip.getFileName().toString());
        output.appendText("  " + StatusTags.OK + " Backup: " + zip + "\n", ACCENT2);
    }

    static void runRestore(Path zip, Path target, Path safetyDir, GuiOutputPanel output) throws IOException {
        int count = BackupManager.restoreBackup(zip, target, safetyDir);
        AuditLogger.getInstance().log("BACKUP_RESTORE", count + " Dateien");
        output.appendText("  " + StatusTags.OK + " " + count
                + " Datei(en) wiederhergestellt – bitte NetTool neu starten\n", ACCENT2);
    }
}

package main.java.networktool.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Basisklasse für Tests mit NetworkStore / AutoBackup.
 *
 * Lifecycle:
 *  - Vor jedem Test: alte Test-Backups löschen + testMode aktivieren
 *  - Nach jedem Test: neu erstellte Test-Backups löschen + testMode deaktivieren
 *
 * Produktiv-Backups (ohne TEST_BACKUP_PREFIX) werden nie berührt.
 */
public abstract class NetworkStoreTestBase {

    @BeforeEach
    void baseSetup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().cleanupBackups(); // lastBackupDate zurücksetzen
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }
}
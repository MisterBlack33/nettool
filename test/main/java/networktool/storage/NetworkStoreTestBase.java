package main.java.networktool.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Basisklasse fÃ¼r Tests mit NetworkStore / AutoBackup.
 *
 * Lifecycle:
 *  - Vor jedem Test: alte Test-Backups lÃ¶schen + testMode aktivieren
 *  - Nach jedem Test: neu erstellte Test-Backups lÃ¶schen + testMode deaktivieren
 *
 * Produktiv-Backups (ohne TEST_BACKUP_PREFIX) werden nie berÃ¼hrt.
 */
public abstract class NetworkStoreTestBase {

    @BeforeEach
    void baseSetup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().cleanupBackups(); // lastBackupDate zurÃ¼cksetzen
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }
}
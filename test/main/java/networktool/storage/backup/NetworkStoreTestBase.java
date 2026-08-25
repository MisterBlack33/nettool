package main.java.networktool.storage.backup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Basisklasse fÃƒÆ’Ã‚Â¼r Tests mit NetworkStore / AutoBackup.
 *
 * Lifecycle:
 *  - Vor jedem Test: alte Test-Backups lÃƒÆ’Ã‚Â¶schen + testMode aktivieren
 *  - Nach jedem Test: neu erstellte Test-Backups lÃƒÆ’Ã‚Â¶schen + testMode deaktivieren
 *
 * Produktiv-Backups (ohne TEST_BACKUP_PREFIX) werden nie berÃƒÆ’Ã‚Â¼hrt.
 */
public abstract class NetworkStoreTestBase {

    @BeforeEach
    void baseSetup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().cleanupBackups(); // lastBackupDate zurÃƒÆ’Ã‚Â¼cksetzen
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }
}
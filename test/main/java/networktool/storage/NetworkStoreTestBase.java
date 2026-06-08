package main.java.networktool.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Basisklasse für Tests mit NetworkStore / AutoBackup.
 *
 * Injiziert @TempDir in AutoBackup → kein Zugriff auf echtes Dateisystem,
 * kein Hängen durch langsame ZIP-Operationen.
 * TEST_BACKUP_PREFIX bleibt erhalten; Produktiv-Backups werden nie berührt.
 */
public abstract class NetworkStoreTestBase {

    @TempDir Path tmpBackup;
    @TempDir Path tmpSrc;

    @BeforeEach
    void baseSetup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().setDirs(tmpBackup, tmpSrc);
        AutoBackup.getInstance().cleanupBackups();
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().resetDirs();
        AutoBackup.testMode = false;
    }
}
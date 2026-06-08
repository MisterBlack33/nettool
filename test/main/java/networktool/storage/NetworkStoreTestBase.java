package main.java.networktool.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstrakte Basisklasse für alle Tests die NetworkStore, AutoBackup
 * oder triggerNow() indirekt aufrufen (save/remove).
 *
 * Setzt lastBackupDate vor und nach jedem Test zurück,
 * damit keine Test-Backups erstellt oder liegengelassen werden.
 */
public abstract class NetworkStoreTestBase {

    @BeforeEach
    void baseSetup() {
        AutoBackup.getInstance().cleanupBackups();
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().cleanupBackups();
    }
}
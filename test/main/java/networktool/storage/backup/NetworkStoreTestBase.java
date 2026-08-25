package main.java.networktool.storage.network;

import main.java.networktool.storage.backup.AutoBackup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class NetworkStoreTestBase {
    @BeforeEach
    void baseSetup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().cleanupBackups();
    }

    @AfterEach
    void baseCleanup() {
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }
}
package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SonifyConfigStoreTest {
    @Test void save_and_load_roundtrip() {
        SonifyConfig cfg = new SonifyConfig();
        cfg.highHz = 1234;
        cfg.lowHz  = 321;
        cfg.toneMs = 99;
        SonifyConfigStore.save(cfg);

        SonifyConfig loaded = SonifyConfigStore.load();
        assertEquals(1234, loaded.highHz);
        assertEquals(321,  loaded.lowHz);
        assertEquals(99,   loaded.toneMs);
    }
    @Test void load_doesNotThrow() {
        assertDoesNotThrow(SonifyConfigStore::load);
    }
}
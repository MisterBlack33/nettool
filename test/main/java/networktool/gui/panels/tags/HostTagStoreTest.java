package main.java.networktool.gui.panels.tags;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HostTagStoreTest {

    @TempDir Path tmp;
    HostTagStore store;

    @BeforeEach void setup() {
        store = HostTagStore.getInstance();
        store.setDataDir(tmp);
    }

    @Test void addTag_appearsInGetTags() {
        store.addTag("1.1.1.1", "server");
        assertTrue(store.getTags("1.1.1.1").contains("server"));
    }

    @Test void addTag_blankIp_ignored() {
        store.addTag("", "x");
        assertTrue(store.getAllTaggedIps().isEmpty());
    }

    @Test void addTag_blankTag_ignored() {
        store.addTag("1.1.1.1", "  ");
        assertTrue(store.getTags("1.1.1.1").isEmpty());
    }

    @Test void removeTag_removesIt() {
        store.addTag("1.1.1.2", "a");
        store.removeTag("1.1.1.2", "a");
        assertTrue(store.getTags("1.1.1.2").isEmpty());
    }

    @Test void removeTag_unknownIp_doesNotThrow() {
        assertDoesNotThrow(() -> store.removeTag("9.9.9.9", "a"));
    }

    @Test void isFavorite_initiallyFalse() {
        assertFalse(store.isFavorite("1.1.1.3"));
    }

    @Test void setFavorite_true_reflected() {
        store.setFavorite("1.1.1.3", true);
        assertTrue(store.isFavorite("1.1.1.3"));
    }

    @Test void setFavorite_false_clears() {
        store.setFavorite("1.1.1.3", true);
        store.setFavorite("1.1.1.3", false);
        assertFalse(store.isFavorite("1.1.1.3"));
    }

    @Test void favoriteCount_countsFavorites() {
        store.setFavorite("1.1.1.4", true);
        store.setFavorite("1.1.1.5", true);
        assertEquals(2, store.favoriteCount());
    }

    @Test void getAllTaggedIps_includesTaggedAndFavorited() {
        store.addTag("2.2.2.2", "x");
        store.setFavorite("3.3.3.3", true);
        assertTrue(store.getAllTaggedIps().containsAll(java.util.List.of("2.2.2.2", "3.3.3.3")));
    }

    @Test void persistence_survivesReinit() {
        store.addTag("4.4.4.4", "persisted");
        store.setFavorite("4.4.4.4", true);
        store.setDataDir(tmp);
        assertTrue(store.getTags("4.4.4.4").contains("persisted"));
        assertTrue(store.isFavorite("4.4.4.4"));
    }

    @Test void getTags_isUnmodifiable() {
        store.addTag("5.5.5.5", "a");
        assertThrows(UnsupportedOperationException.class,
                () -> store.getTags("5.5.5.5").add("b"));
    }

    @Test void setDataDir_emptyDir_startsEmpty() {
        assertTrue(store.getAllTaggedIps().isEmpty());
    }
}

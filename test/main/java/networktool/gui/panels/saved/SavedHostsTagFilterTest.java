package main.java.networktool.gui.panels.saved;

import main.java.networktool.gui.panels.tags.HostTagStore;
import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class SavedHostsTagFilterTest {

    @TempDir Path tmp;
    HostTagStore store;
    final List<HostResult> hosts = List.of(
            new HostResult("1.1.1.1", "a", "Linux"),
            new HostResult("1.1.1.2", "b", "Win"),
            new HostResult("1.1.1.3", "c", "Mac"));

    @BeforeEach void setup() {
        store = HostTagStore.getInstance();
        store.setDataDir(tmp);
        store.addTag("1.1.1.1", "Server");
        store.addTag("1.1.1.2", "office");
        store.setFavorite("1.1.1.3", true);
    }

    @Test void byTag_matchesCaseInsensitive() {
        List<HostResult> r = SavedHostsTagFilter.byTag(hosts, " server ", store);
        assertEquals(1, r.size());
        assertEquals("1.1.1.1", r.get(0).ip);
    }

    @Test void byTag_unknownTag_empty() {
        assertTrue(SavedHostsTagFilter.byTag(hosts, "nope", store).isEmpty());
    }

    @Test void byTag_blankOrNull_empty() {
        assertTrue(SavedHostsTagFilter.byTag(hosts, " ", store).isEmpty());
        assertTrue(SavedHostsTagFilter.byTag(hosts, null, store).isEmpty());
    }

    @Test void favorites_onlyFavorited() {
        List<HostResult> r = SavedHostsTagFilter.favorites(hosts, store);
        assertEquals(1, r.size());
        assertEquals("1.1.1.3", r.get(0).ip);
    }

    @Test void select_blank_returnsFavorites() {
        assertEquals("1.1.1.3", SavedHostsTagFilter.select(hosts, "", store).get(0).ip);
        assertEquals(1, SavedHostsTagFilter.select(hosts, null, store).size());
    }

    @Test void select_tag_returnsTagged() {
        assertEquals("1.1.1.2", SavedHostsTagFilter.select(hosts, "office", store).get(0).ip);
    }

    @Test void emptyHosts_empty() {
        assertTrue(SavedHostsTagFilter.select(List.of(), "server", store).isEmpty());
    }
}

package main.java.networktool.gui.map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapSnapshotTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void capture_returnsNonNullLists() {
        MapSnapshot snap = MapSnapshot.capture();
        assertNotNull(snap.nodes());
        assertNotNull(snap.edges());
    }

    @Test void capture_snapshotIsImmutable() {
        MapSnapshot snap = MapSnapshot.capture();
        assertThrows(UnsupportedOperationException.class, () -> snap.nodes().add(null));
    }
}

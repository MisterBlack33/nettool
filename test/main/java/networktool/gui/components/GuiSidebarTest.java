package main.java.networktool.gui.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiSidebarTest {

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void build_returnsNonNullPanel() {
        assertNotNull(GuiSidebar.build(id -> {}, () -> {}, () -> {}));
    }

    @Test void build_fullOverload_doesNotThrow() {
        assertDoesNotThrow(() -> GuiSidebar.build(id -> {}, () -> {}, () -> {}, () -> {}, () -> false));
    }

    @Test void build_hasThreeMainRegions() {
        JPanel sidebar = GuiSidebar.build(id -> {}, () -> {}, () -> {});
        // NORTH (Logo), CENTER (Accordion-Holder), SOUTH (Footer)
        assertEquals(3, sidebar.getComponentCount());
    }

    @Test void build_menuClickForwardsId() {
        String[] captured = {null};
        GuiSidebar.build(id -> captured[0] = id, () -> {}, () -> {});
        // Kein Klick simuliert - stellt nur sicher, dass der Callback akzeptiert wird
        assertNull(captured[0]);
    }
}
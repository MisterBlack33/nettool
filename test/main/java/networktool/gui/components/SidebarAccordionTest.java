package main.java.networktool.gui.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SidebarAccordionTest {

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    private static final String[][] ITEMS = {
            {null,  "GROUP_A",     null, "false"},
            {"a1",  "ItemA1",      null, "false"},
            {null,  "GROUP_ADMIN", null, "true"},
            {"x1",  "ItemX1",      null, "false"},
            {null,  "GROUP_B",     null, "false"},
            {"b1",  "ItemB1",      null, "false"},
            {"b2",  "ItemB2",      null, "true"},
    };

    private JPanel container(SidebarAccordion.AccessLevel level) {
        JScrollPane sp = SidebarAccordion.build(ITEMS, level, id -> {});
        return (JPanel) sp.getViewport().getView();
    }

    /** Liest Header-Label und die Button-Menü-IDs jeder Gruppe in Reihenfolge aus. */
    private List<String[]> groupHeaderLabels(JPanel container) {
        List<String[]> result = new ArrayList<>();
        Component[] kids = container.getComponents();
        for (int i = 0; i + 1 < kids.length; i += 2) {
            if (!(kids[i] instanceof JPanel header) || !(kids[i + 1] instanceof JPanel content)) break;
            String label = headerText(header);
            List<String> ids = new ArrayList<>();
            for (Component c : content.getComponents())
                if (c instanceof JButton btn) ids.add((String) btn.getClientProperty("menuId"));
            result.add(new String[]{label, String.join(",", ids)});
        }
        return result;
    }

    private String headerText(JPanel header) {
        for (Component c : header.getComponents())
            if (c instanceof JLabel lbl) return lbl.getText().trim();
        return "";
    }

    // ── USER: admin-only Gruppe komplett unsichtbar ────────────────────────

    @Test
    void user_adminOnlyGroupIsAbsent() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER));
        assertTrue(groups.stream().noneMatch(g -> g[0].equals("GROUP_ADMIN")));
    }

    @Test
    void user_onlyStandardGroupsRemain() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER));
        assertEquals(2, groups.size());
        assertEquals("GROUP_A", groups.get(0)[0]);
        assertEquals("GROUP_B", groups.get(1)[0]);
    }

    /** Regressionstest: Kind-Elemente einer gefilterten Sektion dürfen nicht in GROUP_A landen. */
    @Test
    void user_childOfHiddenGroup_doesNotLeakIntoPreviousGroup() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER));
        String groupAItems = groups.get(0)[1];
        assertEquals("a1", groupAItems);
        assertFalse(groupAItems.contains("x1"));
    }

    @Test
    void user_adminOnlyItemUnderVisibleGroup_isFiltered() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER));
        String groupBItems = groups.get(1)[1];
        assertEquals("b1", groupBItems);
        assertFalse(groupBItems.contains("b2"));
    }

    // ── ADMIN: alles sichtbar ───────────────────────────────────────────────

    @Test
    void admin_allGroupsPresent() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN));
        assertEquals(3, groups.size());
        assertEquals("GROUP_A", groups.get(0)[0]);
        assertEquals("GROUP_ADMIN", groups.get(1)[0]);
        assertEquals("GROUP_B", groups.get(2)[0]);
    }

    @Test
    void admin_adminOnlyGroupContainsItsItem() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN));
        assertEquals("x1", groups.get(1)[1]);
    }

    @Test
    void admin_adminOnlyItemUnderVisibleGroup_isIncluded() {
        List<String[]> groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN));
        assertEquals("b1,b2", groups.get(2)[1]);
    }

    // ── Struktur ─────────────────────────────────────────────────────────

    @Test
    void build_firstStandardGroup_isInitiallyOpen() {
        JPanel c = container(SidebarAccordion.AccessLevel.USER);
        JPanel firstContent = (JPanel) c.getComponents()[1];
        assertTrue(firstContent.isVisible());
    }

    @Test
    void build_secondGroup_isInitiallyClosed() {
        JPanel c = container(SidebarAccordion.AccessLevel.USER);
        JPanel secondContent = (JPanel) c.getComponents()[3];
        assertFalse(secondContent.isVisible());
    }
}
package main.java.networktool.gui;

import org.junit.jupiter.api.*;

import javax.swing.table.DefaultTableModel;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class GuiAuditPanelTest {

    // ── GuiAuditTable ─────────────────────────────────────────────────────

    @Nested
    class GuiAuditTableTest {

        @Test
        void createModel_hasCorrectColumns() {
            DefaultTableModel m = GuiAuditTable.createModel();
            assertEquals(5, m.getColumnCount());
            assertEquals("Zeit",   m.getColumnName(0));
            assertEquals("User",   m.getColumnName(1));
            assertEquals("Aktion", m.getColumnName(2));
            assertEquals("Detail", m.getColumnName(3));
            assertEquals("_full",  m.getColumnName(4));
        }

        @Test
        void createModel_notEditable() {
            DefaultTableModel m = GuiAuditTable.createModel();
            m.addRow(new Object[]{"ts", "u", "A", "d", "full"});
            assertFalse(m.isCellEditable(0, 0));
            assertFalse(m.isCellEditable(0, 3));
        }

        @Test
        void createModel_startsEmpty() {
            assertEquals(0, GuiAuditTable.createModel().getRowCount());
        }

        // actionColor

        @Test
        void actionColor_null_returnsFg() {
            assertEquals(GuiTheme.FG, GuiAuditTable.actionColor(null));
        }

        @Test
        void actionColor_empty_returnsFg() {
            assertEquals(GuiTheme.FG, GuiAuditTable.actionColor(""));
        }

        @Test
        void actionColor_login_returnsAccent2() {
            assertEquals(GuiTheme.ACCENT2, GuiAuditTable.actionColor("LOGIN"));
        }

        @Test
        void actionColor_loginFailed_returnsWarn() {
            assertEquals(GuiTheme.WARN, GuiAuditTable.actionColor("LOGIN_FAILED"));
        }

        @Test
        void actionColor_loginBlocked_returnsWarn() {
            assertEquals(GuiTheme.WARN, GuiAuditTable.actionColor("LOGIN_BLOCKED"));
        }

        @Test
        void actionColor_securityAlert_returnsOrange() {
            Color c = GuiAuditTable.actionColor("SECURITY_ALERT_ARP");
            assertEquals(new Color(0xFF, 0xA0, 0x30), c);
        }

        @Test
        void actionColor_arpMonitor_returnsOrange() {
            assertEquals(new Color(0xFF, 0xA0, 0x30), GuiAuditTable.actionColor("ARP_MONITOR_START"));
        }

        @Test
        void actionColor_scan_returnsInfo() {
            assertEquals(GuiTheme.INFO, GuiAuditTable.actionColor("SCAN_FULL"));
        }

        @Test
        void actionColor_cidr_returnsInfo() {
            assertEquals(GuiTheme.INFO, GuiAuditTable.actionColor("CIDR_SCAN"));
        }

        @Test
        void actionColor_diagnose_returnsInfo() {
            assertEquals(GuiTheme.INFO, GuiAuditTable.actionColor("DIAGNOSE_QUICK"));
        }

        @Test
        void actionColor_export_returnsYellow() {
            assertEquals(new Color(0xD0, 0xC0, 0x60), GuiAuditTable.actionColor("EXPORT_CSV"));
        }

        @Test
        void actionColor_import_returnsYellow() {
            assertEquals(new Color(0xD0, 0xC0, 0x60), GuiAuditTable.actionColor("IMPORT_JSON"));
        }

        @Test
        void actionColor_userCreated_returnsAccent() {
            assertEquals(GuiTheme.ACCENT, GuiAuditTable.actionColor("USER_CREATED"));
        }

        @Test
        void actionColor_appStart_returnsAccent() {
            assertEquals(GuiTheme.ACCENT, GuiAuditTable.actionColor("APP_START"));
        }

        @Test
        void actionColor_arpSpoof_returnsWarn() {
            assertEquals(GuiTheme.WARN, GuiAuditTable.actionColor("ARP_SPOOFING"));
        }

        @Test
        void actionColor_rogueDevice_returnsWarn() {
            assertEquals(GuiTheme.WARN, GuiAuditTable.actionColor("ROGUE_DEVICE"));
        }

        @Test
        void actionColor_caseInsensitive() {
            assertEquals(GuiTheme.ACCENT2, GuiAuditTable.actionColor("login"));
        }

        @Test
        void actionColor_unknown_returnsFg() {
            assertEquals(GuiTheme.FG, GuiAuditTable.actionColor("SOME_UNKNOWN_ACTION"));
        }

        @Test
        void buildTable_notNull() {
            assertNotNull(GuiAuditTable.buildTable(GuiAuditTable.createModel()));
        }

        @Test
        void buildTable_rowHeight() {
            var table = GuiAuditTable.buildTable(GuiAuditTable.createModel());
            assertEquals(19, table.getRowHeight());
        }

        @Test
        void buildTable_notEditable() {
            var model = GuiAuditTable.createModel();
            model.addRow(new Object[]{"ts", "u", "A", "d", "full"});
            var table = GuiAuditTable.buildTable(model);
            assertFalse(table.isCellEditable(0, 0));
        }

        @Test
        void buildTable_hiddenColumnZeroWidth() {
            var table = GuiAuditTable.buildTable(GuiAuditTable.createModel());
            assertEquals(0, table.getColumnModel().getColumn(4).getMaxWidth());
        }

        @Test
        void buildTable_headerNotReorderable() {
            var table = GuiAuditTable.buildTable(GuiAuditTable.createModel());
            assertFalse(table.getTableHeader().getReorderingAllowed());
        }
    }

    // ── GuiAuditLegend ────────────────────────────────────────────────────

    @Nested
    class GuiAuditLegendTest {

        @Test
        void build_notNull() {
            assertNotNull(GuiAuditLegend.build(GuiTheme.PANEL_BG));
        }

        @Test
        void build_hasToggleButton() {
            var panel = GuiAuditLegend.build(GuiTheme.PANEL_BG);
            boolean hasButton = false;
            for (var c : panel.getComponents()) {
                if (c instanceof javax.swing.JButton b && b.getText().contains("Aktions-Codes")) {
                    hasButton = true; break;
                }
            }
            assertTrue(hasButton);
        }

        @Test
        void build_hasHintLabel() {
            var panel = GuiAuditLegend.build(GuiTheme.PANEL_BG);
            boolean found = false;
            for (var c : panel.getComponents()) {
                if (c instanceof javax.swing.JLabel l && l.getText().contains("Rotation")) {
                    found = true; break;
                }
            }
            assertTrue(found);
        }

        @Test
        void build_differentBgColors_noThrow() {
            assertDoesNotThrow(() -> GuiAuditLegend.build(new Color(0x0F, 0x13, 0x10)));
            assertDoesNotNull(() -> GuiAuditLegend.build(new Color(0xE8, 0xE6, 0xE0)));
        }

        // helper to avoid repetition
        private void assertDoesNotNull(java.util.function.Supplier<?> s) {
            assertNotNull(s.get());
        }
    }
}
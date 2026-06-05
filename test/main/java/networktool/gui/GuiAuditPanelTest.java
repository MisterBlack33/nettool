package main.java.networktool.gui;

import main.java.networktool.security.AuditLogger;
import org.junit.jupiter.api.*;

import java.awt.Color;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für GuiAuditPanel.actionColor() – package-private via Reflection.
 * Läuft headless (kein Display nötig).
 */
class GuiAuditPanelTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static Color actionColor(String action) {
        try {
            Method m = GuiAuditPanel.class.getDeclaredMethod("actionColor", String.class);
            m.setAccessible(true);
            return (Color) m.invoke(null, action);
        } catch (Exception e) {
            throw new RuntimeException("actionColor reflection failed: " + e.getMessage(), e);
        }
    }

    @Nested
    class GuiAuditTableTest {

        @Test void actionColor_login_returnsAccent2()              { assertEquals(GuiTheme.ACCENT2, actionColor("LOGIN")); }
        @Test void actionColor_loginFailed_returnsWarn()           { assertEquals(GuiTheme.WARN, actionColor("LOGIN_FAILED")); }
        @Test void actionColor_loginBlocked_returnsWarn()          { assertEquals(GuiTheme.WARN, actionColor("LOGIN_BLOCKED")); }
        @Test void actionColor_securityAlert_returnsWarn()         { assertEquals(GuiTheme.WARN, actionColor("SECURITY_ALERT_ARP_SPOOFING")); }
        @Test void actionColor_securityMonitorStart_returnsOrange(){ assertEquals(new Color(0xFF,0xA0,0x30), actionColor("SECURITY_MONITOR_START")); }
        @Test void actionColor_arpMonitor_returnsOrange()          { assertEquals(new Color(0xFF,0xA0,0x30), actionColor("ARP_MONITOR_START")); }
        @Test void actionColor_portMonitor_returnsOrange()         { assertEquals(new Color(0xFF,0xA0,0x30), actionColor("PORT_MONITOR_START")); }
        @Test void actionColor_scan_returnsInfo()                  { assertEquals(GuiTheme.INFO, actionColor("SCAN_CIDR")); }
        @Test void actionColor_exportCsv_returnsYellow()           { assertEquals(new Color(0xD0,0xC0,0x60), actionColor("EXPORT_CSV")); }
        @Test void actionColor_userCreated_returnsAccent()         { assertEquals(GuiTheme.ACCENT, actionColor("USER_CREATED")); }
        @Test void actionColor_appStart_returnsAccent()            { assertEquals(GuiTheme.ACCENT, actionColor("APP_START")); }
        @Test void actionColor_null_returnsFg()                    { assertEquals(GuiTheme.FG, actionColor(null)); }
        @Test void actionColor_unknown_returnsFg()                 { assertEquals(GuiTheme.FG, actionColor("SOME_UNKNOWN_ACTION")); }
        @Test void actionColor_rogueDevice_returnsWarn()           { assertEquals(GuiTheme.WARN, actionColor("ROGUE_DEVICE")); }
        @Test void actionColor_arpSpoofing_returnsWarn()           { assertEquals(GuiTheme.WARN, actionColor("ARP_SPOOFING")); }
    }

    @Nested
    class AuditLoggerParseTest {
        @Test void parse_validTabLine_notNull() { assertNotNull(AuditLogger.parse("2024-01-01 10:00:00\tuser\tLOGIN\tdetail")); }
        @Test void parse_null_returnsNull()     { assertNull(AuditLogger.parse(null)); }
        @Test void parse_empty_returnsNull()    { assertNull(AuditLogger.parse("")); }
    }
}
package main.java.networktool_v3.gui;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für GuiNetworkMap Switch-Erkennungslogik.
 * Nur statische/package-sichtbare Methoden werden direkt getestet.
 */
class GuiNetworkMapTest {

    // ── isEndDevice ───────────────────────────────────────────────────────

    @Test void endDevice_windows()       { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Windows","desktop-pc")); }
    @Test void endDevice_android()       { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Android (Samsung)","galaxy-s23")); }
    @Test void endDevice_samsung_a21()   { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Unbekannt","A21s-von-Elias")); }
    @Test void endDevice_tablet_hn()     { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Unbekannt","My-iPad-Pro")); }
    @Test void generic_hostname_stays_host() {
        // "Primary-Workspace" hat kein bekanntes Keyword → bleibt HOST, wird NICHT Switch
        assertFalse(GuiNetworkMap.isEndDevice("1.1.1.1","Unbekannt","Primary-Workspace"));
        assertFalse(GuiNetworkMap.isSwitchByKeyword("Unbekannt","Primary-Workspace"));
        // → korrekt: bleibt HOST, kein falsches Switch-Promoten
    }
    @Test void endDevice_drucker_os()    { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Drucker (JetDirect)","Drucker.fritz.box")); }
    @Test void endDevice_linux()         { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Linux/Unix","srv")); }
    @Test void endDevice_macos()         { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","macOS","macbook-pro")); }
    @Test void endDevice_raspberry()     { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","Raspberry Pi (Linux)","raspberrypi")); }
    @Test void endDevice_iphone_hn()     { assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1","","my-iphone")); }
    @Test void notEndDevice_unknown()    { assertFalse(GuiNetworkMap.isEndDevice("1.1.1.1","Unbekannt","host-192-168-178-36")); }
    @Test void notEndDevice_switch_kw()  { assertFalse(GuiNetworkMap.isEndDevice("1.1.1.1","Switch","sw-office")); }

    // ── isSwitchByKeyword ─────────────────────────────────────────────────

    @Test void keyword_switch_os()       { assertTrue(GuiNetworkMap.isSwitchByKeyword("Router / Switch","any")); }
    @Test void keyword_fritz_hn()        { assertTrue(GuiNetworkMap.isSwitchByKeyword("","fritz.box")); }
    @Test void keyword_unifi_hn()        { assertTrue(GuiNetworkMap.isSwitchByKeyword("","unifi-ap")); }
    @Test void keyword_sw_dash_hn()      { assertTrue(GuiNetworkMap.isSwitchByKeyword("","sw-office")); }
    @Test void keyword_dash_sw_hn()      { assertTrue(GuiNetworkMap.isSwitchByKeyword("","core-sw")); }
    @Test void keyword_procurve_os()     { assertTrue(GuiNetworkMap.isSwitchByKeyword("procurve","hp-2530")); }
    @Test void keyword_no_match()        { assertFalse(GuiNetworkMap.isSwitchByKeyword("Unbekannt","host-192-168-1-5")); }
    @Test void keyword_windows_no()      { assertFalse(GuiNetworkMap.isSwitchByKeyword("Windows","desktop-work")); }
    @Test void keyword_null_safe()       { assertFalse(GuiNetworkMap.isSwitchByKeyword(null, null)); }

    // ── isSwitchByOui ─────────────────────────────────────────────────────

    @Test void oui_cisco_match()         { assertTrue(GuiNetworkMap.isSwitchByOui("sw [00:1A:A1:12:34:56]")); }
    @Test void oui_hp_procurve()         { assertTrue(GuiNetworkMap.isSwitchByOui("hp [00:17:A4:AB:CD:EF]")); }
    @Test void oui_no_bracket()          { assertFalse(GuiNetworkMap.isSwitchByOui("plain-hostname")); }
    @Test void oui_unknown_mac()         { assertFalse(GuiNetworkMap.isSwitchByOui("device [FF:FF:FF:12:34:56]")); }
    @Test void oui_null()                { assertFalse(GuiNetworkMap.isSwitchByOui(null)); }
    @Test void oui_empty_bracket()       { assertFalse(GuiNetworkMap.isSwitchByOui("host []")); }
    @Test void oui_dash_format()         { assertTrue(GuiNetworkMap.isSwitchByOui("sw [00-1A-A1-12-34-56]")); }

    // ── subnet24 / lastOctet ──────────────────────────────────────────────

    @Test void subnet24_normal()         { assertEquals("192.168.1",  GuiNetworkMap.subnet24("192.168.1.50")); }
    @Test void subnet24_null()           { assertNull(GuiNetworkMap.subnet24(null)); }
    @Test void subnet24_no_dot()         { assertNull(GuiNetworkMap.subnet24("noip")); }
    @Test void lastOctet_normal()        { assertEquals(36, GuiNetworkMap.lastOctet("192.168.178.36")); }
    @Test void lastOctet_null()          { assertEquals(999, GuiNetworkMap.lastOctet(null)); }

    // ── Persistenz MANUAL_SWITCHES ────────────────────────────────────────

    @Test void saveAndLoad_manualSwitches() {
        GuiNetworkMap.MANUAL_SWITCHES.clear();
        GuiNetworkMap.MANUAL_SWITCHES.add("192.168.1.36");
        GuiNetworkMap.saveManualSwitches();
        GuiNetworkMap.MANUAL_SWITCHES.clear();
        GuiNetworkMap.loadManualSwitches();
        assertTrue(GuiNetworkMap.MANUAL_SWITCHES.contains("192.168.1.36"));
        GuiNetworkMap.MANUAL_SWITCHES.remove("192.168.1.36");
        GuiNetworkMap.saveManualSwitches();
    }

    @Test void saveManualSwitches_doesNotThrow() {
        assertDoesNotThrow(GuiNetworkMap::saveManualSwitches);
    }

    @Test void loadManualSwitches_doesNotThrow() {
        assertDoesNotThrow(GuiNetworkMap::loadManualSwitches);
    }

    // ── HOP_PARENT ────────────────────────────────────────────────────────

    @Test void hopParent_put_and_get() {
        GuiNetworkMap.HOP_PARENT.put("192.168.1.50", "192.168.1.36");
        assertEquals("192.168.1.36", GuiNetworkMap.HOP_PARENT.get("192.168.1.50"));
        GuiNetworkMap.HOP_PARENT.remove("192.168.1.50");
    }

    @Test void hopParent_clear() {
        GuiNetworkMap.HOP_PARENT.put("10.0.0.1", "10.0.0.254");
        GuiNetworkMap.HOP_PARENT.clear();
        assertTrue(GuiNetworkMap.HOP_PARENT.isEmpty());
    }

    @Test void hopParent_missing_returns_null() {
        assertNull(GuiNetworkMap.HOP_PARENT.get("99.99.99.99"));
    }

    @Test void runHopDiscovery_doesNotThrow() {
        // Keine Hosts → kein Crash
        assertDoesNotThrow(GuiNetworkMap::runHopDiscovery);
    }

    @Test void runHopDiscovery_noGateway_doesNotThrow() {
        // Auch ohne erreichbaren Gateway kein Crash
        assertDoesNotThrow(GuiNetworkMap::runHopDiscovery);
    }

    @Test void manualSwitch_addRemove() {
        String ip = "10.0.0.99";
        GuiNetworkMap.MANUAL_SWITCHES.add(ip);
        assertTrue(GuiNetworkMap.MANUAL_SWITCHES.contains(ip));
        GuiNetworkMap.MANUAL_SWITCHES.remove(ip);
        assertFalse(GuiNetworkMap.MANUAL_SWITCHES.contains(ip));
    }

    // ── isEndDevice verhindert Switch-Promotion ───────────────────────────

    @Test void endDevice_blocks_keyword() {
        // "router" im OS, aber Windows → kein Switch
        assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1", "Windows Router", "desktop"));
    }

    @Test void endDevice_a21_not_promoted() {
        // Samsung A21s darf nie als Switch erkannt werden
        assertFalse(GuiNetworkMap.isSwitchByKeyword("Unbekannt", "A21s-von-Elias"));
        assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1", "Unbekannt", "A21s-von-Elias"));
    }

    @Test void endDevice_w11_not_promoted() {
        // W11 PC darf nie als Switch erkannt werden
        assertTrue(GuiNetworkMap.isEndDevice("1.1.1.1", "Windows", "W11-Pro-Remote"));
    }
}
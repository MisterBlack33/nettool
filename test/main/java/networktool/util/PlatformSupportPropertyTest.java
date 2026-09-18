package main.java.networktool.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Property-artige Tests für die exec()-Eingabe-Validatoren aus PlatformSupport. */
class PlatformSupportPropertyTest {

    @ParameterizedTest
    @ValueSource(strings = {"; rm -rf /", "&& calc", "| id", "`id`", "$(id)", "' OR 1=1", "\nid"})
    void isSafeIp_rejectsAnyShellMetacharSuffix(String payload) {
        assertFalse(PlatformSupport.isSafeIp("1.2.3.4" + payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"; rm -rf /", "&& calc", "| id", "`id`", "$(id)"})
    void isSafeInterface_rejectsShellMetachars(String payload) {
        assertFalse(PlatformSupport.isSafeInterface("eth0" + payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"; rm -rf /", "&& calc", "| id", "`id`"})
    void isSafeMac_rejectsShellMetachars(String payload) {
        assertFalse(PlatformSupport.isSafeMac("AA:BB:CC:DD:EE:FF" + payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "1.1.1.1", "255.255.255.255", "10.20.30.40"})
    void isSafeIp_acceptsWellFormedIps(String ip) {
        assertTrue(PlatformSupport.isSafeIp(ip));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:11:22:33:44:55", "AA-BB-CC-DD-EE-FF", "ff:ff:ff:ff:ff:ff"})
    void isSafeMac_acceptsWellFormedMacs(String mac) {
        assertTrue(PlatformSupport.isSafeMac(mac));
    }
}

package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformUtilsTest {
    @Test void isWindows_returnsBoolean() { assertNotNull(PlatformUtils.isWindows()); }
    @Test void isMac_returnsBoolean() { assertNotNull(PlatformUtils.isMac()); }
    @Test void isLinux_returnsBoolean() { assertNotNull(PlatformUtils.isLinux()); }
    @Test void isSafeIp_valid() { assertTrue(PlatformUtils.isSafeIp("10.0.0.1")); }
    @Test void isSafeIp_invalid() { assertFalse(PlatformUtils.isSafeIp("10.0.0.1; rm -rf")); }
    @Test void isSafeIp_null() { assertFalse(PlatformUtils.isSafeIp(null)); }
    @Test void isSafeInterface_valid() { assertTrue(PlatformUtils.isSafeInterface("eth0")); }
    @Test void isSafeInterface_invalid() { assertFalse(PlatformUtils.isSafeInterface("eth0; calc")); }
    @Test void isSafeMac_colonForm() { assertTrue(PlatformUtils.isSafeMac("AA:BB:CC:DD:EE:FF")); }
    @Test void isSafeMac_dashForm() { assertTrue(PlatformUtils.isSafeMac("AA-BB-CC-DD-EE-FF")); }
    @Test void isSafeMac_invalid() { assertFalse(PlatformUtils.isSafeMac("not-a-mac")); }
    @Test void isSafeCidr_valid() { assertTrue(PlatformUtils.isSafeCidr("192.168.0.0/24")); }
    @Test void isSafeCidr_invalid() { assertFalse(PlatformUtils.isSafeCidr("192.168.0.0/24; evil")); }
    @Test void isSafeHostname_valid() { assertTrue(PlatformUtils.isSafeHostname("my-host.local")); }
    @Test void isSafeHostname_invalid() { assertFalse(PlatformUtils.isSafeHostname("host; rm -rf /")); }
    @Test void isSafeSubnetPrefix_valid() { assertTrue(PlatformUtils.isSafeSubnetPrefix("192.168.1")); }
    @Test void isSafeSubnetPrefix_invalid() { assertFalse(PlatformUtils.isSafeSubnetPrefix("192.168.1.5")); }
    @Test void requireSafeIp_valid_returnsInput() { assertEquals("1.2.3.4", PlatformUtils.requireSafeIp("1.2.3.4")); }
    @Test void requireSafeIp_invalid_throws() { assertThrows(IllegalArgumentException.class, () -> PlatformUtils.requireSafeIp("1.2.3.4; bad")); }
    @Test void requireSafeInterface_valid_returnsInput() { assertEquals("eth0", PlatformUtils.requireSafeInterface("eth0")); }
    @Test void requireSafeInterface_invalid_throws() { assertThrows(IllegalArgumentException.class, () -> PlatformUtils.requireSafeInterface("bad iface!")); }
    @Test void requireSafeMac_valid_returnsInput() { assertEquals("AA:BB:CC:DD:EE:FF", PlatformUtils.requireSafeMac("AA:BB:CC:DD:EE:FF")); }
    @Test void requireSafeMac_invalid_throws() { assertThrows(IllegalArgumentException.class, () -> PlatformUtils.requireSafeMac("nope")); }
    @Test void requireSafeSubnetPrefix_valid_returnsInput() { assertEquals("10.0.0", PlatformUtils.requireSafeSubnetPrefix("10.0.0")); }
    @Test void requireSafeSubnetPrefix_invalid_throws() { assertThrows(IllegalArgumentException.class, () -> PlatformUtils.requireSafeSubnetPrefix("10.0.0; evil")); }
    @Test void escapePowerShell_quote() { assertTrue(PlatformUtils.escapePowerShell("it's").contains("''")); }
    @Test void escapePowerShell_null() { assertEquals("", PlatformUtils.escapePowerShell(null)); }
    @Test void escapePowerShell_newline() { assertFalse(PlatformUtils.escapePowerShell("a\nb").contains("\n")); }
    @Test void escapeSshArg_backslash() { assertTrue(PlatformUtils.escapeSshArg("a\\b").contains("\\\\")); }
    @Test void escapeSshArg_quote() { assertTrue(PlatformUtils.escapeSshArg("it's").contains("\\'")); }
    @Test void escapeSshArg_null() { assertEquals("", PlatformUtils.escapeSshArg(null)); }
}

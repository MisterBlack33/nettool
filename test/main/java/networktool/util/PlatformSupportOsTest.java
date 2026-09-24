package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformSupportOsTest {

    private static final String OS = System.getProperty("os.name", "").toLowerCase();

    @Test void isWindows_matchesOsName() { assertEquals(OS.contains("win"), PlatformSupport.isWindows()); }
    @Test void isMac_matchesOsName()     { assertEquals(OS.contains("mac"), PlatformSupport.isMac()); }
    @Test void isLinux_matchesOsName() {
        assertEquals(OS.contains("linux") || OS.contains("nix") || OS.contains("nux"), PlatformSupport.isLinux());
    }
    @Test void escapePowerShell_newlineReplaced() {
        assertFalse(PlatformSupport.escapePowerShell("a\nb").contains("\n"));
    }
    @Test void requireSafeInterface_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> PlatformSupport.requireSafeInterface("bad iface!"));
    }
    @Test void requireSafeMac_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> PlatformSupport.requireSafeMac("nope"));
    }
}

package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformUtilsSubnetTest {

    @Test void isSafeSubnetPrefix_valid()        { assertTrue(PlatformUtils.isSafeSubnetPrefix("192.168.1")); }
    @Test void isSafeSubnetPrefix_null()          { assertFalse(PlatformUtils.isSafeSubnetPrefix(null)); }
    @Test void isSafeSubnetPrefix_extraOctet()    { assertFalse(PlatformUtils.isSafeSubnetPrefix("192.168.1.5")); }
    @Test void isSafeSubnetPrefix_injection()     { assertFalse(PlatformUtils.isSafeSubnetPrefix("192.168.1'; calc")); }
    @Test void isSafeSubnetPrefix_tooFewOctets()  { assertFalse(PlatformUtils.isSafeSubnetPrefix("192.168")); }

    @Test void requireSafeSubnetPrefix_valid_returnsInput() {
        assertEquals("10.0.0", PlatformUtils.requireSafeSubnetPrefix("10.0.0"));
    }

    @Test void requireSafeSubnetPrefix_invalid_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PlatformUtils.requireSafeSubnetPrefix("10.0.0; evil"));
    }
}
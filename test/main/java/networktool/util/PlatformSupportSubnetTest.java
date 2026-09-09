package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformSupportSubnetTest {

    @Test void isSafeSubnetPrefix_valid()        { assertTrue(PlatformSupport.isSafeSubnetPrefix("192.168.1")); }
    @Test void isSafeSubnetPrefix_null()          { assertFalse(PlatformSupport.isSafeSubnetPrefix(null)); }
    @Test void isSafeSubnetPrefix_extraOctet()    { assertFalse(PlatformSupport.isSafeSubnetPrefix("192.168.1.5")); }
    @Test void isSafeSubnetPrefix_injection()     { assertFalse(PlatformSupport.isSafeSubnetPrefix("192.168.1'; calc")); }
    @Test void isSafeSubnetPrefix_tooFewOctets()  { assertFalse(PlatformSupport.isSafeSubnetPrefix("192.168")); }

    @Test void requireSafeSubnetPrefix_valid_returnsInput() {
        assertEquals("10.0.0", PlatformSupport.requireSafeSubnetPrefix("10.0.0"));
    }

    @Test void requireSafeSubnetPrefix_invalid_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PlatformSupport.requireSafeSubnetPrefix("10.0.0; evil"));
    }
}

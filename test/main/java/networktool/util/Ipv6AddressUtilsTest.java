package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Ipv6AddressUtilsTest {

    @Test void isValidIpv6_full()          { assertTrue(Ipv6AddressUtils.isValidIpv6("2001:0db8:0000:0000:0000:0000:0000:0001")); }
    @Test void isValidIpv6_compressed()    { assertTrue(Ipv6AddressUtils.isValidIpv6("2001:db8::1")); }
    @Test void isValidIpv6_loopback()      { assertTrue(Ipv6AddressUtils.isValidIpv6("::1")); }
    @Test void isValidIpv6_unspecified()   { assertTrue(Ipv6AddressUtils.isValidIpv6("::")); }
    @Test void isValidIpv6_null_false()    { assertFalse(Ipv6AddressUtils.isValidIpv6(null)); }
    @Test void isValidIpv6_ipv4_false()    { assertFalse(Ipv6AddressUtils.isValidIpv6("192.168.1.1")); }
    @Test void isValidIpv6_garbage_false() { assertFalse(Ipv6AddressUtils.isValidIpv6("not an ip")); }
    @Test void isValidIpv6_tooManyGroups() { assertFalse(Ipv6AddressUtils.isValidIpv6("1:2:3:4:5:6:7:8:9")); }
    @Test void isValidIpv6_trimsWhitespace() { assertTrue(Ipv6AddressUtils.isValidIpv6("  ::1  ")); }

    @Test void isValidCidr_valid()         { assertTrue(Ipv6AddressUtils.isValidCidr("2001:db8::/32")); }
    @Test void isValidCidr_maxPrefix()     { assertTrue(Ipv6AddressUtils.isValidCidr("::1/128")); }
    @Test void isValidCidr_noSlash_false() { assertFalse(Ipv6AddressUtils.isValidCidr("2001:db8::1")); }
    @Test void isValidCidr_prefixTooBig()  { assertFalse(Ipv6AddressUtils.isValidCidr("::1/129")); }
    @Test void isValidCidr_negativePrefix(){ assertFalse(Ipv6AddressUtils.isValidCidr("::1/-1")); }
    @Test void isValidCidr_nonNumeric()    { assertFalse(Ipv6AddressUtils.isValidCidr("::1/abc")); }
    @Test void isValidCidr_null_false()    { assertFalse(Ipv6AddressUtils.isValidCidr(null)); }
    @Test void isValidCidr_invalidAddr()   { assertFalse(Ipv6AddressUtils.isValidCidr("not-an-ip/64")); }

    @Test void parsePrefixLength_present() {
        assertEquals(64, Ipv6AddressUtils.parsePrefixLength("2001:db8::/64").orElseThrow());
    }
    @Test void parsePrefixLength_invalid_empty() {
        assertTrue(Ipv6AddressUtils.parsePrefixLength("bad").isEmpty());
    }

    @Test void expand_compressedMiddle() {
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001", Ipv6AddressUtils.expand("2001:db8::1"));
    }
    @Test void expand_loopback() {
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", Ipv6AddressUtils.expand("::1"));
    }
    @Test void expand_allZero() {
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0000", Ipv6AddressUtils.expand("::"));
    }
    @Test void expand_alreadyFull_unchangedGroups() {
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001",
                Ipv6AddressUtils.expand("2001:0db8:0000:0000:0000:0000:0000:0001"));
    }
    @Test void expand_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Ipv6AddressUtils.expand("not-an-ip"));
    }

    @Test void isLinkLocal_true()  { assertTrue(Ipv6AddressUtils.isLinkLocal("fe80::1")); }
    @Test void isLinkLocal_false() { assertFalse(Ipv6AddressUtils.isLinkLocal("2001:db8::1")); }
    @Test void isLinkLocal_invalidInput_false() { assertFalse(Ipv6AddressUtils.isLinkLocal("garbage")); }
}

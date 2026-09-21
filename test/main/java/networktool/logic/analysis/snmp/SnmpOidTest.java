package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnmpOidTest {

    @Test void parse_dotted() { assertEquals(List.of(1, 3, 6, 1), SnmpOid.parse("1.3.6.1").arcs()); }
    @Test void parse_leadingDot() { assertEquals(SnmpOid.parse("1.3.6"), SnmpOid.parse(".1.3.6")); }
    @Test void parse_trimsWhitespace() { assertEquals("1.3", SnmpOid.parse("  1.3 ").toString()); }
    @Test void parse_null_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse(null)); }
    @Test void parse_blank_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("  ")); }
    @Test void parse_nonNumeric_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("1.3.x")); }
    @Test void parse_emptyArc_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("1..3")); }
    @Test void parse_singleArc_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("1")); }
    @Test void parse_negativeArc_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("1.-3")); }
    @Test void parse_firstArcTooLarge_throws() { assertThrows(IllegalArgumentException.class, () -> SnmpOid.parse("3.1")); }

    @Test void toString_roundtrip() { assertEquals("1.3.6.1.2.1", SnmpOid.parse("1.3.6.1.2.1").toString()); }

    @Test void startsWith_prefix() { assertTrue(SnmpOid.parse("1.3.6.1.2").startsWith(SnmpOid.parse("1.3.6"))); }
    @Test void startsWith_self() { assertTrue(SnmpOid.parse("1.3.6").startsWith(SnmpOid.parse("1.3.6"))); }
    @Test void startsWith_longerPrefix_false() { assertFalse(SnmpOid.parse("1.3").startsWith(SnmpOid.parse("1.3.6"))); }
    @Test void startsWith_differentBranch_false() { assertFalse(SnmpOid.parse("1.3.7.1").startsWith(SnmpOid.parse("1.3.6"))); }

    @Test void lastArc() { assertEquals(9, SnmpOid.parse("1.3.6.9").lastArc()); }
    @Test void child_appendsArc() { assertEquals("1.3.4", SnmpOid.parse("1.3").child(4).toString()); }

    @Test void compareTo_lexicographic() {
        assertTrue(SnmpOid.parse("1.3.6.2").compareTo(SnmpOid.parse("1.3.6.10")) < 0);
    }
    @Test void compareTo_shorterPrefixIsSmaller() {
        assertTrue(SnmpOid.parse("1.3.6").compareTo(SnmpOid.parse("1.3.6.1")) < 0);
    }
    @Test void compareTo_equal() { assertEquals(0, SnmpOid.parse("1.3").compareTo(SnmpOid.parse("1.3"))); }
}

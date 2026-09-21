package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BerCodecTest {

    private static BerReader.Element roundtrip(byte[] encoded) {
        return new BerReader(encoded).next();
    }

    @Test void integer_roundtrip() {
        for (long v : new long[]{0, 1, 127, 128, 255, 65_536, -1, -129, Integer.MAX_VALUE}) {
            assertEquals(v, roundtrip(BerWriter.integer(v)).number().longValue(), "Wert " + v);
        }
    }

    @Test void octetString_roundtrip() {
        assertEquals("public", roundtrip(BerWriter.octetString("public")).text());
    }

    @Test void octetString_longFormLength() {
        String longText = "x".repeat(300);
        assertEquals(longText, roundtrip(BerWriter.octetString(longText)).text());
    }

    @Test void nullValue_hasEmptyContent() {
        BerReader.Element e = roundtrip(BerWriter.nullValue());
        assertEquals(BerTag.NULL, e.tag());
        assertEquals(0, e.content().length);
    }

    @Test void oid_roundtrip_variants() {
        for (String oid : new String[]{"1.3.6.1.2.1.2.2.1.2", "1.3.6.1.4.1.311.1.2", "2.999.3", "0.39", "1.3.6.1.2.16384"}) {
            assertEquals(SnmpOid.parse(oid), roundtrip(BerWriter.oid(SnmpOid.parse(oid))).oid(), oid);
        }
    }

    @Test void unsignedTypes_ignoreSignBit() {
        BerReader.Element gauge = roundtrip(BerWriter.tlv(BerTag.GAUGE32, new byte[]{(byte) 0xFF}));
        assertEquals(255, gauge.number().intValue());
    }

    @Test void number_emptyContent_throws() {
        assertThrows(SnmpProtocolException.class, () -> roundtrip(BerWriter.tlv(BerTag.INTEGER)).number());
    }

    @Test void oid_emptyContent_throws() {
        assertThrows(SnmpProtocolException.class, () -> roundtrip(BerWriter.tlv(BerTag.OID)).oid());
    }

    @Test void oid_arcOverflow_throws() {
        byte[] huge = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x01};
        assertThrows(SnmpProtocolException.class, () -> roundtrip(BerWriter.tlv(BerTag.OID, huge)).oid());
    }

    @Test void reader_emptyInput_throws() {
        assertThrows(SnmpProtocolException.class, () -> new BerReader(new byte[0]).next());
    }

    @Test void reader_missingLength_throws() {
        assertThrows(SnmpProtocolException.class, () -> new BerReader(new byte[]{0x04}).next());
    }

    @Test void reader_truncatedContent_throws() {
        assertThrows(SnmpProtocolException.class, () -> new BerReader(new byte[]{0x04, 0x05, 0x01}).next());
    }

    @Test void reader_invalidLengthByteCount_throws() {
        byte[] bad = {0x04, (byte) 0x85, 0, 0, 0, 0, 1};
        assertThrows(SnmpProtocolException.class, () -> new BerReader(bad).next());
    }

    @Test void reader_unexpectedTag_throws() {
        byte[] encoded = BerWriter.integer(1);
        assertThrows(SnmpProtocolException.class, () -> new BerReader(encoded).next(BerTag.OCTET_STRING));
    }

    @Test void reader_hasMore_tracksPosition() {
        BerReader reader = new BerReader(BerWriter.integer(1));
        assertTrue(reader.hasMore());
        reader.next();
        assertFalse(reader.hasMore());
    }
}

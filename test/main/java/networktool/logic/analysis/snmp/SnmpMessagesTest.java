package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SnmpMessagesTest {

    private static final SnmpOid OID = SnmpOid.parse("1.3.6.1.2.1.1.1.0");

    private static Optional<SnmpVarBind> parse(byte[] valueTlv) {
        return SnmpMessages.parseResponse(FakeSnmpAgent.response(7, 0, OID, valueTlv), 7);
    }

    @Test void request_containsCommunityIdAndOid() {
        byte[] request = SnmpMessages.getNextRequest("secret", 42, OID);
        FakeSnmpAgent.Request parsed = FakeSnmpAgent.parse(request);
        assertEquals(42, parsed.id());
        assertEquals(OID, parsed.oid());
        BerReader message = new BerReader(request).next(BerTag.SEQUENCE).reader();
        assertEquals(1, message.next().number().intValue());
        assertEquals("secret", message.next().text());
    }

    @Test void response_integer() { assertEquals("42", parse(BerWriter.integer(42)).orElseThrow().value()); }
    @Test void response_octetString() { assertEquals("eth0", parse(BerWriter.octetString("eth0")).orElseThrow().value()); }
    @Test void response_oidValue() {
        assertEquals("1.3.6.1", parse(BerWriter.oid(SnmpOid.parse("1.3.6.1"))).orElseThrow().value());
    }
    @Test void response_gauge() {
        byte[] gauge = BerWriter.tlv(BerTag.GAUGE32, new byte[]{0x3B, (byte) 0x9A, (byte) 0xCA, 0x00});
        assertEquals("1000000000", parse(gauge).orElseThrow().value());
    }
    @Test void response_unknownType_emptyValue() { assertEquals("", parse(BerWriter.nullValue()).orElseThrow().value()); }
    @Test void response_carriesOid() { assertEquals(OID, parse(BerWriter.integer(1)).orElseThrow().oid()); }

    @Test void response_endOfMib_empty() {
        assertTrue(parse(BerWriter.tlv(BerTag.END_OF_MIB_VIEW)).isEmpty());
    }
    @Test void response_noSuchObject_empty() {
        assertTrue(parse(BerWriter.tlv(BerTag.NO_SUCH_OBJECT)).isEmpty());
    }
    @Test void response_noSuchInstance_empty() {
        assertTrue(parse(BerWriter.tlv(BerTag.NO_SUCH_INSTANCE)).isEmpty());
    }
    @Test void response_errorStatus_empty() {
        byte[] data = FakeSnmpAgent.response(7, 2, OID, BerWriter.integer(1));
        assertTrue(SnmpMessages.parseResponse(data, 7).isEmpty());
    }
    @Test void response_foreignRequestId_throws() {
        byte[] data = FakeSnmpAgent.response(8, 0, OID, BerWriter.integer(1));
        assertThrows(SnmpProtocolException.class, () -> SnmpMessages.parseResponse(data, 7));
    }
    @Test void response_garbage_throws() {
        assertThrows(SnmpProtocolException.class, () -> SnmpMessages.parseResponse(new byte[]{1, 2, 3}, 7));
    }
    @Test void response_requestInsteadOfResponse_throws() {
        byte[] request = SnmpMessages.getNextRequest("public", 7, OID);
        assertThrows(SnmpProtocolException.class, () -> SnmpMessages.parseResponse(request, 7));
    }
}

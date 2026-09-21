package main.java.networktool.logic.analysis.snmp;

import java.util.Optional;

/** Baut SNMPv2c-GETNEXT-Anfragen und wertet die zugehörigen Antworten aus. */
final class SnmpMessages {

    private static final int VERSION_2C = 1;
    private static final int NO_ERROR = 0;

    private SnmpMessages() {}

    static byte[] getNextRequest(String community, int requestId, SnmpOid oid) {
        byte[] varBind = BerWriter.tlv(BerTag.SEQUENCE, BerWriter.oid(oid), BerWriter.nullValue());
        byte[] varBindList = BerWriter.tlv(BerTag.SEQUENCE, varBind);
        byte[] pdu = BerWriter.tlv(BerTag.GET_NEXT_REQUEST,
                BerWriter.integer(requestId), BerWriter.integer(NO_ERROR),
                BerWriter.integer(NO_ERROR), varBindList);
        return BerWriter.tlv(BerTag.SEQUENCE,
                BerWriter.integer(VERSION_2C), BerWriter.octetString(community), pdu);
    }

    /** Leer bei Fehlerstatus oder MIB-Ende; wirft bei unlesbarer oder fremder Antwort. */
    static Optional<SnmpVarBind> parseResponse(byte[] data, int expectedRequestId) {
        BerReader message = new BerReader(data).next(BerTag.SEQUENCE).reader();
        message.next();
        message.next();
        BerReader pdu = message.next(BerTag.RESPONSE).reader();
        int requestId = pdu.next(BerTag.INTEGER).number().intValue();
        if (requestId != expectedRequestId) throw new SnmpProtocolException("Fremde Request-ID");
        int errorStatus = pdu.next(BerTag.INTEGER).number().intValue();
        pdu.next(BerTag.INTEGER);
        BerReader varBind = pdu.next(BerTag.SEQUENCE).reader().next(BerTag.SEQUENCE).reader();
        SnmpOid oid = varBind.next(BerTag.OID).oid();
        BerReader.Element value = varBind.next();
        if (errorStatus != NO_ERROR || isMibException(value.tag())) return Optional.empty();
        return Optional.of(new SnmpVarBind(oid, describe(value)));
    }

    private static boolean isMibException(int tag) {
        return tag == BerTag.NO_SUCH_OBJECT || tag == BerTag.NO_SUCH_INSTANCE
                || tag == BerTag.END_OF_MIB_VIEW;
    }

    private static String describe(BerReader.Element value) {
        return switch (value.tag()) {
            case BerTag.INTEGER, BerTag.COUNTER32, BerTag.GAUGE32,
                 BerTag.TIME_TICKS, BerTag.COUNTER64 -> value.number().toString();
            case BerTag.OCTET_STRING -> value.text();
            case BerTag.OID -> value.oid().toString();
            default -> "";
        };
    }
}

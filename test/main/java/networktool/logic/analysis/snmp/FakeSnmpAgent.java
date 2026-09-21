package main.java.networktool.logic.analysis.snmp;

import java.util.Optional;
import java.util.TreeMap;

/** Simuliert einen SNMP-Agenten mit einer kleinen MIB; beantwortet GETNEXT lexikographisch. */
final class FakeSnmpAgent implements SnmpTransport {

    record Request(int id, SnmpOid oid) {}

    private final TreeMap<SnmpOid, byte[]> mib = new TreeMap<>();
    int requestCount;

    FakeSnmpAgent text(String oid, String value) {
        mib.put(SnmpOid.parse(oid), BerWriter.octetString(value));
        return this;
    }

    FakeSnmpAgent integer(String oid, long value) {
        mib.put(SnmpOid.parse(oid), BerWriter.integer(value));
        return this;
    }

    FakeSnmpAgent gauge(String oid, long value) {
        mib.put(SnmpOid.parse(oid), BerWriter.tlv(BerTag.GAUGE32, java.math.BigInteger.valueOf(value).toByteArray()));
        return this;
    }

    @Override
    public Optional<byte[]> exchange(byte[] request) {
        requestCount++;
        Request parsed = parse(request);
        SnmpOid next = mib.higherKey(parsed.oid());
        if (next == null) {
            return Optional.of(response(parsed.id(), 0, parsed.oid(), BerWriter.tlv(BerTag.END_OF_MIB_VIEW)));
        }
        return Optional.of(response(parsed.id(), 0, next, mib.get(next)));
    }

    static Request parse(byte[] request) {
        BerReader message = new BerReader(request).next(BerTag.SEQUENCE).reader();
        message.next();
        message.next();
        BerReader pdu = message.next(BerTag.GET_NEXT_REQUEST).reader();
        int id = pdu.next(BerTag.INTEGER).number().intValue();
        pdu.next();
        pdu.next();
        BerReader varBind = pdu.next(BerTag.SEQUENCE).reader().next(BerTag.SEQUENCE).reader();
        return new Request(id, varBind.next(BerTag.OID).oid());
    }

    static byte[] response(int id, int errorStatus, SnmpOid oid, byte[] valueTlv) {
        byte[] varBind = BerWriter.tlv(BerTag.SEQUENCE, BerWriter.oid(oid), valueTlv);
        byte[] pdu = BerWriter.tlv(BerTag.RESPONSE, BerWriter.integer(id), BerWriter.integer(errorStatus),
                BerWriter.integer(0), BerWriter.tlv(BerTag.SEQUENCE, varBind));
        return BerWriter.tlv(BerTag.SEQUENCE, BerWriter.integer(1), BerWriter.octetString("public"), pdu);
    }
}

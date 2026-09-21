package main.java.networktool.logic.analysis.snmp;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** BER-Encoding der wenigen Typen, die eine GETNEXT-Anfrage benötigt. */
final class BerWriter {

    private static final int SHORT_FORM_MAX = 0x7F;
    private static final int LONG_FORM_FLAG = 0x80;
    private static final int SUBID_MASK = 0x7F;
    private static final int SUBID_BITS = 7;
    private static final int SUBID_CONTINUATION = 0x80;
    private static final int SUBID_MAX_GROUPS = 5;
    private static final int FIRST_ARC_FACTOR = 40;

    private BerWriter() {}

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (byte[] part : parts) content.writeBytes(part);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        writeLength(out, content.size());
        out.writeBytes(content.toByteArray());
        return out.toByteArray();
    }

    static byte[] integer(long value) {
        return tlv(BerTag.INTEGER, BigInteger.valueOf(value).toByteArray());
    }

    static byte[] octetString(String text) {
        return tlv(BerTag.OCTET_STRING, text.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] nullValue() {
        return tlv(BerTag.NULL);
    }

    static byte[] oid(SnmpOid oid) {
        List<Integer> arcs = oid.arcs();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeSubId(body, (long) arcs.get(0) * FIRST_ARC_FACTOR + arcs.get(1));
        for (int i = 2; i < arcs.size(); i++) writeSubId(body, arcs.get(i));
        return tlv(BerTag.OID, body.toByteArray());
    }

    private static void writeLength(ByteArrayOutputStream out, int length) {
        if (length <= SHORT_FORM_MAX) {
            out.write(length);
            return;
        }
        byte[] raw = BigInteger.valueOf(length).toByteArray();
        int start = raw[0] == 0 ? 1 : 0;
        out.write(LONG_FORM_FLAG | (raw.length - start));
        out.write(raw, start, raw.length - start);
    }

    private static void writeSubId(ByteArrayOutputStream out, long value) {
        byte[] groups = new byte[SUBID_MAX_GROUPS];
        int count = 0;
        long rest = value;
        do {
            groups[count++] = (byte) (rest & SUBID_MASK);
            rest >>= SUBID_BITS;
        } while (rest > 0);
        for (int i = count - 1; i >= 0; i--) {
            out.write(i > 0 ? groups[i] | SUBID_CONTINUATION : groups[i]);
        }
    }
}

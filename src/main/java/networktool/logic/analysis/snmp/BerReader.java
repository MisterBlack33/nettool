package main.java.networktool.logic.analysis.snmp;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sequenzieller BER-Leser über einem Byte-Array. */
final class BerReader {

    private static final int LONG_FORM_FLAG = 0x80;
    private static final int LENGTH_COUNT_MASK = 0x7F;
    private static final int MAX_LENGTH_BYTES = 4;
    private static final int BYTE_MASK = 0xFF;
    private static final int SUBID_MASK = 0x7F;
    private static final int SUBID_BITS = 7;
    private static final int SUBID_CONTINUATION = 0x80;
    private static final int FIRST_ARC_FACTOR = 40;
    private static final int MAX_FIRST_ARC = 2;

    private final byte[] data;
    private int pos;

    BerReader(byte[] data) {
        this.data = data;
    }

    /** Ein gelesenes Tag-Länge-Wert-Element. */
    record Element(int tag, byte[] content) {

        BerReader reader() {
            return new BerReader(content);
        }

        BigInteger number() {
            if (content.length == 0) throw new SnmpProtocolException("Leere Zahl");
            return tag == BerTag.INTEGER ? new BigInteger(content) : new BigInteger(1, content);
        }

        String text() {
            return new String(content, StandardCharsets.UTF_8);
        }

        SnmpOid oid() {
            List<Integer> arcs = new ArrayList<>();
            long value = 0;
            for (byte b : content) {
                value = (value << SUBID_BITS) | (b & SUBID_MASK);
                if (value > Integer.MAX_VALUE) throw new SnmpProtocolException("OID-Arc zu groß");
                if ((b & SUBID_CONTINUATION) == 0) {
                    addArc(arcs, (int) value);
                    value = 0;
                }
            }
            if (arcs.size() < 2) throw new SnmpProtocolException("OID unvollständig");
            return new SnmpOid(arcs);
        }

        private static void addArc(List<Integer> arcs, int subId) {
            if (!arcs.isEmpty()) {
                arcs.add(subId);
                return;
            }
            int first = Math.min(subId / FIRST_ARC_FACTOR, MAX_FIRST_ARC);
            arcs.add(first);
            arcs.add(subId - first * FIRST_ARC_FACTOR);
        }
    }

    boolean hasMore() {
        return pos < data.length;
    }

    Element next() {
        require(pos < data.length, "Nachricht abgeschnitten");
        int tag = data[pos++] & BYTE_MASK;
        int length = readLength();
        require(length >= 0 && pos + length <= data.length, "Ungültige Länge");
        Element element = new Element(tag, Arrays.copyOfRange(data, pos, pos + length));
        pos += length;
        return element;
    }

    Element next(int expectedTag) {
        Element element = next();
        require(element.tag() == expectedTag, "Unerwartetes Tag 0x" + Integer.toHexString(element.tag()));
        return element;
    }

    private int readLength() {
        require(pos < data.length, "Länge fehlt");
        int first = data[pos++] & BYTE_MASK;
        if (first < LONG_FORM_FLAG) return first;
        int count = first & LENGTH_COUNT_MASK;
        require(count >= 1 && count <= MAX_LENGTH_BYTES && pos + count <= data.length, "Ungültige Längenangabe");
        int length = 0;
        for (int i = 0; i < count; i++) length = (length << 8) | (data[pos++] & BYTE_MASK);
        return length;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new SnmpProtocolException(message);
    }
}

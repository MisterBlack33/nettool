package main.java.networktool.logic.analysis.snmp;

/** BER-/SNMP-Tags, die für Walk-Anfragen und -Antworten relevant sind. */
final class BerTag {

    static final int INTEGER = 0x02;
    static final int OCTET_STRING = 0x04;
    static final int NULL = 0x05;
    static final int OID = 0x06;
    static final int SEQUENCE = 0x30;
    static final int COUNTER32 = 0x41;
    static final int GAUGE32 = 0x42;
    static final int TIME_TICKS = 0x43;
    static final int COUNTER64 = 0x46;
    static final int NO_SUCH_OBJECT = 0x80;
    static final int NO_SUCH_INSTANCE = 0x81;
    static final int END_OF_MIB_VIEW = 0x82;
    static final int GET_NEXT_REQUEST = 0xA1;
    static final int RESPONSE = 0xA2;

    private BerTag() {}
}

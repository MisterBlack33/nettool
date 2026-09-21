package main.java.networktool.logic.analysis.snmp;

/** Wird geworfen, wenn eine SNMP-Nachricht nicht dekodiert werden kann. */
public final class SnmpProtocolException extends RuntimeException {

    public SnmpProtocolException(String message) {
        super(message);
    }
}

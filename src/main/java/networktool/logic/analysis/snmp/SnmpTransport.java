package main.java.networktool.logic.analysis.snmp;

import java.util.Optional;

/** Überträgt eine SNMP-Anfrage und liefert die Rohantwort, sofern eine eintrifft. */
public interface SnmpTransport {

    Optional<byte[]> exchange(byte[] request);
}

package main.java.networktool.logic.analysis.snmp;

/** Ein OID/Wert-Paar aus einer SNMP-Antwort; der Wert liegt als Text vor. */
public record SnmpVarBind(SnmpOid oid, String value) {}

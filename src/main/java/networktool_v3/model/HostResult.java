package main.java.networktool_v3.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Repräsentiert einen gefundenen oder gespeicherten Netzwerk-Host.
 *
 * Felder:
 *   ip        – IPv4-Adresse
 *   hostname  – Hostname oder IP falls kein DNS
 *   os        – erkanntes Betriebssystem
 *   savedAt   – Speicherzeitpunkt (gesetzt von SavedHostsStore)
 *   ports     – offene Ports → Banner-Text
 *   notes     – freie Notiz (editierbar in der Gespeicherte-Hosts-Tabelle)
 */
public class HostResult {

    public String              ip;
    public String              hostname;
    public String              os;
    public String              savedAt;
    public Map<Integer, String> ports;
    public String              notes;

    /** Vollständiger Konstruktor. */
    public HostResult(String ip, String hostname, String os, String savedAt,
                      Map<Integer, String> ports, String notes) {
        this.ip       = ip;
        this.hostname = hostname;
        this.os       = os;
        this.savedAt  = savedAt;
        this.ports    = ports != null ? new TreeMap<>(ports) : new TreeMap<>();
        this.notes    = notes != null ? notes : "";
    }

    public HostResult(String ip, String hostname, String os, String savedAt,
                      Map<Integer, String> ports) {
        this(ip, hostname, os, savedAt, ports, "");
    }

    public HostResult(String ip, String hostname, String os, String savedAt) {
        this(ip, hostname, os, savedAt, null, "");
    }

    public HostResult(String ip, String hostname, String os) {
        this(ip, hostname, os, null, null, "");
    }

    public Map<Integer, String> getPorts() {
        return Collections.unmodifiableMap(ports);
    }

    public String portsToString() {
        if (ports == null || ports.isEmpty()) return "";
        return ports.keySet().toString();
    }
}

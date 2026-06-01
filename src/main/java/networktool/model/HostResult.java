package main.java.networktool.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class HostResult {

    public String ip;
    public String hostname;
    public String os;
    public String savedAt;
    public Map<Integer, String> ports;
    public String notes;

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
        return (ports == null || ports.isEmpty()) ? "" : ports.keySet().toString();
    }
}
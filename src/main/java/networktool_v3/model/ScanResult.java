package main.java.networktool_v3.model;

import java.util.Map;

public final class ScanResult {

    private final String ip;
    private final String hostname;
    private final Map<Integer, String> openPorts;
    private final String osGuess;

    public ScanResult(String ip, String hostname, Map<Integer, String> openPorts, String osGuess) {
        this.ip        = ip;
        this.hostname  = hostname;
        this.openPorts = openPorts;
        this.osGuess   = osGuess;
    }

    public String               getIp()        { return ip; }
    public String               getHostname()  { return hostname; }
    public Map<Integer, String> getOpenPorts() { return openPorts; }
    public String               getOsGuess()   { return osGuess; }
}
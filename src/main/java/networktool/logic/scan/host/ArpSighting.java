package main.java.networktool.logic.scan.host;

/** Eine beobachtete ARP-Zuordnung; {@code previousMac} ist null, wenn die IP neu ist. */
public record ArpSighting(String ip, String mac, String previousMac) {

    public boolean isNewHost() {
        return previousMac == null;
    }
}

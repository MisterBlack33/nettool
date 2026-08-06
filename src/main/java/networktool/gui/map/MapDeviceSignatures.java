package main.java.networktool.gui.map;

import java.util.List;
import java.util.Set;

/**
 * Statische Signaturdaten zur Geräteklassifizierung (Endgerät vs. Switch/Router):
 * OS-/Hostname-Keywords, typische Ports und bekannte Switch-Hersteller-OUIs.
 */
final class MapDeviceSignatures {

    private MapDeviceSignatures() {}

    static final List<String> ENDDEVICE_OS = List.of(
            "windows", "android", "ios", "ipad", "ipados", "macos", "apple", "linux", "unix",
            "raspberry", "samsung", "xiaomi", "huawei", "oppo", "realme", "oneplus",
            "drucker", "printer", "jetdirect", "ipp", "lpd", "cups"
    );

    static final List<String> ENDDEVICE_HN = List.of(
            "desktop", "laptop", "phone", "mobile", "tablet", "pad",
            "iphone", "ipad", "galaxy", "pixel", "redmi", "poco",
            // Samsung Galaxy S-Serie (alle Generationen)
            "s10", "s11", "s20", "s21", "s22", "s23", "s24",
            // Samsung Galaxy A-Serie (alle Generationen)
            "a10", "a11", "a12", "a13", "a14", "a15",
            "a20", "a21", "a22", "a23", "a24", "a25",
            "a30", "a31", "a32", "a33", "a34", "a35",
            "a40", "a41", "a42", "a43", "a44", "a45",
            "a50", "a51", "a52", "a53", "a54", "a55",
            "a60", "a70", "a71", "a72", "a73", "a80",
            "a90", "a91",
            // Samsung Galaxy Z-Serie (Foldables)
            "z-flip", "z-fold",
            // Samsung Galaxy Note-Serie
            "note", "sm-a", "sm-g", "sm-s", "sm-n",
            // Drucker und Scanner
            "printer", "drucker", "epson", "canon", "brother", "kyocera", "xerox", "ricoh",
            "macbook", "imac", "workstation"
    );

    static final Set<Integer> ENDDEVICE_PORTS = Set.of(3389, 445, 5985, 5986, 9100, 515, 631);

    // Ports die auf ein Netzwerkinfrastruktur-Gerät hindeuten
    static final Set<Integer> NETWORK_INFRA_PORTS = Set.of(
            161,  // SNMP
            162,  // SNMP trap
            23,   // Telnet (Managed Switches)
            179,  // BGP
            646,  // LDP
            830,  // NETCONF
            4786  // Cisco Smart Install
    );

    // DNS/DHCP-Server = Infrastruktur (kein Endgerät)
    static final Set<Integer> DNS_DHCP_PORTS = Set.of(53, 67, 68);

    static final Set<String> SWITCH_OUIS = Set.of(
            // Cisco
            "00:00:0C", "00:1A:A1", "00:1B:54", "00:1C:57", "00:1D:70", "00:1E:BD",
            "00:1F:CA", "00:21:A0", "00:22:90", "00:23:AC", "00:24:14", "00:25:84",
            "00:26:CB", "00:90:BF", "C8:9C:1D", "D0:72:DC",
            // HP/Aruba/ProCurve
            "00:17:A4", "00:18:71", "00:1A:4B", "00:1C:2E", "00:1F:FE", "00:21:5A",
            "00:22:64", "00:23:47", "00:24:81", "00:25:B3", "00:26:55", "00:30:C1",
            // Juniper
            "3C:D9:2B", "40:B0:34", "50:65:F3", "5C:8A:38", "6C:C2:17", "78:AC:C0",
            "80:C1:6E", "84:34:97", "9C:8E:99", "A8:97:DC", "B4:39:D6", "C4:34:6B",
            "D8:C7:C8", "F0:92:1C", "F4:CE:46",
            // Netgear
            "00:14:6A", "00:1C:10", "00:60:2F", "A0:E0:AF", "CC:46:D6", "E8:40:F2",
            "FC:FB:FB",
            // Ubiquiti
            "50:C7:BF", "AC:84:C9", "C4:6E:1F", "F8:1A:67",
            // MikroTik
            "00:09:5B", "00:0F:B5", "00:14:6C", "00:18:4D",
            "20:E5:2A", "44:94:FC", "60:38:E0", "B0:7F:B9", "C0:3F:0E", "E0:91:F5",
            // TP-Link
            "6C:5A:B0", "A0:F3:C1", "B0:BE:76", "E8:DE:27",
            // D-Link
            "00:05:5D", "00:17:9A", "00:1B:11", "00:26:5A", "1C:7E:E5",
            // Zyxel
            "00:13:49", "00:19:CB", "00:A0:C5", "A4:2B:8C",
            // Fritz!Box / AVM
            "C8:0E:14", "DC:9F:DB", "E0:28:6D"
    );
}

package main.java.networktool_v3.logic.analysis;

public final class OsDetector {

    private OsDetector() {}

    public enum Confidence { HOCH, MITTEL, NIEDRIG }

    public static final class OsResult {
        public final String os;
        public final Confidence confidence;
        public final String method;
        public OsResult(String os, Confidence c, String m) { this.os=os; this.confidence=c; this.method=m; }
        public String display() { return os + " [" + confidence.name().charAt(0) + "]"; }
    }

    public static OsResult detectWithConfidence(String ip) {
        String byPort = OsDetectorPorts.detectByPorts(ip);
        if (!byPort.equals("Unbekannt"))
            return new OsResult(byPort, Confidence.HOCH, "Port");

        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac != null) {
            String vendor = OuiDatabase.lookup(mac);
            if (vendor != null) return new OsResult(vendor, Confidence.MITTEL, "OUI/MAC");
        }

        String fromHostname = detectFromHostnameOnly(ip);
        if (fromHostname != null)
            return new OsResult(fromHostname, Confidence.MITTEL, "Hostname");

        int ttl = OsDetectorArp.getTtl(ip);
        if (ttl <= 0)   return new OsResult("Unbekannt",              Confidence.NIEDRIG, "—");
        if (ttl <= 32)  return new OsResult("Router / Netzwerkgerät", Confidence.NIEDRIG, "TTL<=32");
        if (ttl <= 64)  return new OsResult(resolveLinuxOrMobile(ip, mac), Confidence.NIEDRIG, "TTL<=64");
        if (ttl <= 128) return new OsResult(resolveWindowsOrAndroid(ip, mac), Confidence.NIEDRIG, "TTL<=128");
        return new OsResult(resolveAppleOrNetwork(ip, mac), Confidence.NIEDRIG, "TTL<=255");
    }

    public static String detect(String ip) { return detectWithConfidence(ip).os; }

    public static String detectFromHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip)) return null;
        if (hostname.startsWith("host-")) return null;
        return classifyHostname(hostname.toLowerCase());
    }

    public static boolean isOpen(String ip, int port) {
        return OsDetectorPorts.isOpen(ip, port);
    }

    public static String getMacFromArp(String ip) {
        return OsDetectorArp.getMacFromArp(ip);
    }

    static String classifyHostname(String h) {
        if (h == null) return null;
        if (h.contains("iphone"))                              return "iPhone (iOS)";
        if (h.contains("ipad"))                                return "iPad (iPadOS)";
        if (h.contains("macbook"))                             return "MacBook (macOS)";
        if (h.contains("imac"))                                return "iMac (macOS)";
        if (h.contains("mac-mini")||h.contains("macmini"))    return "Mac mini (macOS)";
        if (h.contains("apple"))                               return "Apple-Gerät";
        if (h.contains("samsung")||h.contains("galaxy")||h.contains("sm-")) return "Android (Samsung)";
        if (h.contains("pixel"))                               return "Android (Google Pixel)";
        if (h.contains("huawei")||h.contains("honor"))         return "Android (Huawei)";
        if (h.contains("xiaomi")||h.contains("redmi")||h.contains("poco")) return "Android (Xiaomi)";
        if (h.contains("oneplus")||h.contains("one-plus"))     return "Android (OnePlus)";
        if (h.contains("nothing"))                             return "Android (Nothing Phone)";
        if (h.contains("oppo"))                                return "Android (OPPO)";
        if (h.contains("realme"))                              return "Android (Realme)";
        if (h.contains("motorola")||h.contains("moto-"))       return "Android (Motorola)";
        if (h.contains("sony")||h.contains("xperia"))          return "Android (Sony)";
        if (h.contains("android"))                             return "Android";
        if (h.contains("desktop-")||h.contains("laptop-")||h.contains("pc-")) return "Windows";
        if (h.contains("windows")||h.contains("workstation"))  return "Windows";
        if (h.contains("raspberry")||h.contains("raspberrypi")) return "Raspberry Pi (Linux)";
        if (h.contains("ubuntu")||h.contains("debian")||h.contains("fedora")
                ||h.contains("centos")||h.contains("arch"))   return "Linux/Unix";
        if (h.contains("linux"))                               return "Linux/Unix";
        if (h.contains("fritz")||h.contains("fritzbox"))       return "Router (FRITZ!Box)";
        if (h.contains("router"))                              return "Router";
        if (h.contains("switch"))                              return "Netzwerk-Switch";
        if (h.contains("ubiquiti")||h.contains("unifi"))       return "Access Point (Ubiquiti)";
        if (h.contains("mikrotik"))                            return "Router (MikroTik)";
        if (h.contains("cisco"))                               return "Cisco-Gerät";
        if (h.contains("synology"))                            return "NAS (Synology)";
        if (h.contains("qnap"))                                return "NAS (QNAP)";
        if (h.contains("nas"))                                 return "NAS";
        if (h.contains("ap-")||h.contains("access-point"))    return "Access Point";
        if (h.contains("printer")||h.contains("print")||h.contains("drucker")) return "Drucker";
        if (h.contains("hp-")||h.contains("epson")||h.contains("canon")
                ||h.contains("brother")||h.contains("kyocera")) return "Drucker";
        if (h.contains("cam")||h.contains("camera"))           return "IP-Kamera";
        if (h.contains("chromecast"))                          return "Chromecast";
        if (h.contains("echo")||h.contains("alexa"))           return "Amazon Echo";
        if (h.contains("fire")||h.contains("kindle"))          return "Amazon-Gerät";
        if (h.contains("sonos"))                               return "Sonos";
        if (h.contains("xbox"))                                return "Xbox";
        if (h.contains("playstation")||h.contains("ps4")||h.contains("ps5")) return "PlayStation";
        if (h.contains("smart")||h.contains("iot"))            return "IoT-Gerät";
        return null;
    }

    private static String detectFromHostnameOnly(String ip) {
        String[] r = {null};
        Thread t = new Thread(() -> {
            try {
                String h = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!h.equals(ip)) r[0] = h;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return r[0] != null ? classifyHostname(r[0].toLowerCase()) : null;
    }

    private static String resolveLinuxOrMobile(String ip, String mac) {
        if (mac != null) { String v = OuiDatabase.lookup(mac); if (v != null) return v; }
        if (isOpen(ip,22)||isOpen(ip,80)||isOpen(ip,443)) return "Linux/Unix";
        return "Mobiles Gerät (Android?)";
    }

    private static String resolveWindowsOrAndroid(String ip, String mac) {
        if (mac != null) { String v = OuiDatabase.lookup(mac); if (v != null) return v; }
        if (isOpen(ip,445)||isOpen(ip,3389)||isOpen(ip,5985)||isOpen(ip,135)) return "Windows";
        if (isOpen(ip,548)||isOpen(ip,5000)) return "macOS";
        return "Android";
    }

    private static String resolveAppleOrNetwork(String ip, String mac) {
        if (mac != null) { String v = OuiDatabase.lookup(mac); if (v != null) return v; }
        if (isOpen(ip,548)||isOpen(ip,5000)) return "macOS";
        if (isOpen(ip,5353))                 return "Apple-Gerät (Bonjour)";
        if (isOpen(ip,23)||isOpen(ip,161))   return "Router / Netzwerkgerät";
        return "iOS / macOS";
    }
}
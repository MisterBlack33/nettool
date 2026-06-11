package main.java.networktool.logic.analysis;

/** Klassifiziert OS/Gerättyp anhand des Hostnamens. Package-private. */
final class OsDetectorHostname {

    private OsDetectorHostname() {}

    static String classify(String hostname) {
        if (hostname == null) return null;
        String h = hostname.toLowerCase();

        // Reihenfolge: spezifisch vor generisch
        String r;
        if ((r = classifyApple(h))   != null) return r;
        if ((r = classifyGaming(h))  != null) return r;  // vor network (Nintendo Switch)
        if ((r = classifyNas(h))     != null) return r;  // vor network (QNAP "switch")
        if ((r = classifyAndroid(h)) != null) return r;
        if ((r = classifyNetwork(h)) != null) return r;
        if ((r = classifyPrinter(h)) != null) return r;
        if ((r = classifyIot(h))     != null) return r;
        return classifyGeneric(h);
    }

    private static String classifyApple(String h) {
        if (h.contains("iphone"))                             return "iPhone (iOS)";
        if (h.contains("ipad"))                               return "iPad (iPadOS)";
        if (h.contains("macbook"))                            return "MacBook (macOS)";
        if (h.contains("imac"))                               return "iMac (macOS)";
        if (h.contains("mac-mini") || h.contains("macmini")) return "Mac mini (macOS)";
        if (h.contains("mac-pro")  || h.contains("macpro"))  return "Mac Pro (macOS)";
        if (h.contains("mac-studio"))                         return "Mac Studio (macOS)";
        if (h.contains("appletv"))                            return "Apple TV";
        if (h.contains("homepod"))                            return "HomePod";
        if (h.contains("apple") || h.contains("airtunes"))   return "Apple-Gerät";
        return null;
    }

    private static String classifyGaming(String h) {
        if (h.contains("xbox"))                                              return "Xbox";
        if (h.contains("playstation") || h.contains("ps4") || h.contains("ps5")) return "PlayStation";
        if (h.contains("nintendo"))                                          return "Nintendo Switch";
        if (h.contains("steamdeck") || h.contains("steam-deck"))             return "Steam Deck";
        return null;
    }

    private static String classifyNas(String h) {
        if (h.contains("synology") || h.contains("diskstation")) return "NAS (Synology)";
        if (h.contains("qnap"))                                  return "NAS (QNAP)";
        if (h.contains("freenas") || h.contains("truenas"))      return "NAS (TrueNAS)";
        if (h.contains("unraid"))                                return "NAS (Unraid)";
        if (h.contains("readynas"))                              return "NAS (NETGEAR ReadyNAS)";
        if (h.contains("nas"))                                   return "NAS";
        return null;
    }

    private static String classifyAndroid(String h) {
        if (h.contains("samsung") || h.contains("galaxy") || h.contains("sm-")) return "Android (Samsung)";
        if (h.contains("pixel"))                               return "Android (Google Pixel)";
        if (h.contains("huawei") || h.contains("honor"))       return "Android (Huawei)";
        if (h.contains("xiaomi") || h.contains("redmi") || h.contains("poco")) return "Android (Xiaomi)";
        if (h.contains("oneplus") || h.contains("one-plus"))   return "Android (OnePlus)";
        if (h.contains("nothing"))                             return "Android (Nothing Phone)";
        if (h.contains("oppo"))                                return "Android (OPPO)";
        if (h.contains("realme"))                              return "Android (Realme)";
        if (h.contains("motorola") || h.contains("moto-"))     return "Android (Motorola)";
        if (h.contains("sony") || h.contains("xperia"))        return "Android (Sony)";
        if (h.contains("nokia"))                               return "Android (Nokia)";
        if (h.contains("asus") && (h.contains("phone") || h.contains("zenfone"))) return "Android (ASUS)";
        if (h.contains("fairphone"))                           return "Android (Fairphone)";
        if (h.contains("android"))                             return "Android";
        return null;
    }

    private static String classifyNetwork(String h) {
        if (h.contains("fritz") || h.contains("fritzbox"))     return "Router (FRITZ!Box)";
        if (h.contains("unifi") || h.contains("ubiquiti"))     return "Access Point (Ubiquiti)";
        if (h.contains("mikrotik"))                            return "Router (MikroTik)";
        if (h.contains("cisco"))                               return "Cisco-Gerät";
        if (h.contains("procurve") || h.contains("aruba"))     return "Switch (HP/Aruba)";
        if (h.contains("netgear"))                             return "Netgear-Gerät";
        if (h.contains("zyxel"))                               return "ZyXEL-Gerät";
        if (h.contains("dlink") || h.contains("d-link"))       return "D-Link-Gerät";
        if (h.contains("tplink") || h.contains("tp-link"))     return "TP-Link-Gerät";
        if (h.contains("openwrt"))                             return "Router (OpenWrt)";
        if (h.contains("ddwrt") || h.contains("dd-wrt"))       return "Router (DD-WRT)";
        if (h.contains("router"))                              return "Router";
        // "switch" nur als eigenständiges Wort oder mit Trennzeichen – nicht als Substring
        if (h.matches(".*\\bswitch\\b.*") || h.contains("sw-") || h.contains("-sw"))
            return "Netzwerk-Switch";
        if (h.contains("hub"))                                 return "Netzwerk-Switch";
        if (h.contains("ap-") || h.contains("-ap") || h.contains("access-point")) return "Access Point";
        return null;
    }

    private static String classifyPrinter(String h) {
        if (h.contains("printer") || h.contains("drucker"))   return "Drucker";
        if (h.contains("hp-") || h.contains("hewlett"))       return "Drucker (HP)";
        if (h.contains("epson"))                              return "Drucker (Epson)";
        if (h.contains("canon"))                              return "Drucker (Canon)";
        if (h.contains("brother"))                            return "Drucker (Brother)";
        if (h.contains("kyocera"))                            return "Drucker (Kyocera)";
        if (h.contains("ricoh"))                              return "Drucker (Ricoh)";
        if (h.contains("lexmark"))                            return "Drucker (Lexmark)";
        if (h.contains("jetdirect"))                          return "Drucker (HP JetDirect)";
        return null;
    }

    private static String classifyIot(String h) {
        if (h.contains("chromecast"))                          return "Chromecast";
        if (h.contains("echo") || h.contains("alexa"))         return "Amazon Echo";
        if (h.contains("fire") || h.contains("kindle"))        return "Amazon-Gerät";
        if (h.contains("sonos"))                               return "Sonos";
        if (h.contains("philips") || h.contains("hue"))        return "Philips Hue";
        if (h.contains("nest"))                                return "Google Nest";
        if (h.contains("shelly"))                              return "Shelly (IoT)";
        if (h.contains("tasmota"))                             return "Tasmota (IoT)";
        if (h.contains("espressif") || h.contains("esp32") || h.contains("esp8266")) return "ESP-Gerät (IoT)";
        if (h.contains("tuya"))                                return "Tuya (IoT)";
        if (h.contains("kodi") || h.contains("plex"))          return "Medienserver";
        if (h.contains("smart") || h.contains("iot") || h.contains("mqtt")) return "IoT-Gerät";
        return null;
    }

    private static String classifyGeneric(String h) {
        if (h.contains("raspberry") || h.contains("raspberrypi")) return "Raspberry Pi (Linux)";
        if (h.contains("ubuntu") || h.contains("debian") || h.contains("fedora")
                || h.contains("centos") || h.contains("arch") || h.contains("opensuse")) return "Linux/Unix";
        if (h.contains("linux"))                               return "Linux/Unix";
        if (h.contains("desktop-") || h.contains("laptop-") || h.contains("pc-")
                || h.contains("workstation") || h.contains("windows")) return "Windows";
        if (h.contains("cam") || h.contains("camera") || h.contains("ipcam")) return "IP-Kamera";
        if (h.contains("voip") || h.contains("sip-"))         return "VoIP-Telefon";
        return null;
    }
}
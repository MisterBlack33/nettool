package main.java.networktool_v3.logic.analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * OS- und Geräteerkennung mit stark verbesserter Präzision.
 *
 * Erkennungsreihenfolge:
 *  1. Port-basiert   – Windows/macOS/Linux/Drucker/Server nach offenem Port
 *  2. OUI/MAC-Lookup – Handy-Hersteller aus MAC-Präfix
 *  3. Hostname       – iPhone-von-Max, DESKTOP-AB1234, fritz.box …
 *  4. TTL-Fallback   – TTL<=64 Linux/Android, <=128 Windows, <=255 Apple
 */
public final class OsDetector {

    private OsDetector() {}

    private static final int TIMEOUT_MS   = 300;
    private static final int THREAD_COUNT = 32;

    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF",
            "00:AA:00:00:00:00", "00:AA:00:AA:00:AA");

    private static final Pattern MAC_COLON =
            Pattern.compile("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");
    private static final Pattern MAC_DASH  =
            Pattern.compile("([0-9A-Fa-f]{2}-){5}[0-9A-Fa-f]{2}");

    // ── Konfidenz ─────────────────────────────────────────────────────────

    public enum Confidence { HOCH, MITTEL, NIEDRIG }

    public static final class OsResult {
        public final String os; public final Confidence confidence; public final String method;
        public OsResult(String os, Confidence c, String m) { this.os=os; this.confidence=c; this.method=m; }
        public String display() { return os + " [" + confidence.name().charAt(0) + "]"; }
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public static OsResult detectWithConfidence(String ip) {
        String byPort = detectByPorts(ip);
        if (!byPort.equals("Unbekannt"))
            return new OsResult(byPort, Confidence.HOCH, "Port");

        String mac = getMacFromArp(ip);
        if (mac != null) {
            String vendor = OuiDatabase.lookup(mac);
            if (vendor != null) return new OsResult(vendor, Confidence.MITTEL, "OUI/MAC");
        }

        String fromHostname = detectFromHostnameOnly(ip);
        if (fromHostname != null)
            return new OsResult(fromHostname, Confidence.MITTEL, "Hostname");

        int ttl = getTtl(ip);
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

    public static String getMacFromArp(String ip) {
        triggerArp(ip);
        String[] cmds = isWin()
                ? new String[]{"arp -a " + ip, "arp -a"}
                : new String[]{"arp -n " + ip, "arp -a -n", "arp -a"};
        for (String cmd : cmds) {
            String mac = queryArp(cmd, ip);
            if (mac != null) return mac;
        }
        return null;
    }

    public static boolean isOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            return true;
        } catch (Exception e) { return false; }
    }

    // ── Port-basierte Erkennung ───────────────────────────────────────────

    private static String detectByPorts(String ip) {
        int[] ports = {
                445,139,135,3389,5985,5986,  // Windows
                548,5000,7000,5353,           // macOS/Apple
                22,80,443,8080,8443,          // Linux/Web
                631,9100,515,                 // Drucker
                53,161,23,67,                 // Netzwerk
                3306,5432,1433,6379,27017,    // Datenbank
                25,110,143,993,995,           // Mail
                1883,8883,                    // IoT
                21,9090,9200                  // FTP/Monitoring
        };
        Map<Integer,Boolean> open = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        for (int p : ports) { final int fp=p; exec.submit(()->open.put(fp,isOpen(ip,fp))); }
        exec.shutdown();
        try { exec.awaitTermination(TIMEOUT_MS+200L,TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Windows (höchste Priorität – eindeutige Ports)
        if (is(open,445)||is(open,3389)||is(open,5985)||is(open,5986)) return "Windows";
        if (is(open,135)&&!is(open,22))                                 return "Windows";

        // macOS (AFP ist macOS-exklusiv)
        if (is(open,548))                                               return "macOS";
        if (is(open,5000)&&!is(open,445)&&!is(open,22))                return "macOS";

        // Drucker
        if (is(open,9100)) return "Drucker (JetDirect)";
        if (is(open,631))  return "Drucker (IPP/CUPS)";
        if (is(open,515))  return "Drucker (LPD)";

        // IoT/MQTT
        if (is(open,1883)||is(open,8883)) return "IoT-Gerät (MQTT)";

        // Netzwerkgeräte
        if (is(open,161)&&!is(open,22)&&!is(open,80))  return "Router / Switch (SNMP)";
        if (is(open,67) &&!is(open,22)&&!is(open,80))  return "DHCP-Server";
        if (is(open,53) &&!is(open,22)&&!is(open,80))  return "DNS-Server";
        if (is(open,23) &&!is(open,22))                 return "Router / Netzwerkgerät";

        // Linux-Server (SSH vorhanden → Dienst-Typ ermitteln)
        if (is(open,22)) {
            if (is(open,80)&&is(open,443))  return detectWebServer(ip);
            if (is(open,80))                return "Web-Server (HTTP)";
            if (is(open,443))               return "Web-Server (HTTPS)";
            if (is(open,25)&&(is(open,143)||is(open,993))) return "Mail-Server";
            if (is(open,25))                return "SMTP-Server";
            if (is(open,21))                return "FTP-Server";
            if (is(open,3306))              return "Datenbankserver (MySQL)";
            if (is(open,5432))              return "Datenbankserver (PostgreSQL)";
            if (is(open,1433))              return "Datenbankserver (MSSQL)";
            if (is(open,6379))              return "Datenbankserver (Redis)";
            if (is(open,27017))             return "Datenbankserver (MongoDB)";
            if (is(open,9090))              return "Monitoring-Server";
            if (is(open,9200))              return "Suchserver (Elasticsearch)";
            return "Linux/Unix";
        }

        // Web ohne SSH
        if (is(open,80)||is(open,8080))  return detectWebServer(ip);
        if (is(open,443)||is(open,8443)) return "Web-Server (HTTPS)";
        if (is(open,53))                 return "DNS-Server";
        if (is(open,25)||is(open,110)||is(open,143)) return "Mail-Server";
        if (is(open,21))                 return "FTP-Server";
        if (is(open,3306))               return "Datenbankserver (MySQL)";
        if (is(open,5432))               return "Datenbankserver (PostgreSQL)";
        if (is(open,1433))               return "Datenbankserver (MSSQL)";
        if (is(open,6379))               return "Datenbankserver (Redis)";
        if (is(open,27017))              return "Datenbankserver (MongoDB)";

        return "Unbekannt";
    }

    private static String detectWebServer(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip,80),400);
            s.setSoTimeout(400);
            s.getOutputStream().write(("HEAD / HTTP/1.1\r\nHost: "+ip+"\r\nConnection: close\r\n\r\n").getBytes());
            s.getOutputStream().flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line=br.readLine())!=null) {
                if (!line.toLowerCase().startsWith("server:")) continue;
                String srv = line.substring(7).trim().toLowerCase();
                if (srv.contains("nginx"))    return "Web-Server (nginx)";
                if (srv.contains("apache"))   return "Web-Server (Apache)";
                if (srv.contains("iis"))      return "Web-Server (IIS/Windows)";
                if (srv.contains("caddy"))    return "Web-Server (Caddy)";
                if (srv.contains("lighttpd")) return "Web-Server (lighttpd)";
                if (!srv.isBlank())           return "Web-Server (" + srv.split("/")[0] + ")";
            }
        } catch (Exception ignored) {}
        return "Web-Server";
    }

    // ── Hostname-Heuristik ────────────────────────────────────────────────

    private static String detectFromHostnameOnly(String ip) {
        String[] r = {null};
        Thread t = new Thread(()->{
            try {
                String h = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!h.equals(ip)) r[0]=h;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true); t.start();
        try { t.join(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return r[0]!=null ? classifyHostname(r[0].toLowerCase()) : null;
    }

    static String classifyHostname(String h) {
        if (h==null) return null;
        // Apple
        if (h.contains("iphone"))                             return "iPhone (iOS)";
        if (h.contains("ipad"))                               return "iPad (iPadOS)";
        if (h.contains("macbook"))                            return "MacBook (macOS)";
        if (h.contains("imac"))                               return "iMac (macOS)";
        if (h.contains("mac-mini")||h.contains("macmini"))   return "Mac mini (macOS)";
        if (h.contains("apple"))                              return "Apple-Gerät";
        // Android (präzise)
        if (h.contains("samsung")||h.contains("galaxy")||h.contains("sm-"))
            return "Android (Samsung)";
        if (h.contains("pixel"))                              return "Android (Google Pixel)";
        if (h.contains("huawei")||h.contains("honor"))        return "Android (Huawei)";
        if (h.contains("xiaomi")||h.contains("redmi")||h.contains("poco"))
            return "Android (Xiaomi)";
        if (h.contains("oneplus")||h.contains("one-plus"))    return "Android (OnePlus)";
        if (h.contains("nothing"))                            return "Android (Nothing Phone)";
        if (h.contains("oppo"))                               return "Android (OPPO)";
        if (h.contains("realme"))                             return "Android (Realme)";
        if (h.contains("motorola")||h.contains("moto-"))      return "Android (Motorola)";
        if (h.contains("sony")||h.contains("xperia"))         return "Android (Sony)";
        if (h.contains("android"))                            return "Android";
        // Windows
        if (h.contains("desktop-")||h.contains("laptop-")||h.contains("pc-"))
            return "Windows";
        if (h.contains("windows")||h.contains("workstation")) return "Windows";
        // Linux
        if (h.contains("raspberry")||h.contains("raspberrypi"))return "Raspberry Pi (Linux)";
        if (h.contains("ubuntu")||h.contains("debian")||h.contains("fedora")
                ||h.contains("centos")||h.contains("arch"))  return "Linux/Unix";
        if (h.contains("linux"))                              return "Linux/Unix";
        // Router/Netzwerk
        if (h.contains("fritz")||h.contains("fritzbox"))      return "Router (FRITZ!Box)";
        if (h.contains("router"))                             return "Router";
        if (h.contains("switch"))                             return "Netzwerk-Switch";
        if (h.contains("ubiquiti")||h.contains("unifi"))      return "Access Point (Ubiquiti)";
        if (h.contains("mikrotik"))                           return "Router (MikroTik)";
        if (h.contains("cisco"))                              return "Cisco-Gerät";
        if (h.contains("synology"))                           return "NAS (Synology)";
        if (h.contains("qnap"))                               return "NAS (QNAP)";
        if (h.contains("nas"))                                return "NAS";
        if (h.contains("ap-")||h.contains("access-point"))   return "Access Point";
        // Drucker
        if (h.contains("printer")||h.contains("print")||h.contains("drucker"))
            return "Drucker";
        if (h.contains("hp-")||h.contains("epson")||h.contains("canon")
                ||h.contains("brother")||h.contains("kyocera"))
            return "Drucker";
        // IoT/Sonstige
        if (h.contains("cam")||h.contains("camera"))          return "IP-Kamera";
        if (h.contains("chromecast"))                         return "Chromecast";
        if (h.contains("echo")||h.contains("alexa"))          return "Amazon Echo";
        if (h.contains("fire")||h.contains("kindle"))         return "Amazon-Gerät";
        if (h.contains("sonos"))                              return "Sonos";
        if (h.contains("xbox"))                               return "Xbox";
        if (h.contains("playstation")||h.contains("ps4")||h.contains("ps5"))
            return "PlayStation";
        if (h.contains("smart")||h.contains("iot"))           return "IoT-Gerät";
        return null;
    }

    // ── TTL-Fallbacks ─────────────────────────────────────────────────────

    private static String resolveLinuxOrMobile(String ip, String mac) {
        if (mac!=null) { String v=OuiDatabase.lookup(mac); if (v!=null) return v; }
        if (isOpen(ip,22)||isOpen(ip,80)||isOpen(ip,443)||isOpen(ip,21)||isOpen(ip,25)||isOpen(ip,3306))
            return "Linux/Unix";
        return "Mobiles Gerät (Android?)";
    }

    private static String resolveWindowsOrAndroid(String ip, String mac) {
        if (mac!=null) { String v=OuiDatabase.lookup(mac); if (v!=null) return v; }
        if (isOpen(ip,445)||isOpen(ip,3389)||isOpen(ip,5985)||isOpen(ip,135))
            return "Windows";
        if (isOpen(ip,548)||isOpen(ip,5000)) return "macOS";
        return "Android";
    }

    private static String resolveAppleOrNetwork(String ip, String mac) {
        if (mac!=null) { String v=OuiDatabase.lookup(mac); if (v!=null) return v; }
        if (isOpen(ip,548)||isOpen(ip,5000)) return "macOS";
        if (isOpen(ip,5353))                 return "Apple-Gerät (Bonjour)";
        if (isOpen(ip,23)||isOpen(ip,161))   return "Router / Netzwerkgerät";
        return "iOS / macOS";
    }

    // ── ARP-Hilfsmethoden ─────────────────────────────────────────────────

    private static void triggerArp(String ip) {
        try {
            String cmd = isWin() ? "ping -n 1 -w 300 "+ip : "ping -c 1 -W 1 "+ip;
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(700,TimeUnit.MILLISECONDS); p.destroy();
        } catch (Exception ignored) {}
    }

    private static String queryArp(String cmd, String targetIp) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line=br.readLine())!=null) {
                    if (!line.contains(targetIp)) continue;
                    String mac=extractMac(line);
                    if (mac!=null) { p.destroy(); return mac; }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractMac(String line) {
        Matcher m1=MAC_COLON.matcher(line);
        if (m1.find()) { String m=m1.group().toUpperCase(); return isValidMac(m)?m:null; }
        Matcher m2=MAC_DASH.matcher(line);
        if (m2.find()) { String m=m2.group().replace("-",":").toUpperCase(); return isValidMac(m)?m:null; }
        return null;
    }

    private static boolean isValidMac(String mac) {
        if (mac==null||mac.length()<17) return false;
        if (INVALID_MACS.contains(mac)) return false;
        if (mac.startsWith("FF:FF"))    return false;
        if (mac.startsWith("01:"))      return false;
        if (mac.replace(":","").replace("0","").isEmpty()) return false;
        return true;
    }

    private static int getTtl(String ip) {
        try {
            Process p=Runtime.getRuntime().exec(isWin()?"ping -n 1 "+ip:"ping -c 1 "+ip);
            BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb=new StringBuilder();
            String line; while((line=br.readLine())!=null) sb.append(line).append(' ');
            p.destroy();
            Matcher m=Pattern.compile("(?i)ttl[=:]\\s*(\\d+)").matcher(sb);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    @SuppressWarnings("unused")
    static String extractOui(String line) {
        Matcher m1=MAC_COLON.matcher(line);
        if (m1.find()) return m1.group().substring(0,8).toUpperCase();
        Matcher m2=MAC_DASH.matcher(line);
        if (m2.find()) return m2.group().replace("-",":").substring(0,8).toUpperCase();
        return null;
    }

    private static boolean is(Map<Integer,Boolean> map, int port) {
        return Boolean.TRUE.equals(map.get(port));
    }

    private static boolean isWin() {
        return System.getProperty("os.name","").toLowerCase().contains("win");
    }
}
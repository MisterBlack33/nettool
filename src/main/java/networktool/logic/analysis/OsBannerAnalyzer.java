// src/main/java/networktool/logic/analysis/OsBannerAnalyzer.java
package main.java.networktool.logic.analysis;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Liest Banner von SSH, HTTP und SMB und leitet daraus das OS ab.
 * Schnell (Timeout 600ms), non-blocking bei geschlossenen Ports.
 */
final class OsBannerAnalyzer {

    private OsBannerAnalyzer() {}

    private static final int TIMEOUT = 600;

    /** Gibt OsSignature zurück oder null wenn kein verwertbarer Banner. */
    static OsSignature analyze(String ip) {
        OsSignature ssh  = analyzeSsh(ip);
        OsSignature http = analyzeHttp(ip);
        OsSignature smb  = analyzeSmb(ip);
        return OsSignature.best(OsSignature.best(ssh, http), smb);
    }

    // ── SSH ───────────────────────────────────────────────────────────────

    private static OsSignature analyzeSsh(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 22), TIMEOUT);
            s.setSoTimeout(TIMEOUT);
            String banner = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                    .readLine();
            if (banner == null) return null;
            return parseSshBanner(banner.toLowerCase());
        } catch (Exception e) { return null; }
    }

    private static OsSignature parseSshBanner(String banner) {
        // WICHTIG: windows VOR openssh prüfen —
        // "openssh_for_windows" enthält beide Strings
        if (banner.contains("windows"))  return OsSignature.of("Windows",             85, "SSH-Banner");
        if (banner.contains("ubuntu"))   return OsSignature.of("Linux (Ubuntu)",       85, "SSH-Banner");
        if (banner.contains("debian"))   return OsSignature.of("Linux (Debian)",       85, "SSH-Banner");
        if (banner.contains("raspbian")) return OsSignature.of("Raspberry Pi (Linux)", 90, "SSH-Banner");
        if (banner.contains("centos"))   return OsSignature.of("Linux (CentOS)",       85, "SSH-Banner");
        if (banner.contains("fedora"))   return OsSignature.of("Linux (Fedora)",       85, "SSH-Banner");
        if (banner.contains("arch"))     return OsSignature.of("Linux (Arch)",         85, "SSH-Banner");
        if (banner.contains("alpine"))   return OsSignature.of("Linux (Alpine)",       85, "SSH-Banner");
        if (banner.contains("freebsd"))  return OsSignature.of("FreeBSD",              85, "SSH-Banner");
        if (banner.contains("openbsd"))  return OsSignature.of("OpenBSD",              85, "SSH-Banner");
        if (banner.contains("openssh"))  return OsSignature.of("Linux/Unix",           60, "SSH-Banner");
        return null;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────

    private static OsSignature analyzeHttp(String ip) {
        OsSignature r = probeHttp(ip, 80);
        return r != null ? r : probeHttp(ip, 8080);
    }

    private static OsSignature probeHttp(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TIMEOUT);
            s.setSoTimeout(TIMEOUT);
            String req = "HEAD / HTTP/1.0\r\nHost: " + ip + "\r\n\r\n";
            s.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                OsSignature sig = parseHttpHeader(line.toLowerCase());
                if (sig != null) return sig;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static OsSignature parseHttpHeader(String header) {
        if (!header.startsWith("server:")) return null;
        String server = header.substring(7).trim();
        if (server.contains("ubuntu"))      return OsSignature.of("Linux (Ubuntu)",  75, "HTTP-Server");
        if (server.contains("debian"))      return OsSignature.of("Linux (Debian)",  75, "HTTP-Server");
        if (server.contains("centos"))      return OsSignature.of("Linux (CentOS)",  75, "HTTP-Server");
        if (server.contains("win"))         return OsSignature.of("Windows",         70, "HTTP-Server");
        if (server.contains("iis"))         return OsSignature.of("Windows (IIS)",   80, "HTTP-Server");
        if (server.contains("mikrotik"))    return OsSignature.of("Router (MikroTik)", 85, "HTTP-Server");
        if (server.contains("routeros"))    return OsSignature.of("Router (MikroTik)", 85, "HTTP-Server");
        if (server.contains("fritz"))       return OsSignature.of("Router (FRITZ!Box)", 90, "HTTP-Server");
        if (server.contains("synology"))    return OsSignature.of("NAS (Synology)",  90, "HTTP-Server");
        if (server.contains("qnap"))        return OsSignature.of("NAS (QNAP)",      90, "HTTP-Server");
        if (server.contains("unifi"))       return OsSignature.of("Access Point (Ubiquiti)", 90, "HTTP-Server");
        if (server.contains("nginx") || server.contains("apache") || server.contains("lighttpd"))
            return OsSignature.of("Linux/Unix",  50, "HTTP-Server");
        return null;
    }

    // ── SMB ───────────────────────────────────────────────────────────────

    private static OsSignature analyzeSmb(String ip) {
        // SMB-Port offen → fast sicher Windows (oder Samba auf Linux)
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 445), TIMEOUT);
            // SMB Negotiate Protocol Request
            byte[] negotiate = {
                    0x00,0x00,0x00,0x54,       // NetBIOS length
                    (byte)0xFF,0x53,0x4D,0x42, // SMB magic
                    0x72,                       // command: Negotiate
                    0x00,0x00,0x00,0x00,       // status
                    0x18,0x01,0x48,0x00,       // flags
                    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00,0x00,0x00,
                    (byte)0xFF,(byte)0xFE,      // PID
                    0x00,0x00,0x00,0x00        // MID
            };
            s.getOutputStream().write(negotiate);
            s.getOutputStream().flush();
            s.setSoTimeout(TIMEOUT);
            byte[] buf = new byte[128];
            int read = s.getInputStream().read(buf);
            if (read > 40) {
                // Check for SMB2 response → Windows or modern Samba
                return OsSignature.of("Windows", 70, "SMB-Probe");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
package main.java.networktool.logic.analysis;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Liest Banner von SSH, HTTP, HTTPS, FTP, SMB.
 * Timeout 700ms; FTP und HTTPS nun aktiv eingebunden.
 */
final class OsBannerAnalyzer {

    private OsBannerAnalyzer() {}

    private static final int TIMEOUT = 700;

    static OsSignature analyze(String ip) {
        OsSignature best = null;
        best = OsSignature.best(best, analyzeSsh(ip));
        if (best != null && best.score >= 85) return best;
        best = OsSignature.best(best, analyzeSmb(ip));
        if (best != null && best.score >= 85) return best;
        best = OsSignature.best(best, analyzeHttp(ip));
        best = OsSignature.best(best, analyzeHttps(ip));
        best = OsSignature.best(best, analyzeFtp(ip));
        return best;
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

    private static OsSignature parseSshBanner(String b) {
        // "openssh_for_windows" enthält beide → windows zuerst prüfen
        if (b.contains("windows"))  return OsSignature.of("Windows",             85, "SSH-Banner");
        if (b.contains("raspbian")) return OsSignature.of("Raspberry Pi (Linux)",90, "SSH-Banner");
        if (b.contains("ubuntu"))   return OsSignature.of("Linux (Ubuntu)",       85, "SSH-Banner");
        if (b.contains("debian"))   return OsSignature.of("Linux (Debian)",       85, "SSH-Banner");
        if (b.contains("centos"))   return OsSignature.of("Linux (CentOS)",       85, "SSH-Banner");
        if (b.contains("fedora"))   return OsSignature.of("Linux (Fedora)",       85, "SSH-Banner");
        if (b.contains("arch"))     return OsSignature.of("Linux (Arch)",         85, "SSH-Banner");
        if (b.contains("alpine"))   return OsSignature.of("Linux (Alpine)",       85, "SSH-Banner");
        if (b.contains("freebsd"))  return OsSignature.of("FreeBSD",              85, "SSH-Banner");
        if (b.contains("openbsd"))  return OsSignature.of("OpenBSD",              85, "SSH-Banner");
        if (b.contains("openssh"))  return OsSignature.of("Linux/Unix",           60, "SSH-Banner");
        return null;
    }

    // ── SMB ───────────────────────────────────────────────────────────────

    private static OsSignature analyzeSmb(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 445), TIMEOUT);
            byte[] negotiate = buildSmbNegotiate();
            s.getOutputStream().write(negotiate);
            s.getOutputStream().flush();
            s.setSoTimeout(TIMEOUT);
            byte[] buf = new byte[128];
            int read = s.getInputStream().read(buf);
            if (read > 40) return OsSignature.of("Windows", 70, "SMB-Probe");
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] buildSmbNegotiate() {
        return new byte[]{
                0x00,0x00,0x00,0x54, (byte)0xFF,0x53,0x4D,0x42,
                0x72, 0x00,0x00,0x00,0x00,
                0x18,0x01,0x48,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,
                (byte)0xFF,(byte)0xFE, 0x00,0x00,0x00,0x00
        };
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
        String srv = header.substring(7).trim();
        if (srv.contains("ubuntu"))    return OsSignature.of("Linux (Ubuntu)",     75, "HTTP-Server");
        if (srv.contains("debian"))    return OsSignature.of("Linux (Debian)",     75, "HTTP-Server");
        if (srv.contains("centos"))    return OsSignature.of("Linux (CentOS)",     75, "HTTP-Server");
        if (srv.contains("win"))       return OsSignature.of("Windows",            70, "HTTP-Server");
        if (srv.contains("iis"))       return OsSignature.of("Windows (IIS)",      80, "HTTP-Server");
        if (srv.contains("mikrotik"))  return OsSignature.of("Router (MikroTik)",  85, "HTTP-Server");
        if (srv.contains("routeros"))  return OsSignature.of("Router (MikroTik)",  85, "HTTP-Server");
        if (srv.contains("fritz"))     return OsSignature.of("Router (FRITZ!Box)", 90, "HTTP-Server");
        if (srv.contains("synology"))  return OsSignature.of("NAS (Synology)",     90, "HTTP-Server");
        if (srv.contains("qnap"))      return OsSignature.of("NAS (QNAP)",         90, "HTTP-Server");
        if (srv.contains("unifi"))     return OsSignature.of("Access Point (Ubiquiti)", 90, "HTTP-Server");
        if (srv.contains("nginx") || srv.contains("apache") || srv.contains("lighttpd"))
            return OsSignature.of("Linux/Unix", 50, "HTTP-Server");
        return null;
    }

    // ── HTTPS TLS-Hello ───────────────────────────────────────────────────
    // Liest Server-Name aus TLS-Extension (SNI-Response / Certificate CN)

    static OsSignature analyzeHttps(String ip) {
        try {
            javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket)
                    javax.net.ssl.SSLSocketFactory.getDefault()
                            .createSocket();
            ssl.connect(new InetSocketAddress(ip, 443), TIMEOUT);
            ssl.setSoTimeout(TIMEOUT);
            ssl.startHandshake();
            java.security.cert.X509Certificate[] certs =
                    (java.security.cert.X509Certificate[])
                            ssl.getSession().getPeerCertificates();
            if (certs.length > 0) {
                String dn = certs[0].getSubjectX500Principal().getName().toLowerCase();
                OsSignature sig = classifyTlsCn(dn);
                ssl.close();
                return sig;
            }
            ssl.close();
        } catch (Exception ignored) {}
        return null;
    }

    private static OsSignature classifyTlsCn(String dn) {
        if (dn.contains("fritz"))     return OsSignature.of("Router (FRITZ!Box)", 88, "TLS-Cert");
        if (dn.contains("synology"))  return OsSignature.of("NAS (Synology)",     88, "TLS-Cert");
        if (dn.contains("mikrotik"))  return OsSignature.of("Router (MikroTik)",  88, "TLS-Cert");
        if (dn.contains("unifi"))     return OsSignature.of("Access Point (Ubiquiti)", 88, "TLS-Cert");
        if (dn.contains("qnap"))      return OsSignature.of("NAS (QNAP)",         88, "TLS-Cert");
        if (dn.contains("windows"))   return OsSignature.of("Windows",            75, "TLS-Cert");
        return null;
    }

    // ── FTP ───────────────────────────────────────────────────────────────

    static OsSignature analyzeFtp(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 21), TIMEOUT);
            s.setSoTimeout(TIMEOUT);
            String banner = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                    .readLine();
            if (banner == null) return null;
            return parseFtpBanner(banner.toLowerCase());
        } catch (Exception e) { return null; }
    }

    private static OsSignature parseFtpBanner(String b) {
        if (b.contains("windows"))  return OsSignature.of("Windows",           65, "FTP-Banner");
        if (b.contains("linux"))    return OsSignature.of("Linux/Unix",         65, "FTP-Banner");
        if (b.contains("busybox"))  return OsSignature.of("Router / IoT-Gerät", 70, "FTP-Banner");
        if (b.contains("synology")) return OsSignature.of("NAS (Synology)",     75, "FTP-Banner");
        if (b.contains("qnap"))     return OsSignature.of("NAS (QNAP)",         75, "FTP-Banner");
        return null;
    }
}
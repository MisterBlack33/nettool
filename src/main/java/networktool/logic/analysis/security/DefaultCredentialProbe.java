package main.java.networktool.logic.analysis.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Prüft bekannte Default-Zugangsdaten (FTP anonym, HTTP Basic-Auth).
 * Wird ausschließlich nach expliziter Nutzerbestätigung pro Host aufgerufen
 * (siehe {@code GuiSecurityFindingsPanel}) — niemals automatisch im Scan-Loop.
 */
public final class DefaultCredentialProbe {

    private static final int TIMEOUT_MS = 1500;

    private DefaultCredentialProbe() {}

    public static Optional<SecurityFinding> checkFtpAnonymous(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 21), TIMEOUT_MS);
            s.setSoTimeout(TIMEOUT_MS);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            in.readLine(); // Banner
            out.print("USER anonymous\r\n"); out.flush();
            in.readLine();
            out.print("PASS anon@nettool.local\r\n"); out.flush();
            String resp = in.readLine();
            if (resp != null && resp.startsWith("230")) {
                return Optional.of(new SecurityFinding(ip, SecurityFinding.Category.DEFAULT_CREDENTIALS,
                        SecurityFinding.Severity.CRITICAL, "FTP: anonymer Login möglich"));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public static Optional<SecurityFinding> checkHttpDefaultLogin(String ip, String user, String pass) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 80), TIMEOUT_MS);
            s.setSoTimeout(TIMEOUT_MS);
            String auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
            String req  = "GET / HTTP/1.0\r\nHost: " + ip + "\r\nAuthorization: Basic " + auth + "\r\n\r\n";
            s.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            String status = readStatusLine(s);
            if (status != null && status.contains(" 200 ")) {
                return Optional.of(new SecurityFinding(ip, SecurityFinding.Category.DEFAULT_CREDENTIALS,
                        SecurityFinding.Severity.CRITICAL, "HTTP: Login " + user + "/" + pass + " akzeptiert"));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static String readStatusLine(Socket s) throws IOException {
        return new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine();
    }
}

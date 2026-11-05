package main.java.networktool.logic.analysis.security;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Prüft TLS-Zertifikate (Ablauf, Self-Signed) von HTTPS-Hosts (Port 443/8443).
 * Reines Read-only-TCP, analog {@code OsBannerAnalyzer.analyzeHttps}.
 */
public final class TlsCertInspector {

    private static final int[] TLS_PORTS       = {443, 8443};
    private static final int   TIMEOUT_MS      = 1500;
    private static final long  EXPIRY_WARN_DAYS = 14;

    private TlsCertInspector() {}

    public static Optional<SecurityFinding> inspect(String ip) {
        for (int port : TLS_PORTS) {
            Optional<SecurityFinding> f = inspectPort(ip, port);
            if (f.isPresent()) return f;
        }
        return Optional.empty();
    }

    private static Optional<SecurityFinding> inspectPort(String ip, int port) {
        try (SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket()) {
            socket.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.startHandshake();
            X509Certificate[] chain = (X509Certificate[]) socket.getSession().getPeerCertificates();
            if (chain.length == 0) return Optional.empty();
            return Optional.of(evaluate(ip, chain[0]));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static SecurityFinding evaluate(String ip, X509Certificate cert) {
        if (isSelfSigned(cert)) {
            return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.WARN,
                    "Self-signed Zertifikat: " + cert.getSubjectX500Principal().getName(),
                    SecurityFinding.Source.TLS_CERT_INSPECTOR, SecurityFinding.Confidence.CONFIRMED,
                    Instant.now());
        }
        long daysLeft = daysUntilExpiry(cert);
        if (daysLeft < 0) {
            return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.CRITICAL,
                    "Zertifikat abgelaufen seit " + (-daysLeft) + " Tag(en)",
                    SecurityFinding.Source.TLS_CERT_INSPECTOR, SecurityFinding.Confidence.CONFIRMED,
                    Instant.now());
        }
        if (daysLeft <= EXPIRY_WARN_DAYS) {
            return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.WARN,
                    "Zertifikat läuft in " + daysLeft + " Tag(en) ab",
                    SecurityFinding.Source.TLS_CERT_INSPECTOR, SecurityFinding.Confidence.CONFIRMED,
                    Instant.now());
        }
        return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.INFO,
                "Zertifikat gültig bis " + cert.getNotAfter(),
                SecurityFinding.Source.TLS_CERT_INSPECTOR, SecurityFinding.Confidence.CONFIRMED,
                Instant.now());
    }

    private static boolean isSelfSigned(X509Certificate cert) {
        if (!cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) return false;
        try {
            cert.verify(cert.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static long daysUntilExpiry(X509Certificate cert) {
        return Duration.between(Instant.now(), cert.getNotAfter().toInstant()).toDays();
    }
}

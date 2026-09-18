package main.java.networktool.logic.ports;

/**
 * Liest den Server-Banner eines offenen Ports.
 * Protokoll-Grabber siehe {@link BannerProtocolProbes}, Textbereinigung siehe {@link BannerText}.
 * Öffentliche API unverändert (Workstream D: reiner Extract-Class-Split von BannerGrabber).
 */
public final class BannerGrabber {

    private BannerGrabber() {}

    private static final int DEFAULT_TIMEOUT = 1_200;

    /**
     * Liest den Banner eines offenen Ports.
     *
     * @param host      Ziel-IP oder Hostname
     * @param port      Ziel-Port
     * @param timeoutMs Verbindungs- und Lese-Timeout in ms
     * @return Banner-Text (nie null, nie leer – Fallback "offen")
     */
    public static String grab(String host, int port, int timeoutMs) {
        int to = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT;
        try {
            String banner = grabBanner(host, port, to);
            return banner != null && !banner.isBlank() ? BannerText.clean(banner) : BannerText.serviceName(port);
        } catch (Exception e) {
            return BannerText.serviceName(port);
        }
    }

    public static String grab(String host, int port) {
        return grab(host, port, DEFAULT_TIMEOUT);
    }

    private static String grabBanner(String host, int port, int timeout) throws Exception {
        return switch (port) {
            case 80, 8080, 8888, 3000  -> BannerProtocolProbes.grabHttp(host, port, timeout, false);
            case 443, 8443             -> BannerProtocolProbes.grabHttps(host, port, timeout);
            case 21, 22, 23            -> BannerProtocolProbes.grabPassive(host, port, timeout);
            case 25, 465, 587          -> BannerProtocolProbes.grabSmtp(host, port, timeout);
            case 110, 995, 143, 993    -> BannerProtocolProbes.grabPassive(host, port, timeout);
            case 3306                  -> BannerProtocolProbes.grabMysql(host, port, timeout);
            case 6379                  -> BannerProtocolProbes.grabRedis(host, port, timeout);
            case 5432                  -> BannerProtocolProbes.grabPassive(host, port, timeout);
            case 27017                 -> BannerProtocolProbes.grabMongo(host, port, timeout);
            case 1883, 8883            -> "MQTT";
            case 5353                  -> "mDNS";
            case 631                   -> BannerProtocolProbes.grabHttp(host, port, timeout, false);
            case 9100                  -> "RAW/JetDirect (Drucker)";
            case 515                   -> "LPD (Drucker)";
            case 548                   -> "AFP (Apple File Sharing)";
            case 161, 162              -> "SNMP";
            case 53                    -> "DNS";
            case 3389                  -> "RDP (Remote Desktop)";
            case 5985                  -> BannerProtocolProbes.grabHttp(host, port, timeout, true);
            case 5986                  -> "WinRM HTTPS";
            case 9200, 9090            -> BannerProtocolProbes.grabHttp(host, port, timeout, false);
            default                    -> BannerProtocolProbes.grabPassive(host, port, timeout);
        };
    }
}

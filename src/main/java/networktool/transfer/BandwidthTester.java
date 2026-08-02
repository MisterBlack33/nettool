package main.java.networktool.transfer;

import java.io.*;
import java.net.*;

/**
 * Misst die Bandbreite einer Verbindung aktiv – ohne eigenen Server.
 *
 * Download: HTTP(S)-GET gegen den Ziel-Host (oder einen öffentlichen
 *           Fallback-Endpunkt fürs Internet), Durchsatz aus Byte/Zeit.
 * Upload:   HTTP(S)-PUT/POST eines Testpuffers, Durchsatz aus Sendezeit.
 * Latenz:   TCP-Connect-Zeiten (mehrere Samples, Avg/Jitter).
 */
public final class BandwidthTester {

    private BandwidthTester() {}

    public static final String DEFAULT_FALLBACK_HOST = "speed.cloudflare.com";
    static final long   TEST_BYTES   = 5 * 1024 * 1024L; // 5 MB
    static final int    CONNECT_TO   = 5_000;
    static final int    IO_TO        = 20_000;
    static final int    BUFFER_SIZE  = 65_536;

    public static void testBoth(String host) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf( "║  Bandwidth-Test → %-25s║%n", host + " ");
        System.out.println("╚══════════════════════════════════════════════╝");

        BandwidthHttpProbe.Result dl = BandwidthHttpProbe.download(host);
        BandwidthHttpProbe.Result ul = BandwidthHttpProbe.upload(host);
        LatencyProbe.Result       lat = LatencyProbe.measure(host);

        if (dl.ok())  System.out.printf("  ▼ Download : %7.2f Mbps%n", dl.mbps());
        else          System.out.println("  ▼ Download : nicht messbar");
        if (ul.ok())  System.out.printf("  ▲ Upload   : %7.2f Mbps%n", ul.mbps());
        else          System.out.println("  ▲ Upload   : nicht messbar");
        if (lat.ok()) System.out.printf("  ↔ Latenz   : %.1f ms (Jitter %.1f ms)%n", lat.avgMs(), lat.jitterMs());
        else          System.out.println("  ↔ Latenz   : nicht messbar");
    }

    public static double testDownload(String host) { return BandwidthHttpProbe.download(host).mbps(); }
    public static double testUpload(String host)   { return BandwidthHttpProbe.upload(host).mbps(); }
}
package main.java.networktool.transfer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/** Führt die eigentliche HTTP-Messung für {@link BandwidthTester} durch. */
final class BandwidthHttpProbe {

    private BandwidthHttpProbe() {}

    record Result(boolean ok, double mbps) {
        static Result fail() { return new Result(false, -1); }
    }

    /** Lädt Testdaten vom Ziel-Host; fällt auf öffentlichen Endpunkt zurück. */
    static Result download(String host) {
        for (String url : candidateDownloadUrls(host)) {
            Result r = tryDownload(url);
            if (r.ok()) return r;
        }
        return Result.fail();
    }

    /** Sendet Testdaten an den Ziel-Host; fällt auf öffentlichen Endpunkt zurück. */
    static Result upload(String host) {
        for (String url : candidateUploadUrls(host)) {
            Result r = tryUpload(url);
            if (r.ok()) return r;
        }
        return Result.fail();
    }

    // ── Download ──────────────────────────────────────────────────────────

    private static Result tryDownload(String urlStr) {
        try {
            HttpURLConnection c = open(urlStr, "GET");
            c.connect();
            if (c.getResponseCode() >= 400) { c.disconnect(); return Result.fail(); }

            long start = System.currentTimeMillis();
            long received = drain(c.getInputStream(), BandwidthTester.TEST_BYTES);
            long ms = System.currentTimeMillis() - start;
            c.disconnect();
            return received > 0 ? new Result(true, toMbps(received, ms)) : Result.fail();
        } catch (Exception e) {
            return Result.fail();
        }
    }

    private static long drain(InputStream in, long limit) throws IOException {
        byte[] buf = new byte[BandwidthTester.BUFFER_SIZE];
        long total = 0;
        int read;
        while (total < limit && (read = in.read(buf)) != -1) total += read;
        return total;
    }

    // ── Upload ────────────────────────────────────────────────────────────

    private static Result tryUpload(String urlStr) {
        try {
            HttpURLConnection c = open(urlStr, "POST");
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(BandwidthTester.TEST_BYTES);
            c.setRequestProperty("Content-Type", "application/octet-stream");

            byte[] chunk = randomBuffer();
            long start = System.currentTimeMillis();
            try (OutputStream out = c.getOutputStream()) {
                long sent = 0;
                while (sent < BandwidthTester.TEST_BYTES) {
                    int n = (int) Math.min(chunk.length, BandwidthTester.TEST_BYTES - sent);
                    out.write(chunk, 0, n);
                    sent += n;
                }
            }
            int code = c.getResponseCode();
            long ms = System.currentTimeMillis() - start;
            c.disconnect();
            return code < 500
                    ? new Result(true, toMbps(BandwidthTester.TEST_BYTES, ms))
                    : Result.fail();
        } catch (Exception e) {
            return Result.fail();
        }
    }

    private static byte[] randomBuffer() {
        byte[] b = new byte[BandwidthTester.BUFFER_SIZE];
        new Random().nextBytes(b);
        return b;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static HttpURLConnection open(String urlStr, String method) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(BandwidthTester.CONNECT_TO);
        c.setReadTimeout(BandwidthTester.IO_TO);
        c.setRequestProperty("User-Agent", "NetTool/3.0 BandwidthProbe");
        return c;
    }

    // BandwidthHttpProbe.java – nur noch Ziel-Host testen, kein Public-Fallback
    private static String[] candidateDownloadUrls(String host) {
        return new String[]{
                "https://" + host + "/__down?bytes=" + BandwidthTester.TEST_BYTES,
                "http://"  + host + "/"
        };
    }

    private static String[] candidateUploadUrls(String host) {
        return new String[]{ "https://" + host + "/__up" };
    }

    private static double toMbps(long bytes, long ms) {
        return ms <= 0 ? 0 : (bytes * 8.0) / (ms * 1_000.0);
    }
}
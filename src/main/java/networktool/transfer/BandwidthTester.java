package main.java.networktool.transfer;

import java.io.*;
import java.net.*;

public final class BandwidthTester {

    private BandwidthTester() {}

    public static final int TEST_PORT    = 9998;
    static final int        TEST_MB      = 20;
    public static final long       TEST_BYTES   = TEST_MB * 1024L * 1024L;
    // Für Tests injizierbar — reduziert Datenmenge auf Loopback
    public static volatile long    testBytes    = TEST_BYTES;

    private static final int BUFFER_SIZE = 65_536;
    private static final int CONNECT_TO  = 5_000;
    private static final int IO_TO       = 60_000;
    private static final int RETRIES     = 2;

    // ── Server ────────────────────────────────────────────────────────────

    public static void startServer() {
        Thread t = new Thread(BandwidthTester::serverLoop, "BW-Server");
        t.setDaemon(true);
        t.start();
    }

    private static void serverLoop() {
        try (ServerSocket ss = new ServerSocket(TEST_PORT)) {
            ss.setReuseAddress(true);
            System.out.println("[BW-Server] Lauscht auf Port " + TEST_PORT);
            while (!ss.isClosed()) {
                try { handleClient(ss.accept()); }
                catch (IOException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[BW-Server] " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) {
        new Thread(() -> {
            try {
                client.setSoTimeout(IO_TO);
                BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream   out = client.getOutputStream();
                String cmd = in.readLine();
                if (cmd == null) return;
                if (cmd.startsWith("DL:")) {
                    sendRandomData(out, Long.parseLong(cmd.substring(3).trim()));
                } else if (cmd.startsWith("UL:")) {
                    receiveData(client.getInputStream(), Long.parseLong(cmd.substring(3).trim()));
                    out.write("OK\n".getBytes());
                    out.flush();
                }
            } catch (Exception e) {
                System.err.println("[BW-Server] Client-Fehler: " + e.getMessage());
            } finally {
                try { client.close(); } catch (IOException ignored) {}
            }
        }, "BW-Handler").start();
    }

    // ── Client ────────────────────────────────────────────────────────────

    public static void testBoth(String host) throws Exception {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf( "║  Bandwidth-Test → %-25s║%n", host + " ");
        System.out.println("╚══════════════════════════════════════════════╝");

        if (!isServerReachable(host)) {
            System.out.println("  ✕ BW-Server nicht erreichbar (Port " + TEST_PORT + ").");
            System.out.println("  → Auf dem Ziel-PC: Menü → Bandwidth → Server starten");
            return;
        }

        double dl = testWithRetry(host, true);
        double ul = testWithRetry(host, false);

        if (dl > 0) System.out.printf("  ▼ Download : %7.2f Mbps%n", dl);
        if (ul > 0) System.out.printf("  ▲ Upload   : %7.2f Mbps%n", ul);
    }

    private static double testWithRetry(String host, boolean download) {
        double[] results = new double[RETRIES];
        int ok = 0;
        for (int i = 0; i < RETRIES; i++) {
            double r = download ? testDownload(host) : testUpload(host);
            if (r > 0) results[ok++] = r;
            if (i < RETRIES - 1) {
                try { Thread.sleep(300); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }
        if (ok == 0) return -1;
        double[] valid = java.util.Arrays.copyOf(results, ok);
        java.util.Arrays.sort(valid);
        return valid[ok / 2];
    }

    public static double testDownload(String host) {
        System.out.print("  ▼ Download... ");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, TEST_PORT), CONNECT_TO);
            s.setSoTimeout(IO_TO);
            PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
            pw.println("DL:" + testBytes);
            long start    = System.currentTimeMillis();
            long received = receiveDataFully(s.getInputStream(), testBytes);
            long ms       = System.currentTimeMillis() - start;
            double mbps   = toMbps(received, ms);
            System.out.printf("%7.2f Mbps  (%d ms)%n", mbps, ms);
            return mbps;
        } catch (ConnectException e) { System.out.println("Server nicht erreichbar."); return -1; }
        catch (Exception e)          { System.out.println("Fehler: " + e.getMessage()); return -1; }
    }

    public static double testUpload(String host) {
        System.out.print("  ▲ Upload...   ");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, TEST_PORT), CONNECT_TO);
            s.setSoTimeout(IO_TO);
            PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
            pw.println("UL:" + testBytes);
            long start = System.currentTimeMillis();
            sendRandomData(s.getOutputStream(), testBytes);
            s.getOutputStream().flush();
            new BufferedReader(new InputStreamReader(s.getInputStream())).readLine();
            long ms     = System.currentTimeMillis() - start;
            double mbps = toMbps(testBytes, ms);
            System.out.printf("%7.2f Mbps  (%d ms)%n", mbps, ms);
            return mbps;
        } catch (ConnectException e) { System.out.println("Server nicht erreichbar."); return -1; }
        catch (Exception e)          { System.out.println("Fehler: " + e.getMessage()); return -1; }
    }

    public static boolean isServerReachable(String host) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, TEST_PORT), CONNECT_TO);
            return true;
        } catch (Exception e) { return false; }
    }

    // ── I/O ───────────────────────────────────────────────────────────────

    private static void sendRandomData(OutputStream out, long totalBytes) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        new java.util.Random().nextBytes(buf);
        long sent = 0;
        while (sent < totalBytes) {
            int chunk = (int) Math.min(BUFFER_SIZE, totalBytes - sent);
            out.write(buf, 0, chunk);
            sent += chunk;
        }
        out.flush();
    }

    private static long receiveData(InputStream in, long totalBytes) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        long received = 0;
        int read;
        while (received < totalBytes && (read = in.read(buf)) != -1)
            received += read;
        return received;
    }

    private static long receiveDataFully(InputStream in, long totalBytes) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        long received = 0;
        int read;
        while (received < totalBytes && (read = in.read(buf)) != -1)
            received += read;
        return received;
    }

    private static double toMbps(long bytes, long ms) {
        return ms <= 0 ? 0 : (bytes * 8.0) / (ms * 1_000.0);
    }
}
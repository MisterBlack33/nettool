package main.java.networktool.transfer;

import java.net.*;
import java.util.*;

/** Misst Latenz und Jitter über TCP-Connect-Zeiten (kein ICMP nötig, keine Root-Rechte). */
final class LatencyProbe {

    private LatencyProbe() {}

    private static final int   SAMPLES     = 5;
    private static final int   TIMEOUT_MS  = 1_500;
    private static final int[] PROBE_PORTS = {443, 80, 22};

    record Result(boolean ok, double avgMs, double jitterMs) {
        static Result fail() { return new Result(false, -1, -1); }
    }

    static Result measure(String host) {
        int port = firstOpenPort(host);
        if (port == -1) return Result.fail();

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            long t = connectTime(host, port);
            if (t >= 0) times.add(t);
        }
        if (times.isEmpty()) return Result.fail();

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        double jitter = jitter(times);
        return new Result(true, avg, jitter);
    }

    private static int firstOpenPort(String host) {
        for (int p : PROBE_PORTS) {
            if (connectTime(host, p) >= 0) return p;
        }
        return -1;
    }

    private static long connectTime(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    private static double jitter(List<Long> times) {
        if (times.size() < 2) return 0;
        double sum = 0;
        for (int i = 1; i < times.size(); i++) sum += Math.abs(times.get(i) - times.get(i - 1));
        return sum / (times.size() - 1);
    }
}
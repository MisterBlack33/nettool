package main.java.networktool.logic.scan;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

/**
 * Schneller ICMP-Sweep.
 * Sendet vor dem isReachable()-Check ein ARP-Ping (ping -c1 / ping -n1)
 * damit der ARP-Cache gefüllt wird – HostAliveChecker kann ihn dann lesen.
 */
public final class PingSweep {

    private PingSweep() {}

    private static final int TIMEOUT_MS   = 800;
    private static final int THREAD_COUNT =
            Math.min(128, Runtime.getRuntime().availableProcessors() * 8);

    public static List<String> sweep(List<String> ips, Runnable progress) {
        if (ips.isEmpty()) return new ArrayList<>();
        List<String> alive = Collections.synchronizedList(new ArrayList<>());
        ExecutorService exec = Executors.newFixedThreadPool(
                Math.min(THREAD_COUNT, ips.size()));

        for (String ip : ips) {
            exec.submit(() -> {
                try {
                    // ARP-Trigger: füllt den lokalen ARP-Cache
                    triggerArp(ip);
                    if (InetAddress.getByName(ip).isReachable(TIMEOUT_MS))
                        alive.add(ip);
                } catch (Exception ignored) {}
                if (progress != null) progress.run();
            });
        }

        exec.shutdown();
        try { exec.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return alive;
    }

    public static List<String> sweepSubnet(String prefix, Runnable progress) {
        List<String> ips = new ArrayList<>(254);
        for (int i = 1; i <= 254; i++) ips.add(prefix + "." + i);
        return sweep(ips, progress);
    }

    public static List<String> sweepAndLog(String prefix) {
        System.out.print("[PingSweep] " + prefix + ".1-254... ");
        long t = System.currentTimeMillis();
        List<String> alive = sweepSubnet(prefix, null);
        System.out.printf("%d aktiv / 254  (%d ms)%n",
                alive.size(), System.currentTimeMillis() - t);
        return alive;
    }

    /** Sendet einen kurzen Ping um den ARP-Cache zu befüllen. */
    private static void triggerArp(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            String[] cmd = win
                    ? new String[]{"ping", "-n", "1", "-w", "200", ip}
                    : new String[]{"ping", "-c", "1", "-W", "1", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(600, java.util.concurrent.TimeUnit.MILLISECONDS);
            p.destroy();
        } catch (Exception ignored) {}
    }
}
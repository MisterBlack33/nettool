package main.java.networktool.logic.analysis;

import main.java.networktool.gui.GUI;
import main.java.networktool.gui.GuiTheme;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kontinuierlicher Ping mit Live-Latenz-Kurve im GUI-Output.
 *
 * Startet einen Hintergrund-Thread der den Ziel-Host ping und
 * eine eingebettete JPanel-Kurve im OutputPane aktualisiert.
 *
 * Im CLI-Modus: Textausgabe pro Ping.
 *
 * Abbruch: über das Thread-Interrupt des GuiMenuHandler.cancel().
 */
public final class PingMonitor {

    private PingMonitor() {}

    private static final int INTERVAL_MS  = 1000;  // Ping-Intervall
    private static final int MAX_SAMPLES  = 60;    // Sichtfenster (60 Sekunden)
    private static final int TIMEOUT_MS   = 2000;
    private static final int PANEL_HEIGHT = 120;
    private static final int PANEL_WIDTH  = 620;

    /**
     * Startet den Dauerping. Blockiert bis Interrupt oder {@code maxSeconds}.
     *
     * @param host       Ziel-IP oder Hostname
     * @param maxSeconds 0 = unbegrenzt (bis Abbruch)
     */
    public static void start(String host, int maxSeconds) throws Exception {
        InetAddress inet = InetAddress.getByName(host);
        String ip = inet.getHostAddress();

        System.out.println("\n  Dauerping → " + ip + "  (Abbrechen: Sidebar → ✕)");
        System.out.println("  ─────────────────────────────────────────────");

        if (GUI.isGuiActive()) {
            startGui(inet, ip, maxSeconds);
        } else {
            startCli(inet, ip, maxSeconds);
        }
    }

    // ── GUI-Modus ─────────────────────────────────────────────────────────

    private static void startGui(InetAddress inet, String ip, int maxSeconds) {
        Deque<Long>  latencies  = new ArrayDeque<>();
        AtomicBoolean running   = new AtomicBoolean(true);
        long[]        stats     = {0, 0, Long.MAX_VALUE, 0}; // sent, lost, min, max

        // Live-Graph Panel
        JPanel graph = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintGraph(g, latencies, getWidth(), getHeight(), stats);
            }
        };
        graph.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        graph.setBackground(GuiTheme.BG);

        // Panel in Output einbetten
        SwingUtilities.invokeLater(() -> {
            GUI.instance().appendText("\n  Dauerping " + ip + "\n\n", GuiTheme.ACCENT);
            JTextPane pane = GUI.instance().getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(graph);
            GUI.instance().appendText("\n", GuiTheme.FG);
        });

        // Ping-Thread
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "PingMonitor"); t.setDaemon(true); return t; });

        long startMs = System.currentTimeMillis();

        exec.scheduleAtFixedRate(() -> {
            if (Thread.currentThread().isInterrupted()
                    || (maxSeconds > 0 && System.currentTimeMillis() - startMs > maxSeconds * 1000L)) {
                running.set(false);
                exec.shutdown();
                return;
            }
            stats[0]++;
            long t = System.currentTimeMillis();
            try {
                boolean alive = inet.isReachable(TIMEOUT_MS);
                long ms = System.currentTimeMillis() - t;
                if (alive) {
                    synchronized (latencies) {
                        latencies.addLast(ms);
                        if (latencies.size() > MAX_SAMPLES) latencies.pollFirst();
                    }
                    if (ms < stats[2]) stats[2] = ms;
                    if (ms > stats[3]) stats[3] = ms;
                } else {
                    stats[1]++;
                    synchronized (latencies) {
                        latencies.addLast(-1L); // -1 = Timeout
                        if (latencies.size() > MAX_SAMPLES) latencies.pollFirst();
                    }
                }
                graph.repaint();
            } catch (Exception e) {
                running.set(false);
                exec.shutdown();
            }
        }, 0, INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Warten bis fertig (blockiert den Hintergrundthread des MenuHandlers)
        try {
            while (running.get()) {
                if (Thread.currentThread().isInterrupted()) { exec.shutdownNow(); break; }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long sent = stats[0], lost = stats[1];
        GUI.instance().appendText(
                String.format("  Gesamt: %d  |  Verlust: %d (%.1f%%)  |  min/max: %d/%d ms%n",
                        sent, lost, lost * 100.0 / Math.max(sent, 1),
                        stats[2] == Long.MAX_VALUE ? 0 : stats[2], stats[3]),
                GuiTheme.ACCENT2);
    }

    private static void paintGraph(Graphics g, Deque<Long> latencies,
                                   int w, int h, long[] stats) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Hintergrund
        g2.setColor(GuiTheme.BG);
        g2.fillRect(0, 0, w, h);

        // Rasterlinien
        g2.setColor(new Color(0x1E, 0x2D, 0x3D));
        for (int y = h / 4; y < h; y += h / 4)
            g2.drawLine(0, y, w, y);

        synchronized (latencies) {
            if (latencies.size() < 2) return;

            Long[] arr = latencies.toArray(new Long[0]);
            long max = Arrays.stream(arr).filter(v -> v >= 0).mapToLong(v -> v)
                    .max().orElse(100);
            if (max < 50) max = 50; // Mindest-Skala

            int barW = Math.max(1, w / MAX_SAMPLES);
            int x    = w - arr.length * barW;

            // Latenz-Kurve
            int prevX = -1, prevY = -1;
            for (int i = 0; i < arr.length; i++) {
                long v = arr[i];
                int cx = x + i * barW + barW / 2;

                if (v < 0) {
                    // Timeout: rotes X
                    g2.setColor(GuiTheme.WARN);
                    g2.drawLine(cx - 3, h - 10, cx + 3, h - 4);
                    g2.drawLine(cx + 3, h - 10, cx - 3, h - 4);
                    prevX = prevY = -1;
                    continue;
                }

                int cy = h - 4 - (int) (v * (h - 8) / max);
                cy = Math.max(2, Math.min(h - 4, cy));

                // Farbe nach Latenz
                Color col = v < 20  ? GuiTheme.ACCENT2
                          : v < 100 ? GuiTheme.ACCENT
                          : v < 300 ? new Color(0xFF, 0xD5, 0x4F)
                          : GuiTheme.WARN;
                g2.setColor(col);

                if (prevX >= 0) {
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(prevX, prevY, cx, cy);
                }
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                prevX = cx; prevY = cy;
            }
        }

        // Stats-Overlay
        g2.setColor(new Color(0xC8, 0xD8, 0xE8, 180));
        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        long sent = stats[0], lost = stats[1];
        g2.drawString(String.format("Gesendet: %d  Verlust: %d  min/max: %d/%d ms",
                sent, lost,
                stats[2] == Long.MAX_VALUE ? 0 : stats[2], stats[3]),
                6, 12);
    }

    // ── CLI-Modus ─────────────────────────────────────────────────────────

    private static void startCli(InetAddress inet, String ip, int maxSeconds)
            throws Exception {
        long start = System.currentTimeMillis();
        long sent = 0, lost = 0, minMs = Long.MAX_VALUE, maxMs = 0;
        int seq = 1;

        while (!Thread.currentThread().isInterrupted()) {
            if (maxSeconds > 0 && System.currentTimeMillis() - start > maxSeconds * 1000L)
                break;
            long t = System.currentTimeMillis();
            boolean alive = inet.isReachable(TIMEOUT_MS);
            long ms = System.currentTimeMillis() - t;
            sent++;
            if (alive) {
                if (ms < minMs) minMs = ms;
                if (ms > maxMs) maxMs = ms;
                System.out.printf("  #%-4d  %s  %4d ms%n", seq++, ip, ms);
            } else {
                lost++;
                System.out.printf("  #%-4d  %s  TIMEOUT%n", seq++, ip);
            }
            Thread.sleep(INTERVAL_MS);
        }
        System.out.printf("%n  Gesamt: %d  |  Verlust: %d  |  min/max: %d/%d ms%n",
                sent, lost, minMs == Long.MAX_VALUE ? 0 : minMs, maxMs);
    }
}

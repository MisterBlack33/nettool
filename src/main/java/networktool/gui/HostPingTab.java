package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.HostDetailRows.detailButton;

/** Tab ②: Live-Ping mit Verlaufsgraph (letzte 20 Pings) und History-Liste. */
final class HostPingTab extends JPanel {

    private final String ip;
    private final List<Long> history = new ArrayList<>(Collections.nCopies(20, -1L));
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JLabel statsLbl;
    private final JPanel graphPanel;

    private volatile boolean running = false;
    private Thread pingThread;

    HostPingTab(String ip, Color panBg) {
        this.ip = ip;
        setLayout(new BorderLayout(0, 8));
        setBackground(panBg);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        statsLbl = new JLabel("  Starte Ping…");
        statsLbl.setFont(MONO_S);
        statsLbl.setForeground(FG_DIM);

        JButton stopBtn = detailButton("■ Stop", WARN);
        stopBtn.addActionListener(e -> stopPinging());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBackground(panBg);
        top.add(statsLbl, BorderLayout.CENTER);
        top.add(stopBtn,  BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        graphPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                PingGraphRenderer.paint((Graphics2D) g, getBackground(), history, getWidth(), getHeight());
            }
        };
        graphPanel.setBackground(GuiTheme.isDark() ? new Color(0x05, 0x08, 0x06) : new Color(0xF8, 0xF6, 0xF2));
        graphPanel.setPreferredSize(new Dimension(0, 160));
        add(graphPanel, BorderLayout.CENTER);

        JScrollPane historyScroll = buildHistoryList(panBg);
        historyScroll.setPreferredSize(new Dimension(0, 200));
        add(historyScroll, BorderLayout.SOUTH);
    }

    void start() {
        if (running) return;
        running = true;
        pingThread = new Thread(this::pingLoop, "HostPingTab-" + ip);
        pingThread.setDaemon(true);
        pingThread.start();
    }

    void restart() {
        stopPinging();
        history.replaceAll(v -> -1L);
        listModel.clear();
        start();
    }

    private void stopPinging() {
        running = false;
        if (pingThread != null) pingThread.interrupt();
    }

    private void pingLoop() {
        long sent = 0, lost = 0, totalMs = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                long t = System.currentTimeMillis();
                boolean alive = InetAddress.getByName(ip).isReachable(2000);
                long ms = System.currentTimeMillis() - t;
                sent++;
                if (alive) totalMs += ms; else lost++;
                recordSample(alive, ms);
                publishStats(sent, lost, totalMs, alive, ms);
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                /* transient lookup failure — retry next cycle */
            }
        }
    }

    private void recordSample(boolean alive, long ms) {
        synchronized (history) {
            history.remove(0);
            history.add(alive ? ms : -1L);
        }
    }

    private void publishStats(long sent, long lost, long totalMs, boolean alive, long ms) {
        String time  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = time + "  " + (alive ? ms + " ms" : "TIMEOUT");
        long avg     = sent - lost > 0 ? totalMs / (sent - lost) : 0;

        SwingUtilities.invokeLater(() -> {
            listModel.add(0, entry);
            if (listModel.size() > 100) listModel.remove(listModel.size() - 1);
            statsLbl.setText(String.format("  Gesendet: %d  Verlust: %d (%.0f%%)  Ø: %d ms",
                    sent, lost, lost * 100.0 / Math.max(sent, 1), avg));
            statsLbl.setForeground(lost == 0 ? ACCENT2 : WARN);
            graphPanel.repaint();
        });
    }

    private JScrollPane buildHistoryList(Color panBg) {
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        list.setForeground(FG);
        list.setBackground(GuiTheme.isDark() ? new Color(0x05, 0x08, 0x06) : Color.WHITE);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(list.getBackground());
        return sp;
    }
}
package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Fortschrittsanzeige. Refresh-Intervall auf 750ms erhöht (war 500ms)
 * um EDT-Last auf schwacher Hardware zu reduzieren.
 */
public class GuiProgressBar {

    // Increased from 500ms → 750ms to reduce EDT load on weak hardware
    private static final int REFRESH_INTERVAL_MS = 750;
    private static final int HIDE_DELAY_MS        = 2500;

    private final JPanel       panel;
    private final JProgressBar bar;
    private final JLabel       label;
    private final JLabel       etaLabel;
    private       Timer        uiTimer;
    private       long         startMs;

    private volatile int     lastDone  = 0;
    private volatile int     lastTotal = 1;
    private volatile boolean finished  = false;

    public GuiProgressBar() {
        panel    = new JPanel(new BorderLayout(12, 0));
        label    = new JLabel("Scan läuft...");
        etaLabel = new JLabel("ETA: --");
        bar      = buildProgressBar();
        assemblePanel();
    }

    public JPanel getPanel() { return panel; }

    public void showProgress(int total) {
        startMs   = System.currentTimeMillis();
        lastDone  = 0;
        lastTotal = total;
        finished  = false;
        SwingUtilities.invokeLater(() -> {
            bar.setMaximum(total);
            bar.setValue(0);
            bar.setForeground(ACCENT);
            label.setForeground(ACCENT);
            label.setText("Scan läuft — 0 / " + total);
            etaLabel.setText("ETA: ...");
            panel.setVisible(true);
            startRefreshTimer();
        });
    }

    public void updateProgress(int done) {
        lastDone = done;
        if (done >= lastTotal) finished = true;
    }

    private void startRefreshTimer() {
        if (uiTimer != null && uiTimer.isRunning()) uiTimer.stop();
        uiTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshUi());
        uiTimer.start();
    }

    private void refreshUi() {
        int  done    = lastDone;
        int  total   = lastTotal;
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        long eta      = done <= 0 ? 0 : (long) (elapsed * (total - done) / (double) done);
        int  pct      = total == 0 ? 100 : (int) (done * 100.0 / total);

        bar.setValue(Math.min(done, total));
        label.setText(String.format("Scan läuft — %d / %d  (%d%%)", done, total, pct));
        etaLabel.setText("ETA: " + eta + "s");

        if (finished || done >= total) {
            finishProgress(total);
        }
    }

    private void finishProgress(int total) {
        uiTimer.stop();
        bar.setValue(total);
        bar.setForeground(ACCENT2);
        label.setForeground(ACCENT2);
        label.setText("Scan abgeschlossen — " + total + " Hosts geprüft");
        etaLabel.setText("");

        Timer hideTimer = new Timer(HIDE_DELAY_MS, ev -> panel.setVisible(false));
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void assemblePanel() {
        boolean dark = GuiTheme.isDark();
        panel.setBackground(dark ? new Color(0x08, 0x0C, 0x0A) : new Color(0xE0, 0xDE, 0xD8));
        panel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 1, 0, BORDER),
                new EmptyBorder(9, 14, 9, 14)
        ));
        label.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        label.setForeground(ACCENT);
        label.setPreferredSize(new Dimension(260, 18));

        etaLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        etaLabel.setForeground(FG_DIM);
        etaLabel.setPreferredSize(new Dimension(100, 18));
        etaLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(label,    BorderLayout.WEST);
        panel.add(bar,      BorderLayout.CENTER);
        panel.add(etaLabel, BorderLayout.EAST);
        panel.setVisible(false);
    }

    private JProgressBar buildProgressBar() {
        JProgressBar b = new JProgressBar(0, 100);
        b.setStringPainted(false);
        b.setBorderPainted(false);
        b.setBackground(new Color(0x1A, 0x28, 0x38));
        b.setForeground(ACCENT);
        b.setPreferredSize(new Dimension(0, 18));
        b.setUI(new RoundedProgressBarUI());
        return b;
    }

    private static class RoundedProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI {

        @Override protected Color getSelectionBackground() { return BG; }
        @Override protected Color getSelectionForeground() { return BG; }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2  = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            JProgressBar bar = (JProgressBar) c;
            int w   = c.getWidth();
            int h   = c.getHeight();
            int arc = h;

            g2.setColor(new Color(0x1A, 0x28, 0x38));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            double pct = bar.getPercentComplete();
            if (pct > 0) {
                int filled = Math.min(Math.max((int) (w * pct), arc), w);
                g2.setClip(0, 0, filled, h);
                g2.setColor(bar.getForeground());
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(2, 1, w - 4, h / 2 - 1, arc - 2, arc - 2);
                g2.setClip(null);
            }
            g2.dispose();
        }
    }
}
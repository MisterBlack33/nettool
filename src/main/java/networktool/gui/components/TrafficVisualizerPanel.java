package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficVisualizer;

import javax.swing.*;
import java.awt.*;

/**
 * Zeichnet die Traffic-Wellenform periodisch neu. Analog zu {@code MapCanvas}
 * (Konstruktor nimmt nur die Hintergrundfarbe).
 */
public final class TrafficVisualizerPanel extends JPanel {

    // 400ms: schnell genug für sichtbare Bewegung, ohne EDT auf schwacher Hardware zu belasten.
    private static final int REPAINT_INTERVAL_MS = 400;

    private final Color bg;
    private final Timer repaintTimer;

    public TrafficVisualizerPanel(Color bg) {
        this.bg = bg;
        setBackground(bg);
        setPreferredSize(new Dimension(0, 140));
        repaintTimer = new Timer(REPAINT_INTERVAL_MS, e -> repaintIfActive());
        repaintTimer.start();
    }

    private void repaintIfActive() {
        if (TrafficVisualizer.getInstance().isActive()) repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        TrafficWaveformRenderer.paint(g2, bg, TrafficVisualizer.getInstance().getSnapshot(),
                getWidth(), getHeight());
        g2.dispose();
    }
}
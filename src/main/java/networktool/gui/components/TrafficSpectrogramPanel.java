package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficVisualizer;

import javax.swing.*;
import java.awt.*;

/**
 * Spektrogramm-Ansicht des Traffic-Datenstroms (Funktion 2 von "Data → Visual").
 * Nutzt dieselbe Datenerfassung wie {@link TrafficVisualizerPanel} (Funktion 1),
 * nur die Darstellung unterscheidet sich (siehe {@link TrafficSpectrogramRenderer}).
 */
public final class TrafficSpectrogramPanel extends JPanel {

    private static final int REPAINT_INTERVAL_MS = 400;

    private final Timer repaintTimer;

    public TrafficSpectrogramPanel() {
        setBackground(new Color(0x05, 0x02, 0x30));
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
        TrafficSpectrogramRenderer.paint(g2, TrafficVisualizer.getInstance().getSnapshot(),
                getWidth(), getHeight());
        g2.dispose();
    }
}
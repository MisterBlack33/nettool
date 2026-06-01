package main.java.networktool.logic.scan;

import main.java.networktool.gui.GUI;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-sichere Fortschrittsanzeige für laufende Scans.
 *
 * Im GUI-Modus wird der Fortschritt an {@link GUI} delegiert.
 * Im CLI-Modus wird ein animierter ASCII-Balken auf {@code System.out} geschrieben
 * (überschreibt die aktuelle Zeile via {@code \r}).
 */
public final class ScanProgress {

    private static final String[] SPINNER = {"|", "/", "-", "\\"};
    private static final int BAR_WIDTH    = 30;
    private static final int CLI_STEP     = 10; // Ausgabe nur jeden N-ten Schritt

    private final int total;
    private final AtomicInteger done  = new AtomicInteger(0);
    private final long startMs        = System.currentTimeMillis();
    private final boolean guiMode;
    private int spinIndex             = 0;

    public ScanProgress(int total) {
        this.total   = total;
        this.guiMode = GUI.isGuiActive();

        if (guiMode) {
            GUI.instance().showProgress(total);
        } else {
            System.out.printf("Scanne %d Hosts...%n", total);
        }
    }

    /** Inkrementiert den Fortschritt um einen Schritt. */
    public void step() {
        int n = done.incrementAndGet();
        if (guiMode) {
            GUI.instance().updateProgress(n);
        } else if (n % CLI_STEP == 0 || n == total) {
            renderCli(n);
        }
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private synchronized void renderCli(int n) {
        double pct    = (double) n / total;
        int filled    = (int) (pct * BAR_WIDTH);
        long elapsed  = (System.currentTimeMillis() - startMs) / 1000;
        long eta      = n == 0 ? 0 : (long) (elapsed / pct) - elapsed;
        String spin   = SPINNER[spinIndex++ % SPINNER.length];

        String bar = spin + " ["
                + "#".repeat(filled)
                + "-".repeat(BAR_WIDTH - filled)
                + "] "
                + String.format("%3d%% (%d/%d) ETA: %ds   ",
                        (int) (pct * 100), n, total, eta);

        System.out.print("\r" + bar);
        if (n >= total) System.out.println();
    }
}

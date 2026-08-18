package main.java.networktool.logic.scan;

/**
 * Gleitender Mittelwert + Streuung nach Welford's-Algorithmus.
 * Hält keine volle Historie — konstanter Speicherbedarf pro Subnetz.
 */
final class SubnetStats {

    private int    count = 0;
    private double mean  = 0.0;
    private double m2    = 0.0;

    synchronized void add(double value) {
        count++;
        double delta = value - mean;
        mean += delta / count;
        m2   += delta * (value - mean);
    }

    synchronized int count() { return count; }

    synchronized double mean() { return mean; }

    synchronized double stddev() {
        return count < 2 ? 0.0 : Math.sqrt(m2 / (count - 1));
    }
}
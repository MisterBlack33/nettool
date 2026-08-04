package main.java.networktool.logic.sonify;

/** Tonhöhe für ausgehenden (HIGH) bzw. eingehenden (LOW) Traffic. */
public enum ToneFrequency {
    HIGH(880),
    LOW(220);

    public final int hz;

    ToneFrequency(int hz) { this.hz = hz; }
}

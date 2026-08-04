package main.java.networktool.logic.sonify;

/** Tondauer für hohe (LONG) bzw. niedrige (SHORT) Lautstärke-Stufe. */
public enum ToneDuration {
    LONG(260),
    SHORT(80);

    public final int ms;

    ToneDuration(int ms) { this.ms = ms; }
}

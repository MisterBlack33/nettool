package main.java.networktool.logic.sonify;

/** Benutzerdefinierte Einstellungen für die Traffic-Sonifizierung. */
public final class SonifyConfig {

    public static final int DEFAULT_HIGH_HZ = 880;
    public static final int DEFAULT_LOW_HZ  = 220;
    public static final int DEFAULT_TONE_MS = 500;

    public int highHz = DEFAULT_HIGH_HZ;
    public int lowHz  = DEFAULT_LOW_HZ;
    public int toneMs = DEFAULT_TONE_MS;

    public SonifyConfig copy() {
        SonifyConfig c = new SonifyConfig();
        c.highHz = highHz; c.lowHz = lowHz; c.toneMs = toneMs;
        return c;
    }
}
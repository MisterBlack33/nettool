package main.java.networktool.logic.sonify;

/**
 * Ein einzelner Ton, der ein Bit eines Traffic-Deltas repräsentiert.
 * audible = Bitwert, frequency = Richtung (ausgehend/eingehend),
 * duration = Lautstärke-Stufe.
 */
public record TrafficTone(boolean audible, ToneFrequency frequency, ToneDuration duration) {

    public static TrafficTone forBit(boolean bit, boolean outgoing, boolean highVolume) {
        return new TrafficTone(
                bit,
                outgoing ? ToneFrequency.HIGH : ToneFrequency.LOW,
                highVolume ? ToneDuration.LONG : ToneDuration.SHORT
        );
    }
}

package main.java.networktool.logic.sonify;

import java.util.ArrayList;
import java.util.List;

/**
 * Kodiert einen Byte-Wert (0–255, mit Wrap-Around für Werte außerhalb) als
 * 8 Bits, höchstwertiges Bit zuerst.
 */
public final class BitEncoder {

    private BitEncoder() {}

    public static List<Boolean> toBits(int value) {
        int v = ((value % 256) + 256) % 256;
        List<Boolean> bits = new ArrayList<>(8);
        for (int i = 7; i >= 0; i--) {
            bits.add(((v >> i) & 1) == 1);
        }
        return bits;
    }
}

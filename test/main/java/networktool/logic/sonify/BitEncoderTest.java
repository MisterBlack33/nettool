package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BitEncoderTest {

    @Test void zero_allFalse()   { assertTrue(BitEncoder.toBits(0).stream().noneMatch(b -> b)); }
    @Test void max_allTrue()     { assertTrue(BitEncoder.toBits(255).stream().allMatch(b -> b)); }
    @Test void one_lastBitTrue() {
        List<Boolean> bits = BitEncoder.toBits(1);
        assertTrue(bits.get(7));
        assertFalse(bits.get(0));
    }
    @Test void size_isEight()    { assertEquals(8, BitEncoder.toBits(42).size()); }
    @Test void wraps_negative()  { assertEquals(BitEncoder.toBits(255), BitEncoder.toBits(-1)); }
    @Test void wraps_over256()   { assertEquals(BitEncoder.toBits(5), BitEncoder.toBits(261)); }
}
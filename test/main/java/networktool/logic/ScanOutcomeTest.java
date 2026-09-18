package main.java.networktool.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanOutcomeTest {

    @Test void success_isSuccessTrue() {
        assertTrue(ScanOutcome.success("x").isSuccess());
    }

    @Test void failure_isSuccessFalse() {
        assertFalse(ScanOutcome.failure("bad").isSuccess());
    }

    @Test void success_orElse_returnsValue() {
        assertEquals("x", ScanOutcome.success("x").orElse("fallback"));
    }

    @Test void failure_orElse_returnsFallback() {
        assertEquals("fallback", ScanOutcome.<String>failure("bad").orElse("fallback"));
    }

    @Test void failure_reasonAccessible() {
        var f = new ScanOutcome.Failure<String>("timeout");
        assertEquals("timeout", f.reason());
    }

    @Test void success_valueAccessible() {
        var s = new ScanOutcome.Success<>(42);
        assertEquals(42, s.value());
    }
}

package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Regressionstests: PowerShell-Script-Injection Ã¼ber IP/PrÃ¤fix muss abgewiesen werden. */
class PsResolverInjectionTest {

    @Test void isOpen_quoteBreakout_rejected() {
        assertFalse(PsPortScanResolver.isOpen("1.1.1.1'; Remove-Item C:\\ -Recurse; '", 80));
    }

    @Test void isOpen_semicolonInjection_rejected() {
        assertFalse(PsPortScanResolver.isOpen("1.1.1.1; calc.exe", 80));
    }

    @Test void isOpen_null_rejected() {
        assertFalse(PsPortScanResolver.isOpen(null, 80));
    }

    @Test void isOpen_validIp_doesNotThrow() {
        assertDoesNotThrow(() -> PsPortScanResolver.isOpen("192.0.2.1", 80));
    }

    @Test void sweep_quoteBreakout_rejected() {
        assertTrue(PsNetScanResolver.sweep("192.168.1'; calc.exe; '").isEmpty());
    }

    @Test void sweep_extraOctet_rejected() {
        // vier Oktette statt drei â†’ kein gÃ¼ltiges 3-Oktett-PrÃ¤fix
        assertTrue(PsNetScanResolver.sweep("192.168.1.5").isEmpty());
    }

    @Test void sweep_null_rejected() {
        assertTrue(PsNetScanResolver.sweep(null).isEmpty());
    }

    @Test void sweep_validPrefix_doesNotThrow() {
        assertDoesNotThrow(() -> PsNetScanResolver.sweep("192.0.2"));
    }
}
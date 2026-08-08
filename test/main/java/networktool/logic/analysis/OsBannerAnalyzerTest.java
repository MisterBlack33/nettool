package main.java.networktool.logic.analysis;

import main.java.networktool.logic.analysis.OsBannerAnalyzer;
import main.java.networktool.logic.analysis.OsSignature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests fÃƒÆ’Ã‚Â¼r OsBannerAnalyzer inkl. HTTPS und FTP. */
class OsBannerAnalyzerTest {

    @Test
    void analyze_unreachable_returnsNull() {
        OsSignature r = OsBannerAnalyzer.analyze("192.0.2.1");
        assertNull(r);
    }

    @Test
    void analyze_doesNotThrow() {
        assertDoesNotThrow(() -> OsBannerAnalyzer.analyze("192.0.2.1"));
    }

    @Test
    void analyzeHttps_unreachable_returnsNull() {
        OsSignature r = OsBannerAnalyzer.analyzeHttps("192.0.2.2");
        assertNull(r);
    }

    @Test
    void analyzeFtp_unreachable_returnsNull() {
        OsSignature r = OsBannerAnalyzer.analyzeFtp("192.0.2.3");
        assertNull(r);
    }
}
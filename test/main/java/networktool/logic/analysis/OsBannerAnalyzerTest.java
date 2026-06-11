// test/main/java/networktool/logic/analysis/OsBannerAnalyzerTest.java
package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class OsBannerAnalyzerTest {

    @Test void analyze_unreachable_returnsNull() {
        assertNull(OsBannerAnalyzer.analyze("192.0.2.1"));
    }

    @Test void analyze_doesNotThrow() {
        assertDoesNotThrow(() -> OsBannerAnalyzer.analyze("192.0.2.1"));
    }

    @Test void parseSshBanner_ubuntu() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseSshBanner", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "ssh-2.0-openssh_8.9p1 ubuntu-3");
        assertNotNull(sig);
        assertTrue(sig.os.contains("Ubuntu"));
        assertTrue(sig.score >= 80);
    }

    @Test void parseSshBanner_openssh_generic() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseSshBanner", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "ssh-2.0-openssh_9.0");
        assertNotNull(sig);
        assertTrue(sig.os.contains("Linux"));
    }

    @Test void parseSshBanner_windows() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseSshBanner", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "ssh-2.0-openssh_for_windows_8.1");
        assertNotNull(sig);
        assertEquals("Windows", sig.os);
    }

    @Test void parseSshBanner_unknown_returnsNull() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseSshBanner", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, "ssh-2.0-dropbear_2022.83"));
    }

    @Test void parseHttpHeader_iis() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseHttpHeader", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "server: microsoft-iis/10.0");
        assertNotNull(sig);
        assertTrue(sig.os.contains("Windows"));
        assertTrue(sig.score >= 80);
    }

    @Test void parseHttpHeader_fritz() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseHttpHeader", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "server: fritz!os");
        assertNotNull(sig);
        assertTrue(sig.os.contains("FRITZ"));
    }

    @Test void parseHttpHeader_nginx() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseHttpHeader", String.class);
        m.setAccessible(true);
        OsSignature sig = (OsSignature) m.invoke(null, "server: nginx/1.24");
        assertNotNull(sig);
        assertTrue(sig.os.contains("Linux"));
    }

    @Test void parseHttpHeader_noServerHeader_null() throws Exception {
        var m = OsBannerAnalyzer.class.getDeclaredMethod("parseHttpHeader", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, "content-type: text/html"));
    }
}
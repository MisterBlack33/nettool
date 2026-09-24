package main.java.networktool.logic.analysis.discovery;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class DhcpOptionAnalyzerExtTest {
    private static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = DhcpOptionAnalyzer.class.getDeclaredMethod(name, types);
        m.setAccessible(true); return m.invoke(null, args);
    }
    private static String classify(String s) throws Exception {
        return (String) call("classify", new Class<?>[]{String.class}, s);
    }
    @Test void classify_knownVendors() throws Exception {
        assertEquals("Windows", classify("MSFT 5.0"));
        assertEquals("Android", classify("android-dhcp-11"));
        assertEquals("macOS", classify("AAPLBSDPC"));
        assertEquals("Linux", classify("dhcpcd-9.4.1"));
        assertEquals("Embedded Linux / Router", classify("udhcp 1.2"));
        assertEquals("Cisco-Gerät", classify("cisco-ap"));
        assertEquals("Access Point (Aruba)", classify("aruba-ap"));
    }
    @Test void classify_unknown_includesRaw() throws Exception {
        assertTrue(classify("SomethingElse").contains("SomethingElse"));
    }
    @Test void buildDiscover_hasMagicCookieAndDiscoverType() throws Exception {
        byte[] p = (byte[]) call("buildDiscover", new Class<?>[]{});
        assertEquals(0x63, p[236] & 0xff); assertEquals(0x35, p[240] & 0xff); assertEquals(1, p[242]);
    }
    @Test void extractOption60_findsAndRejectsMissing() throws Exception {
        byte[] p = (byte[]) call("buildDiscover", new Class<?>[]{});
        assertEquals("NetTool", call("extractOption60", new Class<?>[]{byte[].class, int.class}, p, p.length));
        byte[] empty = new byte[300]; empty[240] = (byte) 255;
        assertNull(call("extractOption60", new Class<?>[]{byte[].class, int.class}, empty, empty.length));
    }
    @Test void resultRecord_fieldsAccessible() {
        DhcpOptionAnalyzer.Result r = new DhcpOptionAnalyzer.Result("MSFT 5.0", "Windows");
        assertEquals("MSFT 5.0", r.vendorClass()); assertEquals("Windows", r.detectedOs());
    }
}

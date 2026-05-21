package networktool_v3.filter;

import main.java.networktool_v3.filter.HostResultPrinter;
import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FilterExtTest {

    @Nested
    class HostResultPrinterTest {

        @Test
        void print_noHosts_doesNotThrow() {
            captureOutput(() ->
                    HostResultPrinter.print(List.of(), "Empty"));
        }

        @Test
        void print_withHosts_outputsIp() {
            String out = captureOutput(() ->
                    HostResultPrinter.print(
                            List.of(new HostResult("1.2.3.4", "host", "Linux")),
                            "Test"));
            assertTrue(out.contains("1.2.3.4"));
        }

        @Test
        void print_sortsByIp() {
            List<HostResult> hosts = List.of(
                    new HostResult("10.0.0.3", "c", "Linux"),
                    new HostResult("10.0.0.1", "a", "Linux"),
                    new HostResult("10.0.0.2", "b", "Linux")
            );
            String out = captureOutput(() -> HostResultPrinter.print(hosts, "Sorted"));
            int i1 = out.indexOf("10.0.0.1");
            int i2 = out.indexOf("10.0.0.2");
            int i3 = out.indexOf("10.0.0.3");
            assertTrue(i1 < i2 && i2 < i3);
        }

        @Test
        void print_labelInOutput() {
            String out = captureOutput(() ->
                    HostResultPrinter.print(List.of(), "MyLabel"));
            assertTrue(out.contains("MyLabel"));
        }

        @Test
        void print_hostWithMac_outputsClean() {
            String out = captureOutput(() ->
                    HostResultPrinter.print(
                            List.of(new HostResult("5.5.5.5", "server [AA:BB:CC]", "Win")),
                            "MAC-Test"));
            assertTrue(out.contains("5.5.5.5"));
        }

        private String captureOutput(Runnable r) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream orig = System.out;
            System.setOut(new PrintStream(buf));
            try { r.run(); } finally { System.setOut(orig); }
            return buf.toString();
        }
    }
}
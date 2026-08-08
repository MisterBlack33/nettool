package networktool.filter;

import main.java.networktool.filter.HostResultPrinter;
import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FilterExtTest {

    @Nested
    class HostResultPrinterTest {

        @Test
        void print_noHosts_doesNotThrow() {
            assertDoesNotThrow(() ->
                    captureOutput(() -> HostResultPrinter.print(List.of(), "Empty")));
        }

        @Test
        void print_withHosts_outputsIp() {
            String out = captureOutput(() ->
                    HostResultPrinter.print(
                            List.of(new HostResult("1.2.3.4", "host", "Linux")), "Test"));
            assertTrue(out.contains("1.2.3.4"));
        }

        @Test
        void print_sortsByIp() {
            String out = captureOutput(() -> HostResultPrinter.print(List.of(
                    new HostResult("10.0.0.3", "c", "Linux"),
                    new HostResult("10.0.0.1", "a", "Linux"),
                    new HostResult("10.0.0.2", "b", "Linux")), "Sorted"));
            assertTrue(out.indexOf("10.0.0.1") < out.indexOf("10.0.0.2"));
            assertTrue(out.indexOf("10.0.0.2") < out.indexOf("10.0.0.3"));
        }

        @Test
        void print_labelInOutput() {
            assertTrue(captureOutput(() ->
                    HostResultPrinter.print(List.of(), "MyLabel")).contains("MyLabel"));
        }

        @Test
        void print_hostWithMac_outputsIp() {
            assertTrue(captureOutput(() ->
                    HostResultPrinter.print(
                            List.of(new HostResult("5.5.5.5", "server [AA:BB:CC]", "Win")),
                            "MAC-Test")).contains("5.5.5.5"));
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
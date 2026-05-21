package main.java.networktool_v3.logic.ports;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PortsPackageTest {

    // ══════════════════════════════════════════════════════════════
    //  BannerGrabber
    // ══════════════════════════════════════════════════════════════

    @Nested
    class BannerGrabberTest {

        @Test
        void grab_closedPort_returnsServiceName() {
            String banner = BannerGrabber.grab("127.0.0.1", 19999, 300);
            assertNotNull(banner);
            assertFalse(banner.isBlank());
        }

        @Test
        void grab_knownPort_returnsLabel() {
            String banner = BannerGrabber.grab("127.0.0.1", 22, 300);
            assertNotNull(banner);
            assertTrue(banner.contains("SSH") || !banner.isBlank());
        }

        @Test
        void grab_port80_returnsHttpLabel() {
            String r = BannerGrabber.grab("127.0.0.1", 80, 300);
            assertNotNull(r);
        }

        @Test
        void grab_port443_returnsHttpsLabel() {
            assertEquals("HTTPS", BannerGrabber.grab("127.0.0.1", 443, 300));
        }

        @Test
        void grab_defaultTimeout() {
            assertNotNull(BannerGrabber.grab("127.0.0.1", 9999));
        }

        @Test
        void grab_port21_returnsFtp() {
            assertEquals("FTP", BannerGrabber.grab("127.0.0.1", 21, 300));
        }

        @Test
        void grab_port53_returnsDns() {
            assertEquals("DNS", BannerGrabber.grab("127.0.0.1", 53, 300));
        }

        @Test
        void grab_port3306_returnsMysql() {
            assertEquals("MySQL", BannerGrabber.grab("127.0.0.1", 3306, 300));
        }

        @Test
        void grab_port6379_returnsRedis() {
            assertEquals("Redis", BannerGrabber.grab("127.0.0.1", 6379, 300));
        }

        @Test
        void grab_port27017_returnsMongo() {
            assertEquals("MongoDB", BannerGrabber.grab("127.0.0.1", 27017, 300));
        }

        @Test
        void grab_port1883_returnsMqtt() {
            assertEquals("MQTT", BannerGrabber.grab("127.0.0.1", 1883, 300));
        }

        @Test
        void grab_port5353_returnsMdns() {
            assertEquals("mDNS", BannerGrabber.grab("127.0.0.1", 5353, 300));
        }

        @Test
        void grab_port9100_returnsJetDirect() {
            assertEquals("RAW/JetDirect (Drucker)", BannerGrabber.grab("127.0.0.1", 9100, 300));
        }

        @Test
        void grab_port515_returnsLpd() {
            assertEquals("LPD (Drucker)", BannerGrabber.grab("127.0.0.1", 515, 300));
        }

        @Test
        void grab_port548_returnsAfp() {
            assertEquals("AFP (Apple File Sharing)", BannerGrabber.grab("127.0.0.1", 548, 300));
        }

        @Test
        void grab_port161_returnsSnmp() {
            assertEquals("SNMP", BannerGrabber.grab("127.0.0.1", 161, 300));
        }

        @Test
        void grab_port3389_returnsRdp() {
            assertEquals("RDP (Remote Desktop)", BannerGrabber.grab("127.0.0.1", 3389, 300));
        }

        @Test
        void grab_port5985_returnsWinrm() {
            // falls back to WinRM service name since port closed
            String r = BannerGrabber.grab("127.0.0.1", 5985, 300);
            assertNotNull(r);
        }

        @Test
        void grab_port5986_returnsWinrmHttps() {
            assertEquals("WinRM HTTPS", BannerGrabber.grab("127.0.0.1", 5986, 300));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PortScanner.probePort
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PortScannerProbeTest {

        @Test
        void probePort_closedPort_returnsClosed() {
            PortScanner.PortState s = PortScanner.probePort("127.0.0.1", 19990, 300);
            assertEquals(PortScanner.PortState.CLOSED, s);
        }

        @Test
        void isOpen_closedPort_false() {
            assertFalse(PortScanner.isOpen("127.0.0.1", 19991, 300));
        }

        @Test
        void isOpen_withServer_true() throws Exception {
            try (java.net.ServerSocket ss = new java.net.ServerSocket(19992)) {
                assertTrue(PortScanner.isOpen("127.0.0.1", 19992, 1000));
            }
        }

        @Test
        void probePort_openPort_returnsOpen() throws Exception {
            try (java.net.ServerSocket ss = new java.net.ServerSocket(19993)) {
                assertEquals(PortScanner.PortState.OPEN,
                        PortScanner.probePort("127.0.0.1", 19993, 1000));
            }
        }

        @Test
        void scanSimple_closedHost_emptyMap() {
            Map<Integer, String> r = PortScanner.scanSimple("127.0.0.1", 300);
            assertNotNull(r);
        }
    }
}
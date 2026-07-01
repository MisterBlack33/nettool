package networktool.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BandwidthTesterActiveTest {

    @Test void testDownload_unreachable_negative() {
        assertTrue(BandwidthTester.testDownload("192.0.2.1") < 0
                || BandwidthTester.testDownload("192.0.2.1") == -1);
    }

    @Test void testUpload_unreachable_negative() {
        assertTrue(BandwidthTester.testUpload("192.0.2.1") < 0
                || BandwidthTester.testUpload("192.0.2.1") == -1);
    }

    @Test void testBoth_doesNotThrow() {
        assertDoesNotThrow(() -> BandwidthTester.testBoth("192.0.2.1"));
    }

    @Test void latencyProbe_unreachable_fails() {
        assertFalse(LatencyProbe.measure("192.0.2.1").ok());
    }

    @Test void latencyProbe_loopback_doesNotThrow() {
        assertDoesNotThrow(() -> LatencyProbe.measure("127.0.0.1"));
    }

    @Test void httpProbe_download_unreachable_fails() {
        assertFalse(BandwidthHttpProbe.download("192.0.2.1").ok());
    }

    @Test void httpProbe_upload_unreachable_fails() {
        assertFalse(BandwidthHttpProbe.upload("192.0.2.1").ok());
    }

    @Test void result_fail_hasNegativeMbps() {
        assertEquals(-1, BandwidthHttpProbe.Result.fail().mbps());
    }
}
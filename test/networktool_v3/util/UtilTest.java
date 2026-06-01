package networktool_v3.util;

import main.java.networktool.gui.GuiLoginRateLimiter;
import main.java.networktool.util.IpValidator;
import main.java.networktool.util.PlatformUtils;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    // ══════════════════════════════════════════════════════════════
    //  PlatformUtils
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PlatformUtilsTest {

        @Test void isSafeIp_valid()        { assertTrue(PlatformUtils.isSafeIp("192.168.1.1")); }
        @Test void isSafeIp_invalid()      { assertFalse(PlatformUtils.isSafeIp("192.168.1.1; rm -rf")); }
        @Test void isSafeIp_null()         { assertFalse(PlatformUtils.isSafeIp(null)); }
        @Test void isSafeIp_empty()        { assertFalse(PlatformUtils.isSafeIp("")); }
        @Test void isSafeIp_ipv6()         { assertFalse(PlatformUtils.isSafeIp("::1")); }

        @Test void isSafeMac_valid()       { assertTrue(PlatformUtils.isSafeMac("AA:BB:CC:DD:EE:FF")); }
        @Test void isSafeMac_dashFormat()  { assertTrue(PlatformUtils.isSafeMac("AA-BB-CC-DD-EE-FF")); }
        @Test void isSafeMac_invalid()     { assertFalse(PlatformUtils.isSafeMac("AA:BB:CC")); }

        @Test void isSafeCidr_valid()      { assertTrue(PlatformUtils.isSafeCidr("10.0.0.0/24")); }
        @Test void isSafeCidr_invalid()    { assertFalse(PlatformUtils.isSafeCidr("10.0.0.0/24; evil")); }

        @Test void isSafeHostname_valid()  { assertTrue(PlatformUtils.isSafeHostname("my-host.local")); }
        @Test void isSafeHostname_inject() { assertFalse(PlatformUtils.isSafeHostname("host; rm -rf /")); }

        @Test void requireSafeIp_valid()   { assertEquals("1.2.3.4", PlatformUtils.requireSafeIp("1.2.3.4")); }
        @Test void requireSafeIp_invalid() { assertThrows(IllegalArgumentException.class,
                () -> PlatformUtils.requireSafeIp("1.2.3.4; bad")); }

        @Test void escapePowerShell_quote()   { assertTrue(PlatformUtils.escapePowerShell("it's").contains("''")); }
        @Test void escapePowerShell_null()    { assertEquals("", PlatformUtils.escapePowerShell(null)); }
        @Test void escapeSshArg_quote()       { assertTrue(PlatformUtils.escapeSshArg("it's").contains("\\'")); }
        @Test void escapeSshArg_backslash()   { assertTrue(PlatformUtils.escapeSshArg("a\\b").contains("\\\\")); }

        @Test void isWindows_returnsBoolean() { assertNotNull(PlatformUtils.isWindows()); }
    }

    // ══════════════════════════════════════════════════════════════
    //  IpValidator
    // ══════════════════════════════════════════════════════════════

    @Nested
    class IpValidatorTest {

        @Test void validIp()             { assertTrue(IpValidator.isValidIpv4("192.168.0.1")); }
        @Test void invalidOctet()        { assertFalse(IpValidator.isValidIpv4("999.0.0.1")); }
        @Test void tooFewOctets()        { assertFalse(IpValidator.isValidIpv4("192.168.1")); }
        @Test void nullIp()              { assertFalse(IpValidator.isValidIpv4(null)); }
        @Test void validCidr()           { assertTrue(IpValidator.isValidCidr("10.0.0.0/24")); }
        @Test void invalidCidrPrefix()   { assertFalse(IpValidator.isValidCidr("10.0.0.0/33")); }
        @Test void validHostname()       { assertTrue(IpValidator.isValidHostname("server.local")); }
        @Test void sanitize_valid()      { assertEquals("1.2.3.4", IpValidator.sanitize("  1.2.3.4  ")); }
        @Test void sanitize_invalid()    { assertNull(IpValidator.sanitize("not-an-ip")); }
        @Test void sanitize_null()       { assertNull(IpValidator.sanitize(null)); }
    }

    // ══════════════════════════════════════════════════════════════
    //  GuiLoginRateLimiter
    // ══════════════════════════════════════════════════════════════

    @Nested
    class GuiLoginRateLimiterTest {

        @BeforeEach  void setup()   { GuiLoginRateLimiter.reset(); }
        @AfterEach   void teardown(){ GuiLoginRateLimiter.reset(); }

        @Test void notLockedInitially()  { assertFalse(GuiLoginRateLimiter.isLocked()); }
        @Test void attemptsStartAtZero() { assertEquals(0, GuiLoginRateLimiter.getAttempts()); }

        @Test void lockAfterMaxAttempts() {
            for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS - 1; i++)
                GuiLoginRateLimiter.recordFailure();
            assertFalse(GuiLoginRateLimiter.isLocked());
            GuiLoginRateLimiter.recordFailure(); // last attempt
            assertTrue(GuiLoginRateLimiter.isLocked());
        }

        @Test void remainingSecondsPositiveWhenLocked() {
            for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS; i++)
                GuiLoginRateLimiter.recordFailure();
            assertTrue(GuiLoginRateLimiter.remainingSeconds() > 0);
        }

        @Test void successResetsCounter() {
            GuiLoginRateLimiter.recordFailure();
            GuiLoginRateLimiter.recordSuccess();
            assertEquals(0, GuiLoginRateLimiter.getAttempts());
            assertFalse(GuiLoginRateLimiter.isLocked());
        }

        @Test void resetClearsAll() {
            GuiLoginRateLimiter.recordFailure();
            GuiLoginRateLimiter.reset();
            assertEquals(0, GuiLoginRateLimiter.getAttempts());
            assertFalse(GuiLoginRateLimiter.isLocked());
        }

        @Test void remainingSecondsZeroWhenNotLocked() {
            assertEquals(0, GuiLoginRateLimiter.remainingSeconds());
        }
    }
}
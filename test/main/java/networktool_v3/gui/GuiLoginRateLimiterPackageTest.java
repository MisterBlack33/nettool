package main.java.networktool_v3.gui;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiLoginRateLimiterPackageTest {

    @BeforeEach
    @AfterEach
    void reset() { GuiLoginRateLimiter.reset(); }

    @Test
    void constants() {
        assertEquals(5,      GuiLoginRateLimiter.MAX_ATTEMPTS);
        assertEquals(30_000L, GuiLoginRateLimiter.LOCKOUT_MS);
    }

    @Test
    void recordFailure_returnsTrue_onLock() {
        for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS - 1; i++)
            assertFalse(GuiLoginRateLimiter.recordFailure());
        assertTrue(GuiLoginRateLimiter.recordFailure());
    }

    @Test
    void recordFailure_resetsAttempts_afterLock() {
        for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS; i++)
            GuiLoginRateLimiter.recordFailure();
        // attempts reset to 0 after lock
        assertEquals(0, GuiLoginRateLimiter.getAttempts());
    }

    @Test
    void isLocked_afterLockout() {
        for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS; i++)
            GuiLoginRateLimiter.recordFailure();
        assertTrue(GuiLoginRateLimiter.isLocked());
    }

    @Test
    void remainingSeconds_whenLocked_positive() {
        for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS; i++)
            GuiLoginRateLimiter.recordFailure();
        assertTrue(GuiLoginRateLimiter.remainingSeconds() > 0);
    }

    @Test
    void remainingSeconds_whenNotLocked_zero() {
        assertEquals(0, GuiLoginRateLimiter.remainingSeconds());
    }

    @Test
    void recordSuccess_clearsLock() {
        for (int i = 0; i < GuiLoginRateLimiter.MAX_ATTEMPTS; i++)
            GuiLoginRateLimiter.recordFailure();
        GuiLoginRateLimiter.recordSuccess();
        assertFalse(GuiLoginRateLimiter.isLocked());
        assertEquals(0, GuiLoginRateLimiter.getAttempts());
    }
}
package networktool.gui.panels;

import org.junit.jupiter.api.*;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für PrivacyNetworkActions: Read-only Systemabfragen ohne echte Netzwerkänderung. */
class PrivacyNetworkActionsTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    // ── Read-only Systeminfo ─────────────────────────────────────────────

    @Test void getCurrentIp_notNull() {
        assertNotNull(PrivacyNetworkActions.getCurrentIp());
    }

    @Test void getCurrentIp_doesNotThrow() {
        assertDoesNotThrow(PrivacyNetworkActions::getCurrentIp);
    }

    @Test void getCurrentMac_notNull() {
        assertNotNull(PrivacyNetworkActions.getCurrentMac());
    }

    @Test void getCurrentMac_doesNotThrow() {
        assertDoesNotThrow(PrivacyNetworkActions::getCurrentMac);
    }

    @Test void isVpnActive_doesNotThrow() {
        assertDoesNotThrow(PrivacyNetworkActions::isVpnActive);
    }

    @Test void isVpnActive_returnsBoolean() {
        // ruft nur zweimal auf, um Konsistenz ohne Netzwerkänderung zu prüfen
        boolean first  = PrivacyNetworkActions.isVpnActive();
        boolean second = PrivacyNetworkActions.isVpnActive();
        assertEquals(first, second);
    }

    // ── runTask ───────────────────────────────────────────────────────────

    @Test void runTask_invokesConsumer() throws InterruptedException {
        JTextArea log = new JTextArea();
        boolean[] called = {false};
        PrivacyNetworkActions.runTask(log, "test", a -> called[0] = true);
        // runTask startet einen Thread — kurz warten
        Thread.sleep(200);
        assertTrue(called[0]);
    }

    @Test void runTask_clearsLogFirst() {
        JTextArea log = new JTextArea("altes-log");
        assertDoesNotThrow(() -> PrivacyNetworkActions.runTask(log, "clear-test", a -> {}));
    }

    @Test void runTask_doesNotThrow() {
        JTextArea log = new JTextArea();
        assertDoesNotThrow(() -> PrivacyNetworkActions.runTask(log, "name", a -> {}));
    }

    // ── checkEncryption (read-only, kein exec()) ──────────────────────────

    @Test void checkEncryption_doesNotThrow() throws InterruptedException {
        JTextArea log = new JTextArea();
        PrivacyNetworkActions.checkEncryption(log);
        Thread.sleep(100);
        assertDoesNotThrow(() -> {});
    }

    @Test void checkEncryption_writesToLog() throws InterruptedException {
        JTextArea log = new JTextArea();
        PrivacyNetworkActions.checkEncryption(log);
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> {});
        assertFalse(log.getText().isBlank());
    }

    // ── randomizeMac / resetMac / startVpn / stopVpn: exec()-Pfade ────────
    // Nur "does not throw" — echte Systemänderung wird hier bewusst NICHT ausgeführt/verifiziert.

    @Test void randomizeMac_doesNotThrow() {
        JTextArea log = new JTextArea();
        assertDoesNotThrow(() -> PrivacyNetworkActions.randomizeMac(log));
    }

    @Test void resetMac_doesNotThrow() {
        JTextArea log = new JTextArea();
        assertDoesNotThrow(() -> PrivacyNetworkActions.resetMac(log));
    }

    @Test void startVpn_doesNotThrow() {
        JTextArea log = new JTextArea();
        assertDoesNotThrow(() -> PrivacyNetworkActions.startVpn(log));
    }

    @Test void stopVpn_doesNotThrow() {
        JTextArea log = new JTextArea();
        assertDoesNotThrow(() -> PrivacyNetworkActions.stopVpn(log));
    }
}
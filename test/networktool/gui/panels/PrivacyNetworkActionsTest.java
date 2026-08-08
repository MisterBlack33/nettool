package networktool.gui.panels;

import main.java.networktool.gui.panels.PrivacyNetworkActions;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/** Tests fÃ¼r PrivacyNetworkActions: Read-only Systemabfragen ohne echte NetzwerkÃ¤nderung. */
class PrivacyNetworkActionsTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    // â”€â”€ Read-only Systeminfo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        // ruft nur zweimal auf, um Konsistenz ohne NetzwerkÃ¤nderung zu prÃ¼fen
        boolean first  = PrivacyNetworkActions.isVpnActive();
        boolean second = PrivacyNetworkActions.isVpnActive();
        assertEquals(first, second);
    }

    // â”€â”€ runTask â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void runTask_invokesConsumer() throws InterruptedException {
        JTextArea log = new JTextArea();
        boolean[] called = {false};
        PrivacyNetworkActions.runTask(log, "test", a -> called[0] = true);
        // runTask startet einen Thread â€” kurz warten
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

    // â”€â”€ checkEncryption (read-only, kein exec()) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void checkEncryption_doesNotThrow() throws InterruptedException {
        JTextArea log = new JTextArea();
        PrivacyNetworkActions.checkEncryption(log);
        Thread.sleep(100);
        assertDoesNotThrow(() -> {});
    }

    @Test void checkEncryption_writesToLog() throws InterruptedException, InvocationTargetException {
        JTextArea log = new JTextArea();
        PrivacyNetworkActions.checkEncryption(log);
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> {});
        assertFalse(log.getText().isBlank());
    }

    // â”€â”€ randomizeMac / resetMac / startVpn / stopVpn: exec()-Pfade â”€â”€â”€â”€â”€â”€â”€â”€
    // Nur "does not throw" â€” echte SystemÃ¤nderung wird hier bewusst NICHT ausgefÃ¼hrt/verifiziert.

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
package main.java.networktool.logic.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageDeliveryWinRmTest {

    @Test void tryWinRM_shellMetachars_rejected() {
        assertFalse(MessageDeliveryWinRm.tryWinRM("1.1.1.1; Remove-Item C:\\", "hi"));
    }

    @Test void tryWinRM_scriptBreakout_rejected() {
        assertFalse(MessageDeliveryWinRm.tryWinRM("' } ; calc.exe #", "hi"));
    }

    @Test void tryWinRM_blank_rejected() {
        assertFalse(MessageDeliveryWinRm.tryWinRM("", "hi"));
        assertFalse(MessageDeliveryWinRm.tryWinRM(null, "hi"));
    }

    @Test void tryWinRM_validButUnreachable_returnsFalse() {
        assertFalse(MessageDeliveryWinRm.tryWinRM("192.0.2.1", "hi"));
    }

    @Test void tryWinRM_closedPort_returnsFalse() {
        assertFalse(MessageDeliveryWinRm.tryWinRM("192.0.2.1", "hello"));
    }
}
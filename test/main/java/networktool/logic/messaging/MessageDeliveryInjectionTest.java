package main.java.networktool.logic.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Regressionstests: Command-/Script-Injection über Ziel-IP muss abgewiesen werden. */
class MessageDeliveryInjectionTest {

    @Test void tryWinRM_shellMetachars_rejected() {
        assertFalse(MessageDelivery.tryWinRM("1.1.1.1; Remove-Item C:\\", "hi"));
    }

    @Test void tryWinRM_scriptBreakout_rejected() {
        assertFalse(MessageDelivery.tryWinRM("' } ; calc.exe #", "hi"));
    }

    @Test void tryWinRM_blank_rejected() {
        assertFalse(MessageDelivery.tryWinRM("", "hi"));
        assertFalse(MessageDelivery.tryWinRM(null, "hi"));
    }

    @Test void trySsh_shellMetachars_rejected() {
        assertFalse(MessageDelivery.trySsh("1.1.1.1 && rm -rf /", "hi", false));
    }

    @Test void trySsh_backtickInjection_rejected() {
        assertFalse(MessageDelivery.trySsh("`id`", "hi", false));
    }

    @Test void trySsh_null_rejected() {
        assertFalse(MessageDelivery.trySsh(null, "hi", true));
    }

    @Test void tryWinRM_validButUnreachable_returnsFalse() {
        // gültige IP-Syntax, aber Port 5985 nicht offen → false, kein Exec-Versuch nötig
        assertFalse(MessageDelivery.tryWinRM("192.0.2.1", "hi"));
    }

    @Test void trySsh_validButUnreachable_returnsFalse() {
        assertFalse(MessageDelivery.trySsh("192.0.2.1", "hi", false));
    }

    @Test void tryListener_injectionAttemptIp_falseNotThrow() {
        assertFalse(MessageDelivery.tryListener("1.1.1.1'; calc", "hi"));
    }

    @Test void tryListener_null_falseNotThrow() {
        assertFalse(MessageDelivery.tryListener(null, "hi"));
    }

    @Test void tryNtfy_injectionAttemptTopic_doesNotThrow() {
        assertDoesNotThrow(() -> MessageDelivery.tryNtfy("__test__'; calc; '", "hi"));
    }
}
package main.java.networktool.logic.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageDeliverySshTest {

    @Test void trySsh_shellMetachars_rejected() {
        assertFalse(MessageDeliverySsh.trySsh("1.1.1.1 && rm -rf /", "hi", false));
    }

    @Test void trySsh_backtickInjection_rejected() {
        assertFalse(MessageDeliverySsh.trySsh("`id`", "hi", false));
    }

    @Test void trySsh_null_rejected() {
        assertFalse(MessageDeliverySsh.trySsh(null, "hi", true));
    }

    @Test void trySsh_validButUnreachable_returnsFalse() {
        assertFalse(MessageDeliverySsh.trySsh("192.0.2.1", "hi", false));
    }

    @Test void trySsh_closedPort_returnsFalse() {
        assertFalse(MessageDeliverySsh.trySsh("192.0.2.1", "hello", false));
    }
}
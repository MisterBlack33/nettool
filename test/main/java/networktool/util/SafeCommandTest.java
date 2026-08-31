package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeCommandTest {

    @Test void build_rawAndValidatedArgs_inOrder() {
        String[] cmd = SafeCommand.of("ip").raw("link", "set").iface("eth0").raw("up").build();
        assertArrayEquals(new String[]{"ip", "link", "set", "eth0", "up"}, cmd);
    }

    @Test void iface_invalid_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SafeCommand.of("ip").iface("eth0; rm -rf /"));
    }

    @Test void ip_valid_accepted() {
        String[] cmd = SafeCommand.of("ping").ip("192.168.1.1").build();
        assertArrayEquals(new String[]{"ping", "192.168.1.1"}, cmd);
    }

    @Test void ip_invalid_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SafeCommand.of("ping").ip("1.1.1.1'; calc"));
    }

    @Test void mac_valid_accepted() {
        String[] cmd = SafeCommand.of("ip").mac("AA:BB:CC:DD:EE:FF").build();
        assertArrayEquals(new String[]{"ip", "AA:BB:CC:DD:EE:FF"}, cmd);
    }

    @Test void mac_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> SafeCommand.of("ip").mac("not-a-mac"));
    }

    @Test void raw_multipleArgs_appended() {
        String[] cmd = SafeCommand.of("echo").raw("a", "b", "c").build();
        assertArrayEquals(new String[]{"echo", "a", "b", "c"}, cmd);
    }

    @Test void exec_invalidLoopback_doesNotThrowBeforeValidation() {
        // Validierung schlägt vor jedem Prozessstart fehl -> kein exec() nötig
        SafeCommand sc = SafeCommand.of("echo");
        assertThrows(IllegalArgumentException.class, () -> sc.iface("bad iface!"));
    }
}
package main.java.networktool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Baut exec()-Argumentlisten, bei denen Interface-/IP-/MAC-Werte zwingend
 * über {@link PlatformUtils#requireSafeInterface} etc. validiert werden,
 * bevor sie in einen Prozessaufruf gelangen.
 */
public final class SafeCommand {

    private final List<String> parts = new ArrayList<>();

    private SafeCommand() {}

    public static SafeCommand of(String executable) {
        SafeCommand c = new SafeCommand();
        c.parts.add(executable);
        return c;
    }

    /** Fügt Argumente ohne Validierung hinzu (z.B. feste Flags wie "-E", "up"). */
    public SafeCommand raw(String... args) {
        for (String a : args) parts.add(a);
        return this;
    }

    public SafeCommand iface(String value) {
        parts.add(PlatformUtils.requireSafeInterface(value));
        return this;
    }

    public SafeCommand ip(String value) {
        parts.add(PlatformUtils.requireSafeIp(value));
        return this;
    }

    public SafeCommand mac(String value) {
        parts.add(PlatformUtils.requireSafeMac(value));
        return this;
    }

    public String[] build() {
        return parts.toArray(new String[0]);
    }

    public Process exec() throws IOException {
        return Runtime.getRuntime().exec(build());
    }
}
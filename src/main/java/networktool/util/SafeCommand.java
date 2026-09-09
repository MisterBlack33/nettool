package main.java.networktool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Baut exec()-Argumentlisten, bei denen Interface-/IP-/MAC-Werte zwingend
 * über {@link PlatformSupport#requireSafeInterface} etc. validiert werden,
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
        if (args == null) return this;
        Collections.addAll(parts, args);
        return this;
    }

    public SafeCommand iface(String value) {
        parts.add(PlatformSupport.requireSafeInterface(value));
        return this;
    }

    public SafeCommand ip(String value) {
        parts.add(PlatformSupport.requireSafeIp(value));
        return this;
    }

    public SafeCommand mac(String value) {
        parts.add(PlatformSupport.requireSafeMac(value));
        return this;
    }

    public String[] build() {
        return parts.toArray(new String[0]);
    }

    public Process exec() throws IOException {
        return Runtime.getRuntime().exec(build());
    }
}

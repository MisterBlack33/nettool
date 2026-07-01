package main.java.networktool.logic.windows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Führt PowerShell-Skripte aus. Nur unter Windows aktiv, sonst leere Ergebnisse. */
public final class PowerShellRunner {

    private static final long DEFAULT_TIMEOUT_MS = 4000;

    private PowerShellRunner() {}

    public static boolean isAvailable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static List<String> run(String script) { return run(script, DEFAULT_TIMEOUT_MS); }

    public static List<String> run(String script, long timeoutMs) {
        if (!isAvailable()) return List.of();
        List<String> lines = new ArrayList<>();
        try {
            Process p = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                    .start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) lines.add(line);
            }
            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) { p.destroyForcibly(); return List.of(); }
        } catch (Exception ignored) { return List.of(); }
        return lines;
    }
}
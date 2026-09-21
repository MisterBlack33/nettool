package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Subnetz-Routen, die Tailscale-Subnet-Router ins Heimnetz bewerben. Über einen
 * L3-Tunnel taucht das Heimnetz in keiner lokalen Interface-Adresse auf; ohne diese
 * Quelle kennt der Scan nur das (Mobilfunk-/Hotspot-)Netz und die Tailscale-/32-Adresse.
 * Leer, wenn Tailscale nicht installiert oder nicht erreichbar ist.
 */
final class TailscaleRouteSource {

    private static final String[] STATUS_COMMAND = {"tailscale", "status", "--json"};
    private static final long TIMEOUT_SEC = 3;

    private final Supplier<String> statusJson;

    TailscaleRouteSource(Supplier<String> statusJson) {
        this.statusJson = statusJson;
    }

    static List<String> read(int minPrefix, int maxPrefix) {
        return new TailscaleRouteSource(() -> runCommand(STATUS_COMMAND)).routes(minPrefix, maxPrefix);
    }

    List<String> routes(int minPrefix, int maxPrefix) {
        return TailscaleRouteParser.parse(statusJson.get()).stream()
                .filter(cidr -> isWithin(cidr, minPrefix, maxPrefix))
                .toList();
    }

    static String runCommand(String[] command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            return readWithTimeout(process);
        } catch (IOException e) {
            DebugLogger.getInstance().log("FINE", "[TailscaleRouteSource] " + command[0] + ": " + e);
            return "";
        }
    }

    private static String readWithTimeout(Process process) {
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process));
        try {
            return output.get(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException e) {
            DebugLogger.getInstance().log("FINE", "[TailscaleRouteSource] Abfrage fehlgeschlagen: " + e);
            return "";
        } finally {
            process.destroyForcibly();
        }
    }

    private static String readAll(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isWithin(String cidr, int minPrefix, int maxPrefix) {
        int prefix = Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
        return prefix >= minPrefix && prefix <= maxPrefix;
    }
}
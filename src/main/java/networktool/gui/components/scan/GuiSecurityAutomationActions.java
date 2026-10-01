package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.core.GuiStatusReporter;
import main.java.networktool.logic.analysis.security.RogueDhcpDetector;
import main.java.networktool.logic.analysis.security.ScanSecurityHook;
import main.java.networktool.logic.analysis.security.TlsCertScheduler;
import main.java.networktool.logic.scan.remote.RemoteNetScanner;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.PlatformSupport;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Test-Suite-Toggles der Security-Automatisierung (Menü-IDs "35"–"37"). */
public final class GuiSecurityAutomationActions {

    private static final int DEFAULT_DHCP_INTERVAL_SEC = 60;
    private static final int DEFAULT_TLS_INTERVAL_MIN  = 60;
    private static final int SECONDS_PER_MINUTE        = 60;

    private GuiSecurityAutomationActions() {}

    public static void toggleRogueDhcp(GuiInputPanel input, GuiOutputPanel output) {
        RogueDhcpDetector detector = RogueDhcpDetector.getInstance();
        if (detector.isActive()) {
            if (detector.stop()) {
                GuiStatusReporter.ok(output, "DHCP-Watch gestoppt");
            } else {
                GuiStatusReporter.warning(output, "DHCP-Watch konnte nicht gestoppt werden");
            }
            return;
        }
        input.ask("Intervall in Sekunden (Standard " + DEFAULT_DHCP_INTERVAL_SEC + "):", sec ->
                input.ask("Vertrauenswürdige DHCP-Server, kommagetrennt (leer = Standard-Gateway):", raw ->
                        startRogueDhcp(output, parseOr(sec, DEFAULT_DHCP_INTERVAL_SEC),
                                resolveTrusted(raw, RemoteNetScanner::detectDefaultGateway))));
    }

    static boolean startRogueDhcp(GuiOutputPanel output, int sec, Set<String> trusted) {
        if (!RogueDhcpDetector.getInstance().start(sec, trusted)) {
            GuiStatusReporter.error(output, "Kein vertrauter DHCP-Server angegeben/erkannt");
            return false;
        }
        GuiStatusReporter.ok(output, "DHCP-Watch aktiv (" + sec + " s)");
        return true;
    }

    /** Ungültige IPs werden verworfen; leere Eingabe fällt auf das Standard-Gateway zurück. */
    static Set<String> resolveTrusted(String raw, Supplier<String> gateway) {
        Set<String> parsed = raw == null ? Set.of() : Arrays.stream(raw.split(","))
                .map(String::trim).filter(PlatformSupport::isSafeIp).collect(Collectors.toSet());
        if (!parsed.isEmpty()) return parsed;
        String gw = gateway.get();
        return PlatformSupport.isSafeIp(gw) ? Set.of(gw) : Set.of();
    }

    public static void toggleScanHook(GuiOutputPanel output) {
        ScanSecurityHook hook = ScanSecurityHook.getInstance();
        hook.setEnabled(!hook.isEnabled());
        AuditLogger.getInstance().log("CVE_HOOK", hook.isEnabled() ? "on" : "off");
        if (hook.isEnabled()) {
            GuiStatusReporter.ok(output, "CVE-Hook aktiv");
        } else {
            GuiStatusReporter.warning(output, "CVE-Hook aus");
        }
    }

    public static void toggleTlsScheduler(GuiInputPanel input, GuiOutputPanel output) {
        TlsCertScheduler scheduler = TlsCertScheduler.getInstance();
        if (scheduler.isActive()) {
            if (scheduler.stop()) {
                GuiStatusReporter.ok(output, "TLS-Scheduler gestoppt");
            } else {
                GuiStatusReporter.warning(output, "TLS-Scheduler konnte nicht gestoppt werden");
            }
            return;
        }
        input.ask("Intervall in Minuten (Standard " + DEFAULT_TLS_INTERVAL_MIN + "):",
                raw -> startTlsScheduler(output, raw));
    }

    static boolean startTlsScheduler(GuiOutputPanel output, String raw) {
        int min = parseOr(raw, DEFAULT_TLS_INTERVAL_MIN);
        boolean started = TlsCertScheduler.getInstance().start(min * SECONDS_PER_MINUTE);
        if (started) {
            GuiStatusReporter.ok(output, "TLS-Scheduler aktiv (" + min + " min)");
        } else {
            GuiStatusReporter.warning(output, "TLS-Scheduler konnte nicht gestartet werden");
        }
        return started;
    }

    static int parseOr(String raw, int fallback) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

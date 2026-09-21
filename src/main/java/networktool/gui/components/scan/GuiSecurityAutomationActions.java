package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.security.RogueDhcpDetector;
import main.java.networktool.logic.analysis.security.ScanSecurityHook;
import main.java.networktool.logic.analysis.security.TlsCertScheduler;
import main.java.networktool.logic.scan.remote.RemoteNetScanner;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.PlatformSupport;
import main.java.networktool.util.StatusTags;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Toggles der Security-Automatisierung (Menü-IDs "35"–"37"). */
public final class GuiSecurityAutomationActions {

    private static final int DEFAULT_DHCP_INTERVAL_SEC = 60;
    private static final int DEFAULT_TLS_INTERVAL_MIN  = 60;
    private static final int SECONDS_PER_MINUTE        = 60;

    private GuiSecurityAutomationActions() {}

    public static void toggleRogueDhcp(GuiInputPanel input, GuiOutputPanel output) {
        RogueDhcpDetector detector = RogueDhcpDetector.getInstance();
        if (detector.isActive()) {
            detector.stop();
            output.appendText("  " + StatusTags.OK + " DHCP-Watch gestoppt\n", WARN);
            return;
        }
        input.ask("Intervall in Sekunden (Standard " + DEFAULT_DHCP_INTERVAL_SEC + "):", sec ->
                input.ask("Vertrauenswürdige DHCP-Server, kommagetrennt (leer = Standard-Gateway):", raw ->
                        startRogueDhcp(output, parseOr(sec, DEFAULT_DHCP_INTERVAL_SEC),
                                resolveTrusted(raw, RemoteNetScanner::detectDefaultGateway))));
    }

    static boolean startRogueDhcp(GuiOutputPanel output, int sec, Set<String> trusted) {
        if (!RogueDhcpDetector.getInstance().start(sec, trusted)) {
            output.appendText("  " + StatusTags.FEHLER + " Kein vertrauter DHCP-Server angegeben/erkannt\n", WARN);
            return false;
        }
        output.appendText("  " + StatusTags.OK + " DHCP-Watch aktiv (" + sec + " s)\n", ACCENT2);
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
        output.appendText("  " + StatusTags.OK + " CVE-Hook "
                + (hook.isEnabled() ? "aktiv" : "aus") + "\n", hook.isEnabled() ? ACCENT2 : WARN);
    }

    public static void toggleTlsScheduler(GuiInputPanel input, GuiOutputPanel output) {
        TlsCertScheduler scheduler = TlsCertScheduler.getInstance();
        if (scheduler.isActive()) {
            scheduler.stop();
            output.appendText("  " + StatusTags.OK + " TLS-Scheduler gestoppt\n", WARN);
            return;
        }
        input.ask("Intervall in Minuten (Standard " + DEFAULT_TLS_INTERVAL_MIN + "):",
                raw -> startTlsScheduler(output, raw));
    }

    static void startTlsScheduler(GuiOutputPanel output, String raw) {
        int min = parseOr(raw, DEFAULT_TLS_INTERVAL_MIN);
        TlsCertScheduler.getInstance().start(min * SECONDS_PER_MINUTE);
        output.appendText("  " + StatusTags.OK + " TLS-Scheduler aktiv (" + min + " min)\n", ACCENT2);
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

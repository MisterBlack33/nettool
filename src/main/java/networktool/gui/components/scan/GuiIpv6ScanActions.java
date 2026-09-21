package main.java.networktool.gui.components.scan;

import main.java.networktool.filter.TablePrinter;
import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.host.NetworkScannerV6;
import main.java.networktool.model.ScanResult;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.Ipv6AddressUtils;
import main.java.networktool.util.StatusTags;

import java.util.List;
import java.util.function.Function;

import static main.java.networktool.theme.GuiTheme.WARN;

/** Test-Suite-Aktion "IPv6-Scan" (Menü-ID "33"). */
public final class GuiIpv6ScanActions {

    private GuiIpv6ScanActions() {}

    public static void handleScan(GuiInputPanel input, GuiOutputPanel output, GuiMenuHandler handler) {
        input.ask("IPv6-CIDR (z.B. 2001:db8::/64):", raw -> {
            String cidr = raw.trim();
            AuditLogger.getInstance().log("SCAN_IPV6", cidr);
            handler.runAsync(() -> scanAndReport(cidr, NetworkScannerV6.withDefaults()::scanCidr, output));
        });
    }

    /** @return true, wenn das CIDR gültig war und der Scan lief. */
    static boolean scanAndReport(String cidr, Function<String, List<ScanResult>> scanner,
                                 GuiOutputPanel output) {
        if (!Ipv6AddressUtils.isValidCidr(cidr)) {
            output.appendText("  " + StatusTags.FEHLER + " Ungültiges IPv6-CIDR\n", WARN);
            return false;
        }
        TablePrinter.print(scanner.apply(cidr));
        return true;
    }
}

package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.logic.scan.RemoteNetScanner;
import main.java.networktool.security.AuditLogger;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static networktool.theme.GuiTheme.*;

/**
 * Dialog-Kaskade für den Fremdnetz-Scanner (Menüpunkt "11").
 */
public final class GuiForeignNetActions {

    private GuiForeignNetActions() {}

    public static void handleRemoteNetScan(GuiInputPanel input, GuiOutputPanel output,
                                            GuiStatusBar status, GuiMenuHandler handler) {
        String gw = RemoteNetScanner.detectDefaultGateway();
        status.set("Gateway: " + (gw != null ? gw : "–"), FG_DIM);
        String[] options = {"Einzelnes Netz", "Mehrere Netze", "Erreichbarkeitstest", "Routing-Hilfe"};
        int choice = JOptionPane.showOptionDialog(null, "Fremdnetz-Modus:", "Fremdnetz-Scanner",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        switch (choice) {
            case 0 -> input.ask("CIDR:", raw -> handler.runAsync(() -> {
                AuditLogger.getInstance().log("REMOTE_SCAN", raw);
                RemoteNetScanner.scanCidr(raw.trim());
            }));
            case 1 -> input.ask("Netze (kommagetrennt):", nets -> {
                List<String> list = Arrays.stream(nets.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (list.isEmpty()) return;
                AuditLogger.getInstance().log("REMOTE_MULTI_SCAN", String.join(",", list));
                handler.runAsync(() -> RemoteNetScanner.scanMultiple(list));
            });
            case 2 -> input.ask("CIDR:", raw -> handler.runAsync(() -> {
                String cidr = RemoteNetScanner.normalizeCidr(raw.trim());
                RemoteNetScanner.ReachResult r = RemoteNetScanner.parallelProbe(cidr);
                output.appendText(r.reachable
                                ? "  ✔ " + cidr + " erreichbar (~" + r.avgMs + " ms)\n"
                                : "  ✕ " + cidr + " nicht erreichbar\n",
                        r.reachable ? ACCENT2 : WARN);
            }));
            case 3 -> input.ask("CIDR:", raw ->
                    handler.runAsync(() -> RemoteNetScanner.printRoutingHints(raw.trim())));
        }
    }
}

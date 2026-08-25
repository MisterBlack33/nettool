package main.java.networktool.gui.panels.privacy;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.theme.GuiTheme;
import static main.java.networktool.theme.GuiTheme.*;

/**
 * Privatsphäre & Tarnung Panel (Menü-ID "30").
 * System-Aktionen siehe {@link PrivacyNetworkActions}, Styling siehe {@link PrivacyPanelStyle}.
 */
public final class GuiPrivacyPanel {

    private GuiPrivacyPanel() {}

    public static void show(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embedPanel(output));
    }

    private static void embedPanel(GuiOutputPanel output) {
        output.appendText("\n🔒 Privatsphäre & Tarnung\n\n", ACCENT);

        Color bg    = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF4, 0xF2, 0xEE);
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xE8, 0xE6, 0xE0);

        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(bg);
        outer.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel statusSection = PrivacyPanelStyle.buildSection("Status", panBg);
        JLabel macLabel = PrivacyPanelStyle.statusLabel("MAC: ...", FG_DIM);
        JLabel ipLabel  = PrivacyPanelStyle.statusLabel("IP:  ...", FG_DIM);
        JLabel vpnLabel = PrivacyPanelStyle.statusLabel("VPN: prüfe...", FG_DIM);
        statusSection.add(macLabel);
        statusSection.add(ipLabel);
        statusSection.add(vpnLabel);
        outer.add(statusSection, BorderLayout.NORTH);

        JPanel actSection = PrivacyPanelStyle.buildSection("Aktionen", panBg);
        JButton macRandBtn  = PrivacyPanelStyle.actionBtn("🎲 MAC randomisieren",    ACCENT);
        JButton macResetBtn = PrivacyPanelStyle.actionBtn("↩ MAC zurücksetzen",      FG_DIM);
        JButton vpnStartBtn = PrivacyPanelStyle.actionBtn("▶ WireGuard starten",     ACCENT2);
        JButton vpnStopBtn  = PrivacyPanelStyle.actionBtn("■ WireGuard stoppen",     WARN);
        JButton checkEncBtn = PrivacyPanelStyle.actionBtn("🔍 Verschlüsselung prüfen", INFO);
        JTextArea logArea   = PrivacyPanelStyle.buildLogArea(bg);

        actSection.add(PrivacyPanelStyle.wrapRow(macRandBtn, macResetBtn));
        actSection.add(PrivacyPanelStyle.wrapRow(vpnStartBtn, vpnStopBtn));
        actSection.add(checkEncBtn);
        actSection.add(new JScrollPane(logArea) {{
            setBorder(new LineBorder(BORDER, 1));
            setPreferredSize(new Dimension(0, 120));
            getViewport().setBackground(bg);
        }});

        JLabel info = new JLabel(
                "<html><small>⚠ MAC-Randomisierung und VPN-Kontrolle können Root-Rechte benötigen.</small></html>");
        info.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        info.setForeground(FG_DIM);
        info.setBorder(new EmptyBorder(4, 8, 4, 8));
        actSection.add(info);
        outer.add(actSection, BorderLayout.CENTER);

        macRandBtn.addActionListener(e ->
                PrivacyNetworkActions.runTask(logArea, "MAC randomisieren", PrivacyNetworkActions::randomizeMac));
        macResetBtn.addActionListener(e ->
                PrivacyNetworkActions.runTask(logArea, "MAC zurücksetzen",  PrivacyNetworkActions::resetMac));
        vpnStartBtn.addActionListener(e ->
                PrivacyNetworkActions.runTask(logArea, "WireGuard starten", PrivacyNetworkActions::startVpn));
        vpnStopBtn.addActionListener(e ->
                PrivacyNetworkActions.runTask(logArea, "WireGuard stoppen", PrivacyNetworkActions::stopVpn));
        checkEncBtn.addActionListener(e ->
                PrivacyNetworkActions.runTask(logArea, "Verschlüsselung prüfen", PrivacyNetworkActions::checkEncryption));

        new Thread(() -> {
            String mac = PrivacyNetworkActions.getCurrentMac();
            String ip  = PrivacyNetworkActions.getCurrentIp();
            String vpn = PrivacyNetworkActions.isVpnActive() ? "✔ aktiv" : "✕ inaktiv";
            SwingUtilities.invokeLater(() -> {
                macLabel.setText("MAC: " + mac); macLabel.setForeground(FG);
                ipLabel.setText("IP:  " + ip);   ipLabel.setForeground(FG);
                vpnLabel.setText("VPN: " + vpn);
                vpnLabel.setForeground(PrivacyNetworkActions.isVpnActive() ? ACCENT2 : FG_DIM);
            });
        }).start();

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }
}

package networktool.gui.hostdetails;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static networktool.theme.GuiTheme.*;
import static networktool.gui.hostdetails.HostDetailRows.detailButton;

/**
 * VollstÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤ndiges Host-Details-Fenster.
 * Tabs: ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â  Info  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â¡ Ping  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â¢ Ports  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â£ Notiz (siehe HostXTab-Klassen).
 * ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ffnen: GuiContextMenu ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ "ÃƒÆ’Ã‚Â°Ãƒâ€¦Ã‚Â¸ÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂÃƒâ€šÃ‚Â Details".
 */
public final class HostDetailsPanel {

    private HostDetailsPanel() {}

    public static void show(String ip, String hostname, String os, String category) {
        SwingUtilities.invokeLater(() -> openWindow(ip, hostname, os, category));
    }

    private static void openWindow(String ip, String hostname, String os, String category) {
        JDialog dlg = new JDialog((Frame) null, "Host Details  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ  " + ip, false);
        dlg.setSize(680, 700);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg    = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF8, 0xF6, 0xF2);
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xEE, 0xEC, 0xE6);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(bg);

        HostPingTab pingTab   = new HostPingTab(ip, panBg);
        HostPortsTab portsTab = new HostPortsTab(ip, panBg);
        buildHeader(root, ip, category, pingTab, portsTab);

        JTabbedPane tabs = buildTabs(ip, hostname, os, category, pingTab, portsTab);
        root.add(tabs, BorderLayout.CENTER);

        pingTab.start();
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private static void buildHeader(JPanel root, String ip, String category,
                                    HostPingTab pingTab, HostPortsTab portsTab) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC));
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        JLabel ipLbl = new JLabel(ip);
        ipLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 18));
        ipLbl.setForeground(ACCENT);

        JLabel catLbl = new JLabel(category != null ? "  [" + category + "]" : "");
        catLbl.setFont(MONO_S);
        catLbl.setForeground(FG_DIM);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(ipLbl);
        left.add(catLbl);
        header.add(left, BorderLayout.WEST);

        JButton refreshBtn = detailButton("ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â Ãƒâ€šÃ‚Â» Refresh", ACCENT);
        refreshBtn.addActionListener(e -> {
            pingTab.restart();
            portsTab.refresh();
        });
        header.add(refreshBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
    }

    private static JTabbedPane buildTabs(String ip, String hostname, String os, String category,
                                         HostPingTab pingTab, HostPortsTab portsTab) {
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xEE, 0xEC, 0xE6);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF8, 0xF6, 0xF2));
        tabs.setForeground(FG);
        tabs.setFont(MONO_S);

        tabs.addTab("  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â  Info  ",  HostInfoTab.build(ip, hostname, os, panBg));
        tabs.addTab("  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â¡ Ping  ",  pingTab);
        tabs.addTab("  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â¢ Ports  ", portsTab);
        tabs.addTab("  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â£ Notiz  ", HostNotesTab.build(ip, category, panBg));
        return tabs;
    }
}

package networktool.gui.components;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import networktool.theme.GuiTheme;
import static networktool.theme.GuiTheme.*;

/**
 * Toolbar, Status-Zeile und Layered-Pane-Aufbau der Netzwerk-Karte.
 */
final class GuiNetworkMapChrome {

    private GuiNetworkMapChrome() {}

    static JLayeredPane buildLayered(MapCanvas canvas, Color bg) {
        JLayeredPane pane = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                Component legend = getComponentsInLayer(JLayeredPane.PALETTE_LAYER)[0];
                Dimension ls = legend.getPreferredSize();
                legend.setBounds(10, h - ls.height - 10, ls.width, ls.height);
            }
        };
        pane.setBackground(bg);
        pane.setOpaque(true);
        pane.add(canvas, JLayeredPane.DEFAULT_LAYER);
        pane.add(MapLegend.build(), JLayeredPane.PALETTE_LAYER);
        return pane;
    }

    static JPanel buildToolbar(MapCanvas canvas, Map<String, String> hopParent, Runnable onRefresh) {
        Color barBg = GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel titleLabel = new JLabel("  Netzwerk-Karte");
        titleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        titleLabel.setForeground(ACCENT);
        canvas.setTitleLabel(titleLabel);

        JTextField switchInput = buildSwitchInput();

        JButton addBtn     = toolBtn("+S",         new Color(0xFF, 0xA0, 0x30));
        JButton refreshBtn = toolBtn("↻",           ACCENT2);
        JButton layoutBtn  = toolBtn("⊞",           INFO);
        JButton probeBtn   = toolBtn("DNS/DHCP",    new Color(0xA0, 0xD8, 0xFF));

        addBtn.setToolTipText("Switch-IP manuell hinzufügen");
        probeBtn.setToolTipText("DNS/DHCP/mDNS-Rollen erneut erkennen");
        refreshBtn.setToolTipText("Karte + Hop-Discovery neu laden");
        layoutBtn.setToolTipText("Layout zurücksetzen");

        addBtn.addActionListener(e -> {
            String ip = switchInput.getText().trim();
            if (!ip.isBlank()) {
                MapSwitchStore.add(ip);
                switchInput.setText("");
                canvas.reload();
            }
        });
        switchInput.addActionListener(e -> addBtn.doClick());

        refreshBtn.addActionListener(e -> {
            hopParent.clear();
            canvas.reload();
            onRefresh.run();
        });
        layoutBtn.addActionListener(e -> canvas.resetLayout());
        probeBtn.addActionListener(e -> canvas.runTrafficProbing());

        bar.add(titleLabel);
        bar.add(switchInput);
        bar.add(addBtn);
        bar.add(refreshBtn);
        bar.add(layoutBtn);
        bar.add(probeBtn);
        return bar;
    }

    static JLabel buildStatusLabel() {
        JLabel lbl = new JLabel("  Lade Karte…");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static JTextField buildSwitchInput() {
        JTextField f = new JTextField(16);
        f.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        f.setForeground(FG);
        f.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x10) : Color.WHITE);
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFF, 0xA0, 0x30), 1),
                new EmptyBorder(3, 6, 3, 6)));
        f.putClientProperty("JTextField.placeholderText", "Switch-IP eingeben…");
        return f;
    }

    private static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 8, 4, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(GuiTheme.isDark() ? new Color(0x24, 0x30, 0x24) : new Color(0xCC, 0xCA, 0xC4));
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
            }
        });
        return b;
    }
}

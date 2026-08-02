package networktool.gui.hostdetails;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import networktool.logic.ports.PortScanner;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static networktool.theme.GuiTheme.*;
import static networktool.gui.hostdetails.HostDetailRows.detailButton;

/** Tab ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“Ãƒâ€šÃ‚Â¢: Offene Ports ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Scan + Banner-Grabbing on-demand. */
final class HostPortsTab extends JPanel {

    private final String ip;
    private final JPanel listPanel;
    private final JLabel statusLbl;

    HostPortsTab(String ip, Color panBg) {
        this.ip = ip;
        setLayout(new BorderLayout(0, 8));
        setBackground(panBg);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        statusLbl = new JLabel("  Noch nicht gescannt  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢  Klicke 'Scan starten'");
        statusLbl.setFont(MONO_S);
        statusLbl.setForeground(FG_DIM);

        JButton scanBtn = detailButton("ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ Scan starten", ACCENT2);
        scanBtn.addActionListener(e -> refresh());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBackground(panBg);
        top.add(statusLbl, BorderLayout.CENTER);
        top.add(scanBtn,   BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(panBg);
        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(panBg);
        add(sp, BorderLayout.CENTER);
    }

    void refresh() {
        statusLbl.setText("  Scanne PortsÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦");
        statusLbl.setForeground(ACCENT);
        listPanel.removeAll();
        listPanel.revalidate();
        listPanel.repaint();

        new Thread(() -> {
            try {
                Map<Integer, String> ports = PortScanner.scanParallel(ip, 1500);
                SwingUtilities.invokeLater(() -> showResults(ports));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "HostPortsTab-Scan").start();
    }

    private void showResults(Map<Integer, String> ports) {
        listPanel.removeAll();
        if (ports.isEmpty()) {
            JLabel none = new JLabel("  Keine offenen Ports gefunden.");
            none.setFont(MONO_S);
            none.setForeground(FG_DIM);
            listPanel.add(none);
        } else {
            ports.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> listPanel.add(portRow(e.getKey(), e.getValue())));
        }
        statusLbl.setText("  " + ports.size() + " Port(s) offen  ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ  "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        statusLbl.setForeground(ACCENT2);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel portRow(int port, String banner) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(getBackground());
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(5, 8, 5, 8)));

        JLabel portLbl = new JLabel(String.format("%-6d", port));
        portLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        portLbl.setForeground(ACCENT);

        JLabel bannerLbl = new JLabel(banner != null ? banner : "offen");
        bannerLbl.setFont(MONO_S);
        bannerLbl.setForeground(FG);

        row.add(portLbl,   BorderLayout.WEST);
        row.add(bannerLbl, BorderLayout.CENTER);
        return row;
    }
}


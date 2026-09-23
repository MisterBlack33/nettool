package main.java.networktool.gui.components;

import main.java.networktool.gui.components.map.GuiNetworkMap;
import main.java.networktool.gui.core.GuiDebugMode;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Power-Zeile der Sidebar: Status-Indikator (RUN/IDLE) und Power-Menü
 * (Abbrechen, Neustart, Abmelden, Beenden).
 */
final class SidebarPowerMenu {

    private SidebarPowerMenu() {}

    static JPanel build(Runnable onCancel, Runnable onRestart, BooleanSupplier isRunning) {
        return build(onCancel, onRestart, () -> {}, isRunning);
    }

    static JPanel build(Runnable onCancel, Runnable onRestart, Runnable onTheme, BooleanSupplier isRunning) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(SIDEBAR_BG);
        row.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(6, 8, 6, 8)));

        // Indikator: GUI-Scan ODER NetworkMap-Hintergrund-Scan
        BooleanSupplier anyRunning = () -> isRunning.getAsBoolean() || GuiNetworkMap.isScanRunning();

        StatusDot dot = new StatusDot(anyRunning);
        dot.setPreferredSize(new Dimension(50, 34));
        dot.start();

        JButton pb = new JButton("POWER");
        pb.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        pb.setForeground(WARN);
        pb.setBackground(BTN_BG);
        pb.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 9, 4, 9)));
        pb.setFocusPainted(false);
        pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pb.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                pb.setBackground(BTN_HOV);
                pb.setBorder(new CompoundBorder(new LineBorder(WARN, 1), new EmptyBorder(4, 9, 4, 9)));
            }
            public void mouseExited(MouseEvent e) {
                pb.setBackground(BTN_BG);
                pb.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 9, 4, 9)));
            }
        });
        pb.addActionListener(e -> showPowerMenu(pb, onCancel, onRestart, anyRunning));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(SIDEBAR_BG);
        left.add(pb); left.add(dot);
        row.add(left, BorderLayout.WEST);
        return row;
    }

    private static void showPowerMenu(JButton anchor, Runnable onCancel,
                                      Runnable onRestart, BooleanSupplier isRunning) {
        JPopupMenu m = new JPopupMenu();
        m.setOpaque(true);
        m.setBackground(PANEL_BG);
        m.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, PANEL_BG),
                new MatteBorder(4, 0, 4, 0, PANEL_BG)));
        m.add(pItem("Abbrechen  Ctrl+A",     WARN,                        onCancel));
        m.add(pSeparator());
        m.add(pItem("Neustart   Ctrl+R",     new Color(0xFF, 0xD0, 0x50), onRestart));
        m.add(pSeparator());
        m.add(pItem("Abmelden", new Color(0x80, 0xC8, 0xFF), () -> {
            AuditLogger.getInstance().log("LOGOUT", UserAuth.getInstance().getCurrentUser());
            GuiDebugMode.disable();
            UserAuth.getInstance().logout();
            onRestart.run();
        }));
        m.add(pSeparator());
        m.add(pItem("Beenden    Ctrl+Q", new Color(0xFF, 0x40, 0x40), () -> confirmQuit(isRunning)));
        m.pack();
        m.show(anchor, 0, -(m.getPreferredSize().height + 2));
    }

    private static void confirmQuit(BooleanSupplier isRunning) {
        if (isRunning.getAsBoolean()) {
            int r = JOptionPane.showConfirmDialog(null,
                    "<html><b>Scan läuft noch.</b><br>Trotzdem beenden?</html>",
                    "Beenden", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        AuditLogger.getInstance().log("APP_EXIT", UserAuth.getInstance().getCurrentUser());
        System.exit(0);
    }

    private static JMenuItem pItem(String text, Color fg, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        item.setForeground(fg); item.setBackground(PANEL_BG);
        item.setBorder(new EmptyBorder(6, 14, 6, 20)); item.setOpaque(true);
        item.addActionListener(e -> action.run());
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(BTN_HOV); }
            public void mouseExited (MouseEvent e) { item.setBackground(PANEL_BG); }
        });
        return item;
    }

    private static JSeparator pSeparator() {
        JSeparator separator = new JSeparator();
        separator.setOpaque(true);
        separator.setBackground(PANEL_BG);
        separator.setForeground(BORDER);
        separator.setBorder(BorderFactory.createEmptyBorder());
        separator.setPreferredSize(new Dimension(1, 1));
        separator.setMinimumSize(new Dimension(1, 1));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separator;
    }

    // ── Status-Indikator ──────────────────────────────────────────────────

    private static class StatusDot extends JComponent {
        private final BooleanSupplier isRunning;
        private float   alpha  = 0.4f;
        private boolean rising = false;
        private Timer   timer;

        StatusDot(BooleanSupplier s) { this.isRunning = s; setOpaque(false); }

        void start() {
            timer = new Timer(80, e -> {
                if (isRunning.getAsBoolean()) {
                    alpha += rising ? 0.07f : -0.07f;
                    if (alpha >= 1f)   { alpha = 1f;   rising = false; }
                    if (alpha <= 0.2f) { alpha = 0.2f; rising = true; }
                } else { alpha = 0.4f; }
                repaint();
            });
            timer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean run = isRunning.getAsBoolean();
            Color base  = run ? ACCENT2 : FG_DIM;
            int cx = getWidth() / 2, cy = 8, r = 5;
            if (run) {
                g2.setColor(new Color(ACCENT2.getRed(), ACCENT2.getGreen(),
                        ACCENT2.getBlue(), (int)(alpha * 60)));
                g2.fillOval(cx-r-4, cy-r-4, (r+4)*2, (r+4)*2);
            }
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    (int)(alpha * 255)));
            g2.fillOval(cx-r, cy-r, r*2, r*2);
            String lbl = run ? "RUN" : "IDLE";
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, 8));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(run ? ACCENT2 : FG_DIM);
            g2.drawString(lbl, (getWidth()-fm.stringWidth(lbl))/2, getHeight()-3);
            g2.dispose();
        }
    }
}

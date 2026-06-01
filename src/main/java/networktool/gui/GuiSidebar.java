package main.java.networktool.gui;

import main.java.networktool.security.UserAuth;
import main.java.networktool.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Linke Seitenleiste als aufklappbares Accordion-Menü.
 *
 * Nur eine Gruppe ist gleichzeitig geöffnet.
 * Ein Klick auf einen Gruppen-Header öffnet diese Gruppe
 * und schließt alle anderen.
 *
 * Einträge: {id, label, groupKey}
 *  id == null → Gruppen-Header
 *  adminOnly == true → nur für Admins sichtbar
 */
public final class GuiSidebar {

    private static final int W = 200;

    /** {id, label, icon, adminOnly} – id==null → Gruppenheader */
    private static final String[][] ITEMS = {
            // HOSTS
            {null,  "HOSTS",         "★", "false"},
            {"09",  "Gespeicherte",  null, "false"},
            // LOKALES NETZ
            {null,  "LOKALES NETZ",  "◎", "false"},
            {"01",  "Übersicht",     null, "false"},
            {"02",  "Interfaces",    null, "false"},
            {"10",  "Hops & Routen", null, "false"},
            // SCAN
            {null,  "SCAN",          "⊕", "false"},
            {"06",  "CIDR-Scan",     null, "false"},
            {"07",  "Filter-Scan",   null, "false"},
            {"12",  "Profile",       null, "false"},
            {"14",  "Scheduler",     null, "false"},
            {"13",  "Scan-Δ",        null, "false"},
            // FREMDNETZ (admin only)
            {null,  "FREMDNETZ",     "✦", "true"},
            {"11",  "Scanner",       null, "true"},
            // DIAGNOSE
            {null,  "DIAGNOSE",      "✚", "false"},
            {"03",  "IP-Analyse",    null, "false"},
            {"16",  "Dauerping",     null, "false"},
            {"15",  "Bandwidth",     null, "false"},
            // TRANSFER
            {null,  "TRANSFER",      "⇄", "false"},
            {"04",  "File-Server",   null, "false"},
            {"05",  "Datei senden",  null, "false"},
            // NACHRICHTEN
            {null,  "NACHRICHTEN",   "✉", "false"},
            {"08",  "Senden",        null, "false"},
            {"19",  "Verlauf",       null, "false"},
            // SICHERHEIT
            {null,  "SICHERHEIT",    "⚑", "false"},
            {"17",  "Monitor",       null, "false"},
            {"23",  "Audit-Log",     null, "true"},  // admin only
            // DATEN
            {null,  "DATEN",         "📦", "false"},
            {"18",  "Export/Import", null, "false"},
            // ANSICHT
            {null,  "ANSICHT",       "🗺", "false"},
            {"20",  "Netzwerk-Karte",null, "false"},
            // KONFIGURATION
            {null,  "KONFIGURATION", "⚙", "false"},
            {"21",  "Port-Liste",    null, "false"},
            {"22",  "Scan-Verlauf",  null, "false"},
            // PRIVATSPHÄRE
            {null,  "PRIVATSPHÄRE",  "🔒", "false"},
            {"30",  "VPN / Tarnung", null, "false"},
    };

    private GuiSidebar() {}

    public static JPanel build(Consumer<String> onMenuClick,
                               Runnable onCancel, Runnable onRestart,
                               Runnable onTheme,
                               java.util.function.BooleanSupplier isRunning) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(W, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sidebar.add(buildLogo(),                    BorderLayout.NORTH);
        sidebar.add(buildAccordion(onMenuClick),    BorderLayout.CENTER);
        sidebar.add(buildPower(onCancel, onRestart, onTheme, isRunning), BorderLayout.SOUTH);
        return sidebar;
    }

    /** Einfachere Überladung (ohne Theme/isRunning). */
    public static JPanel build(Consumer<String> onMenuClick,
                               Runnable onCancel, Runnable onRestart) {
        return build(onMenuClick, onCancel, onRestart, () -> {}, () -> false);
    }

    // ── Logo ──────────────────────────────────────────────────────────────

    private static JPanel buildLogo() {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(SIDEBAR_BG);
        p.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(13, 14, 11, 14)));

        JLabel title = new JLabel("NetTool");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 15));
        title.setForeground(ACCENT);

        String user = UserAuth.getInstance().getCurrentUser();
        boolean admin = UserAuth.getInstance().isAdmin();
        String subText = user != null
                ? "👤 " + user + (admin ? "  [admin]" : "")
                : "v3 · Network Suite";
        JLabel sub = new JLabel(subText);
        sub.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        sub.setForeground(admin ? ACCENT : (user != null ? ACCENT2 : FG_DIM));

        p.add(title, BorderLayout.CENTER);
        p.add(sub,   BorderLayout.SOUTH);
        return p;
    }

    // ── Accordion ─────────────────────────────────────────────────────────

    private static JScrollPane buildAccordion(Consumer<String> onMenuClick) {
        boolean isAdmin = UserAuth.getInstance().isAdmin();

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(SIDEBAR_BG);
        container.setBorder(new EmptyBorder(4, 0, 8, 0));

        // Gruppen aufbauen
        List<GroupEntry> groups = buildGroups(isAdmin);

        // Erste nicht-Admin-Gruppe standardmäßig öffnen
        if (!groups.isEmpty()) groups.get(0).setOpen(true);

        for (GroupEntry group : groups) {
            container.add(group.header);
            container.add(group.content);
            // Header-Klick: diese öffnen, alle anderen schließen
            group.header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    boolean wasOpen = group.isOpen();
                    groups.forEach(g -> g.setOpen(false));
                    group.setOpen(!wasOpen);
                    container.revalidate();
                    container.repaint();
                }
            });
            // Buttons in der Gruppe verdrahten
            for (Component c : group.content.getComponents()) {
                if (c instanceof JButton btn) {
                    String id = (String) btn.getClientProperty("menuId");
                    if (id != null) btn.addActionListener(e -> onMenuClick.accept(id));
                }
            }
        }

        container.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(container,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(SIDEBAR_BG);
        sp.getVerticalScrollBar().setBackground(SIDEBAR_BG);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(3, 0));
        sp.getVerticalScrollBar().setUnitIncrement(40);
        sp.getVerticalScrollBar().setBlockIncrement(200);
        return sp;
    }

    private static List<GroupEntry> buildGroups(boolean isAdmin) {
        List<GroupEntry> groups = new ArrayList<>();
        GroupEntry current = null;

        for (String[] item : ITEMS) {
            boolean adminOnly = "true".equals(item[3]);
            if (adminOnly && !isAdmin) continue;

            if (item[0] == null) {
                // Neuer Gruppen-Header
                current = new GroupEntry(item[1], item[2]);
                groups.add(current);
            } else if (current != null) {
                current.addButton(item[1], item[0]);
            }
        }
        return groups;
    }

    // ── GroupEntry ────────────────────────────────────────────────────────

    private static class GroupEntry {
        final JPanel header;
        final JPanel content;
        private boolean open = false;

        GroupEntry(String label, String icon) {
            header  = buildHeader(label, icon, this);
            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(SIDEBAR_BG);
            content.setVisible(false);
        }

        void addButton(String label, String id) {
            JButton btn = buildMenuBtn(label);
            btn.putClientProperty("menuId", id);
            content.add(btn);
            content.add(Box.createVerticalStrut(1));
        }

        boolean isOpen() { return open; }

        void setOpen(boolean open) {
            this.open = open;
            content.setVisible(open);
            // Pfeil im Header drehen
            for (Component c : header.getComponents()) {
                if (c instanceof JLabel lbl && (lbl.getText().equals("▶") || lbl.getText().equals("▼"))) {
                    lbl.setText(open ? "▼" : "▶");
                }
            }
        }
    }

    // ── Header + Button Styling ───────────────────────────────────────────

    private static JPanel buildHeader(String label, String icon, GroupEntry entry) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x11) : new Color(0xE0, 0xDE, 0xD8));
        p.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(7, 10, 7, 10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String text = (icon != null ? icon + "  " : "") + label;
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        lbl.setForeground(GuiTheme.isDark() ? new Color(0x80, 0x78, 0x50) : new Color(0x72, 0x58, 0x18));

        JLabel arrow = new JLabel("▶");
        arrow.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
        arrow.setForeground(FG_DIM);

        p.add(lbl,   BorderLayout.CENTER);
        p.add(arrow, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                p.setBackground(BTN_HOV);
                lbl.setForeground(ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                p.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x11) : new Color(0xE0, 0xDE, 0xD8));
                lbl.setForeground(GuiTheme.isDark() ? new Color(0x80, 0x78, 0x50) : new Color(0x72, 0x58, 0x18));
            }
        });
        return p;
    }

    private static JButton buildMenuBtn(String label) {
        Color fg = GuiTheme.isDark() ? new Color(0xD8, 0xD4, 0xC4) : new Color(0x18, 0x1A, 0x16);
        JButton btn = new JButton("    " + label);
        btn.setFont(BTN_F_S);
        btn.setForeground(fg);
        btn.setBackground(SIDEBAR_BG);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(BTN_HOV);
                btn.setForeground(ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
                btn.setBackground(SIDEBAR_BG);
                btn.setForeground(fg);
            }
        });
        return btn;
    }

    // ── Power-Zeile ───────────────────────────────────────────────────────

    private static JPanel buildPower(Runnable onCancel, Runnable onRestart,
                                     Runnable onTheme,
                                     java.util.function.BooleanSupplier isRunning) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(SIDEBAR_BG);
        row.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(6, 8, 6, 8)));

        StatusDot dot = new StatusDot(isRunning);
        dot.setPreferredSize(new Dimension(50, 34));
        dot.start();

        JButton pb = new JButton("⏻");
        pb.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        pb.setForeground(WARN);
        pb.setBackground(BTN_BG);
        pb.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(4, 9, 4, 9)));
        pb.setFocusPainted(false);
        pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pb.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                pb.setBackground(BTN_HOV);
                pb.setBorder(new CompoundBorder(
                        new LineBorder(WARN, 1), new EmptyBorder(4, 9, 4, 9)));
            }
            public void mouseExited(MouseEvent e) {
                pb.setBackground(BTN_BG);
                pb.setBorder(new CompoundBorder(
                        new LineBorder(BORDER, 1), new EmptyBorder(4, 9, 4, 9)));
            }
        });
        pb.addActionListener(e -> showPowerMenu(pb, onCancel, onRestart, onTheme, isRunning));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(SIDEBAR_BG);
        left.add(pb); left.add(dot);
        row.add(left, BorderLayout.WEST);
        return row;
    }

    private static void showPowerMenu(JButton anchor, Runnable onCancel,
                                      Runnable onRestart, Runnable onTheme,
                                      java.util.function.BooleanSupplier isRunning) {
        JPopupMenu m = new JPopupMenu();
        m.setBackground(PANEL_BG);
        m.setBorder(new CompoundBorder(
                new LineBorder(BORDER_LT, 1), new EmptyBorder(4, 0, 4, 0)));
        m.add(pItem("☀/🌙  Theme",           new Color(0xB8, 0xD0, 0xFF), onTheme));
        m.addSeparator();
        m.add(pItem("✕  Abbrechen  Ctrl+A",  WARN,                        onCancel));
        m.addSeparator();
        m.add(pItem("↺  Neustart    Ctrl+R", new Color(0xFF, 0xD0, 0x50), onRestart));
        m.addSeparator();
        m.add(pItem("🚪  Abmelden", new Color(0x80, 0xC8, 0xFF), () -> {
            AuditLogger.getInstance()
                    .log("LOGOUT", UserAuth.getInstance().getCurrentUser());
            UserAuth.getInstance().logout();
            onRestart.run();
        }));
        m.addSeparator();
        m.add(pItem("⏻  Beenden     Ctrl+Q", new Color(0xFF, 0x40, 0x40),
                () -> confirmQuit(isRunning)));
        m.pack();
        m.show(anchor, 0, -(m.getPreferredSize().height + 2));
    }

    private static void confirmQuit(java.util.function.BooleanSupplier isRunning) {
        if (isRunning.getAsBoolean()) {
            int r = JOptionPane.showConfirmDialog(null,
                    "<html><b>Scan läuft noch.</b><br>Trotzdem beenden?</html>",
                    "Beenden", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        AuditLogger.getInstance()
                .log("APP_EXIT", UserAuth.getInstance().getCurrentUser());
        System.exit(0);
    }

    private static JMenuItem pItem(String text, Color fg, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        item.setForeground(fg);
        item.setBackground(PANEL_BG);
        item.setBorder(new EmptyBorder(6, 14, 6, 20));
        item.setOpaque(true);
        item.addActionListener(e -> action.run());
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(BTN_HOV); }
            public void mouseExited (MouseEvent e) { item.setBackground(PANEL_BG); }
        });
        return item;
    }

    // ── Status-Indikator ──────────────────────────────────────────────────

    private static class StatusDot extends JComponent {
        private final java.util.function.BooleanSupplier isRunning;
        private float   alpha  = 0.4f;
        private boolean rising = false;
        private Timer   timer;

        StatusDot(java.util.function.BooleanSupplier s) { this.isRunning = s; setOpaque(false); }

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

        @Override
        protected void paintComponent(Graphics g) {
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
package main.java.networktool.gui.components;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import main.java.networktool.theme.GuiTheme;
import static main.java.networktool.theme.GuiTheme.*;

/**
 * Aufklappbares Accordion-Menü der Sidebar. Ersetzt frühere Boolean-Flags
 * (isAdmin/isTestSuite) durch benannte Typen {@link AccessLevel}/{@link SectionKind}.
 */
final class SidebarAccordion {

    enum AccessLevel { ADMIN, USER }
    enum SectionKind { STANDARD, TEST_SUITE }

    private static final Color TEST_SECTION_BG     = new Color(0x1A, 0x10, 0x10);
    private static final Color TEST_SECTION_BORDER = new Color(0x60, 0x30, 0x30);

    private SidebarAccordion() {}

    static JScrollPane build(String[][] items, AccessLevel accessLevel, Consumer<String> onMenuClick) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(SIDEBAR_BG);
        container.setBorder(new EmptyBorder(4, 0, 8, 0));

        List<GroupEntry> groups = buildGroups(items, accessLevel);
        groups.stream().filter(g -> g.kind == SectionKind.STANDARD).findFirst().ifPresent(g -> g.setOpen(true));

        for (GroupEntry group : groups) {
            container.add(group.header);
            container.add(group.content);
            group.header.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    boolean wasOpen = group.isOpen();
                    groups.forEach(g -> g.setOpen(false));
                    group.setOpen(!wasOpen);
                    container.revalidate(); container.repaint();
                }
            });
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

    /**
     * Baut Gruppen aus den flachen ITEMS. Wird ein Header wegen fehlender
     * Admin-Rechte gefiltert, werden auch dessen Kind-Zeilen bis zum nächsten
     * Header verworfen — sie dürfen nicht in die vorherige Gruppe rutschen.
     */
    private static List<GroupEntry> buildGroups(String[][] items, AccessLevel accessLevel) {
        List<GroupEntry> groups = new ArrayList<>();
        GroupEntry current = null;
        boolean sectionHidden = false;

        for (String[] item : items) {
            boolean adminOnly = "true".equals(item[3]);
            boolean isHeader  = item[0] == null;

            if (isHeader) {
                sectionHidden = adminOnly && accessLevel != AccessLevel.ADMIN;
                if (sectionHidden) { current = null; continue; }
                SectionKind kind = "true".equals(item[2]) ? SectionKind.TEST_SUITE : SectionKind.STANDARD;
                current = new GroupEntry(item[1], kind);
                groups.add(current);
                continue;
            }

            if (sectionHidden || current == null) continue;
            if (adminOnly && accessLevel != AccessLevel.ADMIN) continue;
            current.addButton(item[1], item[0]);
        }
        return groups;
    }

    // ── GroupEntry ────────────────────────────────────────────────────────

    private static class GroupEntry {
        final JPanel header, content;
        final SectionKind kind;
        private boolean open = false;

        GroupEntry(String label, SectionKind kind) {
            this.kind = kind;
            header  = buildHeader(label, kind);
            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(kind == SectionKind.TEST_SUITE ? TEST_SECTION_BG : SIDEBAR_BG);
            content.setVisible(false);
        }

        void addButton(String label, String id) {
            JButton btn = buildMenuBtn(label, kind);
            btn.putClientProperty("menuId", id);
            content.add(btn);
            content.add(Box.createVerticalStrut(1));
        }

        boolean isOpen() { return open; }

        void setOpen(boolean open) {
            this.open = open;
            content.setVisible(open);
            for (Component c : header.getComponents()) {
                if (c instanceof JLabel lbl && (lbl.getText().equals("[+]") || lbl.getText().equals("[-]")))
                    lbl.setText(open ? "[-]" : "[+]");
            }
        }
    }

    // ── Header ────────────────────────────────────────────────────────────

    private static JPanel buildHeader(String label, SectionKind kind) {
        return kind == SectionKind.TEST_SUITE ? buildTestSuiteHeader(label) : buildStandardHeader(label);
    }

    private static JPanel buildStandardHeader(String label) {
        Color bg = GuiTheme.isDark() ? new Color(0x10, 0x14, 0x11) : new Color(0xE0, 0xDE, 0xD8);
        Color labelFg = GuiTheme.isDark() ? new Color(0x80, 0x78, 0x50) : new Color(0x72, 0x58, 0x18);
        return assembleHeader(label, bg, BORDER, labelFg, BTN_HOV, ACCENT, 1);
    }

    private static JPanel buildTestSuiteHeader(String label) {
        Color labelFg = new Color(0xD0, 0x80, 0x80);
        return assembleHeader(label, TEST_SECTION_BG, TEST_SECTION_BORDER, labelFg,
                new Color(0x2A, 0x18, 0x18), new Color(0xE0, 0xA0, 0xA0), 2);
    }

    private static JPanel assembleHeader(String label, Color bg, Color border, Color labelFg,
                                         Color hoverBg, Color hoverFg, int borderTop) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(bg);
        p.setBorder(new CompoundBorder(
                new MatteBorder(borderTop, 0, 0, 0, border), new EmptyBorder(7, 10, 7, 10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel("  " + label);
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        lbl.setForeground(labelFg);

        JLabel arrow = new JLabel("[+]");
        arrow.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        arrow.setForeground(FG_DIM);

        p.add(lbl,   BorderLayout.CENTER);
        p.add(arrow, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { p.setBackground(hoverBg); lbl.setForeground(hoverFg); }
            public void mouseExited(MouseEvent e)  { p.setBackground(bg); lbl.setForeground(labelFg); }
        });
        return p;
    }

    // ── Menü-Buttons ──────────────────────────────────────────────────────

    private static JButton buildMenuBtn(String label, SectionKind kind) {
        return assembleMenuBtn(label, kind == SectionKind.TEST_SUITE ? TEST_SECTION_BG : SIDEBAR_BG);
    }

    private static JButton assembleMenuBtn(String label, Color sidebarBg) {
        Color fg = GuiTheme.isDark() ? new Color(0xD8, 0xD4, 0xC4) : new Color(0x18, 0x1A, 0x16);
        JButton btn = new JButton("    " + label);
        btn.setFont(BTN_F_S);
        btn.setForeground(fg);
        btn.setBackground(sidebarBg);
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
                btn.setContentAreaFilled(true); btn.setBackground(BTN_HOV); btn.setForeground(ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false); btn.setBackground(sidebarBg); btn.setForeground(fg);
            }
        });
        return btn;
    }
}
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import networktool.theme.GuiTheme;
import static networktool.theme.GuiTheme.*;

/**
 * Aufklappbares Accordion-Menü der Sidebar: Gruppen-Header, Buttons,
 * Auf-/Zuklapp-Verhalten (immer nur eine Gruppe gleichzeitig offen).
 */
final class SidebarAccordion {

    private SidebarAccordion() {}

    static JScrollPane build(String[][] items, boolean isAdmin, Consumer<String> onMenuClick) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(SIDEBAR_BG);
        container.setBorder(new EmptyBorder(4, 0, 8, 0));

        List<GroupEntry> groups = buildGroups(items, isAdmin);
        if (!groups.isEmpty()) groups.get(0).setOpen(true);

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

    private static List<GroupEntry> buildGroups(String[][] items, boolean isAdmin) {
        List<GroupEntry> groups = new ArrayList<>();
        GroupEntry current = null;
        for (String[] item : items) {
            boolean adminOnly = "true".equals(item[3]);
            if (adminOnly && !isAdmin) continue;
            if (item[0] == null) {
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
        final JPanel header, content;
        private boolean open = false;

        GroupEntry(String label, String icon) {
            header  = buildHeader(label, icon);
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
            for (Component c : header.getComponents()) {
                if (c instanceof JLabel lbl && (lbl.getText().equals("▶") || lbl.getText().equals("▼")))
                    lbl.setText(open ? "▼" : "▶");
            }
        }
    }

    // ── Header + Button Styling ───────────────────────────────────────────

    private static JPanel buildHeader(String label, String icon) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x11) : new Color(0xE0, 0xDE, 0xD8));
        p.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(7, 10, 7, 10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String text = (icon != null ? icon + "  " : "") + label;
        JLabel lbl  = new JLabel("  " + text);
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        lbl.setForeground(GuiTheme.isDark() ? new Color(0x80, 0x78, 0x50) : new Color(0x72, 0x58, 0x18));

        JLabel arrow = new JLabel("▶");
        arrow.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
        arrow.setForeground(FG_DIM);

        p.add(lbl,   BorderLayout.CENTER);
        p.add(arrow, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { p.setBackground(BTN_HOV); lbl.setForeground(ACCENT); }
            public void mouseExited(MouseEvent e)  {
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
                btn.setContentAreaFilled(true); btn.setBackground(BTN_HOV); btn.setForeground(ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false); btn.setBackground(SIDEBAR_BG); btn.setForeground(fg);
            }
        });
        return btn;
    }
}

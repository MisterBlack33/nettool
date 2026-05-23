package main.java.networktool_v3.gui;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Schnellsuche über gespeicherte Hosts.
 *
 * Sichtbar nur wenn der Gespeicherte-Hosts-Bereich aktiv ist.
 * Wird von {@link GuiSavedHostsPanel} aktiviert/deaktiviert.
 * Ctrl+F öffnet/schließt (nur wenn sichtbar).
 */
public final class GuiSearchBar extends JPanel {

    private final JTextField  field;
    private final JPanel      resultPanel;
    private final JScrollPane resultScroll;
    private volatile boolean  visible = false;

    public GuiSearchBar() {
        setLayout(new BorderLayout(0, 0));
        setBackground(GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE8, 0xE6, 0xE0));
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(6, 10, 6, 10)));

        JLabel icon = new JLabel("🔍  ");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        icon.setForeground(FG_DIM);

        field = new JTextField();
        field.setFont(MONO_S);
        field.setForeground(GuiTheme.isDark() ? new Color(0xE8, 0xE4, 0xD8) : new Color(0x14, 0x16, 0x12));
        field.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x11) : Color.WHITE);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(3, 8, 3, 8)));
        field.putClientProperty("JTextField.placeholderText", "IP, Hostname, OS, Notiz...");

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(MONO_XS);
        closeBtn.setForeground(FG_DIM);
        closeBtn.setBackground(getBackground());
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> hideSearch());
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(WARN); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(FG_DIM); }
        });

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setOpaque(false);
        inputRow.add(icon,     BorderLayout.WEST);
        inputRow.add(field,    BorderLayout.CENTER);
        inputRow.add(closeBtn, BorderLayout.EAST);
        add(inputRow, BorderLayout.NORTH);

        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : Color.WHITE);

        resultScroll = new JScrollPane(resultPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultScroll.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        resultScroll.getViewport().setBackground(resultPanel.getBackground());
        resultScroll.setPreferredSize(new Dimension(0, 180));
        resultScroll.setVisible(false);
        add(resultScroll, BorderLayout.CENTER);

        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { search(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { search(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { search(); }
        });
        field.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) hideSearch();
            }
        });

        // Standardmäßig unsichtbar – wird von GuiSavedHostsPanel aktiviert
        setVisible(false);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    /** Zeigt die Suchleiste (nur wenn SavedHosts aktiv). */
    public void showSearch() {
        setVisible(true);
        visible = true;
        field.requestFocus();
        field.selectAll();
    }

    /** Versteckt die Suchleiste. */
    public void hideSearch() {
        if (!visible) return;
        visible = false;
        setVisible(false);
        field.setText("");
        resultPanel.removeAll();
        resultScroll.setVisible(false);
    }

    /**
     * Called by GUI.java and GuiSavedHostsPanel to show the search bar.
     * Delegates to {@link #showSearch()}.
     */
    public void show() { showSearch(); }

    /**
     * Called by GUI.java and GuiSavedHostsPanel to hide the search bar.
     * Delegates to {@link #hideSearch()}.
     */
    public void hide() { hideSearch(); }

    public boolean isSearchVisible() { return visible; }

    // ── Suche ─────────────────────────────────────────────────────────────

    private void search() {
        String query = field.getText().trim().toLowerCase();
        resultPanel.removeAll();

        if (query.isEmpty()) {
            resultScroll.setVisible(false);
            revalidate(); repaint();
            return;
        }

        List<HostResult> hits = NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> matches(h, query))
                .limit(50)
                .collect(Collectors.toList());

        if (hits.isEmpty()) {
            JLabel none = new JLabel("  Keine Treffer für: " + query);
            none.setFont(MONO_S);
            none.setForeground(FG_DIM);
            none.setBorder(new EmptyBorder(8, 10, 8, 10));
            resultPanel.add(none);
        } else {
            hits.forEach(h -> resultPanel.add(buildResultRow(h)));
            JLabel count = new JLabel("  " + hits.size() + " Treffer"
                    + (hits.size() == 50 ? " (max)" : ""));
            count.setFont(MONO_XS);
            count.setForeground(FG_DIM);
            count.setBorder(new EmptyBorder(4, 10, 4, 10));
            resultPanel.add(count);
        }

        resultScroll.setVisible(true);
        resultPanel.revalidate();
        resultPanel.repaint();
        revalidate(); repaint();
    }

    private boolean matches(HostResult h, String q) {
        return contains(h.ip, q) || contains(h.hostname, q)
                || contains(h.os, q) || contains(h.notes, q);
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }

    private JPanel buildResultRow(HostResult h) {
        Color rowBg  = GuiTheme.isDark() ? new Color(0x0C, 0x10, 0x0D) : new Color(0xF4, 0xF2, 0xEE);
        Color rowHov = GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xE4, 0xE2, 0xDC);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(rowBg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(4, 10, 4, 10)));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel ipLbl = new JLabel(h.ip);
        ipLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        ipLbl.setForeground(ACCENT);
        ipLbl.setPreferredSize(new Dimension(120, 20));

        String hn = h.hostname != null && h.hostname.contains(" [")
                ? h.hostname.substring(0, h.hostname.indexOf(" [")) : h.hostname;
        JLabel hnLbl = new JLabel(hn != null ? hn : "");
        hnLbl.setFont(MONO_XS);
        hnLbl.setForeground(GuiTheme.isDark() ? new Color(0xC0, 0xBC, 0xB0) : new Color(0x30, 0x32, 0x2E));

        JLabel osLbl = new JLabel(h.os != null ? h.os : "");
        osLbl.setFont(MONO_XS);
        osLbl.setForeground(osColor(h.os));
        osLbl.setPreferredSize(new Dimension(130, 20));
        osLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(ipLbl, BorderLayout.WEST);
        row.add(hnLbl, BorderLayout.CENTER);
        row.add(osLbl, BorderLayout.EAST);
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(rowHov); }
            public void mouseExited(MouseEvent e)  { row.setBackground(rowBg); }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String cat = NetworkStore.getInstance().findNetwork(h.ip);
                    HostDetailsPanel.show(h.ip, h.hostname, h.os, cat);
                }
            }
        });
        return row;
    }
}
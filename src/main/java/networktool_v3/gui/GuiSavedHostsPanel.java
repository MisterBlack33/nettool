package main.java.networktool_v3.gui;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;
import static main.java.networktool_v3.gui.TableConfig.*;

/**
 * Panel "Gespeicherte Hosts" (Menüpunkt 09).
 *
 * Aufbau:
 *  NORD  – Netzwerk-Tabs (GuiNetworkBar) + Präfix-Editor
 *  MITTE – Host-Tabelle (In-Place-Update bei Tab-Wechsel)
 *
 * Aktiviert GuiSearchBar wenn das Panel geöffnet wird.
 * Tab "Alle" zeigt alle Hosts dedupliziert (7 Spalten).
 */
public class GuiSavedHostsPanel {

    private static final String[] COLUMNS =
            {"IP", "Hostname / MAC", "OS / Gerät", "Ports", "Gespeichert am", "Notiz"};
    private static final String[] COLUMNS_ALL =
            {"IP", "Hostname / MAC", "OS / Gerät", "Ports", "Gespeichert am", "Kategorie", "Notiz"};

    private final GuiOutputPanel output;
    private final GuiContextMenu contextMenu;
    private final GuiSearchBar   searchBar;   // wird beim Öffnen aktiviert

    private String            activeNetwork;
    private DefaultTableModel tableModel;
    private JLabel            prefixLabel;
    private JTextField        prefixField;
    private JPanel            tabBarHolder;

    public GuiSavedHostsPanel(GuiMenuHandler menuHandler, GuiOutputPanel output,
                              GuiContextMenu contextMenu, GuiSearchBar searchBar) {
        this.output      = output;
        this.contextMenu = contextMenu;
        this.searchBar   = searchBar;
        this.activeNetwork = NetworkStore.ALL_CATEGORY;
        NetworkStore.getInstance().addChangeListener(this::onStoreChanged);
    }

    /** Menüpunkt-Klick: SearchBar aktivieren, Tabelle einbetten. */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            if (searchBar != null) searchBar.show();
            tableModel = null;
            activeNetwork = NetworkStore.ALL_CATEGORY;
            output.appendText("\n★ Gespeicherte Hosts\n\n", ACCENT);
            embedFullPanel();
        });
    }

    // ── Einmaliges Einbetten ──────────────────────────────────────────────

    private void embedFullPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(BG);

        tabBarHolder = new JPanel(new BorderLayout());
        tabBarHolder.setBackground(BG);
        tabBarHolder.add(buildNetworkTabBar(), BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout(0, 2));
        north.setBackground(BG);
        north.add(tabBarHolder,     BorderLayout.NORTH);
        north.add(buildPrefixBar(), BorderLayout.SOUTH);
        outer.add(north, BorderLayout.NORTH);
        outer.add(buildTableScrollPane(), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG);
        south.add(buildHint(),         BorderLayout.WEST);
        south.add(buildManualAddBtn(), BorderLayout.EAST);
        outer.add(south, BorderLayout.SOUTH);

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    // ── Tab-Leiste ────────────────────────────────────────────────────────

    private JPanel buildNetworkTabBar() {
        return GuiNetworkBar.build(activeNetwork,
                this::onNew, this::onRename, this::onDelete,
                this::switchTab);
    }

    private void switchTab(String name) {
        activeNetwork = name;
        refreshTable();
        refreshPrefixBar();
    }

    // ── Präfix-Leiste ─────────────────────────────────────────────────────

    private JPanel buildPrefixBar() {
        String cur = NetworkStore.getInstance().getPrefix(activeNetwork);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bar.setBackground(new Color(0x09, 0x12, 0x1A));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0,1,1,1,BORDER), new EmptyBorder(3,8,3,8)));

        prefixLabel = mkLabel("IP-Präfix für \"" + activeNetwork + "\":");
        prefixField = new JTextField(cur, 16);
        prefixField.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        prefixField.setForeground(FG); prefixField.setBackground(BG);
        prefixField.setCaretColor(ACCENT);
        prefixField.setBorder(new CompoundBorder(
                new LineBorder(BORDER,1), new EmptyBorder(2,6,2,6)));

        bar.add(prefixLabel); bar.add(prefixField);
        bar.add(mkLabel("(z.B. 192.168.1.  – leer = kein Filter)"));
        bar.add(GuiNetworkBar.iconBtn("✔", ACCENT2, () ->
                applyPrefix(activeNetwork, prefixField.getText().trim())));
        return bar;
    }

    private void refreshPrefixBar() {
        if (prefixLabel == null) return;
        boolean isAll = activeNetwork.equals(NetworkStore.ALL_CATEGORY);
        prefixLabel.setText("IP-Präfix für \"" + activeNetwork + "\":");
        prefixField.setText(isAll ? "" : NetworkStore.getInstance().getPrefix(activeNetwork));
        prefixField.setEnabled(!isAll);
    }

    // ── Tabellen-Bereich ──────────────────────────────────────────────────

    private JScrollPane buildTableScrollPane() {
        tableModel = createModel();
        JTable table = buildJTable(tableModel);
        int h = Math.max(preferredHeight(table), 60);
        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN); sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, Math.min(h, 400)));
        return sp;
    }

    private void refreshTable() {
        if (tableModel == null) return;
        SwingUtilities.invokeLater(() -> {
            if (tableModel == null) return;
            Object[][] data = buildData();
            tableModel.setRowCount(0);
            for (Object[] row : data) tableModel.addRow(row);
        });
    }

    private String[] activeColumns() {
        return activeNetwork.equals(NetworkStore.ALL_CATEGORY) ? COLUMNS_ALL : COLUMNS;
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(buildData(), activeColumns()) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == getColumnCount() - 1 || c == 2;
            }
        };
    }

    private JTable buildJTable(DefaultTableModel model) {
        boolean isAll = activeNetwork.equals(NetworkStore.ALL_CATEGORY);
        JTable table = isAll ? TableConfig.buildSavedTableAll(model)
                : TableConfig.buildSavedTable(model);
        installNotesListener(table, model);
        if (!isAll) installMoveMenu(table);
        contextMenu.attach(table);
        GuiTableRenderer.installDoubleClickCopy(table);
        return table;
    }

    private Object[][] buildData() {
        boolean isAll = activeNetwork.equals(NetworkStore.ALL_CATEGORY);
        List<HostResult> hosts = NetworkStore.getInstance().getAll(activeNetwork);

        if (isAll) {
            if (hosts.isEmpty())
                return new Object[][]{{"–", "Noch keine Hosts", "", "", "", "", ""}};
            return hosts.stream().map(h -> {
                String cat = NetworkStore.getInstance().findNetwork(h.ip);
                return new Object[]{
                        h.ip,
                        h.hostname != null ? h.hostname : h.ip,
                        h.os       != null ? h.os       : "",
                        h.ports    != null && !h.ports.isEmpty() ? h.portsToString() : "",
                        h.savedAt  != null ? h.savedAt  : "–",
                        cat        != null ? cat         : "",
                        h.notes    != null ? h.notes    : ""};
            }).toArray(Object[][]::new);
        } else {
            if (hosts.isEmpty())
                return new Object[][]{{"–", "Noch keine Hosts", "", "", "", ""}};
            return hosts.stream().map(h -> new Object[]{
                    h.ip,
                    h.hostname != null ? h.hostname : h.ip,
                    h.os       != null ? h.os       : "",
                    h.ports    != null && !h.ports.isEmpty() ? h.portsToString() : "",
                    h.savedAt  != null ? h.savedAt  : "–",
                    h.notes    != null ? h.notes    : ""})
                    .toArray(Object[][]::new);
        }
    }

    // ── Netzwerk-Aktionen ─────────────────────────────────────────────────

    private void applyPrefix(String name, String prefix) {
        if (name.equals(NetworkStore.ALL_CATEGORY)) return;
        List<HostResult> hosts = new java.util.ArrayList<>(NetworkStore.getInstance().getAll(name));
        NetworkStore.getInstance().deleteNetwork(name);
        NetworkStore.getInstance().createNetwork(name, prefix);
        hosts.forEach(h -> NetworkStore.getInstance().save(h, name));
        output.appendText("  ✔ Präfix: \"" + (prefix.isBlank() ? "kein Filter" : prefix) + "\"\n", ACCENT2);
    }

    private void onNew() {
        String[] r = GuiNetworkDialogs.promptNew();
        if (r == null) return;
        NetworkStore.getInstance().createNetwork(r[0], r[1]);
        activeNetwork = r[0];
        rebuildTabBar(); refreshTable(); refreshPrefixBar();
    }

    private void onRename() {
        String newName = GuiNetworkDialogs.promptRename(activeNetwork);
        if (newName == null) return;
        NetworkStore.getInstance().renameNetwork(activeNetwork, newName);
        activeNetwork = newName;
        rebuildTabBar(); refreshTable(); refreshPrefixBar();
    }

    private void onDelete() {
        if (!GuiNetworkDialogs.confirmDelete(activeNetwork)) return;
        String deleted = activeNetwork;
        activeNetwork = NetworkStore.getInstance().getNetworkNames()
                .stream().filter(n -> !n.equals(deleted)).findFirst()
                .orElse(NetworkStore.ALL_CATEGORY);
        NetworkStore.getInstance().deleteNetwork(deleted);
        rebuildTabBar(); refreshTable(); refreshPrefixBar();
    }

    private void rebuildTabBar() {
        if (tabBarHolder == null) return;
        tabBarHolder.removeAll();
        tabBarHolder.add(buildNetworkTabBar());
        tabBarHolder.revalidate();
        tabBarHolder.repaint();
    }

    // ── Notiz-Listener ────────────────────────────────────────────────────

    private void installNotesListener(JTable table, DefaultTableModel model) {
        final int COL_NOTES = model.getColumnCount() - 1;
        model.addTableModelListener(e -> {
            int col = e.getColumn();
            if (e.getFirstRow() < 0 || (col != COL_NOTES && col != 2)) return;
            Object ip  = model.getValueAt(e.getFirstRow(), 0);
            Object val = model.getValueAt(e.getFirstRow(), col);
            if (ip == null || "–".equals(ip.toString())) return;
            SwingUtilities.invokeLater(() -> {
                String ipStr  = ip.toString();
                String valStr = val != null ? val.toString() : "";
                if (col == COL_NOTES) {
                    NetworkStore.getInstance().updateNotes(ipStr, activeNetwork, valStr);
                } else {
                    NetworkStore.getInstance().updateOs(ipStr, activeNetwork, valStr);
                    output.appendText("  OS gesetzt: " + ipStr + " = " + valStr + "\n", ACCENT2);
                }
            });
        });
    }

    // ── Verschieben-Menü ──────────────────────────────────────────────────

    private void installMoveMenu(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) { tryShowMove(e, table); }
            @Override public void mousePressed(MouseEvent e)  { tryShowMove(e, table); }
        });
    }

    private void tryShowMove(MouseEvent e, JTable table) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        Object ipVal = table.getValueAt(row, 0);
        if (ipVal == null || "–".equals(ipVal.toString())) return;
        String ip = ipVal.toString();
        List<String> others = NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(activeNetwork) && !n.equals(NetworkStore.ALL_CATEGORY))
                .toList();
        if (others.isEmpty()) return;

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x13,0x19,0x21));
        menu.setBorder(new CompoundBorder(new LineBorder(BORDER,1), new EmptyBorder(4,0,4,0)));
        JMenuItem hdr = new JMenuItem("Verschieben nach:");
        hdr.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        hdr.setForeground(FG_DIM); hdr.setEnabled(false);
        hdr.setBackground(new Color(0x13,0x19,0x21));
        menu.add(hdr); menu.addSeparator();
        for (String target : others) {
            JMenuItem item = new JMenuItem("→  " + target);
            item.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
            item.setForeground(ACCENT); item.setBackground(new Color(0x13,0x19,0x21));
            item.setBorder(new EmptyBorder(6,16,6,24)); item.setOpaque(true);
            item.addActionListener(ev -> {
                NetworkStore.getInstance().moveHost(ip, activeNetwork, target);
                output.appendText("  → " + ip + " → \"" + target + "\"\n", ACCENT2);
                refreshTable();
            });
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent ev) { item.setBackground(new Color(0x1E,0x2D,0x3D)); }
                public void mouseExited(MouseEvent ev)  { item.setBackground(new Color(0x13,0x19,0x21)); }
            });
            menu.add(item);
        }
        menu.show(e.getComponent(), e.getX() + 160, e.getY());
    }

    // ── Manuell hinzufügen ────────────────────────────────────────────────

    private JButton buildManualAddBtn() {
        JButton btn = new JButton("+ IP manuell speichern");
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        btn.setForeground(ACCENT2); btn.setBackground(BTN_BG);
        btn.setBorder(new CompoundBorder(new LineBorder(ACCENT2,1), new EmptyBorder(4,12,4,12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(BTN_BG); }
        });
        btn.addActionListener(e -> promptManualAdd());
        return btn;
    }

    private void promptManualAdd() {
        String ip = JOptionPane.showInputDialog(null,
                "IP-Adresse eingeben:", "IP manuell speichern", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.isBlank()) return;
        ip = ip.trim();
        String hostname = JOptionPane.showInputDialog(null,
                "Hostname (leer = IP):", "Hostname", JOptionPane.PLAIN_MESSAGE);
        if (hostname == null || hostname.isBlank()) hostname = ip;
        String os = JOptionPane.showInputDialog(null,
                "OS / Gerätetyp:", "OS", JOptionPane.PLAIN_MESSAGE);
        if (os == null || os.isBlank()) os = "Unbekannt";
        String notes = JOptionPane.showInputDialog(null,
                "Notiz (optional):", "Notiz", JOptionPane.PLAIN_MESSAGE);
        if (notes == null) notes = "";

        List<String> networks = NetworkStore.getInstance().getNetworkNames()
                .stream().filter(n -> !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (networks.isEmpty()) { output.appendText("  ✕ Kein Netzwerk vorhanden.\n", WARN); return; }
        String targetNet;
        if (networks.size() == 1) {
            targetNet = networks.get(0);
        } else {
            List<String> matching = NetworkStore.getInstance().matchingNetworks(ip);
            Object chosen = JOptionPane.showInputDialog(null,
                    "In welches Netzwerk?", "Netzwerk wählen",
                    JOptionPane.QUESTION_MESSAGE, null,
                    networks.toArray(), matching.isEmpty() ? networks.get(0) : matching.get(0));
            if (chosen == null) return;
            targetNet = chosen.toString();
        }
        HostResult h = new HostResult(
                ip, hostname, os, null, null, notes);
        boolean saved = NetworkStore.getInstance().save(h, targetNet);
        output.appendText(saved
                ? "  ★ " + ip + " gespeichert in \"" + targetNet + "\"\n"
                : "  ✕ Speichern fehlgeschlagen\n",
                saved ? ACCENT2 : WARN);
        refreshTable();
    }

    // ── Store-Listener ────────────────────────────────────────────────────

    private void onStoreChanged() {
        refreshTable();
        output.appendText("  ★ Hosts aktualisiert ("
                + NetworkStore.getInstance().getAllHosts().size() + ")\n", ACCENT2);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private JLabel mkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        l.setForeground(FG_DIM);
        return l;
    }

    private JPanel buildHint() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        p.setBackground(BG);
        p.add(mkLabel("  Doppelklick IP → Clipboard  |  Notiz: editierbar  |  Rechtsklick: Aktionen"));
        return p;
    }
}

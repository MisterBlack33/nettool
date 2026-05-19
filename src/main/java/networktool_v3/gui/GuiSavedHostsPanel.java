package main.java.networktool_v3.gui;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static main.java.networktool_v3.gui.GuiTheme.*;
import static main.java.networktool_v3.gui.TableConfig.*;

/**
 * Gespeicherte Hosts Panel.
 * Änderungen: sortierbare Spaltenheader (IP/Hostname/OS), Checkbox-Spalte,
 * Bulk-Aktionen "Alle löschen" und "In anderes Netz verschieben".
 */
public class GuiSavedHostsPanel {

    private static final String[] COLUMNS     = {"☐", "IP", "Hostname / MAC", "OS / Gerät", "Ports", "Gespeichert am", "Notiz"};
    private static final String[] COLUMNS_ALL = {"☐", "IP", "Hostname / MAC", "OS / Gerät", "Ports", "Gespeichert am", "Kategorie", "Notiz"};
    private static final int COL_CB   = 0;
    private static final int COL_IP   = 1;
    private static final int COL_HN   = 2;
    private static final int COL_OS   = 3;

    private final GuiOutputPanel output;
    private final GuiContextMenu contextMenu;
    private final GuiSearchBar   searchBar;

    private String            activeNetwork;
    private DefaultTableModel tableModel;
    private JLabel            prefixLabel;
    private JTextField        prefixField;
    private JPanel            tabBarHolder;

    // Sort state
    private int  sortCol = COL_IP;
    private boolean sortAsc = true;

    public GuiSavedHostsPanel(GuiMenuHandler menuHandler, GuiOutputPanel output,
                              GuiContextMenu contextMenu, GuiSearchBar searchBar) {
        this.output       = output;
        this.contextMenu  = contextMenu;
        this.searchBar    = searchBar;
        this.activeNetwork = NetworkStore.ALL_CATEGORY;
        NetworkStore.getInstance().addChangeListener(this::onStoreChanged);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            if (searchBar != null) searchBar.show();
            tableModel    = null;
            activeNetwork = NetworkStore.ALL_CATEGORY;
            output.appendText("\n★ Gespeicherte Hosts\n\n", ACCENT);
            embedFullPanel();
        });
    }

    // ── Panel assembly ────────────────────────────────────────────────────

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
        south.add(buildHint(),        BorderLayout.WEST);
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(buildBulkDeleteBtn());
        rightBtns.add(buildBulkMoveBtn());
        rightBtns.add(buildManualAddBtn());
        south.add(rightBtns, BorderLayout.EAST);
        outer.add(south, BorderLayout.SOUTH);

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────

    private JPanel buildNetworkTabBar() {
        return GuiNetworkBar.build(activeNetwork,
                this::onNew, this::onRename, this::onDelete, this::switchTab);
    }

    private void switchTab(String name) {
        activeNetwork = name;
        sortCol = COL_IP; sortAsc = true;
        syncSortToStore();
        refreshTable(); refreshPrefixBar();
    }

    // ── Prefix bar ────────────────────────────────────────────────────────

    private JPanel buildPrefixBar() {
        String cur = NetworkStore.getInstance().getPrefix(activeNetwork);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bar.setBackground(new Color(0x09, 0x12, 0x1A));
        bar.setBorder(new CompoundBorder(new MatteBorder(0,1,1,1,BORDER), new EmptyBorder(3,8,3,8)));

        prefixLabel = mkLabel("IP-Präfix für \"" + activeNetwork + "\":");
        prefixField = new JTextField(cur, 16);
        prefixField.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        prefixField.setForeground(FG); prefixField.setBackground(BG);
        prefixField.setCaretColor(ACCENT);
        prefixField.setBorder(new CompoundBorder(new LineBorder(BORDER,1), new EmptyBorder(2,6,2,6)));

        bar.add(prefixLabel); bar.add(prefixField);
        bar.add(mkLabel("(leer = kein Filter)"));
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

    // ── Table ─────────────────────────────────────────────────────────────

    private JScrollPane buildTableScrollPane() {
        tableModel = createModel();
        JTable table = buildJTable(tableModel);
        installHeaderSortListener(table);
        int h = Math.max(preferredHeight(table), 60);
        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN); sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, Math.min(h, 400)));
        return sp;
    }

    /** Click on IP/Hostname/OS header toggles sort. */
    private void installHeaderSortListener(JTable table) {
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col != COL_IP && col != COL_HN && col != COL_OS) return;
                if (col == sortCol) sortAsc = !sortAsc;
                else { sortCol = col; sortAsc = true; }
                syncSortToStore();
                refreshTable();
            }
        });
    }

    private void syncSortToStore() {
        NetworkStore.SortField sf = switch (sortCol) {
            case COL_HN -> NetworkStore.SortField.HOSTNAME;
            case COL_OS -> NetworkStore.SortField.OS;
            default     -> NetworkStore.SortField.IP;
        };
        NetworkStore.getInstance().setSortField(sf, sortAsc);
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
            @Override public Class<?> getColumnClass(int c) { return c == COL_CB ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) {
                return c == COL_CB || c == getColumnCount() - 1 || c == COL_OS;
            }
        };
    }

    private JTable buildJTable(DefaultTableModel model) {
        boolean isAll = activeNetwork.equals(NetworkStore.ALL_CATEGORY);
        JTable table = isAll ? TableConfig.buildSavedTableAll(model)
                : TableConfig.buildSavedTable(model);
        // checkbox column width
        table.getColumnModel().getColumn(COL_CB).setMaxWidth(28);
        table.getColumnModel().getColumn(COL_CB).setMinWidth(28);
        table.getColumnModel().getColumn(COL_CB).setPreferredWidth(28);
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
            if (hosts.isEmpty()) return new Object[][]{{Boolean.FALSE, "–", "Noch keine Hosts", "", "", "", "", ""}};
            return hosts.stream().map(h -> {
                String cat = NetworkStore.getInstance().findNetwork(h.ip);
                return new Object[]{Boolean.FALSE, h.ip, hn(h), str(h.os), str(h.portsToString()), str(h.savedAt), str(cat), str(h.notes)};
            }).toArray(Object[][]::new);
        } else {
            if (hosts.isEmpty()) return new Object[][]{{Boolean.FALSE, "–", "Noch keine Hosts", "", "", "", ""}};
            return hosts.stream().map(h -> new Object[]{
                    Boolean.FALSE, h.ip, hn(h), str(h.os), str(h.portsToString()), str(h.savedAt), str(h.notes)
            }).toArray(Object[][]::new);
        }
    }

    // ── Bulk actions ──────────────────────────────────────────────────────

    private List<String> checkedIps() {
        if (tableModel == null) return List.of();
        List<String> ips = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object cb = tableModel.getValueAt(r, COL_CB);
            if (Boolean.TRUE.equals(cb)) {
                Object ip = tableModel.getValueAt(r, COL_IP);
                if (ip != null && !ip.toString().equals("–")) ips.add(ip.toString());
            }
        }
        return ips;
    }

    private JButton buildBulkDeleteBtn() {
        JButton btn = mkActionBtn("✕ Auswahl löschen", WARN);
        btn.addActionListener(e -> {
            List<String> ips = checkedIps();
            if (ips.isEmpty()) { output.appendText("  Keine Hosts ausgewählt.\n", FG_DIM); return; }
            int ok = JOptionPane.showConfirmDialog(null,
                    ips.size() + " Host(s) löschen?", "Bulk-Löschen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            ips.forEach(ip -> NetworkStore.getInstance().remove(ip, activeNetwork));
            output.appendText("  ✕ " + ips.size() + " Host(s) gelöscht\n", WARN);
            refreshTable();
        });
        return btn;
    }

    private JButton buildBulkMoveBtn() {
        JButton btn = mkActionBtn("→ Auswahl verschieben", ACCENT);
        btn.addActionListener(e -> {
            List<String> ips = checkedIps();
            if (ips.isEmpty()) { output.appendText("  Keine Hosts ausgewählt.\n", FG_DIM); return; }
            List<String> targets = NetworkStore.getInstance().getNetworkNames().stream()
                    .filter(n -> !n.equals(activeNetwork) && !n.equals(NetworkStore.ALL_CATEGORY))
                    .collect(Collectors.toList());
            if (targets.isEmpty()) { output.appendText("  Kein Zielnetzwerk vorhanden.\n", FG_DIM); return; }
            Object chosen = JOptionPane.showInputDialog(null, "Verschieben nach:",
                    "Bulk-Verschieben", JOptionPane.QUESTION_MESSAGE, null,
                    targets.toArray(), targets.get(0));
            if (chosen == null) return;
            String target = chosen.toString();
            ips.forEach(ip -> NetworkStore.getInstance().moveHost(ip, activeNetwork, target));
            output.appendText("  → " + ips.size() + " Host(s) nach \"" + target + "\"\n", ACCENT2);
            refreshTable();
        });
        return btn;
    }

    // ── Network actions ───────────────────────────────────────────────────

    private void applyPrefix(String name, String prefix) {
        if (name.equals(NetworkStore.ALL_CATEGORY)) return;
        List<HostResult> hosts = new ArrayList<>(NetworkStore.getInstance().getAll(name));
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
                .stream().filter(n -> !n.equals(deleted)).findFirst().orElse(NetworkStore.ALL_CATEGORY);
        NetworkStore.getInstance().deleteNetwork(deleted);
        rebuildTabBar(); refreshTable(); refreshPrefixBar();
    }

    private void rebuildTabBar() {
        if (tabBarHolder == null) return;
        tabBarHolder.removeAll();
        tabBarHolder.add(buildNetworkTabBar());
        tabBarHolder.revalidate(); tabBarHolder.repaint();
    }

    // ── Notes / OS listener ───────────────────────────────────────────────

    private void installNotesListener(JTable table, DefaultTableModel model) {
        final int COL_NOTES = model.getColumnCount() - 1;
        model.addTableModelListener(e -> {
            int col = e.getColumn();
            if (e.getFirstRow() < 0 || (col != COL_NOTES && col != COL_OS)) return;
            Object ip  = model.getValueAt(e.getFirstRow(), COL_IP);
            Object val = model.getValueAt(e.getFirstRow(), col);
            if (ip == null || "–".equals(ip.toString())) return;
            SwingUtilities.invokeLater(() -> {
                String ipStr  = ip.toString();
                String valStr = val != null ? val.toString() : "";
                if (col == COL_NOTES) NetworkStore.getInstance().updateNotes(ipStr, activeNetwork, valStr);
                else { NetworkStore.getInstance().updateOs(ipStr, activeNetwork, valStr);
                    output.appendText("  OS gesetzt: " + ipStr + " = " + valStr + "\n", ACCENT2); }
            });
        });
    }

    // ── Move context menu ─────────────────────────────────────────────────

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
        Object ipVal = table.getValueAt(row, COL_IP);
        if (ipVal == null || "–".equals(ipVal.toString())) return;
        String ip = ipVal.toString();
        List<String> others = NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(activeNetwork) && !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (others.isEmpty()) return;
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x13,0x19,0x21));
        menu.setBorder(new CompoundBorder(new LineBorder(BORDER,1), new EmptyBorder(4,0,4,0)));
        JMenuItem hdr = new JMenuItem("Verschieben nach:");
        hdr.setFont(new Font("JetBrains Mono", Font.BOLD, 11)); hdr.setForeground(FG_DIM);
        hdr.setBackground(new Color(0x13,0x19,0x21)); hdr.setEnabled(false); menu.add(hdr); menu.addSeparator();
        for (String target : others) {
            JMenuItem item = new JMenuItem("→  " + target);
            item.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
            item.setForeground(ACCENT); item.setBackground(new Color(0x13,0x19,0x21));
            item.setBorder(new EmptyBorder(6,16,6,24)); item.setOpaque(true);
            item.addActionListener(ev -> {
                NetworkStore.getInstance().moveHost(ip, activeNetwork, target);
                output.appendText("  → " + ip + " → \"" + target + "\"\n", ACCENT2); refreshTable();
            });
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent ev) { item.setBackground(new Color(0x1E,0x2D,0x3D)); }
                public void mouseExited(MouseEvent ev)  { item.setBackground(new Color(0x13,0x19,0x21)); }
            });
            menu.add(item);
        }
        menu.show(e.getComponent(), e.getX() + 160, e.getY());
    }

    // ── Manual add ────────────────────────────────────────────────────────

    private JButton buildManualAddBtn() {
        JButton btn = mkActionBtn("+ IP manuell speichern", ACCENT2);
        btn.addActionListener(e -> promptManualAdd());
        return btn;
    }

    private void promptManualAdd() {
        String ip = JOptionPane.showInputDialog(null, "IP-Adresse:", "IP manuell speichern", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.isBlank()) return;
        ip = ip.trim();
        String hn    = JOptionPane.showInputDialog(null, "Hostname (leer = IP):", "Hostname", JOptionPane.PLAIN_MESSAGE);
        String os    = JOptionPane.showInputDialog(null, "OS / Gerätetyp:", "OS", JOptionPane.PLAIN_MESSAGE);
        String notes = JOptionPane.showInputDialog(null, "Notiz (optional):", "Notiz", JOptionPane.PLAIN_MESSAGE);
        if (hn == null || hn.isBlank()) hn = ip;
        if (os == null || os.isBlank()) os = "Unbekannt";
        if (notes == null) notes = "";
        List<String> networks = NetworkStore.getInstance().getNetworkNames()
                .stream().filter(n -> !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (networks.isEmpty()) { output.appendText("  ✕ Kein Netzwerk vorhanden.\n", WARN); return; }
        String targetNet = networks.size() == 1 ? networks.get(0) : pickNetwork(ip, networks);
        if (targetNet == null) return;
        boolean saved = NetworkStore.getInstance().save(new HostResult(ip, hn, os, null, null, notes), targetNet);
        output.appendText(saved ? "  ★ " + ip + " gespeichert in \"" + targetNet + "\"\n"
                : "  ✕ Speichern fehlgeschlagen\n", saved ? ACCENT2 : WARN);
        refreshTable();
    }

    private String pickNetwork(String ip, List<String> networks) {
        List<String> matching = NetworkStore.getInstance().matchingNetworks(ip);
        Object chosen = JOptionPane.showInputDialog(null, "In welches Netzwerk?", "Netzwerk wählen",
                JOptionPane.QUESTION_MESSAGE, null, networks.toArray(),
                matching.isEmpty() ? networks.get(0) : matching.get(0));
        return chosen == null ? null : chosen.toString();
    }

    // ── Store listener ────────────────────────────────────────────────────

    private void onStoreChanged() {
        refreshTable();
        output.appendText("  ★ Hosts aktualisiert (" + NetworkStore.getInstance().getAllHosts().size() + ")\n", ACCENT2);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String hn(HostResult h) { return h.hostname != null ? h.hostname : h.ip; }
    private static String str(String s)    { return s != null ? s : ""; }

    private JLabel mkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        l.setForeground(FG_DIM);
        return l;
    }

    private JPanel buildHint() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        p.setBackground(BG);
        p.add(mkLabel("  ☐ auswählen · Doppelklick IP → Clipboard · Notiz: editierbar · Rechtsklick: Aktionen"));
        return p;
    }

    private JButton mkActionBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg); b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg,1), new EmptyBorder(4,12,4,12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        return b;
    }
}
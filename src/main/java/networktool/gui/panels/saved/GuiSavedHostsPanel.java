package main.java.networktool.gui.panels.saved;

import main.java.networktool.gui.components.actions.GuiContextMenu;
import main.java.networktool.gui.components.GuiNetworkBar;
import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.network.NetworkStore;
import main.java.networktool.util.TableConfig;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;
import static main.java.networktool.util.TableConfig.*;

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
    private final GuiSearchBar searchBar;

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
        rightBtns.add(SavedHostsBulkActions.buildBulkDeleteBtn(() -> tableModel, () -> activeNetwork, output, this::refreshTable));
        rightBtns.add(SavedHostsBulkActions.buildBulkMoveBtn(() -> tableModel, () -> activeNetwork, output, this::refreshTable));
        rightBtns.add(SavedHostsManualAdd.buildButton(output, this::refreshTable));
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

        prefixLabel = SavedHostsStyle.label("IP-Präfix für \"" + activeNetwork + "\":");
        prefixField = new JTextField(cur, 16);
        prefixField.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        prefixField.setForeground(FG); prefixField.setBackground(BG);
        prefixField.setCaretColor(ACCENT);
        prefixField.setBorder(new CompoundBorder(new LineBorder(BORDER,1), new EmptyBorder(2,6,2,6)));

        bar.add(prefixLabel); bar.add(prefixField);
        bar.add(SavedHostsStyle.label("(leer = kein Filter)"));
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
        if (!isAll) SavedHostsMoveMenu.install(table, () -> activeNetwork, output, this::refreshTable);
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
                return new Object[]{Boolean.FALSE, h.ip, SavedHostsStyle.hostname(h), SavedHostsStyle.orEmpty(h.os), SavedHostsStyle.orEmpty(h.portsToString()), SavedHostsStyle.orEmpty(h.savedAt), SavedHostsStyle.orEmpty(cat), SavedHostsStyle.orEmpty(h.notes)};
            }).toArray(Object[][]::new);
        } else {
            if (hosts.isEmpty()) return new Object[][]{{Boolean.FALSE, "–", "Noch keine Hosts", "", "", "", ""}};
            return hosts.stream().map(h -> new Object[]{
                    Boolean.FALSE, h.ip, SavedHostsStyle.hostname(h), SavedHostsStyle.orEmpty(h.os), SavedHostsStyle.orEmpty(h.portsToString()), SavedHostsStyle.orEmpty(h.savedAt), SavedHostsStyle.orEmpty(h.notes)
            }).toArray(Object[][]::new);
        }
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

    private JPanel buildHint() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        p.setBackground(BG);
        p.add(SavedHostsStyle.label("  ☐ auswählen · Doppelklick IP → Clipboard · Notiz: editierbar · Rechtsklick: Aktionen"));
        return p;
    }

    // ── Store listener ────────────────────────────────────────────────────

    private void onStoreChanged() {
        refreshTable();
        output.appendText("  ★ Hosts aktualisiert (" + NetworkStore.getInstance().getAllHosts().size() + ")\n", ACCENT2);
    }

}
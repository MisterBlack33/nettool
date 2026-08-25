package main.java.networktool.gui.panels.saved;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.storage.network.NetworkStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Bulk-Aktionen (Löschen/Verschieben) für die per Checkbox ausgewählten
 * Hosts im {@link GuiSavedHostsPanel}.
 */
final class SavedHostsBulkActions {

    private static final int COL_CB = 0;
    private static final int COL_IP = 1;

    private SavedHostsBulkActions() {}

    static List<String> checkedIps(DefaultTableModel tableModel) {
        if (tableModel == null) return List.of();
        List<String> ips = new java.util.ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object cb = tableModel.getValueAt(r, COL_CB);
            if (Boolean.TRUE.equals(cb)) {
                Object ip = tableModel.getValueAt(r, COL_IP);
                if (ip != null && !ip.toString().equals("–")) ips.add(ip.toString());
            }
        }
        return ips;
    }

    static JButton buildBulkDeleteBtn(Supplier<DefaultTableModel> model, Supplier<String> activeNetwork,
                                      GuiOutputPanel output, Runnable refreshTable) {
        JButton btn = SavedHostsStyle.actionBtn("✕ Auswahl löschen", WARN);
        btn.addActionListener(e -> {
            List<String> ips = checkedIps(model.get());
            if (ips.isEmpty()) { output.appendText("  Keine Hosts ausgewählt.\n", FG_DIM); return; }
            int ok = JOptionPane.showConfirmDialog(null,
                    ips.size() + " Host(s) löschen?", "Bulk-Löschen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            ips.forEach(ip -> NetworkStore.getInstance().remove(ip, activeNetwork.get()));
            output.appendText("  ✕ " + ips.size() + " Host(s) gelöscht\n", WARN);
            refreshTable.run();
        });
        return btn;
    }

    static JButton buildBulkMoveBtn(Supplier<DefaultTableModel> model, Supplier<String> activeNetwork,
                                     GuiOutputPanel output, Runnable refreshTable) {
        JButton btn = SavedHostsStyle.actionBtn("→ Auswahl verschieben", ACCENT);
        btn.addActionListener(e -> {
            List<String> ips = checkedIps(model.get());
            if (ips.isEmpty()) { output.appendText("  Keine Hosts ausgewählt.\n", FG_DIM); return; }
            List<String> targets = NetworkStore.getInstance().getNetworkNames().stream()
                    .filter(n -> !n.equals(activeNetwork.get()) && !n.equals(NetworkStore.ALL_CATEGORY))
                    .collect(Collectors.toList());
            if (targets.isEmpty()) { output.appendText("  Kein Zielnetzwerk vorhanden.\n", FG_DIM); return; }
            Object chosen = JOptionPane.showInputDialog(null, "Verschieben nach:",
                    "Bulk-Verschieben", JOptionPane.QUESTION_MESSAGE, null,
                    targets.toArray(), targets.get(0));
            if (chosen == null) return;
            String target = chosen.toString();
            ips.forEach(ip -> NetworkStore.getInstance().moveHost(ip, activeNetwork.get(), target));
            output.appendText("  → " + ips.size() + " Host(s) nach \"" + target + "\"\n", ACCENT2);
            refreshTable.run();
        });
        return btn;
    }
}

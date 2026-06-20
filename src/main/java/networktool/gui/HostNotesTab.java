package main.java.networktool.gui;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.HostDetailRows.detailButton;

/** Tab ④: Notiz-Editor – direkt bearbeiten und speichern. */
final class HostNotesTab {

    private HostNotesTab() {}

    static JPanel build(String ip, String category, Color panBg) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(panBg);
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JTextArea area = buildTextArea(currentNotes(ip));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new LineBorder(BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildSaveRow(ip, category, area, panBg), BorderLayout.SOUTH);
        return panel;
    }

    private static String currentNotes(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip)).findFirst()
                .map(h -> h.notes != null ? h.notes : "")
                .orElse("");
    }

    private static JTextArea buildTextArea(String current) {
        JTextArea area = new JTextArea(current);
        area.setFont(MONO);
        area.setForeground(GuiTheme.isDark() ? new Color(0xFF, 0xE8, 0x90) : new Color(0x60, 0x48, 0x08));
        area.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x0E, 0x04) : new Color(0xFF, 0xFB, 0xE8));
        area.setCaretColor(ACCENT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    private static JPanel buildSaveRow(String ip, String category, JTextArea area, Color panBg) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        row.setBackground(panBg);

        JLabel savedLbl = new JLabel("");
        savedLbl.setFont(MONO_XS);
        savedLbl.setForeground(ACCENT2);

        JButton saveBtn = detailButton("💾 Speichern", ACCENT2);
        saveBtn.addActionListener(e -> {
            String cat = category != null ? category : NetworkStore.ALL_CATEGORY;
            NetworkStore.getInstance().updateNotes(ip, cat, area.getText());
            savedLbl.setText("✔ gespeichert  "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        row.add(savedLbl);
        row.add(saveBtn);
        return row;
    }
}
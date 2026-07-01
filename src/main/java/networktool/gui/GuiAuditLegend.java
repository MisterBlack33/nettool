package networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static networktool.gui.GuiTheme.*;

/** Legende mit Aktions-Codes und Menü-IDs für den Audit-Log-Viewer. */
final class GuiAuditLegend {

    private GuiAuditLegend() {}

    private static final String[][] ACTION_LEGEND = {
            {"LOGIN",             "Erfolgreiche Anmeldung"},
            {"LOGIN_FAILED",      "Fehlgeschlagener Login"},
            {"LOGOUT",            "Abmeldung"},
            {"USER_CREATED",      "Neuer Benutzer angelegt"},
            {"APP_START/EXIT",    "Programm gestartet / beendet"},
            {"APP_RESTART",       "Neustart"},
            {"MENU",              "Menüpunkt geklickt (Detail = Menü-ID)"},
            {"SCAN / DIAGNOSE",   "Netzwerk-Scan / IP-Diagnose"},
            {"SECURITY_ALERT",    "Sicherheitswarnung"},
            {"EXPORT / IMPORT",   "Datenexport / -import"},
            {"CANCEL",            "Laufenden Scan abgebrochen"},
            {"AUDIT_LOG_CLEARED", "Audit-Log manuell geleert"},
    };

    private static final String[][] MENU_ID_LEGEND = {
            {"01", "Minimale Netzwerkinfo"},   {"02", "Vollständige Netzwerkinfo"},
            {"03", "IP-Diagnose"},             {"04", "File-Server starten"},
            {"05", "Datei senden"},            {"06", "CIDR-Scan"},
            {"07", "Filter-Scan"},             {"08", "Nachricht senden"},
            {"09", "Gespeicherte Hosts"},      {"10", "Hops & Routen"},
            {"11", "Fremdnetz-Scanner"},       {"12", "Scan-Profile"},
            {"13", "Scan-Delta"},              {"14", "Scheduler"},
            {"15", "Bandwidth-Test"},          {"16", "Dauerping"},
            {"17", "Sicherheitsmonitor"},      {"18", "Export / Import"},
            {"19", "Benachrichtigungs-Verlauf"},{"20", "Netzwerk-Karte"},
            {"21", "Port-Liste"},              {"22", "Scan-Verlauf"},
            {"23", "Audit-Log"},               {"30", "Privatsphäre / VPN"},
    };

    static JPanel build(Color panBg) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(panBg);
        wrapper.setBorder(new MatteBorder(0, 1, 1, 1, BORDER));

        JButton toggleBtn = new JButton("▶  Aktions-Codes & Menü-IDs");
        toggleBtn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        toggleBtn.setForeground(FG_DIM);
        toggleBtn.setBackground(panBg);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setBorder(new EmptyBorder(4, 10, 4, 10));
        toggleBtn.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setBackground(panBg);
        content.setBorder(new EmptyBorder(4, 14, 8, 14));
        content.setVisible(false);
        content.add(buildGrid(panBg, "Aktions-Codes", ACTION_LEGEND, ACCENT));
        content.add(buildGrid(panBg, "Menü-IDs",      MENU_ID_LEGEND, INFO));

        JLabel hint = new JLabel("  Rotation ab 200.000 Einträgen  ·  Logs bleiben über Neustarts erhalten");
        hint.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        hint.setForeground(FG_DIM);
        hint.setBorder(new EmptyBorder(2, 10, 4, 10));

        toggleBtn.addActionListener(e -> {
            boolean open = content.isVisible();
            content.setVisible(!open);
            toggleBtn.setText((open ? "▶" : "▼") + "  Aktions-Codes & Menü-IDs");
            wrapper.revalidate(); wrapper.repaint();
        });

        wrapper.add(toggleBtn, BorderLayout.NORTH);
        wrapper.add(content,   BorderLayout.CENTER);
        wrapper.add(hint,      BorderLayout.SOUTH);
        return wrapper;
    }

    private static JPanel buildGrid(Color panBg, String title, String[][] rows, Color keyColor) {
        JPanel p = new JPanel(new GridLayout(0, 2, 10, 1));
        p.setBackground(panBg);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        titleLbl.setForeground(ACCENT);
        p.add(titleLbl);
        p.add(new JLabel(""));
        for (String[] row : rows) {
            JLabel key = new JLabel(row[0]);
            key.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
            key.setForeground(keyColor);
            JLabel desc = new JLabel(row[1]);
            desc.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
            desc.setForeground(FG_DIM);
            p.add(key);
            p.add(desc);
        }
        return p;
    }
}
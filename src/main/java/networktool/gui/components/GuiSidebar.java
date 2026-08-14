package main.java.networktool.gui.components;

import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.function.Consumer;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Linke Seitenleiste als aufklappbares Accordion-Menü.
 * Accordion-Mechanik siehe {@link SidebarAccordion}, Power-Zeile siehe {@link SidebarPowerMenu}.
 */
public final class GuiSidebar {

    private static final int W = 200;

    private static final String[][] ITEMS = {
            {null,  "HOSTS",         "★", "false"},
            {"09",  "Gespeicherte",  null, "false"},
            {null,  "LOKALES NETZ",  "◎", "false"},
            {"01",  "Übersicht",     null, "false"},
            {"02",  "Interfaces",    null, "false"},
            {"10",  "Hops & Routen", null, "false"},
            {null,  "SCAN",          "⊕", "false"},
            {"06",  "CIDR-Scan",     null, "false"},
            {"07",  "Filter-Scan",   null, "false"},
            {"12",  "Profile",       null, "false"},
            {"14",  "Scheduler",     null, "false"},
            {"13",  "Scan-Δ",        null, "false"},
            {null,  "FREMDNETZ",     "✦", "true"},
            {"11",  "Scanner",       null, "true"},
            {null,  "DIAGNOSE",      "✚", "false"},
            {"03",  "IP-Analyse",    null, "false"},
            {"16",  "Dauerping",     null, "false"},
            {"15",  "Bandwidth",     null, "false"},
            {null,  "TRANSFER",      "⇄", "false"},
            {"04",  "File-Server",   null, "false"},
            {"05",  "Datei senden",  null, "false"},
            {null,  "NACHRICHTEN",   "✉", "false"},
            {"08",  "Senden",        null, "false"},
            {"19",  "Verlauf",       null, "false"},
            {null,  "SICHERHEIT",    "⚑", "false"},
            {"17",  "Monitor",       null, "false"},
            {"23",  "Audit-Log",     null, "true"},
            {null,  "DATEN",         "📦", "false"},
            {"18",  "Export/Import", null, "false"},
            {null,  "ANSICHT",       "🗺", "false"},
            {"20",  "Netzwerk-Karte",null, "false"},
            {null,  "KONFIGURATION", "⚙", "false"},
            {"21",  "Port-Liste",    null, "false"},
            {"22",  "Scan-Verlauf",  null, "false"},
            {null,  "PRIVATSPHÄRE",  "🔒", "false"},
            {"30",  "VPN / Tarnung", null, "false"},
            {null,  "TEST-SUITE",    "🧪", "false"},
            {"24",  "Data → Sound",  null, "false"},
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
        sidebar.add(SidebarPowerMenu.build(onCancel, onRestart, onTheme, isRunning), BorderLayout.SOUTH);
        return sidebar;
    }

    public static JPanel build(Consumer<String> onMenuClick, Runnable onCancel, Runnable onRestart) {
        return build(onMenuClick, onCancel, onRestart, () -> {}, () -> false);
    }

    // ── Logo ──────────────────────────────────────────────────────────────

    private static JPanel buildLogo() {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(SIDEBAR_BG);
        p.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(13, 14, 11, 14)));

        JLabel title = new JLabel("NetTool");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 15));
        title.setForeground(ACCENT);

        String user  = UserAuth.getInstance().getCurrentUser();
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
        return SidebarAccordion.build(ITEMS, isAdmin, onMenuClick);
    }
}
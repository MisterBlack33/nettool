package main.java.networktool.gui.components;

import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.function.Consumer;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Linke Seitenleiste als aufklappbares Accordion-Menü.
 * Accordion-Mechanik siehe {@link SidebarAccordion}, Power-Zeile siehe {@link SidebarPowerMenu},
 * Admin-Freischaltung siehe {@link SidebarAdminButton}.
 *
 * Zeilenformat {@code ITEMS}: {menuId, label, testSuiteFlag, adminOnly}.
 * Bei Gruppen-Headern (menuId == null) markiert testSuiteFlag == "true" die
 * Test-Suite-Sektion (eigene Optik, standardmäßig eingeklappt, siehe SidebarAccordion).
 */
public final class GuiSidebar {

    private static final int W = 200;

    private static final String[][] ITEMS = {
            {null,  "HOSTS",         null, "false"},
            {"09",  "Gespeicherte",  null, "false"},
            {null,  "LOKALES NETZ",  null, "false"},
            {"01",  "Übersicht",     null, "false"},
            {"02",  "Interfaces",    null, "false"},
            {"10",  "Hops & Routen", null, "false"},
            {null,  "SCAN",          null, "false"},
            {"06",  "CIDR-Scan",     null, "false"},
            {"07",  "Filter-Scan",   null, "false"},
            {"12",  "Profile",       null, "false"},
            {"14",  "Scheduler",     null, "false"},
            {"13",  "Scan-Δ",        null, "false"},
            {null,  "FREMDNETZ",     null, "true"},
            {"11",  "Scanner",       null, "true"},
            {null,  "DIAGNOSE",      null, "false"},
            {"03",  "IP-Analyse",    null, "false"},
            {"16",  "Dauerping",     null, "false"},
            {"15",  "Bandwidth",     null, "false"},
            {null,  "TRANSFER",      null, "false"},
            {"04",  "File-Server",   null, "false"},
            {"05",  "Datei senden",  null, "false"},
            {null,  "NACHRICHTEN",   null, "false"},
            {"08",  "Senden",        null, "false"},
            {"19",  "Verlauf",       null, "false"},
            {null,  "SICHERHEIT",    null, "false"},
            {"17",  "Monitor",       null, "false"},
            {"23",  "Audit-Log",     null, "true"},
            {null,  "DATEN",         null, "false"},
            {"18",  "Export/Import", null, "false"},
            {null,  "ANSICHT",       null, "false"},
            {"20",  "Netzwerk-Karte",null, "false"},
            {null,  "KONFIGURATION", null, "false"},
            {"21",  "Port-Liste",    null, "false"},
            {"22",  "Scan-Verlauf",  null, "false"},
            {null,  "PRIVATSPHÄRE",  null, "false"},
            {"30",  "VPN / Tarnung", null, "false"},
            {null,  "TEST-SUITE (nur Entwicklung)", "true", "true"},
            {"24",  "Data → Sound",  null, "true"},
            {"25",  "Data → Visual (Balken)",       null, "true"},
            {"26",  "Data → Visual (Spektrogramm)", null, "true"},
            {"27",  "Security-Findings",  null, "true"},
            {"28",  "Bandwidth-Verlauf",  null, "true"},
            {"29",  "Dashboard",          null, "true"},
    };

    private GuiSidebar() {}

    public static JPanel build(Consumer<String> onMenuClick,
                               Runnable onCancel, Runnable onRestart,
                               java.util.function.BooleanSupplier isRunning) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(W, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

        JPanel accordionHolder = new JPanel(new BorderLayout());
        accordionHolder.setBackground(SIDEBAR_BG);
        accordionHolder.add(buildAccordion(onMenuClick), BorderLayout.CENTER);

        JPanel footerHolder = new JPanel(new BorderLayout());
        footerHolder.setBackground(SIDEBAR_BG);
        Runnable[] rebuildAllHolder = new Runnable[1];
        Runnable rebuildAccordion = () -> {
            accordionHolder.removeAll();
            accordionHolder.add(buildAccordion(onMenuClick), BorderLayout.CENTER);
            accordionHolder.revalidate();
            accordionHolder.repaint();
        };
        Runnable rebuildFooter = () -> {
            footerHolder.removeAll();
            footerHolder.add(buildFooter(onCancel, onRestart, isRunning, rebuildAllHolder[0], onMenuClick),
                    BorderLayout.CENTER);
            footerHolder.revalidate();
            footerHolder.repaint();
        };
        rebuildAllHolder[0] = () -> {
            rebuildAccordion.run();
            rebuildFooter.run();
        };

        sidebar.add(buildLogo(),      BorderLayout.NORTH);
        sidebar.add(accordionHolder, BorderLayout.CENTER);
        rebuildFooter.run();
        sidebar.add(footerHolder, BorderLayout.SOUTH);
        return sidebar;
    }

    public static JPanel build(Consumer<String> onMenuClick, Runnable onCancel, Runnable onRestart) {
        return build(onMenuClick, onCancel, onRestart, () -> false);
    }

    public static JPanel build(Consumer<String> onMenuClick,
                                Runnable onCancel, Runnable onRestart,
                                Runnable onTheme,
                                java.util.function.BooleanSupplier isRunning) {
        return build(onMenuClick, onCancel, onRestart, isRunning);
    }

    // ── Footer (Admin-Button + Power-Menü) ───────────────────────────────

    private static JPanel buildFooter(Runnable onCancel, Runnable onRestart,
                                      java.util.function.BooleanSupplier isRunning,
                                      Runnable onAdminGranted,
                                      Consumer<String> onMenuClick) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SIDEBAR_BG);
        footer.add(buildAdminRow(onAdminGranted, onMenuClick), BorderLayout.NORTH);
        footer.add(SidebarPowerMenu.build(onCancel, onRestart, isRunning), BorderLayout.SOUTH);
        return footer;
    }

    private static JPanel buildAdminRow(Runnable onAdminGranted, Consumer<String> onMenuClick) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(SIDEBAR_BG);
        row.setBorder(new EmptyBorder(0, 6, 0, 0));
        JButton adminButton = SidebarAdminButton.build(onAdminGranted);
        adminButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(adminButton);
        if (UserAuth.getInstance().isAdmin()) {
            JButton passwordButton = SidebarAdminButton.buildPasswordChangeButton();
            passwordButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(passwordButton);
            JButton debugButton = SidebarDebugButton.build(() -> onMenuClick.accept("46"));
            debugButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(debugButton);
        }
        return row;
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
                ? user + (admin ? "  [admin]" : "")
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
        SidebarAccordion.AccessLevel accessLevel = isAdmin ? SidebarAccordion.AccessLevel.ADMIN : SidebarAccordion.AccessLevel.USER;
        return SidebarAccordion.build(ITEMS, accessLevel, onMenuClick);
    }
}

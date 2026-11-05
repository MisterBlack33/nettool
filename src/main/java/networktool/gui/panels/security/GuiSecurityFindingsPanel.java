package main.java.networktool.gui.panels.security;

import main.java.networktool.gui.core.GuiDebugMode;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.security.DefaultCredentialProbe;
import main.java.networktool.logic.analysis.security.SecurityFinding;
import main.java.networktool.logic.analysis.security.SecurityFindingsCollector;
import main.java.networktool.logic.analysis.security.SecurityFindingsExporter;
import main.java.networktool.logic.analysis.security.TlsCertInspector;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.TableConfig;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Panel: listet Security-Findings, erlaubt manuelle Host-Checks (Menü-ID "27"). */
public final class GuiSecurityFindingsPanel {

    private static final DateTimeFormatter OBSERVED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private GuiSecurityFindingsPanel() {}

    public static void show(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embed(output));
    }

    public static void showDebug(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embed(output, true));
    }

    private static void embed(GuiOutputPanel output) {
        embed(output, false);
    }

    private static void embed(GuiOutputPanel output, boolean debug) {
        output.appendText("\nSecurity-Findings\n\n", ACCENT);

        DefaultTableModel model = buildModel();
        if (debug) {
            model.addRow(new Object[]{"192.168.10.10", "TLS_CERT", "WARN", "CONFIRMED",
                    "TLS_CERT_INSPECTOR", "Beispielzeit", "Zertifikat läuft in 21 Tagen ab"});
            model.addRow(new Object[]{"192.168.10.20", "DEFAULT_CREDENTIALS", "CRITICAL", "MEDIUM",
                    "DEFAULT_CREDENTIAL_PROBE", "Beispielzeit", "HTTP-Login erhielt HTTP 200"});
            model.addRow(new Object[]{"192.168.10.30", "KNOWN_VULNERABLE", "CRITICAL", "LOW",
                    "CVE_BANNER_MATCH", "Beispielzeit", "Beispiel: Banner passt zu lokalem CVE-Eintrag"});
        } else {
            reload(model);
        }
        JTable table = TableConfig.buildTable(model, new int[]{120, 150, 80, 100, 180, 150, 360});

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(0, 220));
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(TableConfig.ROW_BG_EVEN);

        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.add(buildToolbar(model), BorderLayout.NORTH);
        outer.add(sp, BorderLayout.CENTER);

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    private static DefaultTableModel buildModel() {
        return new DefaultTableModel(new Object[0][],
                new String[]{"IP", "Kategorie", "Stufe", "Konfidenz", "Quelle", "Beobachtet", "Beleg / Detail"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JPanel buildToolbar(DefaultTableModel model) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBackground(PANEL_BG);

        JTextField ipField = new JTextField(16);
        ipField.setFont(MONO_S);

        JButton tlsBtn   = smallBtn("TLS prüfen");
        JButton credBtn  = smallBtn("Default-Login prüfen");
        JButton exportBtn = smallBtn("CSV exportieren");
        JButton clearBtn = smallBtn("Leeren");

        tlsBtn.addActionListener(e -> runTls(ipField.getText().trim(), model));
        credBtn.addActionListener(e -> runCredentialCheck(ipField.getText().trim(), model));
        exportBtn.addActionListener(e -> exportCsv());
        clearBtn.addActionListener(e -> { SecurityFindingsCollector.getInstance().clear(); reload(model); });

        bar.add(new JLabel("IP:"));
        bar.add(ipField);
        bar.add(tlsBtn);
        bar.add(credBtn);
        bar.add(exportBtn);
        bar.add(clearBtn);
        return bar;
    }

    private static void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("nettool-security-findings.csv"));
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        Path file = chooser.getSelectedFile().toPath();
        if (java.nio.file.Files.exists(file)
                && JOptionPane.showConfirmDialog(null, "Datei überschreiben?",
                "CSV exportieren", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            SecurityFindingsExporter.exportCsv(file, SecurityFindingsCollector.getInstance().getAll());
            AuditLogger.getInstance().log("SECURITY_FINDINGS_EXPORT", file.toString());
            JOptionPane.showMessageDialog(null, "Sicherheitsbefunde exportiert:\n" + file,
                    "CSV exportieren", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "CSV-Export fehlgeschlagen:\n" + e.getMessage(),
                    "CSV exportieren", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void runTls(String ip, DefaultTableModel model) {
        if (ip.isBlank()) return;
        new Thread(() -> {
            TlsCertInspector.inspect(ip).ifPresent(SecurityFindingsCollector.getInstance()::add);
            SwingUtilities.invokeLater(() -> reload(model));
        }, "TlsCheck-" + ip).start();
    }

    /** Aktive Zugangsdaten-Probes laufen erst nach expliziter Bestätigung — nie automatisch. */
    private static void runCredentialCheck(String ip, DefaultTableModel model) {
        if (ip.isBlank()) return;
        int ok = JOptionPane.showConfirmDialog(null,
                "<html>Default-Zugangsdaten für <b>" + ip + "</b> aktiv testen?<br>"
                        + "<small>FTP anonym + HTTP admin/admin.</small></html>",
                "Bestätigung erforderlich", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        new Thread(() -> {
            DefaultCredentialProbe.checkFtpAnonymous(ip).ifPresent(SecurityFindingsCollector.getInstance()::add);
            DefaultCredentialProbe.checkHttpDefaultLogin(ip, "admin", "admin")
                    .ifPresent(SecurityFindingsCollector.getInstance()::add);
            SwingUtilities.invokeLater(() -> reload(model));
        }, "CredCheck-" + ip).start();
    }

    private static void reload(DefaultTableModel model) {
        model.setRowCount(0);
        for (SecurityFinding f : SecurityFindingsCollector.getInstance().getAll())
            model.addRow(toRow(f));
    }

    static Object[] toRow(SecurityFinding finding) {
        return new Object[]{finding.ip(), finding.category(), finding.severity(), finding.confidence(),
                finding.source(), formatObservedAt(finding.observedAt()), finding.detail()};
    }

    static String formatObservedAt(Instant observedAt) {
        return OBSERVED_AT_FORMAT.format(observedAt);
    }

    private static JButton smallBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(ACCENT);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        return b;
    }
}

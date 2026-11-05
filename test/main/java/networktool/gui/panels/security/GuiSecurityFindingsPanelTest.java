package main.java.networktool.gui.panels.security;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.security.SecurityFinding;
import main.java.networktool.logic.analysis.security.SecurityFindingsCollector;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GuiSecurityFindingsPanelTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @AfterEach void clear() { SecurityFindingsCollector.getInstance().clear(); }

    @Test void show_doesNotThrow() {
        GuiOutputPanel output = new GuiOutputPanel();
        assertDoesNotThrow(() -> GuiSecurityFindingsPanel.show(output));
    }

    @Test void rowIncludesSourceConfidenceTimestampAndEvidence() {
        SecurityFinding finding = new SecurityFinding("192.168.10.5", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.CRITICAL, "Banner matches CVE entry",
                SecurityFinding.Source.CVE_BANNER_MATCH, SecurityFinding.Confidence.LOW,
                Instant.parse("2026-10-29T12:00:00Z"));

        Object[] row = GuiSecurityFindingsPanel.toRow(finding);

        assertArrayEquals(new Object[]{"192.168.10.5", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.CRITICAL, SecurityFinding.Confidence.LOW,
                SecurityFinding.Source.CVE_BANNER_MATCH,
                GuiSecurityFindingsPanel.formatObservedAt(finding.observedAt()), "Banner matches CVE entry"}, row);
    }
}

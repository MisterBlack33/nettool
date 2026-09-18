package main.java.networktool.gui.dashboard;

import main.java.networktool.gui.panels.tags.HostTagStore;
import main.java.networktool.logic.analysis.security.FindingsSource;
import main.java.networktool.logic.analysis.security.FindingsSourceRegistry;
import main.java.networktool.logic.analysis.security.SecurityFinding;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiDashboardStatsTest {

    @TempDir Path tmp;

    @AfterEach void clearRegistry() { FindingsSourceRegistry.register(null); }

    @Test void capture_returnsNonNullSnapshot() {
        assertNotNull(GuiDashboardStats.capture());
    }

    @Test void capture_hostCount_nonNegative() {
        assertTrue(GuiDashboardStats.capture().hostCount() >= 0);
    }

    @Test void capture_lastScanLabel_notBlank() {
        assertFalse(GuiDashboardStats.capture().lastScanLabel().isBlank());
    }

    @Test void capture_noFindingsSource_zeroFindings() {
        assertEquals(0, GuiDashboardStats.capture().findingsCount());
    }

    @Test void capture_withRegisteredFindingsSource_reflectsCount() {
        FindingsSource src = () -> List.of(
                new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                        SecurityFinding.Severity.WARN, "expired"));
        FindingsSourceRegistry.register(src);
        assertEquals(1, GuiDashboardStats.capture().findingsCount());
        FindingsSourceRegistry.unregister(src);
    }

    @Test void capture_favoriteCount_reflectsHostTagStore() {
        HostTagStore.getInstance().setDataDir(tmp);
        HostTagStore.getInstance().setFavorite("2.2.2.2", true);
        assertEquals(1, GuiDashboardStats.capture().favoriteCount());
    }
}

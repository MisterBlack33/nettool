package main.java.networktool.gui.components.scan;

import main.java.networktool.model.ScanProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiScanProfilePreviewTest {

    @Test
    void preview_showsTargetsAndEstimatedScanOptions() {
        ScanProfile profile = new ScanProfile("Home");
        profile.cidrs.add("192.168.1.0/24");
        profile.ports.add(443);
        profile.osFilter = "Linux";

        String preview = GuiScanProfileActions.preview(profile);

        assertTrue(preview.contains("192.168.1.0/24"));
        assertTrue(preview.contains("Geschätzte Hosts: 254"));
        assertTrue(preview.contains("Aktive Ports: 1"));
        assertTrue(preview.contains("OS-Filter: Linux"));
    }

    @Test
    void preview_rejectsInvalidAndOversizedRanges() {
        ScanProfile profile = new ScanProfile("Invalid");
        profile.cidrs.add("10.0.0.0/8");
        assertNull(GuiScanProfileActions.preview(profile));

        profile.cidrs.set(0, "bad");
        assertNull(GuiScanProfileActions.preview(profile));
    }

    @Test
    void preview_rejectsProfilesWhoseCombinedTargetsExceedLimit() {
        ScanProfile profile = new ScanProfile("Large");
        profile.cidrs.add("10.0.0.0/16");
        profile.cidrs.add("10.1.0.0/16");

        assertNull(GuiScanProfileActions.preview(profile));
    }

    @Test
    void previewForLocalProfileIdentifiesLocalDiscovery() {
        assertTrue(GuiScanProfileActions.preview(new ScanProfile("Local"))
                .contains("lokales Netzwerk"));
    }
}

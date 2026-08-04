package networktool.gui.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapDeviceSignaturesTest {

    @Test void endDeviceOsListIsNotEmptyAndLowercase() {
        assertFalse(MapDeviceSignatures.ENDDEVICE_OS.isEmpty());
        for (String s : MapDeviceSignatures.ENDDEVICE_OS)
            assertEquals(s.toLowerCase(), s, "Keyword sollte klein geschrieben sein: " + s);
    }

    @Test void endDeviceHostnameListContainsCommonPhoneModels() {
        assertTrue(MapDeviceSignatures.ENDDEVICE_HN.contains("iphone"));
        assertTrue(MapDeviceSignatures.ENDDEVICE_HN.contains("galaxy"));
        assertTrue(MapDeviceSignatures.ENDDEVICE_HN.contains("s23"));
    }

    @Test void switchOuisAreValidFormat() {
        assertFalse(MapDeviceSignatures.SWITCH_OUIS.isEmpty());
        for (String oui : MapDeviceSignatures.SWITCH_OUIS)
            assertTrue(oui.matches("[0-9A-F]{2}(:[0-9A-F]{2}){2}"),
                    "OUI hat falsches Format: " + oui);
    }

    @Test void portSetsDoNotOverlap() {
        // Endgeräte-Ports und Infra-Ports dürfen sich nicht widersprechen
        for (Integer p : MapDeviceSignatures.ENDDEVICE_PORTS)
            assertFalse(MapDeviceSignatures.NETWORK_INFRA_PORTS.contains(p),
                    "Port " + p + " ist sowohl Endgerät als auch Infra markiert");
    }

    @Test void dnsAndDhcpPortsArePresent() {
        assertTrue(MapDeviceSignatures.DNS_DHCP_PORTS.contains(53));
        assertTrue(MapDeviceSignatures.DNS_DHCP_PORTS.contains(67));
    }
}

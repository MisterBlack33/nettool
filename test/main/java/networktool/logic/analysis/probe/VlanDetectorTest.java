package main.java.networktool.logic.analysis.probe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VlanDetectorTest {

    @Test void detect_dotSuffix()          { assertEquals(100, VlanDetector.detect("eth0.100").orElseThrow()); }
    @Test void detect_vlanPrefix()         { assertEquals(20, VlanDetector.detect("vlan20").orElseThrow()); }
    @Test void detect_vlanDotPrefix()      { assertEquals(30, VlanDetector.detect("vlan.30").orElseThrow()); }
    @Test void detect_caseInsensitive()    { assertEquals(5, VlanDetector.detect("VLAN5").orElseThrow()); }
    @Test void detect_null_empty()         { assertTrue(VlanDetector.detect(null).isEmpty()); }
    @Test void detect_blank_empty()        { assertTrue(VlanDetector.detect("  ").isEmpty()); }
    @Test void detect_plainName_empty()    { assertTrue(VlanDetector.detect("eth0").isEmpty()); }
    @Test void detect_outOfRange_empty()   { assertTrue(VlanDetector.detect("eth0.9999").isEmpty()); }
    @Test void detect_zeroVlan_empty()     { assertTrue(VlanDetector.detect("vlan0").isEmpty()); }
    @Test void detect_maxValidVlan()       { assertEquals(4094, VlanDetector.detect("eth0.4094").orElseThrow()); }

    @Test void detectForActiveInterfaces_doesNotThrow() {
        assertDoesNotThrow(VlanDetector::detectForActiveInterfaces);
    }

    @Test void detectForActiveInterfaces_returnsOptional() {
        assertNotNull(VlanDetector.detectForActiveInterfaces());
    }
}

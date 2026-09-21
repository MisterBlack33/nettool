package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubnetDetectorRouteTest {

    @Test void addRoutePrefixes_slash24_addsOnePrefix() {
        List<String> result = new ArrayList<>();
        SubnetDetector.addRoutePrefixes(result, List.of("192.168.178.0/24"));
        assertEquals(List.of("192.168.178"), result);
    }

    @Test void addRoutePrefixes_slash23_addsTwoPrefixes() {
        List<String> result = new ArrayList<>();
        SubnetDetector.addRoutePrefixes(result, List.of("10.0.0.0/23"));
        assertEquals(List.of("10.0.0", "10.0.1"), result);
    }

    @Test void addRoutePrefixes_existingPrefix_notDuplicated() {
        List<String> result = new ArrayList<>(List.of("192.168.178"));
        SubnetDetector.addRoutePrefixes(result, List.of("192.168.178.0/24"));
        assertEquals(1, result.size());
    }

    @Test void addRoutePrefixes_atCapacity_addsNothing() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < SubnetDetector.MAX_SUBNETS; i++) result.add("x" + i);
        SubnetDetector.addRoutePrefixes(result, List.of("10.0.0.0/24"));
        assertEquals(SubnetDetector.MAX_SUBNETS, result.size());
    }

    @Test void addRouteCidrs_addsOnlyNewOnes() {
        List<String> cidrs = new ArrayList<>(List.of("192.168.1.0/24"));
        SubnetDetector.addRouteCidrs(cidrs, List.of("192.168.1.0/24", "10.0.0.0/24"));
        assertEquals(List.of("192.168.1.0/24", "10.0.0.0/24"), cidrs);
    }

    @Test void addRouteCidrs_noRoutes_unchanged() {
        List<String> cidrs = new ArrayList<>(List.of("192.168.1.0/24"));
        SubnetDetector.addRouteCidrs(cidrs, List.of());
        assertEquals(1, cidrs.size());
    }
}
package main.java.networktool.filter;

import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanFilterNullFieldsTest {

    @Test
    void profileFiltersSkipNullOsAndHostnameValues() {
        List<ScanResult> results = List.of(new ScanResult("192.0.2.1", null, Map.of(), null));

        assertTrue(ScanFilter.filterByOS(results, "linux").isEmpty());
        assertTrue(ScanFilter.filterByHostnameRegex(results, "printer").isEmpty());
    }
}

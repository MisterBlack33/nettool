package main.java.networktool.filter;

import main.java.networktool.model.ScanResult;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ScanFilter {

    private ScanFilter() {}

    public static List<ScanResult> filterByOS(List<ScanResult> results, String os) {
        String lower = os.toLowerCase();
        return results.stream()
                .filter(r -> r.getOsGuess().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    public static List<ScanResult> filterByPort(List<ScanResult> results, int port) {
        return results.stream()
                .filter(r -> r.getOpenPorts().containsKey(port))
                .collect(Collectors.toList());
    }

    public static List<ScanResult> filterByHostnameRegex(List<ScanResult> results, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return results.stream()
                .filter(r -> pattern.matcher(r.getHostname()).find())
                .collect(Collectors.toList());
    }

    public static List<ScanResult> filterCombined(List<ScanResult> results, String os, int port) {
        String lower = os.toLowerCase();
        return results.stream()
                .filter(r -> r.getOsGuess().toLowerCase().contains(lower))
                .filter(r -> r.getOpenPorts().containsKey(port))
                .collect(Collectors.toList());
    }
}
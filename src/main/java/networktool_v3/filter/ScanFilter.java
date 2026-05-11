package main.java.networktool_v3.filter;

import main.java.networktool_v3.model.ScanResult;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Filtert {@link ScanResult}-Listen nach OS, Port oder Hostname-Regex.
 */
public final class ScanFilter {

    private ScanFilter() {}

    /** Filtert nach OS-Typ (Teilstring-Vergleich, Groß-/Kleinschreibung egal). */
    public static List<ScanResult> filterByOS(List<ScanResult> results, String os) {
        String osLower = os.toLowerCase();
        return results.stream()
                .filter(r -> r.getOsGuess().toLowerCase().contains(osLower))
                .collect(Collectors.toList());
    }

    /** Filtert nach offenem Port. */
    public static List<ScanResult> filterByPort(List<ScanResult> results, int port) {
        return results.stream()
                .filter(r -> r.getOpenPorts().containsKey(port))
                .collect(Collectors.toList());
    }

    /** Filtert nach Hostname-Regex (Groß-/Kleinschreibung egal). */
    public static List<ScanResult> filterByHostnameRegex(List<ScanResult> results, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return results.stream()
                .filter(r -> pattern.matcher(r.getHostname()).find())
                .collect(Collectors.toList());
    }

    /** Kombinierter Filter: OS-Typ UND offener Port müssen übereinstimmen. */
    public static List<ScanResult> filterCombined(List<ScanResult> results, String os, int port) {
        String osLower = os.toLowerCase();
        return results.stream()
                .filter(r -> r.getOsGuess().toLowerCase().contains(osLower))
                .filter(r -> r.getOpenPorts().containsKey(port))
                .collect(Collectors.toList());
    }
}

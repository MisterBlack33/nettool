package main.java.networktool_v3.filter;

import main.java.networktool_v3.model.HostResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtert eine Liste von {@link HostResult}s nach OS und/oder Hostname.
 */
public final class HostResultFilter {

    private HostResultFilter() {}

    /**
     * Filtert nach OS-Typ und Hostname-Fragment.
     * Ein leerer oder "alle"-Filter wird ignoriert.
     */
    public static List<HostResult> filter(List<HostResult> found,
                                          String osFilter,
                                          String hostnameFilter) {
        boolean filterOs   = isActive(osFilter);
        boolean filterHost = isActive(hostnameFilter);

        List<HostResult> result = new ArrayList<>();
        for (HostResult r : found) {
            boolean osMatch       = !filterOs   || r.os.toLowerCase().contains(osFilter.toLowerCase());
            boolean hostnameMatch = !filterHost || matchesHostname(r, hostnameFilter);
            if (osMatch && hostnameMatch) result.add(r);
        }
        return result;
    }

    /** Erstellt ein beschreibendes Label für die verwendeten Filter. */
    public static String buildLabel(String osFilter, String hostnameFilter) {
        if (!isActive(osFilter) && !isActive(hostnameFilter)) return "Alle Geräte";
        List<String> parts = new ArrayList<>();
        if (isActive(osFilter))       parts.add("OS: " + osFilter);
        if (isActive(hostnameFilter)) parts.add("Hostname: *" + hostnameFilter + "*");
        return "Filter — " + String.join(", ", parts);
    }

    private static boolean matchesHostname(HostResult r, String filter) {
        String pureName = r.hostname.contains(" [")
                ? r.hostname.substring(0, r.hostname.indexOf(" ["))
                : r.hostname;
        return !pureName.equals(r.ip)
                && pureName.toLowerCase().contains(filter.toLowerCase());
    }

    private static boolean isActive(String filter) {
        return filter != null && !filter.isEmpty() && !filter.equalsIgnoreCase("alle");
    }
}

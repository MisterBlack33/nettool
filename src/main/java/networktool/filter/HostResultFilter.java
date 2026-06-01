package main.java.networktool.filter;

import main.java.networktool.model.HostResult;

import java.util.ArrayList;
import java.util.List;

public final class HostResultFilter {

    private HostResultFilter() {}

    public static List<HostResult> filter(List<HostResult> found,
                                          String osFilter,
                                          String hostnameFilter) {
        boolean filterOs   = isActive(osFilter);
        boolean filterHost = isActive(hostnameFilter);

        List<HostResult> result = new ArrayList<>();
        for (HostResult r : found) {
            boolean osMatch   = !filterOs   || r.os.toLowerCase().contains(osFilter.toLowerCase());
            boolean hostMatch = !filterHost || matchesHostname(r, hostnameFilter);
            if (osMatch && hostMatch) result.add(r);
        }
        return result;
    }

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
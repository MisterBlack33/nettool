package main.java.networktool.logic.scan.host;

import main.java.networktool.util.IpValidator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Liest die von Subnet-Routern beworbenen IPv4-Routen aus {@code tailscale status --json}. */
final class TailscaleRouteParser {

    private static final Pattern ROUTE_LIST = Pattern.compile("\"PrimaryRoutes\"\\s*:\\s*\\[([^\\]]*)]");
    private static final Pattern IPV4_CIDR  = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}/\\d{1,2}");

    private TailscaleRouteParser() {}

    static List<String> parse(String statusJson) {
        if (statusJson == null || statusJson.isBlank()) return List.of();
        Set<String> routes = new LinkedHashSet<>();
        Matcher lists = ROUTE_LIST.matcher(statusJson);
        while (lists.find()) collectCidrs(lists.group(1), routes);
        return List.copyOf(routes);
    }

    private static void collectCidrs(String list, Set<String> target) {
        Matcher cidrs = IPV4_CIDR.matcher(list);
        while (cidrs.find()) {
            if (IpValidator.isValidCidr(cidrs.group())) target.add(cidrs.group());
        }
    }
}
package main.java.networktool.logic.analysis.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Bewertet gesehene DHCP-Server gegen eine explizit vertraute Menge.
 * Es gibt keine automatische Baseline: ein Rogue-Server, der zuerst antwortet,
 * würde sonst dauerhaft als vertrauenswürdig gelten.
 */
final class DhcpOfferTracker {

    private final Set<String> trusted  = new HashSet<>();
    private final Set<String> reported = new HashSet<>();

    DhcpOfferTracker() { this(Set.of()); }

    DhcpOfferTracker(Set<String> trustedServers) { trusted.addAll(trustedServers); }

    synchronized void trust(Set<String> servers) { trusted.addAll(servers); }

    synchronized List<SecurityFinding> evaluate(Set<String> servers) {
        return new TreeSet<>(servers).stream()
                .filter(ip -> !trusted.contains(ip))
                .filter(reported::add)
                .map(DhcpOfferTracker::toFinding)
                .toList();
    }

    private static SecurityFinding toFinding(String ip) {
        return new SecurityFinding(ip, SecurityFinding.Category.ROGUE_DHCP,
                SecurityFinding.Severity.CRITICAL, "Unbekannter DHCP-Server antwortet auf Discover");
    }
}

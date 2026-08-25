package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nutzerbezogene Sicht auf gespeicherte Hosts.
 *
 * Host-Daten bleiben zentral in {@link NetworkStore} (ein Eintrag pro
 * Netz+IP), sichtbare Zuordnung zum Nutzer läuft über {@link HostOwnership}.
 * Speichern zwei Nutzer denselben Host aus demselben Netz, existiert er nur
 * einmal, wird aber beiden angezeigt.
 */
public final class UserHostStore {

    private UserHostStore() {}

    public static boolean save(HostResult host, String network, String username) {
        if (host == null || username == null || username.isBlank()) return false;
        boolean saved = NetworkStore.getInstance().save(host, network);
        if (!saved) return false;
        HostOwnership.getInstance().addOwner(network, host.ip, username);
        return true;
    }

    public static void remove(String ip, String network, String username) {
        if (ip == null || username == null) return;
        boolean orphaned = HostOwnership.getInstance().removeOwner(network, ip, username);
        if (orphaned) NetworkStore.getInstance().remove(ip, network);
    }

    public static List<HostResult> getAll(String network, String username) {
        if (username == null) return List.of();
        return NetworkStore.getInstance().getAll(network).stream()
                .filter(h -> HostOwnership.getInstance().isOwnedBy(network, h.ip, username))
                .collect(Collectors.toList());
    }

    /** Alle Hosts des Nutzers über alle Netzwerke hinweg, nach IP dedupliziert. */
    public static List<HostResult> getAllHosts(String username) {
        if (username == null) return List.of();
        Map<String, HostResult> byIp = new LinkedHashMap<>();
        for (String network : NetworkStore.getInstance().getAllNetworkNames()) {
            if (network.equals(NetworkStore.ALL_CATEGORY)) continue;
            for (HostResult h : getAll(network, username)) byIp.putIfAbsent(h.ip, h);
        }
        return new ArrayList<>(byIp.values());
    }

    /** Alle Nutzer, die denselben Host (Netz+IP) ebenfalls gespeichert haben. */
    public static Set<String> getCoOwners(String ip, String network) {
        return HostOwnership.getInstance().getOwners(network, ip);
    }
}
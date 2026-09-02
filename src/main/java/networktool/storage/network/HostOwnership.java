package main.java.networktool.storage.network;

import main.java.networktool.storage.StorageLocations;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trackt welche Nutzer welchen Host (Netzwerk+IP) referenzieren.
 * Speicherort: saves/networkdata (siehe {@link StorageLocations}).
 *
 * Ermöglicht Host-Deduplizierung: derselbe Host im selben Netz wird nur
 * einmal in {@link NetworkStore} gespeichert, aber pro Nutzer über diese
 * Referenzliste sichtbar gemacht (siehe {@link UserHostStore}).
 */
public final class HostOwnership {

    private static final class Holder { static final HostOwnership INSTANCE = new HostOwnership(); }
    public static HostOwnership getInstance() { return Holder.INSTANCE; }

    private static final String SEP = "\u0001";

    private final Map<String, Set<String>> ownersByKey = new ConcurrentHashMap<>();
    private final Path dataDir;

    private HostOwnership() {
        dataDir = StorageLocations.networkData();
        HostOwnershipPersistence.load(dataDir).forEach((k, v) -> {
            Set<String> set = ConcurrentHashMap.newKeySet();
            set.addAll(v);
            ownersByKey.put(k, set);
        });
    }

    public synchronized void addOwner(String network, String ip, String username) {
        if (network == null || ip == null || username == null || username.isBlank()) return;
        ownersByKey.computeIfAbsent(key(network, ip), k -> ConcurrentHashMap.newKeySet()).add(username);
        persist();
    }

    /** @return true wenn der Host danach keine Besitzer mehr hat (Daten können gelöscht werden). */
    public synchronized boolean removeOwner(String network, String ip, String username) {
        Set<String> owners = ownersByKey.get(key(network, ip));
        if (owners == null) return true;
        owners.remove(username);
        boolean orphaned = owners.isEmpty();
        if (orphaned) ownersByKey.remove(key(network, ip));
        persist();
        return orphaned;
    }

    public boolean isOwnedBy(String network, String ip, String username) {
        Set<String> owners = ownersByKey.get(key(network, ip));
        return owners != null && owners.contains(username);
    }

    public Set<String> getOwners(String network, String ip) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(
                ownersByKey.getOrDefault(key(network, ip), Set.of())));
    }

    /** Entfernt alle Referenzen eines gesamten Netzwerks (z.B. bei Netz-Löschung). */
    synchronized void removeAllForNetwork(String network) {
        ownersByKey.keySet().removeIf(k -> k.startsWith(network + SEP));
        persist();
    }

    private void persist() { HostOwnershipPersistence.save(dataDir, ownersByKey); }

    private static String key(String network, String ip) { return network + SEP + ip; }
}
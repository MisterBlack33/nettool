package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class NetworkStore {

    private static final class Holder { static final NetworkStore INSTANCE = new NetworkStore(); }
    public static NetworkStore getInstance() { return Holder.INSTANCE; }

    public static final String ALL_CATEGORY = NetworkRegistry.ALL_CATEGORY;

    /** Networks whose names start with this prefix are JUnit-only and hidden from GUI. */
    static final String TEST_PREFIX = "__junit__";

    private final NetworkRegistry        registry  = new NetworkRegistry();
    private final List<Runnable>         listeners = new ArrayList<>();
    public  final Path dataDir;

    public enum SortField { IP, HOSTNAME, OS }
    private volatile SortField sortField = SortField.IP;
    private volatile boolean   sortAsc   = true;

    private NetworkStore() {
        dataDir = NetworkStorePersistence.resolveDataDir();
        System.out.println("[NetworkStore] " + dataDir.toAbsolutePath());
        try { Files.createDirectories(NetworkStorePersistence.savedDir(dataDir)); }
        catch (IOException ignored) {}
        loadAll();
        registry.ensureDefault();
    }

    public void setSortField(SortField field, boolean asc) {
        sortField = field;
        sortAsc   = asc;
    }

    // ── Network management ────────────────────────────────────────────────

    public synchronized void createNetwork(String name, String prefix) {
        if (registry.create(name, prefix)) persist(safeName(name));
    }

    public synchronized void renameNetwork(String oldName, String newName) {
        String safe = safeName(newName);
        if (registry.rename(oldName, newName, dataDir)) {
            persist(safe);
            notifyListeners();
        }
    }

    public synchronized void deleteNetwork(String name) {
        if (registry.delete(name, dataDir)) {
            regenerateAllFile();
            notifyListeners();
        }
    }

    public synchronized List<String> getNetworkNames() {
        return registry.names().stream()
                .filter(n -> !isTestNetwork(n))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns all network names including test networks (for internal/test use). */
    public synchronized List<String> getAllNetworkNames() {
        return registry.names();
    }

    public synchronized String        getPrefix(String cat)                 { return registry.prefix(cat); }
    public synchronized boolean       ipMatchesNetwork(String ip, String c) { return registry.ipMatches(ip, c); }
    public synchronized List<String>  matchingNetworks(String ip)           { return registry.matchingNetworks(ip); }

    // ── Host management ───────────────────────────────────────────────────

    public synchronized boolean save(HostResult host, String cat) {
        if (host == null || host.ip == null || host.ip.isBlank() || cat.equals(ALL_CATEGORY)) return false;
        if (!registry.contains(cat)) registry.create(cat, "");
        if (!registry.ipMatches(host.ip, cat)) return false;
        boolean isNew = NetworkStoreHostOps.addOrMerge(host.ip, registry.networks(), cat, host);
        persist(cat);
        if (isNew) notifyListeners();
        return true;
    }

    public synchronized void moveHost(String ip, String from, String to) {
        if (from.equals(ALL_CATEGORY) || to.equals(ALL_CATEGORY)) return;
        if (!registry.contains(from) || !registry.contains(to)) return;
        registry.networks().get(from).stream()
                .filter(h -> h.ip.equals(ip)).findFirst().ifPresent(h -> {
                    registry.networks().get(from).remove(h);
                    registry.networks().get(to).add(h);
                    persist(from);
                    persist(to);
                    notifyListeners();
                });
    }

    public synchronized void remove(String ip, String cat) {
        if (cat.equals(ALL_CATEGORY)) { removeFromAll(ip); return; }
        boolean changed = NetworkStoreHostOps.removeFrom(ip, registry.networks(), cat);
        if (changed) { persist(cat); notifyListeners(); }
    }

    public synchronized void removeFromAll(String ip) {
        boolean changed = NetworkStoreHostOps.removeFromAll(ip, registry.networks());
        if (changed) { registry.networks().keySet().forEach(this::persist); notifyListeners(); }
    }

    public synchronized void updateOs(String ip, String cat, String os) {
        NetworkStoreHostOps.updateOs(ip, os, registry.networks());
        persistOwner(ip);
    }

    public synchronized void updateNotes(String ip, String cat, String notes) {
        NetworkStoreHostOps.updateNotes(ip, notes, registry.networks());
        persistOwner(ip);
    }

    /** Returns hosts for given category. Test networks are excluded for GUI (ALL_CATEGORY). */
    public synchronized List<HostResult> getAll(String cat) {
        List<HostResult> raw = cat.equals(ALL_CATEGORY)
                ? visibleHosts()
                : new ArrayList<>(registry.networks().getOrDefault(cat, Collections.emptyList()));
        return NetworkStoreHostOps.sorted(raw, sortField, sortAsc);
    }

    /** Returns all hosts excluding test-network entries (GUI-safe). */
    public synchronized List<HostResult> getAllHosts() {
        return NetworkStoreHostOps.sorted(visibleHosts(), sortField, sortAsc);
    }

    /** Returns ALL hosts including test entries (internal/test use only). */
    public synchronized List<HostResult> getAllHostsInternal() {
        return NetworkStoreHostOps.sorted(
                NetworkStoreHostOps.allMutable(registry.networks()), sortField, sortAsc);
    }

    public synchronized String findNetwork(String ip) {
        return NetworkStoreHostOps.findNetwork(ip, registry.networks());
    }

    public synchronized void addChangeListener(Runnable l) {
        if (l != null) listeners.add(l);
    }

    public List<String> getNtfyTopics()             { return NetworkStorePersistence.loadNtfyTopics(dataDir); }
    public void         saveNtfyTopic(String topic)  { NetworkStorePersistence.saveNtfyTopic(dataDir, topic); }

    // ── Internal ──────────────────────────────────────────────────────────

    static boolean isTestNetwork(String name) {
        return name != null && name.startsWith(TEST_PREFIX);
    }

    private List<HostResult> visibleHosts() {
        Map<String, List<HostResult>> visible = new LinkedHashMap<>();
        registry.networks().forEach((k, v) -> { if (!isTestNetwork(k)) visible.put(k, v); });
        return NetworkStoreHostOps.allMutable(visible);
    }

    private void persistOwner(String ip) {
        String cat = NetworkStoreHostOps.findNetwork(ip, registry.networks());
        if (cat != null) NetworkStorePersistence.saveNetwork(
                dataDir, cat, registry.networks().get(cat), registry.prefix(cat));
    }

    private void loadAll() {
        NetworkStorePersistence.loadAll(dataDir, registry.networks(), registry.prefixes());
        System.out.println("[NetworkStore] " + registry.networks().size() + " Netz(e), "
                + NetworkStoreHostOps.allMutable(registry.networks()).size() + " Hosts.");
    }

    private void persist(String cat) {
        NetworkStorePersistence.saveNetwork(dataDir, cat,
                registry.networks().getOrDefault(cat, Collections.emptyList()),
                registry.prefix(cat));
        regenerateAllFile();
    }

    private void regenerateAllFile() {
        NetworkStorePersistence.saveAllFile(dataDir, registry.networks());
    }

    private void notifyListeners() {
        regenerateAllFile();
        for (Runnable l : listeners) javax.swing.SwingUtilities.invokeLater(l);
    }

    private static String safeName(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-_]", "_").trim();
    }
}
package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class NetworkStore {

    private static final class Holder { static final NetworkStore INSTANCE = new NetworkStore(); }
    public  static NetworkStore getInstance() { return Holder.INSTANCE; }

    public  static final String ALL_CATEGORY = "Alle";
    private static final String DEFAULT_CAT  = "Standard";

    private final Map<String, List<HostResult>> networks  = new LinkedHashMap<>();
    private final Map<String, String>           prefixes  = new LinkedHashMap<>();
    private final List<Runnable>                listeners = new ArrayList<>();
    public  final Path txtDir;

    public enum SortField { IP, HOSTNAME, OS }
    private volatile SortField sortField = SortField.IP;
    private volatile boolean   sortAsc   = true;

    private NetworkStore() {
        txtDir = NetworkStorePersistence.resolveTxtDir();
        System.out.println("[NetworkStore] " + txtDir.toAbsolutePath());
        try { Files.createDirectories(NetworkStorePersistence.savedDir(txtDir)); }
        catch (IOException ignored) {}
        importLegacyIfNeeded();
        loadAll();
        if (networks.isEmpty()) networks.put(DEFAULT_CAT, new ArrayList<>());
        AutoBackup.getInstance().start();
    }

    public void setSortField(SortField field, boolean asc) { sortField = field; sortAsc = asc; }

    // ── Network management ────────────────────────────────────────────────

    public synchronized void createNetwork(String name, String prefix) {
        if (name == null || name.isBlank() || name.equals(ALL_CATEGORY)) return;
        String safe = safe(name);
        if (!networks.containsKey(safe)) {
            networks.put(safe, new ArrayList<>());
            prefixes.put(safe, prefix != null ? prefix.trim() : "");
            persist(safe);
        }
    }

    public synchronized void renameNetwork(String oldName, String newName) {
        if (!networks.containsKey(oldName) || newName == null || newName.isBlank()) return;
        String safe = safe(newName);
        if (networks.containsKey(safe)) return;
        networks.put(safe, networks.remove(oldName));
        prefixes.put(safe, prefixes.remove(oldName));
        try { Files.deleteIfExists(NetworkStorePersistence.savedDir(txtDir)
                .resolve(oldName + NetworkStorePersistence.FILE_EXT)); }
        catch (IOException ignored) {}
        persist(safe);
        notifyListeners();
    }

    public synchronized void deleteNetwork(String name) {
        if (!networks.containsKey(name)) return;
        networks.remove(name); prefixes.remove(name);
        try { Files.deleteIfExists(NetworkStorePersistence.savedDir(txtDir)
                .resolve(name + NetworkStorePersistence.FILE_EXT)); }
        catch (IOException ignored) {}
        if (networks.isEmpty()) networks.put(DEFAULT_CAT, new ArrayList<>());
        regenerateAllFile(); notifyListeners();
    }

    public synchronized List<String> getNetworkNames() {
        List<String> n = new ArrayList<>(); n.add(ALL_CATEGORY); n.addAll(networks.keySet());
        return Collections.unmodifiableList(n);
    }

    public synchronized String  getPrefix(String cat)               { return prefixes.getOrDefault(cat,""); }
    public synchronized boolean ipMatchesNetwork(String ip, String cat) {
        if (cat.equals(ALL_CATEGORY)) return true;
        String p = prefixes.getOrDefault(cat,"");
        return p.isBlank() || (ip != null && ip.startsWith(p));
    }
    public synchronized List<String> matchingNetworks(String ip) {
        return networks.keySet().stream().filter(n -> ipMatchesNetwork(ip,n)).collect(Collectors.toList());
    }

    // ── Host management ───────────────────────────────────────────────────

    /** FIX: AutoBackup außerhalb synchronized → kein Deadlock-Risiko. */
    public boolean save(HostResult host, String cat) {
        boolean isNew;
        synchronized (this) {
            if (host == null || host.ip == null || host.ip.isBlank() || cat.equals(ALL_CATEGORY)) return false;
            if (!networks.containsKey(cat)) createNetwork(cat,"");
            if (!ipMatchesNetwork(host.ip, cat)) return false;
            isNew = NetworkStoreHostOps.addOrMerge(host.ip, networks, cat, host);
            persist(cat);
            if (isNew) notifyListeners();
        }
        if (isNew) AutoBackup.getInstance().triggerNow();
        return true;
    }

    public synchronized void moveHost(String ip, String from, String to) {
        if (from.equals(ALL_CATEGORY)||to.equals(ALL_CATEGORY)) return;
        if (!networks.containsKey(from)||!networks.containsKey(to)) return;
        networks.get(from).stream().filter(h -> h.ip.equals(ip)).findFirst().ifPresent(h -> {
            networks.get(from).remove(h); networks.get(to).add(h);
            persist(from); persist(to); notifyListeners();
        });
    }

    public void remove(String ip, String cat) {
        boolean changed;
        synchronized (this) {
            if (cat.equals(ALL_CATEGORY)) { removeFromAll(ip); return; }
            changed = NetworkStoreHostOps.removeFrom(ip, networks, cat);
            if (changed) { persist(cat); notifyListeners(); }
        }
        if (changed) AutoBackup.getInstance().triggerNow();
    }

    public void removeFromAll(String ip) {
        boolean changed;
        synchronized (this) {
            changed = NetworkStoreHostOps.removeFromAll(ip, networks);
            if (changed) { networks.keySet().forEach(this::persist); notifyListeners(); }
        }
        if (changed) AutoBackup.getInstance().triggerNow();
    }

    public synchronized void updateOs(String ip, String cat, String os) {
        NetworkStoreHostOps.updateOs(ip, os, networks);
        persistOwner(ip);
    }

    public synchronized void updateNotes(String ip, String cat, String notes) {
        NetworkStoreHostOps.updateNotes(ip, notes, networks);
        persistOwner(ip);
    }

    public synchronized List<HostResult> getAll(String cat) {
        List<HostResult> raw = cat.equals(ALL_CATEGORY)
                ? NetworkStoreHostOps.allMutable(networks)
                : new ArrayList<>(networks.getOrDefault(cat, Collections.emptyList()));
        return NetworkStoreHostOps.sorted(raw, sortField, sortAsc);
    }

    public synchronized List<HostResult> getAllHosts() {
        return NetworkStoreHostOps.sorted(NetworkStoreHostOps.allMutable(networks), sortField, sortAsc);
    }

    public synchronized String findNetwork(String ip) {
        return NetworkStoreHostOps.findNetwork(ip, networks);
    }

    public synchronized void addChangeListener(Runnable l) { if (l != null) listeners.add(l); }

    public List<String> getNtfyTopics()             { return NetworkStorePersistence.loadNtfyTopics(txtDir); }
    public void         saveNtfyTopic(String topic) { NetworkStorePersistence.saveNtfyTopic(txtDir, topic); }

    // ── Internal ──────────────────────────────────────────────────────────

    private void persistOwner(String ip) {
        String ownerCat = NetworkStoreHostOps.findNetwork(ip, networks);
        if (ownerCat != null) NetworkStorePersistence.saveNetwork(
                txtDir, ownerCat, networks.get(ownerCat), prefixes.getOrDefault(ownerCat,""));
    }

    private void loadAll() {
        NetworkStorePersistence.loadAll(txtDir, networks, prefixes);
        System.out.println("[NetworkStore] " + networks.size() + " Netz(e), "
                + NetworkStoreHostOps.allMutable(networks).size() + " Hosts.");
    }

    private void persist(String cat) {
        NetworkStorePersistence.saveNetwork(txtDir, cat,
                networks.getOrDefault(cat, Collections.emptyList()),
                prefixes.getOrDefault(cat,""));
        regenerateAllFile();
    }

    private void regenerateAllFile() { NetworkStorePersistence.saveAllFile(txtDir, networks); }

    private void importLegacyIfNeeded() {
        if (!NetworkStorePersistence.needsLegacyImport(txtDir)) return;
        Path legacy = txtDir.resolve(NetworkStorePersistence.LEGACY_FILE);
        if (!Files.exists(legacy)) return;
        networks.put(DEFAULT_CAT, new ArrayList<>());
        NetworkStorePersistence.loadFile(legacy, DEFAULT_CAT, networks);
        persist(DEFAULT_CAT);
    }

    private void notifyListeners() {
        regenerateAllFile();
        for (Runnable l : listeners) javax.swing.SwingUtilities.invokeLater(l);
    }

    private static String safe(String s) {
        return s.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-]","_").trim();
    }
}
package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class NetworkStore {

    private static final class Holder { static final NetworkStore INSTANCE = new NetworkStore(); }
    public static NetworkStore getInstance() { return Holder.INSTANCE; }

    public static final String ALL_CATEGORY = NetworkRegistry.ALL_CATEGORY;

    private final NetworkRegistry        registry  = new NetworkRegistry();
    private final List<Runnable>         listeners = new ArrayList<>();
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
        registry.ensureDefault();
        AutoBackup.getInstance().start();
    }

    public void setSortField(SortField field, boolean asc) {
        sortField = field;
        sortAsc   = asc;
    }

    // ── Network management ────────────────────────────────────────────────

    public synchronized void createNetwork(String name, String prefix) {
        if (registry.create(name, prefix)) persist(name.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-]", "_").trim());
    }

    public synchronized void renameNetwork(String oldName, String newName) {
        String safe = newName != null ? newName.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-]", "_").trim() : null;
        if (registry.rename(oldName, newName, txtDir)) {
            persist(safe);
            notifyListeners();
        }
    }

    public synchronized void deleteNetwork(String name) {
        if (registry.delete(name, txtDir)) {
            regenerateAllFile();
            notifyListeners();
        }
    }

    public synchronized List<String>  getNetworkNames()                    { return registry.names(); }
    public synchronized String        getPrefix(String cat)                 { return registry.prefix(cat); }
    public synchronized boolean       ipMatchesNetwork(String ip, String c) { return registry.ipMatches(ip, c); }
    public synchronized List<String>  matchingNetworks(String ip)           { return registry.matchingNetworks(ip); }

    // ── Host management ───────────────────────────────────────────────────

    public boolean save(HostResult host, String cat) {
        boolean isNew;
        synchronized (this) {
            if (host == null || host.ip == null || host.ip.isBlank() || cat.equals(ALL_CATEGORY)) return false;
            if (!registry.contains(cat)) registry.create(cat, "");
            if (!registry.ipMatches(host.ip, cat)) return false;
            isNew = NetworkStoreHostOps.addOrMerge(host.ip, registry.networks(), cat, host);
            persist(cat);
            if (isNew) notifyListeners();
        }
        if (isNew) AutoBackup.getInstance().triggerNow();
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

    public void remove(String ip, String cat) {
        boolean changed;
        synchronized (this) {
            if (cat.equals(ALL_CATEGORY)) { removeFromAll(ip); return; }
            changed = NetworkStoreHostOps.removeFrom(ip, registry.networks(), cat);
            if (changed) { persist(cat); notifyListeners(); }
        }
        if (changed) AutoBackup.getInstance().triggerNow();
    }

    public void removeFromAll(String ip) {
        boolean changed;
        synchronized (this) {
            changed = NetworkStoreHostOps.removeFromAll(ip, registry.networks());
            if (changed) { registry.networks().keySet().forEach(this::persist); notifyListeners(); }
        }
        if (changed) AutoBackup.getInstance().triggerNow();
    }

    public synchronized void updateOs(String ip, String cat, String os) {
        NetworkStoreHostOps.updateOs(ip, os, registry.networks());
        persistOwner(ip);
    }

    public synchronized void updateNotes(String ip, String cat, String notes) {
        NetworkStoreHostOps.updateNotes(ip, notes, registry.networks());
        persistOwner(ip);
    }

    public synchronized List<HostResult> getAll(String cat) {
        List<HostResult> raw = cat.equals(ALL_CATEGORY)
                ? NetworkStoreHostOps.allMutable(registry.networks())
                : new ArrayList<>(registry.networks().getOrDefault(cat, Collections.emptyList()));
        return NetworkStoreHostOps.sorted(raw, sortField, sortAsc);
    }

    public synchronized List<HostResult> getAllHosts() {
        return NetworkStoreHostOps.sorted(NetworkStoreHostOps.allMutable(registry.networks()), sortField, sortAsc);
    }

    public synchronized String findNetwork(String ip) {
        return NetworkStoreHostOps.findNetwork(ip, registry.networks());
    }

    public synchronized void addChangeListener(Runnable l) {
        if (l != null) listeners.add(l);
    }

    public List<String> getNtfyTopics()             { return NetworkStorePersistence.loadNtfyTopics(txtDir); }
    public void         saveNtfyTopic(String topic)  { NetworkStorePersistence.saveNtfyTopic(txtDir, topic); }

    // ── Internal ──────────────────────────────────────────────────────────

    private void persistOwner(String ip) {
        String cat = NetworkStoreHostOps.findNetwork(ip, registry.networks());
        if (cat != null) NetworkStorePersistence.saveNetwork(
                txtDir, cat, registry.networks().get(cat), registry.prefix(cat));
    }

    private void loadAll() {
        NetworkStorePersistence.loadAll(txtDir, registry.networks(), registry.prefixes());
        System.out.println("[NetworkStore] " + registry.networks().size() + " Netz(e), "
                + NetworkStoreHostOps.allMutable(registry.networks()).size() + " Hosts.");
    }

    private void persist(String cat) {
        NetworkStorePersistence.saveNetwork(txtDir, cat,
                registry.networks().getOrDefault(cat, Collections.emptyList()),
                registry.prefix(cat));
        regenerateAllFile();
    }

    private void regenerateAllFile() {
        NetworkStorePersistence.saveAllFile(txtDir, registry.networks());
    }

    private void importLegacyIfNeeded() {
        if (!NetworkStorePersistence.needsLegacyImport(txtDir)) return;
        Path legacy = txtDir.resolve(NetworkStorePersistence.LEGACY_FILE);
        if (!java.nio.file.Files.exists(legacy)) return;
        registry.networks().put(NetworkRegistry.DEFAULT_CAT, new ArrayList<>());
        NetworkStorePersistence.loadFile(legacy, NetworkRegistry.DEFAULT_CAT, registry.networks());
        persist(NetworkRegistry.DEFAULT_CAT);
    }

    private void notifyListeners() {
        regenerateAllFile();
        for (Runnable l : listeners) javax.swing.SwingUtilities.invokeLater(l);
    }
}
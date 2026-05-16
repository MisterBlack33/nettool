package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

public final class NetworkStore {

    private static final class Holder { static final NetworkStore INSTANCE = new NetworkStore(); }
    public  static NetworkStore getInstance() { return Holder.INSTANCE; }

    public  static final String ALL_CATEGORY  = "Alle";
    private static final String DEFAULT_CAT   = "Standard";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, List<HostResult>> networks  = new LinkedHashMap<>();
    private final Map<String, String>           prefixes  = new LinkedHashMap<>();
    private final List<Runnable>                listeners = new ArrayList<>();
    public  final Path txtDir;

    private NetworkStore() {
        txtDir = NetworkStorePersistence.resolveTxtDir();
        System.out.println("[NetworkStore] " + txtDir.toAbsolutePath());
        try { Files.createDirectories(NetworkStorePersistence.savedDir(txtDir)); }
        catch (IOException ignored) {}
        importLegacyIfNeeded();
        loadAll();
        if (networks.isEmpty()) networks.put(DEFAULT_CAT, new ArrayList<>());
    }

    // ── Network management ────────────────────────────────────────────────

    public synchronized void createNetwork(String name, String prefix) {
        if (name == null || name.isBlank() || name.equals(ALL_CATEGORY)) return;
        String safe = safeName(name);
        if (!networks.containsKey(safe)) {
            networks.put(safe, new ArrayList<>());
            prefixes.put(safe, prefix != null ? prefix.trim() : "");
            persist(safe);
        }
    }

    public synchronized void renameNetwork(String oldName, String newName) {
        if (!networks.containsKey(oldName) || newName == null || newName.isBlank()) return;
        String safe = safeName(newName);
        if (networks.containsKey(safe)) return;
        networks.put(safe, networks.remove(oldName));
        prefixes.put(safe, prefixes.remove(oldName));
        try { Files.deleteIfExists(
                NetworkStorePersistence.savedDir(txtDir).resolve(oldName + NetworkStorePersistence.FILE_EXT));
        } catch (IOException ignored) {}
        persist(safe);
        notifyListeners();
    }

    public synchronized void deleteNetwork(String name) {
        if (!networks.containsKey(name)) return;
        networks.remove(name);
        prefixes.remove(name);
        try { Files.deleteIfExists(
                NetworkStorePersistence.savedDir(txtDir).resolve(name + NetworkStorePersistence.FILE_EXT));
        } catch (IOException ignored) {}
        if (networks.isEmpty()) networks.put(DEFAULT_CAT, new ArrayList<>());
        regenerateAllFile();
        notifyListeners();
    }

    public synchronized List<String> getNetworkNames() {
        List<String> names = new ArrayList<>();
        names.add(ALL_CATEGORY);
        names.addAll(networks.keySet());
        return Collections.unmodifiableList(names);
    }

    public synchronized String getPrefix(String cat) {
        return prefixes.getOrDefault(cat, "");
    }

    public synchronized boolean ipMatchesNetwork(String ip, String cat) {
        if (cat.equals(ALL_CATEGORY)) return true;
        String p = prefixes.getOrDefault(cat, "");
        return p.isBlank() || (ip != null && ip.startsWith(p));
    }

    public synchronized List<String> matchingNetworks(String ip) {
        return networks.keySet().stream()
                .filter(n -> ipMatchesNetwork(ip, n))
                .collect(Collectors.toList());
    }

    // ── Host management ───────────────────────────────────────────────────

    public synchronized boolean save(HostResult host, String cat) {
        if (host == null || host.ip == null || host.ip.isBlank() || cat.equals(ALL_CATEGORY)) return false;
        if (!networks.containsKey(cat)) createNetwork(cat, "");
        if (!ipMatchesNetwork(host.ip, cat)) return false;
        List<HostResult> list = networks.get(cat);
        list.stream().filter(e -> e.ip.equals(host.ip)).findFirst().ifPresent(e -> {
            if (host.ports != null && !host.ports.isEmpty()) e.ports.putAll(host.ports);
        });
        if (list.stream().anyMatch(e -> e.ip.equals(host.ip))) { persist(cat); return true; }
        host.savedAt = LocalDateTime.now().format(DATE_FMT);
        if (host.notes == null) host.notes = "";
        list.add(host);
        persist(cat);
        notifyListeners();
        return true;
    }

    public synchronized void moveHost(String ip, String from, String to) {
        if (from.equals(ALL_CATEGORY) || to.equals(ALL_CATEGORY)) return;
        if (!networks.containsKey(from) || !networks.containsKey(to)) return;
        List<HostResult> src = networks.get(from);
        src.stream().filter(h -> h.ip.equals(ip)).findFirst().ifPresent(h -> {
            src.remove(h);
            networks.get(to).add(h);
            persist(from);
            persist(to);
            notifyListeners();
        });
    }

    public synchronized void remove(String ip, String cat) {
        if (cat.equals(ALL_CATEGORY)) { removeFromAll(ip); return; }
        List<HostResult> list = networks.getOrDefault(cat, Collections.emptyList());
        if (list.removeIf(e -> e.ip.equals(ip))) { persist(cat); notifyListeners(); }
    }

    public synchronized void removeFromAll(String ip) {
        boolean changed = false;
        for (Map.Entry<String, List<HostResult>> e : networks.entrySet()) {
            if (e.getValue().removeIf(h -> h.ip.equals(ip))) {
                persist(e.getKey());
                changed = true;
            }
        }
        if (changed) notifyListeners();
    }

    public synchronized void updateOs(String ip, String cat, String os) {
        allHostsMutable().stream().filter(e -> e.ip.equals(ip)).findFirst().ifPresent(e -> {
            e.os = os != null ? os : "";
            String ownerCat = findNetwork(ip);
            if (ownerCat != null)
                NetworkStorePersistence.saveNetwork(txtDir, ownerCat,
                        networks.get(ownerCat), prefixes.getOrDefault(ownerCat, ""));
        });
    }

    public synchronized void updateNotes(String ip, String cat, String notes) {
        allHostsMutable().stream().filter(e -> e.ip.equals(ip)).findFirst().ifPresent(e -> {
            e.notes = notes != null ? notes : "";
            String ownerCat = findNetwork(ip);
            if (ownerCat != null)
                NetworkStorePersistence.saveNetwork(txtDir, ownerCat,
                        networks.get(ownerCat), prefixes.getOrDefault(ownerCat, ""));
        });
    }

    public synchronized List<HostResult> getAll(String cat) {
        if (cat.equals(ALL_CATEGORY)) return getAllHosts();
        return Collections.unmodifiableList(
                new ArrayList<>(networks.getOrDefault(cat, Collections.emptyList())));
    }

    public synchronized List<HostResult> getAllHosts() {
        Set<String> seen = new LinkedHashSet<>();
        return networks.values().stream().flatMap(Collection::stream)
                .filter(h -> seen.add(h.ip))
                .collect(Collectors.toUnmodifiableList());
    }

    public synchronized String findNetwork(String ip) {
        return networks.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(h -> h.ip.equals(ip)))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public synchronized void addChangeListener(Runnable l) {
        if (l != null) listeners.add(l);
    }

    // ── ntfy topics ───────────────────────────────────────────────────────

    public List<String> getNtfyTopics()             { return NetworkStorePersistence.loadNtfyTopics(txtDir); }
    public void         saveNtfyTopic(String topic)  { NetworkStorePersistence.saveNtfyTopic(txtDir, topic); }

    // ── Internal ──────────────────────────────────────────────────────────

    private void loadAll() {
        NetworkStorePersistence.loadAll(txtDir, networks, prefixes);
        System.out.println("[NetworkStore] " + networks.size() + " network(s), "
                + getAllHosts().size() + " hosts.");
    }

    private void persist(String cat) {
        NetworkStorePersistence.saveNetwork(txtDir, cat,
                networks.getOrDefault(cat, Collections.emptyList()),
                prefixes.getOrDefault(cat, ""));
        regenerateAllFile();
    }

    private void regenerateAllFile() {
        NetworkStorePersistence.saveAllFile(txtDir, networks);
    }

    private void importLegacyIfNeeded() {
        if (!NetworkStorePersistence.needsLegacyImport(txtDir)) return;
        Path legacy = txtDir.resolve(NetworkStorePersistence.LEGACY_FILE);
        if (!Files.exists(legacy)) return;
        System.out.println("[NetworkStore] Importing legacy: " + legacy.getFileName());
        networks.put(DEFAULT_CAT, new ArrayList<>());
        NetworkStorePersistence.loadFile(legacy, DEFAULT_CAT, networks);
        persist(DEFAULT_CAT);
    }

    private void notifyListeners() {
        regenerateAllFile();
        for (Runnable l : listeners) javax.swing.SwingUtilities.invokeLater(l);
    }

    private List<HostResult> allHostsMutable() {
        Set<String> seen = new LinkedHashSet<>();
        return networks.values().stream().flatMap(Collection::stream)
                .filter(h -> seen.add(h.ip)).collect(Collectors.toList());
    }

    private static String safeName(String s) {
        return s.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-]", "_").trim();
    }
}
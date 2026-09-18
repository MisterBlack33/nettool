package main.java.networktool.logic.analysis.security;

import main.java.networktool.storage.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Lokale Banner→CVE-Zuordnung (JSON-Ressource unter resources/, kein Netzwerkzugriff). */
public final class CveLookup {

    private static final String RESOURCE = "/cve-table.json";
    private static final List<Entry> ENTRIES = load();

    private CveLookup() {}

    record Entry(String pattern, String cve, String desc) {}

    public static Optional<SecurityFinding> classify(String ip, String banner) {
        if (banner == null || banner.isBlank()) return Optional.empty();
        for (Entry e : ENTRIES) {
            if (banner.contains(e.pattern())) {
                return Optional.of(new SecurityFinding(ip, SecurityFinding.Category.KNOWN_VULNERABLE,
                        SecurityFinding.Severity.CRITICAL, e.cve() + ": " + e.desc()));
            }
        }
        return Optional.empty();
    }

    static int entryCount() { return ENTRIES.size(); }

    private static List<Entry> load() {
        try (InputStream is = CveLookup.class.getResourceAsStream(RESOURCE)) {
            if (is == null) return List.of();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<Entry> result = new ArrayList<>();
            for (String obj : JsonCodec.extractObjects(json, json.indexOf('['))) {
                String pattern = JsonCodec.extractStr(obj, "pattern");
                String cve     = JsonCodec.extractStr(obj, "cve");
                String desc    = JsonCodec.extractStr(obj, "desc");
                if (pattern != null && cve != null) result.add(new Entry(pattern, cve, desc != null ? desc : ""));
            }
            return List.copyOf(result);
        } catch (IOException e) {
            return List.of();
        }
    }
}

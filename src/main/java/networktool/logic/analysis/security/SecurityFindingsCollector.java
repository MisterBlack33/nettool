package main.java.networktool.logic.analysis.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Sammelt Security-Findings in der lokalen Historie (Singleton).
 * Registriert sich selbst als {@link FindingsSource}, damit Konsumenten
 * (z.B. das Dashboard) reale Daten statt einer leeren Liste sehen.
 */
public final class SecurityFindingsCollector implements FindingsSource {

    private static final SecurityFindingsCollector INSTANCE = new SecurityFindingsCollector();

    static {
        FindingsSourceRegistry.register(INSTANCE);
    }

    public static SecurityFindingsCollector getInstance() { return INSTANCE; }

    private final List<SecurityFinding> findings = Collections.synchronizedList(new ArrayList<>());
    private final SecurityFindingsStore store = new SecurityFindingsStore();

    private SecurityFindingsCollector() {
        try {
            findings.addAll(store.load());
        } catch (IOException e) {
            throw new UncheckedIOException("Security-Findings konnten nicht geladen werden.", e);
        }
    }

    public void add(SecurityFinding finding) {
        if (finding == null) return;
        synchronized (findings) {
            findings.add(finding);
            persist();
        }
    }

    /** Fügt nur hinzu, wenn ein gleiches Finding noch nicht vorhanden ist. */
    public boolean addIfNew(SecurityFinding finding) {
        if (finding == null) return false;
        synchronized (findings) {
            for (int i = 0; i < findings.size(); i++) {
                SecurityFinding existing = findings.get(i);
                if (!sameObservation(existing, finding)) continue;
                if (existing.status() == SecurityFinding.Status.RESOLVED) {
                    findings.set(i, finding.withStatus(SecurityFinding.Status.OPEN));
                    persist();
                    return true;
                }
                return false;
            }
            findings.add(finding);
            persist();
            return true;
        }
    }

    private static boolean sameObservation(SecurityFinding first, SecurityFinding second) {
        return first.ip().equals(second.ip())
                && first.category() == second.category()
                && first.severity() == second.severity()
                && first.detail().equals(second.detail())
                && first.source() == second.source()
                && first.confidence() == second.confidence();
    }

    @Override
    public List<SecurityFinding> getAll() {
        return List.copyOf(findings);
    }

    public void setStatus(SecurityFinding finding, SecurityFinding.Status status) {
        if (finding == null || status == null) return;
        synchronized (findings) {
            int index = findings.indexOf(finding);
            setStatus(index, status);
        }
    }

    public void setStatus(int index, SecurityFinding.Status status) {
        if (status == null) return;
        synchronized (findings) {
            if (index < 0 || index >= findings.size()) return;
            findings.set(index, findings.get(index).withStatus(status));
            persist();
        }
    }

    public void clear() {
        synchronized (findings) {
            findings.clear();
            persist();
        }
    }

    public void reset() {
        synchronized (findings) {
            findings.clear();
            persist();
        }
        FindingsSourceRegistry.reset();
    }

    private void persist() {
        try {
            store.save(findings);
        } catch (IOException e) {
            throw new UncheckedIOException("Security-Findings konnten nicht gespeichert werden.", e);
        }
    }
}

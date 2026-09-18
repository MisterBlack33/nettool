package main.java.networktool.logic.analysis.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sammelt Security-Findings über die laufende GUI-Session (Singleton).
 * Registriert sich selbst als {@link FindingsSource}, damit Konsumenten
 * (z.B. das Dashboard, Workstream C) reale Daten statt einer leeren Liste sehen.
 */
public final class SecurityFindingsCollector implements FindingsSource {

    private static final SecurityFindingsCollector INSTANCE = new SecurityFindingsCollector();

    static {
        FindingsSourceRegistry.register(INSTANCE);
    }

    public static SecurityFindingsCollector getInstance() { return INSTANCE; }

    private final List<SecurityFinding> findings = Collections.synchronizedList(new ArrayList<>());

    private SecurityFindingsCollector() {}

    public void add(SecurityFinding finding) {
        if (finding != null) findings.add(finding);
    }

    @Override
    public List<SecurityFinding> getAll() {
        return List.copyOf(findings);
    }

    public void clear() { findings.clear(); }
}

package main.java.networktool.logic.analysis.security;

import java.util.List;

/** Stabiler Lese-Contract für andere Workstreams (z.B. GUI-Dashboard). */
public interface FindingsSource {
    List<SecurityFinding> getAll();
}

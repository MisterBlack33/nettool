package main.java.networktool_v3.model;

import java.util.*;

/**
 * Speichert ein benanntes Scan-Profil.
 *
 * Enthält alle Parameter für einen wiederholbaren Scan:
 *   name       – Anzeigename (z.B. "Schüler", "Lehrer", "Heim")
 *   cidrs      – Liste von CIDR-Bereichen oder Subnetz-Präfixen
 *   osFilter   – OS-Filter (leer = alle)
 *   hnFilter   – Hostname-Filter (leer = alle)
 *   ports      – eigene Port-Liste (leer = Standard-Ports)
 *   autoSave   – Ergebnisse automatisch in Kategorie speichern
 *   category   – Ziel-Kategorie für autoSave
 *   lastRun    – Zeitstempel des letzten Laufs
 */
public final class ScanProfile {

    public String       name;
    public List<String> cidrs     = new ArrayList<>();
    public String       osFilter  = "";
    public String       hnFilter  = "";
    public List<Integer> ports    = new ArrayList<>();
    public boolean      autoSave  = false;
    public String       category  = "";
    public String       lastRun   = "";

    public ScanProfile(String name) {
        this.name = name;
    }

    /** Kurzdarstellung für Tabelle/Log. */
    public String summary() {
        return name + "  ["
                + (cidrs.isEmpty() ? "lokal" : String.join(", ", cidrs))
                + (osFilter.isBlank() ? "" : "  OS:" + osFilter)
                + (hnFilter.isBlank() ? "" : "  HN:" + hnFilter)
                + "]";
    }
}

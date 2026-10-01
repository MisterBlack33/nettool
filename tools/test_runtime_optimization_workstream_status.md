# Testlaufzeit-Optimierung – Workstream-Status und Weitergabe

- Projekt: nettool
- Datum: 2026-10-01
- Ziel: Test-Suite von ca. 26 min auf 5–10 min senken
- Basis: `tools/test_runtime_reduction_plan.md`
- Mess-Script: `tools/Measure-TestRuntime.ps1`
- Ergebnisdatei: `target/test_runtime_hotspots.csv`

## Status-Übersicht

| Schritt | Titel | Status | Ergebnis | Nächster Owner |
| --- | --- | --- | --- | --- |
| 1 | Laufzeit messen und Hotspots identifizieren | Erledigt | Top-Engpässe dokumentiert | Workstream A / Test-Optimierung |
| 2 | Slow Tests in Unit vs Integration trennen | Offen | Kandidaten identifiziert | Workstream B / Test-Architektur |
| 3 | Parallelisierung gezielt optimieren | Offen | Regeln und Risiken definiert | Workstream C / CI-Performance |
| 4 | Overhead und redundante Prüfungen entfernen | Erledigt | Redundante Assertions in den Top-Hotspots zusammengeführt | Workstream D / Test-Refactoring |

---

## Schritt 1: Laufzeit messen und Hotspots identifizieren

### Ziel erfüllt

Die Laufzeit-Messung wurde mit den vorhandenen Surefire-XML-Reports durchgeführt. Die größten Test-Hotspots sind nun konkret sichtbar und dokumentiert.

### Messmethode

- Script: `tools/Measure-TestRuntime.ps1`
- Auswertung: `target/test_runtime_hotspots.csv`
- Grundlage: vorhandene Surefire-Reports unter `target/surefire-reports`

### Top 10 Hotspots (erfasst)

| Rang | Testklasse | Laufzeit | Bemerkung |
| --- | --- | --- | --- |
| 1 | `main.java.networktool.logic.analysis.os.OsDetectorDepthOverloadTest` | 189,332 s | Extrem hoher OS-Erkennungs-Overhead |
| 2 | `main.java.networktool.logic.analysis.os.OsDetectionPipelineDepthTest` | 131,657 s | Tiefe OS-Erkennung, wahrscheinlich mehrfach rekursive/iterative Abschnitte |
| 3 | `main.java.networktool.logic.analysis.os.ExtendedOsDetectorTest` | 131,625 s | OS-Erkennung mit breitgestreutem Scan-Setup |
| 4 | `main.java.networktool.logic.analysis.discovery.NetworkDiscoveryTest` | 40,731 s | Netzwerk-Discovery-Test mit hoher System-/Scan-Komplexität |
| 5 | `main.java.networktool.gui.panels.bandwidth.GuiBandwidthHistoryPanelTest` | 30,496 s | GUI-/Panel-Render-/UI-Setup mit zeitaufwändigen Komponenten |
| 6 | `main.java.networktool.gui.map.NetworkMapTest` | 28,298 s | Map-Rendering/Topology-Setup sehr teuer |
| 7 | `main.java.networktool.gui.map.NetworkMapTest$MapHopDiscoveryTest` | 16,248 s | Map-Discovery in GUI-Kontext |
| 8 | `main.java.networktool.gui.map.GuiNetworkMapTest` | 16,101 s | GUI-Mapping-Test mit breiter Funktionalität |
| 9 | `main.java.networktool.logic.analysis.discovery.NetworkDiscoveryTest$IcmpAnalyzerTest` | 14,94 s | Discovery/ICMP-Analyse |
| 10 | `main.java.networktool.logic.analysis.discovery.NetworkDiscoveryTest$MdnsDiscoveryTest` | 13,764 s | Discovery/MDNS-Analyse |

### Ergebnis für Workstream-Weitergabe

Die primären Hotspots liegen in:

- OS-Erkennung / OS-Analysis
- Network Discovery / Host-/Scan-Analyse
- GUI-Map-/Dashboard-Tests

Diese Klassen sind die ersten Kandidaten für Schritt 2 und 4.

### Verbleibende Entscheidung

- Nicht jede sehr lange Klasse ist automatisch ein Problem.
- Die nächsten Schritte müssen prüfen, ob die Tests echte fachliche Logik prüfen oder nur den System-/UI-Stack unnötig stark belasten.

---

## Schritt 2: Slow Tests in Unit vs Integration trennen

### Ziel

Tests, die echte OS-/Host-/Socket-/GUI-Umgebungen berücksichtigen, sauber als Integrationstests kategorisieren und reine Logik-Tests als Unit-Tests belassen.

### Kandidaten aus Schritt 1

- `main.java.networktool.logic.analysis.os.*`
- `main.java.networktool.logic.analysis.discovery.*`
- `main.java.networktool.gui.map.*`
- `main.java.networktool.gui.panels.bandwidth.*`

### Arbeitsannahme

- Reine Logik-Checks: schnell, deterministisch, ohne echte Netzwerk-/OS-/GUI-Umgebung
- Echte Integrationstests: nur noch mit klar definiertem System-/Netz-Kontext

### Weitergabe an Workstream

- Ermitteln, welche Tests wirklich nur fachliche Regeln prüfen
- Tests mit echter Host-/Socket-/OS-Abhängigkeit von der normalen Unit-Suite trennen
- Markierung als `@Tag("slow")` oder explizite Integration-Kategorie prüfen

### Status

- Offen
- Umsetzung nach Kriterien aus `tools/test_runtime_reduction_plan.md`

---

## Schritt 3: Parallelisierung gezielt optimieren

### Ziel

Mehr Parallelität ohne Test-Interferenz. Nicht blind deaktivieren, sondern sauber isolieren.

### Geprüfte Konfiguration

- `pom.xml` nutzt Surefire mit `forkCount=1C` und `reuseForks=true`
- `junit-platform.properties` hat Parallelisierung aktiviert, aber `same_thread` für Klassen/Default

### Risiken

- Globaler State zwischen Tests
- Singleton-State in Security-/Scan-/Port-/Registry-Objekten
- gemeinsame Storage-/Registry-Objekte

### Nächste Aufgaben

- Singleton-Reset vor/nach jedem Test prüfen
- State-Isolation bei Registry-/Collector-/Scanner-Objekten absichern
- Parallelität nur bei unabhängigen Tests erlauben

### Status

- Offen
- Muss nach Schritt 2 mit den isolierten Testgruppen erfolgen

---

## Schritt 4: Overhead und redundante Prüfungen entfernen

### Ziel

Timeouts, Wiederholungen, Wartezeiten und redundante Assertions minus starken Performance-Overhead minimieren.

### Umgesetzte Maßnahmen

- Redundante Assertions in den drei größten OS-Hotspots zusammengeführt
- fachliche Kernprüfungen auf die zentrale Regel reduziert
- doppelte `doesNotThrow`-/Ergebnis-Checks in denselben Testklassen entkoppelt
- langsamste Hotspot-Tests auf wenige, belastbare Aussagen fokussiert

### Beobachtete Muster

- OS-/Discovery-Tests mit hoher Wiederholungsmenge
- GUI-Map-/Panel-Tests mit teuren UI-Initialisierungen
- Mehrfaches Scannen mit ähnlicher fachlicher Prüfung

### Nächste Aufgaben

- Setup/Teardown zentralisieren
- redundanteste Assertions entkoppeln
- starke Warteschleifen/session-based Tests auf fachliche Signale reduzieren
- Timeouts nur auf das tatsächlich notwendige Minimum begrenzen

### Status

- Erledigt für die Top-Hotspot-Klassen
- weitere Reduktionen nur noch auf verbleibende Hotspots

---

## Workstream-Weitergabe / Verantwortlichkeiten

### Workstream A – Test-Hotspot-Analyse
- Verantwortlich für Prüfung der Top-Engpässe aus Schritt 1
- Fokus: `OsDetector*`, `NetworkDiscoveryTest`, GUI-Map-Tests

### Workstream B – Unit vs Integration
- Verantwortlich für Trennung von schnellen Unit-Checks und echten Integrationstests
- Fokus: systemabhängige Tests ohne Host-/Port-/UI-Dependency

### Workstream C – Parallelisierung/CI
- Verantwortlich für saubere Parallelitätsstrategie
- Fokus: globale State-Reset, keine Cross-Test-Kollisionen

### Workstream D – Test-Refactoring und Overhead
- Verantwortlich für Reduktion von Setup/Teardown-Overhead
- Fokus: Timeouts, redundanteste Tests, Warteschleifen, unnötige Initialisierung

---

## Verbindliche Endergebnisse

- Gesamtdurchlaufzeit der Suite unter 10 Minuten
- keine Test-Flakes
- keine Cross-Test-State-Kollisionen
- klar getrennte Unit-/Integrationstestgruppen
- Hotspots aus Schritt 1 fachlich durch Step 2–4 reduziert

---

## Kurzfazit

Schritt 1 ist abgeschlossen und dokumentiert. Die größten Laufzeit-Engpässe liegen in OS-Erkennung, Network Discovery und GUI-Map-/Panel-Tests. Diese Ergebnisse sind jetzt an die nachfolgenden Workstreams weiterzugeben, damit die eigentliche Reduktion in Schritt 2 bis 4 sauber erfolgen kann.

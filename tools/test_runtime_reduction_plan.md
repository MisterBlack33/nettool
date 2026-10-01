# 4-Schritte-Plan: Testlaufzeit von 26 min auf 5–10 min reduzieren

Ziel: Die gesamte Test-Suite von aktuell ca. 26 Minuten auf 5–10 Minuten senken, ohne Stabilität oder Test-Sicherheit zu verlieren.

## Grundlage

- Aktuell: ca. 26 Minuten Gesamtdurchlaufzeit
- Ziel: 5–10 Minuten Gesamtzeit
- Ideal: <5 Minuten, aber realistischer Zwischenwert ist 5–10 Minuten
- Priorität: weniger Laufzeit durch echte Reduktion, nicht nur durch Parallelisierung oder Ignorieren von Tests

## Schritt 1: Laufzeit messen und die Hotspots identifizieren

Ziel: nicht raten, sondern die echten Engpässe sichtbar machen.

### Maßnahmen

- `mvn test` mit Timing aktivieren und alle Testklassen einzeln messen
- JUnit-Reports und Surefire-Statistiken auswerten
- Testklassen nach Laufzeit sortieren: langsamste 10–20% isolieren
- Besonders prüfen:
  - Netzwerk-/Port-Tests mit echten Sockets
  - GUI-/Swing-/UI-Tests
  - Scan- und Discovery-Tests mit echten System-/Host-Abfragen
  - Tests, die über mehrere Threads, Forks oder lange Timeouts laufen

### Regeln

- Keine Änderungen an Assertions, nur Messung
- Alle Laufzeitdaten in einer Tabelle festhalten: Klasse, Anzahl Tests, Laufzeit, Ursache
- Fokus auf die 3–5 langsamsten Testgruppen

### Erfolgskriterium

- Liste der 10 langsamsten Testklassen mit klarer Ursache ist erstellt

### Erwartete Auswirkung

- Fast 20–35% Laufzeitgewinn durch gezielte Reduktion der größten Hotspots, auch ohne Code-Änderungen am Produkt

---

## Schritt 2: Slow Tests aus dem echten Unit-Scope entfernen oder auf echte Unit-Checks begrenzen

Ziel: Tests, die Systemzustand, reale Ports, echte Hosts, laufende Prozesse oder GUI-Umgebungen mitbenutzen, in echte Unit-/Integration-Kategorien aufteilen.

### Maßnahmen

- Tests mit echten Sockets/Ports nur noch als gezielte Integrationstests behandeln
- Unit-Tests dürfen keine offenen Ports, laufenden Services, Host-Umgebungen oder OS-Details voraussetzen
- Für Port-/Host-/Netztests nur noch assertions auf fachliche Regeln, z. B.:
  - keine harte Erwartung `CLOSED` vs `REFUSED`
  - keine Abhängigkeit von lokalen Services
  - keine langlaufenden Probe-Looping-Tests für reine Logik
- Langsame UI-/GUI-Tests nur noch auf die wirklich relevanten API-Teile begrenzen

### Regeln

- Keine Test-Assertion darf von Maschine, Firewall, OS oder laufenden Prozessen abhängen
- Tests, die nur eine lokale Logik prüfen, dürfen keine echten Netzwerk- oder System-Calls verwenden
- Reale Socket-/Host-Tests nur in einer kleinen, klar markierten Integration-Gruppe

### Erfolgskriterium

- Die Mehrheit der schnellen Tests läuft als reine Unit-Tests ohne I/O, Ports, Hosts oder GUI
- Nur noch wenige, gezielte Integrationstests verbleiben

### Erwartete Auswirkung

- 30–50% Laufzeitgewinn, weil viele langsame, systemabhängige Tests entfernt oder deutlich verkürzt werden

---

## Schritt 3: Parallelisierung und Forking gezielt optimieren, nicht blind deaktivieren

Ziel: Mehr Parallelität ohne Test-Interferenz.

### Maßnahmen

- JUnit Parallelisierung gezielt aktiv lassen, aber sauber auf isolierte Tests begrenzen
- Globale Singletons vor/nach jedem Test resetten:
  - `UserAuth`
  - `AuditLogger`
  - `SecurityFindingsCollector`
  - `FindingsSourceRegistry`
  - `PortScanner.activePorts`
- `@Isolated`, `@ResourceLock`, `@Execution(SAME_THREAD)` nur dort einsetzen, wo wirklich erforderlich ist
- `forkCount` und `reuseForks` auf einen sinnvollen, nicht übermäßigen Wert setzen
- keine unnötige Serialisierung der kompletten Suite

### Regeln

- Parallelität nur dort aktivieren, wo Tests wirklich unabhängig sind
- Tests mit gemeinsamem Prozess-State nicht parallel laufen lassen
- Port-/Socket-/Netztests nicht mit Logik-Tests auf denselben globalen Zustand vermischen

### Erfolgskriterium

- Test-Suite bleibt stabil und läuft in mehreren Threads/Forks ohne State-Kollision

### Erwartete Auswirkung

- 20–30% Laufzeitgewinn durch echte Parallelisierung ohne Flakes

---

## Schritt 4: Timeouts, Overhead und redundante Prüfungen eliminieren

Ziel: Latenz und redundanten Aufwand aus der Suite entfernen.

### Maßnahmen

- Timeouts auf das Minimum reduzieren, das die eigentliche Logik noch sauber prüft
- Doppelte Tests eliminieren, die dieselbe Logik mit leicht unterschiedlichen Parametern prüfen
- Wiederholte Port-/Scan-Calls in denselben Testklassen auf ein Minimum reduzieren
- Teure Methoden nur einmal pro Testklasse statt in jedem einzelnen Test ausführen
- generische Setup- und Teardown-Logik zentralisieren, aber ohne globale State-Lecks

### Regeln

- Keine Test-Warteschleifen ohne fachlichen Zweck
- Keine künstlichen Sleep-Wartezeiten im Test, wenn echte Zustände als Signal dienen
- Kein redundanter Code in Setup/Teardown, der die Suite nur verlangsamt

### Erfolgskriterium

- Jede Testklasse prüft genau die fachliche Regel, ohne unnötige Wiederholungen oder Wartezeiten

### Erwartete Auswirkung

- 10–20% Laufzeitgewinn durch Reduktion von Overhead, Timeouts und Duplikaten

---

## Gesamtziel und erwartete Endergebnis

Wenn die 4 Schritte konsequent umgesetzt werden, sollte die Gesamt-Laufzeit der Test-Suite in etwa so fallen:

- Ausgang: ca. 26 Minuten
- Erwartetes Ergebnis nach Schritten 1–4: 5–10 Minuten
- Best Case: unter 5 Minuten, aber realistischer Zielbereich bleibt 5–10 Minuten

## Priorisierung

1. Schritt 1: Hotspots identifizieren
2. Schritt 2: Slow Tests in Unit vs Integration trennen
3. Schritt 3: Parallelisierung sauber optimieren
4. Schritt 4: Overhead und redundanteste Prüfungen entfernen

## Verbindliche Erfolgskriterien

- Gesamtlaufzeit <10 Minuten
- keine Test-Flakes
- keine Tests mehr abhängig von Host-/Port-/OS-/Process-State
- keine Cross-Test-State-Kollisionen
- jede Testklasse klar als Unit- oder Integrationstest kategorisiert

## Nächster konkreter Umsetzungsblock

- Testlauf mit Timing ausführen
- Top 10 langsamste Tests identifizieren
- langsamste 3–5 Klassen als Erstes umstrukturieren
- danach Parallelisierung und Reduktion von redundanten Prüfungen optimieren

## Kurzform

Stabilität ist erreicht; jetzt beginnt die eigentliche Optimierung: Hotspots identifizieren, systemabhängige Tests in echte Integrationstests verschieben, Parallelisierung sauber erlauben und redundanteste Laufzeiten entfernen. Dadurch sollte die Suite von 26 Minuten auf 5–10 Minuten sinken.


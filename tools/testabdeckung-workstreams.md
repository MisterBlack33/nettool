# Plan: Testabdeckung auf über 90 %

## Ziel und Ausgangslage

Ziel ist eine belastbare Gesamt-Line-Coverage von mehr als 90 % mit JaCoCo, ohne reine
Dummy-Tests und ohne eine Absenkung des bestehenden Qualitätsniveaus.

Die letzte aufgezeichnete Messung vom 21.09.2026 zeigt:

- Gesamt-Line-Coverage: **47,52 %** (`5744 / 12088`)
- Gesamt-Branch-Coverage: **44,24 %** (`2687 / 6074`)
- Gesamt-Method-Coverage: **54,20 %** (`1478 / 2727`)
- Besonders große Lücken: `gui` **29,15 %**, `transfer` **21,25 %**, `util` **46,42 %**
- Bereits stark abgedeckt: `model` **100 %**, `logging` **90,62 %**, `storage` **78,98 %**

Das Projekt verwendet JUnit 5, Maven und JaCoCo. Das bestehende Coverage-Gate in
`pom.xml` verlangt bereits mindestens 90 % Line-Coverage je geprüftem Package.

## Arbeitsweise für parallele Bearbeitung

Die vier Workstreams können gleichzeitig bearbeitet werden, weil sie jeweils
unterschiedliche Quell- und Testbereiche besitzen. Jeder Workstream arbeitet auf
einem eigenen Branch:

```text
coverage/ws-a-util-transfer
coverage/ws-b-logic-security
coverage/ws-c-gui
coverage/ws-d-gate-ci
```

Gemeinsame Regeln:

1. Bestehende Tests nicht löschen oder abschwächen.
2. Neue Tests mit JUnit 5 und vorhandenen Test-Hilfsklassen schreiben.
3. Externe Systeme, Netzwerkzugriffe und echte GUI-Anzeigen deterministisch isolieren.
4. Für jede neue Testgruppe mindestens Happy Path, Fehlerpfad und relevante Grenzfälle abdecken.
5. Nach jedem sinnvollen Paketfortschritt `mvn test` und anschließend den JaCoCo-Bericht ausführen.
6. Änderungen an gemeinsam genutzten Test-Hilfen früh kommunizieren und klein halten.
7. Keine Coverage-Ausnahmen hinzufügen, wenn der Code sinnvoll testbar ist.

---

## Workstream A: Util, Filter, Model und Transfer

**Verantwortung:** Kleine, deterministische Fachkomponenten und Netzwerktransfer

**Quellbereiche:**

- `src/main/java/networktool/util`
- `src/main/java/networktool/filter`
- `src/main/java/networktool/model`
- `src/main/java/networktool/transfer`

**Bestehende Ausgangslage:**

- `util`: 46,42 % Line-Coverage
- `filter`: 84,47 % Line-Coverage
- `model`: 100 % Line-Coverage
- `transfer`: 21,25 % Line-Coverage

**Aufgaben:**

- Bestehende Utility-Tests auf fehlende Grenzwerte und Exception-Pfade prüfen.
- Für `CIDRUtils`, IP-/IPv6-Validierung, Plattformlogik und `SafeCommand`
  parameterisierte Tests für gültige, ungültige und extreme Eingaben ergänzen.
- Für Filter und Renderer leere Ergebnisse, mehrere Treffer, Sonderzeichen und
  unbekannte Filterwerte testen.
- Transfer-Komponenten mit lokalen Test-Doubles testen:
  - erfolgreicher Verbindungsaufbau
  - Timeout und Verbindungsabbruch
  - ungültige Dateien und Berechtigungsfehler
  - Teilübertragung und sauberes Ressourcen-Schließen
- Öffentliche API-Verträge durch Assertions auf Rückgabewerte und Exceptions absichern.

**Ergebnis:**

- `util`, `filter`, `model` und `transfer` jeweils mindestens 90 % Line-Coverage
  oder dokumentierte, technisch nicht erreichbare Restpfade.
- Keine echten Netzwerk- oder Dateiübertragungen im Standard-Testlauf.

**Abnahmekriterien:**

- Alle zugeordneten Tests laufen mit `mvn -Dtest=... test` reproduzierbar.
- Transfer-Fehlerfälle werden explizit geprüft und nicht durch breite Catch-Blöcke
  verschluckt.

---

## Workstream B: Logic, Security und Windows-Adapter

**Verantwortung:** Fachlogik, Sicherheitslogik und Betriebssystem-/Netzwerkadapter

**Quellbereiche:**

- `src/main/java/networktool/logic`
- `src/main/java/networktool/security`

**Bestehende Ausgangslage:**

- `logic`: 57,17 % Line-Coverage
- `security`: 75,58 % Line-Coverage

**Aufgaben:**

- Zuerst die noch ungetesteten Klassen und Methoden aus dem JaCoCo-HTML-Bericht
  nach fehlenden Branches priorisieren.
- Scan-, Analyse- und Scheduler-Logik mit kontrollierten Test-Doubles testen:
  - erfolgreicher Scan
  - leere oder unvollständige Antworten
  - Timeout, Retry und Abbruch
  - parallele Ausführung und Statusübergänge
- Parser und Protokollcode mit gültigen, verkürzten, fehlerhaften und unbekannten
  Eingaben testen, insbesondere SNMP-, Route-, ARP- und Windows-Resolver.
- Sicherheitskomponenten abdecken:
  - Authentifizierung und Passwortregeln
  - Lockout- und Session-Abläufe
  - Audit-Logging
  - TLS-/Credential-Fehler
  - Duplikat- und Prioritätslogik bei Findings
- Zeit, Zufall, Prozesse und Netzwerkzugriffe über vorhandene Injektionspunkte
  deterministisch machen; keine Betriebssystemannahmen im Test voraussetzen.

**Ergebnis:**

- `logic` und `security` jeweils mindestens 90 % Line-Coverage.
- Kritische Branches für Fehlerbehandlung, Security-Failsafe und Abbruchpfade sind
  durch Assertions verifiziert.

**Abnahmekriterien:**

- Tests laufen auf Windows und in einer headless CI-Umgebung.
- Kein Test benötigt einen erreichbaren Host, installierte Fremdsoftware oder
  Administratorrechte.

---

## Workstream C: GUI und Theme

**Verantwortung:** Swing-Komponenten, GUI-Actions, Panels, Renderer und Theme-Code

**Quellbereiche:**

- `src/main/java/networktool/gui`
- `src/main/java/networktool/theme`

**Bestehende Ausgangslage:**

- `gui`: 29,15 % Line-Coverage
- `theme`: 50,00 % Line-Coverage

**Aufgaben:**

- GUI-Code in testbare Einheiten aufteilen, ohne das Verhalten der Anwendung zu
  verändern: Konstruktoren, Event-Handler, Renderer und Datenaufbereitung getrennt
  prüfen.
- Headless-Swing-Tests mit `java.awt.headless=true` für folgende Bereiche ergänzen:
  - Tabellen-, Karten-, Dashboard- und Status-Renderer
  - Menüs, Actions und Enable-/Disable-Zustände
  - Panels mit leerem, vollständigem und fehlerhaftem Datenbestand
  - Login-Rate-Limiter und Validierungslogik
  - Export-, Scan- und Dialog-Actions über Test-Doubles
- Theme-Tests für helle/dunkle Palette, Farben, Fonts und Null-/Fallback-Werte
  ergänzen.
- Reine Dialog-Wrapper, die ohne UI-Test-Framework nicht sinnvoll testbar sind,
  getrennt dokumentieren; vorhandene JaCoCo-Ausnahme für
  `networktool.gui.login` nur beibehalten, nicht ausweiten.
- EDT-Regeln einhalten: UI-Erzeugung und Events auf dem Event Dispatch Thread,
  Assertions nach Abschluss des Events.

**Ergebnis:**

- `theme` mindestens 90 % Line-Coverage.
- `gui` zunächst mindestens 75 %, danach schrittweise auf mindestens 90 % für
  testbare Packages erhöhen.
- Keine flakey Tests durch sichtbare Fenster, Sleeps oder globale Zustände.

**Abnahmekriterien:**

- Standardlauf bleibt headless-fähig.
- GUI-Tests laufen wiederholbar mindestens fünfmal hintereinander.
- Jede verbleibende GUI-Ausnahme ist im JaCoCo-Setup und in der Testdokumentation
  fachlich begründet.

---

## Workstream D: Coverage-Gate, Reporting und Integration

**Verantwortung:** Messbarkeit, CI, gemeinsame Test-Infrastruktur und Endintegration

**Quellbereiche und Konfiguration:**

- `pom.xml`
- `.github`
- `tools/New-CoverageChart.ps1`
- `tools/Update-TestCoverageHistory.ps1`
- gemeinsame Test-Ressourcen und Test-Hilfsklassen

**Aufgaben:**

- Baseline und Messlauf standardisieren:
  - `mvn clean verify`
  - JaCoCo-Report unter `target/site/jacoco`
  - Coverage-Historie über vorhandene PowerShell-Skripte aktualisieren
- Prüfen, dass das 90-%-Gate die beabsichtigten Packages erfasst und Testklassen
  nicht als Produktivcode bewertet.
- CI-Schritt ergänzen oder korrigieren, der `mvn verify` bei Unterschreitung
  zuverlässig fehlschlagen lässt und den JaCoCo-Bericht als Artifact ablegt.
- Coverage-Deltas zwischen Baseline und Pull Request sichtbar machen.
- Gemeinsame Test-Ressourcen für temporäre Verzeichnisse, Testdaten,
  Fake-Transporte und deterministische Zeitquellen bereitstellen.
- Vollständige Integration der drei fachlichen Workstreams durchführen und
  Konflikte in gemeinsamen Dateien auflösen.
- Optional nach Erreichen der 90-%-Line-Coverage einen Mutation-Test-Lauf im
  vorhandenen `mutation`-Profil vorbereiten.

**Ergebnis:**

- `mvn clean verify` ist der verbindliche Qualitätscheck.
- Gesamt-Line-Coverage liegt über 90 %.
- CI verhindert Regressionen unter den Zielwert.
- Coverage-Historie enthält einen nachvollziehbaren neuen Lauf.

**Abnahmekriterien:**

- Standard-Build, Coverage-Report und CI verwenden dieselbe Testauswahl.
- Das Gate schlägt bei einer absichtlichen Unterschreitung reproduzierbar fehl.
- Keine unnötige globale Ausnahmeregel im JaCoCo-Setup.

---

## Gemeinsame Meilensteine

| Meilenstein | Ziel | Verantwortlich |
|---|---|---|
| M1 | Baseline, fehlende Klassen und Branches je Workstream inventarisiert | D + alle |
| M2 | A: Util/Filter/Model/Transfer mindestens 80 % | A |
| M3 | B: Logic/Security mindestens 80 % | B |
| M4 | C: GUI/Theme testbare Bereiche mindestens 70 % | C |
| M5 | Alle Workstreams liefern ihre Branches, Einzelpakete mindestens 90 % | A/B/C |
| M6 | Integration, CI-Gate und Gesamt-Line-Coverage über 90 % | D + alle |

## Übergaben und Konfliktregeln

- Workstreams committen ausschließlich Änderungen in ihrem Scope; gemeinsame
  Test-Hilfen werden vorher abgestimmt.
- Workstream D pflegt die Baseline und führt den finalen Integrationslauf aus.
- Bei Konflikten zwischen höherer Coverage und beobachtbarem Produktionsverhalten
  gilt das Produktionsverhalten; zuerst wird der Test korrigiert, nicht das Gate.
- Ein Workstream gilt erst als abgeschlossen, wenn Tests, JaCoCo-Auswertung und
  Abnahmekriterien erfüllt sind.

## Verbindliche Abschlussprüfung

```powershell
mvn clean verify
```

Danach werden `target/site/jacoco/index.html`, die Package-Werte und der aktualisierte
Eintrag in `test_coverage_history.csv` geprüft. Der Plan ist abgeschlossen, wenn die
Gesamt-Line-Coverage über 90 % liegt und der nächste CI-Lauf denselben Wert mindestens
als Gate erzwingt.

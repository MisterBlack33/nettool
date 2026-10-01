# Roadmap: vollständigen Testlauf auf ca. 10 Minuten verkürzen

## Ziel und Leitplanken

- **Ausgangslage:** Ein vollständiger Testlauf dauert aktuell mehr als 25 Minuten.
- **Ziel:** Die vollständige Suite einschließlich aller bisher enthaltenen Tests in höchstens ca. 10 Minuten ausführen.
- **Qualität:** Keine Assertions entfernen oder Tests nur zur Laufzeitverkürzung aus dem vollständigen Lauf ausschließen. Das Ergebnis muss stabil und reproduzierbar bleiben.
- **Messbarkeit:** Jeder Testlauf erhält seine eigene Laufzeit. Historische Chart-Punkte dürfen nie die Laufzeit des aktuellsten Laufs übernehmen.

Die Laufzeiten in `target/test_runtime_hotspots.csv` sind eine Momentaufnahme und können von der aktuellen Baseline abweichen. Vor jeder Optimierung ist daher eine frische Messung erforderlich.

## Aktueller Stand und Hinweise

- `tools/Measure-TestRuntime.ps1` liest Laufzeiten aus den Surefire-XML-Berichten und listet die langsamsten Testklassen auf.
- Die erfasste Momentaufnahme zeigt unter anderem `IpInspectorExtTest` (454 s), `OsDetectorDepthOverloadTest` (176 s), `PsPortScanResolverTest` (148 s) und `OsDetectionPipelineDepthTest` (129 s) als Kandidaten. Ursache und Aktualität vor Änderungen erneut prüfen.
- In `pom.xml` ist Surefire mit `forkCount=1C` konfiguriert. JUnit ist zwar aktiviert, führt Klassen und Methoden mit der aktuellen Einstellung aber standardmäßig seriell aus (`same_thread`).
- `tools/Update-TestCoverageHistory.ps1` hängt Laufzeitdaten an die Coverage-CSV an. Die bestehende CSV kann noch das ältere Schema ohne `total_runtime_sec` enthalten.
- `tools/New-CoverageChart.ps1` liest Laufzeiten aus historischen CSV-Zeilen, fällt bei fehlenden historischen Werten aber auf die gerade vorhandenen Surefire-Reports zurück. Damit kann dieselbe aktuelle Laufzeit fälschlich an mehrere ältere Chart-Punkte gelangen.

## Phasenplan

### 1. Verlässliche Baseline und Laufzeit-Historie herstellen

**Maßnahmen**

- Einen vollständigen Lauf auf der Referenzumgebung durchführen und Wandzeit, Anzahl Tests, Ergebnis, JDK, Betriebssystem, Commit und Maven-Befehl festhalten.
- Surefire-XML-Berichte für Klassenlaufzeiten und den gesamten Lauf auswerten; die Top-20-Liste als Diagnose verwenden, nicht als alleinige Gesamtlaufzeit.
- Für jeden Testlauf eine eindeutige Lauf-ID und einen Zeitstempel vergeben. Laufzeit, Testanzahl, Fehler, übersprungene Tests, Commit und Ausführungsprofil als eigenen Datensatz speichern.
- Das Runtime-Historienformat versioniert und additiv erweitern. Alte Coverage-Historie ohne Laufzeitwerte als „nicht verfügbar“ behandeln; keine Laufzeit nachträglich aus dem neuesten `target`-Ordner ergänzen.
- Chart-Daten anhand der Lauf-ID verbinden. Fehlende historische Laufzeit bleibt leer („–“); niemals den neuesten Messwert auf frühere Datenpunkte verteilen.

**Abnahme**

- Zwei nacheinander ausgeführte Läufe erzeugen zwei unterschiedliche Laufzeitwerte, die auch in den jeweiligen HTML-Chart-Punkten erscheinen.
- Das erneute Erzeugen des HTML-Berichts verändert historische Laufzeitwerte nicht.
- Fehlende Laufzeitdaten alter Läufe bleiben leer und werden nicht durch den aktuellen Lauf ersetzt.

### 2. Hotspots reproduzieren und Ursachen beseitigen

**Maßnahmen**

- Die frische Top-Liste prüfen und für die größten Klassen nach tatsächlichen Wartezeiten, I/O, Sleeps, Retries, Timeouts, externen Prozessen und wiederholtem Setup suchen.
- Netzwerk-, Port-, OS- und Discovery-Tests auf echte externe Abhängigkeiten untersuchen. Reine Fachlogik mit vorhandenen Test-Doubles prüfen; reale Systemintegration nur dort behalten, wo sie fachlich nötig ist.
- Langsame Tests so umbauen, dass sie Zustände deterministisch simulieren, statt reale Timeouts abzuwarten. Sleeps nur durch belastbare Zustands-/Synchronisationssignale ersetzen.
- Teures gemeinsames Setup innerhalb einer Klasse wiederverwenden, sofern dadurch keine Tests voneinander abhängig werden.
- Optimierungen für jede Hotspot-Klasse einzeln messen und mit den bisherigen Assertions absichern.

**Priorität für die erste Analyse**

1. `IpInspectorExtTest`
2. `OsDetectorDepthOverloadTest`
3. `PsPortScanResolverTest`
4. `OsDetectionPipelineDepthTest`
5. weitere Klassen nach der aktuellen Messung

**Abnahme**

- Für jede bearbeitete Klasse sind Ursache, Änderung und Laufzeit vorher/nachher dokumentiert.
- Keine fachliche Assertion wurde entfernt oder abgeschwächt.
- Wiederholte Läufe zeigen keine Flakes und keine Abhängigkeit von erreichbaren Fremdhosts oder lokalen Diensten.

### 3. Sichere Parallelisierung messen

**Maßnahmen**

- Nach der Isolation globalen Zustands und gemeinsamer Ressourcen Klassenparallelität in kleinen Stufen erproben.
- Tests mit gemeinsamen Dateien, Ports, statischen Singletons, GUI-Ressourcen oder Prozesszustand gezielt serialisieren bzw. isolieren; den gesamten Testlauf nicht vorsorglich serialisieren.
- `forkCount`, JUnit-Klassenparallelität und verfügbare CPU-/Speicherressourcen getrennt variieren. Je Konfiguration Wandzeit, CPU-Auslastung und Fehler/Flakes aufzeichnen.
- Die schnellste stabile Konfiguration wählen; die vorhandene `1C`-Fork-Einstellung nicht zusätzlich unkontrolliert mit maximaler JUnit-Parallelität multiplizieren.

**Abnahme**

- Die Suite besteht mindestens fünf vollständige Wiederholungsläufe mit der gewählten Konfiguration.
- Keine Cross-Test-State-Kollisionen, Portkonflikte oder sporadischen Fehler.
- Parallelisierung verbessert die gemessene Wandzeit auf der Zielumgebung tatsächlich.

### 4. Gesamtlauf optimieren und Ziel absichern

**Maßnahmen**

- Nach jeder Phase einen vollständigen Lauf mit identischem Befehl und vergleichbarer Umgebung durchführen.
- Kompilierungs-, JaCoCo- und Surefire-Overhead getrennt messen. Coverage-Gates für den verbindlichen vollständigen Lauf beibehalten; nur einen optionalen schnellen Entwickler-Feedback-Lauf ohne Gate ergänzen, falls erforderlich.
- Den vollständigen Testlauf nicht durch das Verschieben oder Ausschließen bestehender Tests künstlich verkürzen. Falls einzelne echte Integrationstests später separat ausgeführt werden sollen, muss der vollständige Qualitätssicherungsablauf weiterhin alle Tests enthalten und dokumentiert werden.
- Zeitbudget nach den Messungen verteilen: zuerst größte Hotspots, danach sichere Parallelisierung und zuletzt Build-/Reporting-Overhead.

**Abnahme**

- Vollständiger Lauf inklusive aller bisherigen Tests liegt auf der Referenzumgebung bei höchstens ca. 10 Minuten.
- Coverage- und sonstige vorhandene Qualitätsprüfungen bestehen unverändert.
- Fünf aufeinanderfolgende vollständige Läufe bestehen ohne Flakes; Median und langsamster Lauf werden dokumentiert.
- Die Laufzeit ist für jeden Lauf separat historisiert und der HTML-Chart zeigt je Punkt ausschließlich dessen gespeicherten Wert.

## Erfolgskriterien

| Kennzahl | Ziel |
| --- | --- |
| Vollständige Suite, Wandzeit | ca. 10 Minuten oder weniger |
| Testbestand und Assertions | unverändert; keine Laufzeit-bedingten Ausschlüsse |
| Stabilität | 5 vollständige Wiederholungsläufe ohne Flakes |
| Runtime-Historie | genau ein eigener Datensatz pro Testlauf |
| HTML-Chart | Laufzeit pro zugehörigem Lauf; fehlende historische Werte als „–“ |

## Arbeitsreihenfolge

1. Baseline frisch messen und Laufzeit-Historisierung/Chart-Verknüpfung korrigieren.
2. Top-Hotspots anhand ihrer Ursachen einzeln optimieren.
3. Erst danach Parallelisierung schrittweise erhöhen und Stabilität wiederholt belegen.
4. Gesamtlauf samt Coverage-Gate messen, Ergebnis dokumentieren und Ziel gegen die vollständige Suite abnehmen.

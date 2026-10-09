# Verlässliche Testläufe

## Lokale Ausführung

Aus dem Repository-Root:

```powershell
.\tools\scripts\Verify-TestSuite.ps1
```

Standardmäßig führt das Skript `mvn --batch-mode clean test` aus. `clean`
verhindert, dass alte Surefire-Berichte einen aktuellen Lauf vortäuschen. Ein
Lauf gilt nur dann als erfolgreich, wenn Maven erfolgreich beendet wurde,
gültige Surefire-XMLs vorhanden sind, mindestens ein Test ausgeführt wurde und
die Berichte keine Fehler, Fehlschläge oder wiederholten Flaky-Failures
enthalten. Übersprungene Tests werden ausgewiesen; einige bestehende
Integrationstests überspringen sich abhängig von verfügbaren Netzwerk- oder
Betriebssystemfunktionen.

Mit `-Verify` wird zusätzlich der Maven-Verify-Schritt inklusive JaCoCo-Gate
ausgeführt. Teststatus und Coverage-Gate sind getrennte Ergebnisse; ein
Coverage-Fehler darf nicht als bestandener Verify-Lauf gemeldet werden.

Gezielte Entwicklungsläufe sind möglich, werden aber nicht als vollständige
Validierung gewertet:

```powershell
.\tools\scripts\Verify-TestSuite.ps1 -TestSelector 'OsParallelStepRunnerTest'
```

Flakiness lässt sich mit unabhängigen vollständigen Läufen sichtbar machen:

```powershell
.\tools\scripts\Verify-TestSuite.ps1 -Runs 5
```

Jeder Lauf startet mit `clean`; ein Fehlschlag beendet die Serie sofort. Es
gibt keine automatischen Test-Retries, die einen Fehlschlag in einen Pass
umdeuten. Nach einer erfolgreichen Serie enthält
`tools/output/test-results/test-stability-standard.json` die pro Lauf
gezählten Ergebnisse, Laufzeiten, übersprungenen Tests und Umgebungsdaten.

`@Tag("slow")`-Tests werden im Standardlauf ausgeschlossen. Sie lassen sich
separat vollständig einschließen:

```powershell
.\tools\scripts\Verify-TestSuite.ps1 -IncludeSlow
```

Die Coverage-Historie muss immer mit allen Tests einschließlich `slow`
aufgezeichnet werden. Dafür den eigenständigen Runner verwenden; er führt die
Vollsuite für Stabilität und Coverage aus, bevor er die Historie aktualisiert:

```powershell
.\tools\scripts\Run-Coverage.ps1 -Comment "Beschreibung der Änderung"
```

Der Coverage-Runner aktualisiert dabei auch
`tools/output/test_runtime_history.csv` und `tools/output/test_runtime_history.html`.
Das HTML-Diagramm zeigt den Verlauf der bis zu zehn langsamsten Testklassen des
neuesten Laufs. Die CSV enthält pro Lauf die Laufzeit jeder Testklasse; beide
Historien-Dateien sind versioniert und können nach einem `git pull` auch auf
anderen Geräten eingesehen werden. Vor einem neuen Lauf sollte die aktuelle
Historie synchronisiert werden; einzelne Testläufe können alternativ direkt
erfasst werden:

```powershell
.\tools\scripts\Update-TestRuntimeHistory.ps1
```

Der eigenständige Aufruf führt standardmäßig `mvn clean test` aus. Mit
`-IncludeSlow` werden zusätzlich die `slow`-Tests ausgeführt; mit
`-SkipTestRun` werden bereits vorhandene Surefire-Berichte verarbeitet.
Änderungen an der Historie müssen committet und gepusht werden, damit sie auf
anderen Geräten verfügbar sind. Alle Dateien unter `tools/output/` sind
versioniert. Temporäre Maven-/Surefire-Berichte unter `target/` und
maschinenlokale Build-Ausgaben bleiben ausgeschlossen.

## Automatisierte Absicherung

GitHub Actions führt die Test-Suite bei Pushes und Pull Requests aus. Der
geplante nächtliche Lauf wiederholt die Standard-Suite fünfmal und führt
zusätzlich einmal alle `slow`-Tests aus. Surefire-Berichte und Zusammenfassungen
werden als Actions-Artefakt gespeichert. Die versionierten Dateien unter
`tools/output/` machen Zusammenfassungen, Laufzeit- und Coverage-Historien
sowie den Kontextbericht geräteübergreifend verfügbar und halten vergangene
Ergebnisse im Git-Verlauf fest. Testzusammenfassungen speichern Java-Version
und Anbieter, aber keinen lokalen JDK-Installationspfad. Temporäre
Surefire-Berichte unter `target/` bleiben ausgeschlossen. Nur ein wirklich
ausgeführter und sauber beendeter Maven-Lauf zählt als bestanden.

Die vorhandene JaCoCo-Prüfung bleibt Teil von `verify`. Für gezielte Prüfung,
ob Tests auf Verhaltensänderungen reagieren, kann zusätzlich das bereits
konfigurierte Mutation-Testing-Profil verwendet werden:

```powershell
mvn -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

Mutationsberichte müssen inhaltlich geprüft werden: Überlebende Mutanten
zeigen mögliche Lücken, während nicht sinnvoll testbare Mutanten begründet
werden müssen. Ein Mutation Score allein ist kein Beweis für Fehlerfreiheit.

## Regeln für Tests und Ergebnisberichte

- Testdaten und erwartete Werte unabhängig von der Produktionslogik
  bestimmen; keine Assertion darf bloß die Implementierung nachrechnen.
- Timing, Zufall, Netzwerk und geteilten Zustand kontrollieren oder gezielt
  isolieren. Synchronisation anhand eines Zustands ist festen Sleeps
  vorzuziehen.
- Keine Assertions abschwächen, Tests deaktivieren oder automatisch
  wiederholen, um Laufzeiten oder Ergebnisse zu schönen. Bedingte
  Umgebungsannahmen müssen im Bericht als übersprungen sichtbar bleiben.
- Nur tatsächlich ausgeführte Befehle und deren Exit-Status als Testergebnis
  angeben. Teil- und übersprungene Läufe klar kennzeichnen; niemals ein
  Ergebnis aus erwarteten oder vermuteten Ausgaben ableiten.

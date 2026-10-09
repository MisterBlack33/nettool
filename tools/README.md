# Tools und Projektinventar

Dieses Verzeichnis enthält Schnellzugriffe, Code-Guides und die Dokumentation
für die Wartungs- und Testscripts des NetTool-Repositories. Die Scripts liegen
in `scripts/`; ihre erzeugten Dateien werden standardmäßig in `output/`
gespeichert und versioniert, damit Testergebnisse und Verläufe zwischen
Geräten geteilt und im Git-Verlauf nachverfolgt werden können.
PowerShell-Scripts werden üblicherweise vom Repository-Root gestartet.

## Kontextbericht erzeugen

`scripts/New-ContextReport.ps1` erstellt ein Markdown-Inventar, das
Coding-Assistenten einen strukturierten Überblick über das Projekt gibt, ohne
den vollständigen Quellcode in den Bericht zu kopieren. Das ist
nützlich, wenn ein vollständiger Projektimport zu groß für das Kontextfenster
ist: Der Bericht liefert Projektstruktur, Dateipfade, Pakete, Abhängigkeiten
und Git-Zustand, damit anschließend gezielt nur relevante Quelldateien
bereitgestellt werden können.

Vom Repository-Root:

```powershell
.\tools\scripts\New-ContextReport.ps1
```

Standardmäßig entsteht `tools/output/context-report.md`. Die Datei ist
versioniert und wird bei jedem Aufruf neu erstellt; sie enthält Inventar-
Metadaten und Git-Status zum Erstellungszeitpunkt.

### Parameter

| Parameter | Standard | Beschreibung |
|---|---|---|
| `-RootPath <Pfad>` | Repository-Root relativ zum Skript | Projektordner, der inventarisiert wird. |
| `-OutputPath <Pfad>` | `tools\output\context-report.md` | Vollständiger oder relativer (zum aktuellen Arbeitsverzeichnis) Zielpfad des Markdown-Berichts. |
| `-IncludeGenerated` | aus | Nimmt typische generierte Ordner wie `target`, `build`, `dist`, `out`, `node_modules` und Cache-Verzeichnisse in die Inventarisierung auf. |
| `-IncludeRuntimeData` | aus | Nimmt temporäre Laufzeitordner wie `tmp` und `.test-results` in die Inventarisierung auf. `saves/` ist standardmäßig enthalten. `tools/output/` bleibt als Ausgabeordner des Berichts ausgeschlossen. |
| `-TopLargeFiles <1..100>` | `20` | Anzahl der größten erfassten Dateien im Größenüberblick. |

Beispiele:

```powershell
# Andere Ausgabe-Datei verwenden
.\tools\scripts\New-ContextReport.ps1 -OutputPath "$env:TEMP\nettool-context.md"

# Auch Build-Artefakte und lokale Laufzeitdaten auflisten
.\tools\scripts\New-ContextReport.ps1 -IncludeGenerated -IncludeRuntimeData

# Anderes Projekt inventarisieren
.\tools\scripts\New-ContextReport.ps1 -RootPath "C:\work\another-project" `
    -OutputPath "C:\work\another-project\context-report.md"
```

### Inhalt des Berichts

- Zusammenfassung mit Anzahl der Dateien und Ordner, Java-Dateien, Packages
  sowie der Gesamtgröße der erfassten Dateien.
- Ausschlussliste für nicht erfasste Verzeichnisse.
- Maven-Koordinaten, Java-Zielversion, Quell-/Testverzeichnisse,
  Main-Klasse und direkt in `pom.xml` angegebene Abhängigkeiten.
- Git-Branch, letzter Commit und Status-Einträge, sofern Git verfügbar ist.
  Der lokale absolute Repository-Pfad wird aus Datenschutzgründen nicht ausgegeben.
- Häufigkeiten und kumulierte Größen nach Dateiendung.
- Größte erfasste Dateien.
- Erkannte Java-Package-Deklarationen.
- Vollständige Verzeichnis- und Dateipfade des erfassten Umfangs, inklusive
  Dateigrößen und Änderungszeitpunkten.
- Vollständiger Inhalt von `README.md` und, sofern vorhanden,
  `docs/testing.md`.
- Lesbare Coverage-Dateien im Projekt-Root und in `tools/output/` (CSV, HTML,
  XML, Markdown und TXT) sowie `target/site/jacoco/jacoco.csv`, sofern
  vorhanden. Binäre Dateien wie XLSX und JaCoCo-EXEC werden nicht eingebettet.

Der Bericht enthält **keine Quelltext-Inhalte** und keine transitiven Maven-
Abhängigkeiten. Die explizit eingebetteten Setup-Dokumente und lesbaren
Coverage-Dateien sind davon ausgenommen. Package-Namen werden aus Java-
Quelldateien gelesen. Abhängigkeiten werden aus der Projekt-`pom.xml`
ermittelt; verwendete Maven-Property-Versionen werden aufgelöst, sofern die
Property dort deklariert ist.

### Umfang und Datenschutz

`.git`-Interna und Verzeichnis-Links werden immer ausgelassen. Standardmäßig
werden typische Build-/Cache-Verzeichnisse und temporäre Laufzeitdaten nicht
durchlaufen. `saves/` und `tools/output/` sind im Git versioniert; der
Kontextbericht listet `saves/` mit auf, lässt aber `tools/output/` aus, um
seine eigenen Ausgaben nicht in das Inventar aufzunehmen. Es gibt keine
persönlichen Benutzerprofile zum Teilen; `saves/userdata/users.json` ist ein
lokaler Zugangsdaten-Speicher für die optionale Admin-Freischaltung und
enthält Passwort-Hashes. Diese Datei bleibt deshalb absichtlich aus Git
ausgeschlossen. `tmp/` bleibt im Repository leer; Inhalte darin werden
ignoriert. Die ausgeschlossenen Verzeichnisse werden mit ihrem Namen und
Grund im Bericht festgehalten. `.git` bleibt immer ausgeschlossen.

Ein vollständiges Dateiinventar kann selbst umfangreich sein. Für eine
kleinere Anfrage an einen Coding-Assistenten kann man den Bericht als Wegweiser
verwenden und danach nur die für die konkrete Änderung relevanten Dateien
bereitstellen. Der Bericht ist eine Inventarliste, kein Ersatz für den Quellcode.

## PowerShell-Profil-Funktion `cctx`

`script fast access.txt` enthält Schnellzugriffe, die in das PowerShell-Profil
kopiert werden können. Die neue Funktion `cctx` ruft das Inventarskript aus
dem aktuellen Arbeitsverzeichnis auf. Daher muss der Aufruf aus dem
Repository-Root erfolgen:

```powershell
cctx
cctx -IncludeGenerated -TopLargeFiles 30
cctx -OutputPath "$env:TEMP\nettool-context.md"
```

Falls das Profil noch nicht eingerichtet ist:

```powershell
New-Item -ItemType File -Path $PROFILE -Force
notepad $PROFILE
```

Die Funktion aus `tools/script fast access.txt` in das Profil übernehmen und
eine neue PowerShell-Sitzung öffnen (oder das Profil mit `. $PROFILE` neu
laden). Das Skript selbst ist auch ohne Profil direkt ausführbar.

## Weitere Skripte

| Skript | Zweck |
|---|---|
| `scripts/Verify-TestSuite.ps1` | Führt die Testsuite aus und prüft, dass Maven- und Surefire-Ergebnisse tatsächlich einen sauberen Testlauf belegen. Unterstützt gezielte Selektoren, Slow-Tests und optionale Verify-Prüfung. Ergebnisse landen in `output/test-results/`. |
| `scripts/Run-Coverage.ps1` | Führt die vollständige Test-/Coverage-Auswertung aus und aktualisiert Laufzeit- und Coverage-Ausgaben in `output/`. |
| `scripts/Measure-TestRuntime.ps1` | Liest Surefire-XML-Berichte aus und erzeugt `output/test_runtime_hotspots.csv`. |
| `scripts/Update-TestRuntimeHistory.ps1` | Hängt die Laufzeiten aller Testklassen an `output/test_runtime_history.csv` an und erstellt den HTML-Verlauf `output/test_runtime_history.html`. |
| `scripts/Update-TestCoverageHistory.ps1` | Aktualisiert die Coverage-Historie aus erzeugten JaCoCo-Ergebnissen in `output/`. |
| `scripts/New-CoverageChart.ps1` | Erstellt beziehungsweise öffnet `output/test_coverage_history.html`. |
| `scripts/Find-DeadCode.ps1` | Sucht heuristisch nach möglichen ungenutzten Java-Klassen und -Methoden und speichert `output/dead_code_report.csv`. Regex-Treffer sind Kandidaten und müssen manuell geprüft werden. |

`chart-range.js` und `chart-tooltip.js` sind interne Hilfsdateien in
`scripts/`. Schnellzugriffe und Code-Guides bleiben direkt unter `tools/`.
Details zu Testläufen, Selektoren und der Interpretation von Testresultaten
stehen in [`../docs/testing.md`](../docs/testing.md). Die Testlaufzeit-Historie
in `output/test_runtime_history.csv` und ihr Diagramm in
`output/test_runtime_history.html` sind versionierte, geräteübergreifende
Daten. Ebenso versioniert sind `output/test_runtime_hotspots.csv`, der
Kontext-Snapshot `output/context-report.md` und die Standard-Testzusammenfassung
`output/test-results/test-stability-standard.json`. Nach dem Synchronisieren
des Repositories lassen sich Historie und Ergebnisse dort einsehen; neue
Einträge werden mit dem Commit des Updates geteilt. Weitere temporäre
Testergebnisse bleiben ausgeschlossen.

# Tools und Projektinventar

Dieses Verzeichnis enthält Hilfsskripte für Wartung, Tests und Auswertung des
NetTool-Repositories. Die Skripte sind für PowerShell unter Windows ausgelegt
und werden üblicherweise vom Repository-Root gestartet.

## Kontextbericht erzeugen

`New-ContextReport.ps1` erstellt ein Markdown-Inventar, das Coding-Assistenten
einen strukturierten Überblick über das Projekt
gibt, ohne den vollständigen Quellcode in den Bericht zu kopieren. Das ist
nützlich, wenn ein vollständiger Projektimport zu groß für das Kontextfenster
ist: Der Bericht liefert Projektstruktur, Dateipfade, Pakete, Abhängigkeiten
und Git-Zustand, damit anschließend gezielt nur relevante Quelldateien
bereitgestellt werden können.

Vom Repository-Root:

```powershell
.\tools\New-ContextReport.ps1
```

Standardmäßig entsteht `context-report.md` im Repository-Root. Die
Datei wird in `.gitignore` ignoriert, weil sie ein veränderlicher,
maschinenlokaler Snapshot ist und normalerweise nicht eingecheckt werden
sollte. Der Bericht wird bei jedem Aufruf neu erstellt.

### Parameter

| Parameter | Standard | Beschreibung |
|---|---|---|
| `-RootPath <Pfad>` | Repository-Root relativ zum Skript | Projektordner, der inventarisiert wird. |
| `-OutputPath <Pfad>` | `<RootPath>\context-report.md` | Vollständiger oder relativer (zum aktuellen Arbeitsverzeichnis) Zielpfad des Markdown-Berichts. |
| `-IncludeGenerated` | aus | Nimmt typische generierte Ordner wie `target`, `build`, `dist`, `out`, `node_modules` und Cache-Verzeichnisse in die Inventarisierung auf. |
| `-IncludeRuntimeData` | aus | Nimmt lokale Laufzeitordner wie `saves`, `tmp` und `.test-results` in die Inventarisierung auf. |
| `-TopLargeFiles <1..100>` | `20` | Anzahl der größten erfassten Dateien im Größenüberblick. |

Beispiele:

```powershell
# Andere Ausgabe-Datei verwenden
.\tools\New-ContextReport.ps1 -OutputPath "$env:TEMP\nettool-context.md"

# Auch Build-Artefakte und lokale Laufzeitdaten auflisten
.\tools\New-ContextReport.ps1 -IncludeGenerated -IncludeRuntimeData

# Anderes Projekt inventarisieren
.\tools\New-ContextReport.ps1 -RootPath "C:\work\another-project" `
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

Der Bericht enthält **keine Dateiinhalte**, keine transitiven Maven-
Abhängigkeiten und keine Rekonstruktion von Datei-Inhalten. Package-Namen
werden aus Java-Quelldateien gelesen. Abhängigkeiten werden aus der
Projekt-`pom.xml` ermittelt; verwendete Maven-Property-Versionen werden
aufgelöst, sofern die Property dort deklariert ist.

### Umfang und Datenschutz

`.git`-Interna und Verzeichnis-Links werden immer ausgelassen. Standardmäßig
werden typische Build-/Cache-Verzeichnisse und lokale Laufzeitdaten nicht
durchlaufen. Dadurch gelangen beispielsweise lokale Konfigurationen, Logs,
gespeicherte Netzwerkdaten und große generierte Test-/Coverage-Ausgaben nicht
ungefragt in den Bericht. Die ausgeschlossenen Verzeichnisse werden mit ihrem
Namen und dem Grund im Bericht festgehalten. Mit den beiden `Include`-
Schaltern lässt sich die jeweilige Gruppe ausdrücklich aufnehmen; `.git`
bleibt immer ausgeschlossen.

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
| `Verify-TestSuite.ps1` | Führt die Testsuite aus und prüft, dass Maven- und Surefire-Ergebnisse tatsächlich einen sauberen Testlauf belegen. Unterstützt gezielte Selektoren, Slow-Tests und optionale Verify-Prüfung. |
| `Run-Coverage.ps1` | Führt die vollständige Test-/Coverage-Auswertung aus und aktualisiert Laufzeit- und Coverage-Ausgaben. |
| `Measure-TestRuntime.ps1` | Liest Surefire-XML-Berichte aus und erzeugt eine Liste langsamer Tests. |
| `Update-TestCoverageHistory.ps1` | Aktualisiert die Coverage-Historie aus erzeugten JaCoCo-Ergebnissen. |
| `New-CoverageChart.ps1` | Erstellt beziehungsweise öffnet die Coverage-Visualisierung. |
| `Find-DeadCode.ps1` | Sucht heuristisch nach möglichen ungenutzten Java-Klassen und -Methoden. Regex-Treffer sind Kandidaten und müssen manuell geprüft werden. |

Details zu Testläufen, Selektoren und der Interpretation von Testresultaten
stehen in [`../docs/testing.md`](../docs/testing.md).

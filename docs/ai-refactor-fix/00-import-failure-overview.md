# Übersicht der Import-/Refactor-Fehler nach dem Roadmap-Umsetzen

## Datum
2026-09-05

## Ausgangslage
Der Refactor wurde nach `refactor-roadmap-parallel.md` gestartet. Dabei wurden die Refactor-Ziele für Workstream A (Logging) teilweise umgesetzt, aber die zentrale Basis-Klasse `LogFileBase` blieb als leerer Stub zurück.

Die Folge ist ein Build-Fehler, der bereits beim `mvn -q test` auftritt.

## Verifizierter Fehlernachweis
Befehl:

```
mvn -q test
```

Wesentliche Compiler-Ausgabe:

```
[ERROR] /C:/Users/.../src/main/java/networktool/logging/DebugLogFile.java:[18,21] Konstruktor LogFileBase in Klasse main.java.networktool.logging.LogFileBase kann nicht auf die angegebenen Typen angewendet werden.
Erforderlich: keine Argumente
Ermittelt:    java.nio.file.Path,java.lang.String,int,DebugLogFile::parseLegacyLine

[ERROR] /C:/Users/.../src/main/java/networktool/security/AuditLogFile.java:[24,21] Konstruktor LogFileBase in Klasse main.java.networktool.logging.LogFileBase kann nicht auf die angegebenen Typen angewendet werden.
Erforderlich: keine Argumente
Ermittelt:    java.nio.file.Path,java.lang.String,int,(line)->null

[ERROR] Symbol nicht gefunden: Methode append(main.java.networktool.logging.LogEntry)
[ERROR] Symbol nicht gefunden: Methode clear()
[ERROR] Symbol nicht gefunden: Methode readRecent(int)
[ERROR] Symbol nicht gefunden: Methode parseNdjson(java.lang.String)
[ERROR] Symbol nicht gefunden: Methode nowFormatted()
```

## Betroffene Stelle

- `src/main/java/networktool/logging/LogFileBase.java`
- `src/main/java/networktool/logging/DebugLogFile.java`
- `src/main/java/networktool/security/AuditLogFile.java`
- `src/main/java/networktool/logging/LogEntry.java`

## Ursache des Problems

`LogFileBase` ist in der Datei `logging/LogFileBase.java` nur als leerer Platzhalter vorhanden:

```java
package main.java.networktool.logging;

public class LogFileBase {
}
```

Gleichzeitig erwarten `DebugLogFile` und `AuditLogFile` aber die vollständige API eines gemeinsamen Log-Kerns:

- Konstruktor mit `Path`, `String`, `int`, `Function<String, LogEntry>`
- `append(LogEntry)`
- `clear()`
- `readRecent(int)`
- `parseNdjson(String)`
- `nowFormatted()`

Das ist exakt die Schnittstelle, die in der Roadmap als Workstream A definiert ist. Die Refactor-Anweisung erwartet diese Klasse als neues gemeinsames Basismodul, aber die Implementierung fehlt noch.

## Relevante Logik aus den betroffenen Klassen

### `DebugLogFile`
- Erstellt `new LogFileBase(dataDir, FILE_NAME, MAX_LINES, DebugLogFile::parseLegacyLine);`
- ruft `base.append(new LogEntry(...))` auf
- ruft `base.clear()` und `base.readRecent(maxLines)` auf
- versucht `LogFileBase.parseNdjson(line)`
- nutzt `LogFileBase.nowFormatted()`

### `AuditLogFile`
- Erstellt `new LogFileBase(dataDir, FILE_NAME, MAX_LINES, line -> null);`
- ruft `base.append(new LogEntry(...))` auf
- ruft `base.clear()` und `base.readRecent(maxLines)` auf
- versucht `LogFileBase.parseNdjson(line)`
- nutzt `LogFileBase.nowFormatted()`

### `LogEntry`
- `LogEntry` ist bereits als Record vorhanden und erfüllt das NDJSON-Schema mit `toNdjson()`.
- Die eigentliche fehlende Leistung ist somit nicht im Record selbst, sondern in der Basisklasse `LogFileBase`, die alle Datei-IO-/Parse-/Rotation-Logik bereitstellen soll.

## Was nicht geändert werden darf

- Keine Produktionscode-Änderung in diesem Dokumentationsschritt.
- Keine Veränderung der öffentlichen API von `AuditLogger` und `DebugLogger`.
- Keine Umstellung von Dateipfaden oder Log-Dateinamen.
- Keine Änderungen an anderen Workstreams als der notwendigen Dokumentation.

## Verknüpfung mit der Roadmap

Die Roadmap beschreibt in Workstream A:

- neue Basisklasse `LogFileBase`
- neues `LogEntry`-Model
- gemeinsame Log-Kern-Implementierung
- Migration/Kompatibilität mit Legacy-Logs

Das aktuelle Projekt hat nur das `LogEntry`-Record, aber die tatsächliche `LogFileBase`-Implementierung fehlt.

## Empfehlung für die AI-Fix-Teams

Jede AI sollte ihre Arbeit auf den zugeordneten Workstream begrenzen und die fehlende `LogFileBase`-Implementierung als Ersttask behandeln. Danach erst die restlichen Workstreams im Sinne der Roadmap realisieren.

## Kurzfazit

Der Fehler ist kein allgemeiner Importfehler, sondern ein unvollständiger Refactor: Die aufrufenden Klassen erwarten eine gemeinsame Log-API, die tatsächlich nicht existiert. Der Fix muss in der Log-Implementierung erfolgen, nicht durch Code-Umstellung in anderen Bereichen.

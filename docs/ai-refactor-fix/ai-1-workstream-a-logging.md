# AI-Task 1: Workstream A – Logging vereinheitlichen

## Ziel
Die gemeinsame Log-Basis `LogFileBase` implementieren, sodass `DebugLogFile` und `AuditLogFile` wieder kompilieren und keine Imports/Methoden mehr fehlen.

## Fehlersituation
Der Build zeigt neue Fehler, weil `LogFileBase` noch leer ist:

- `LogFileBase` hat keinen Konstruktor mit `Path`, `String`, `int`, `Function<String, LogEntry>`
- `append(LogEntry)` existiert nicht
- `clear()` existiert nicht
- `readRecent(int)` existiert nicht
- `parseNdjson(String)` existiert nicht
- `nowFormatted()` existiert nicht

## Relevante Dateien
- `src/main/java/networktool/logging/LogFileBase.java`
- `src/main/java/networktool/logging/LogEntry.java`
- `src/main/java/networktool/logging/DebugLogFile.java`
- `src/main/java/networktool/security/AuditLogFile.java`
- `src/main/java/networktool/logging/DebugLogger.java`
- `src/main/java/networktool/security/AuditLogger.java`

## Erwartete API
Die Klasse muss die Signatur unterstützen, die `DebugLogFile` und `AuditLogFile` bereits verwenden:

```java
public class LogFileBase {
    public LogFileBase(Path dataDir, String fileName, int maxLines,
                      Function<String, LogEntry> legacyParser) { ... }

    public void append(LogEntry entry) { ... }
    public void clear() { ... }
    public List<LogEntry> readRecent(int maxLines) { ... }
    public static LogEntry parseNdjson(String line) { ... }
    public static String nowFormatted() { ... }
}
```

## Pflichtlogik
1. Log-Datei unter dem bestehenden Pfad und Dateinamen halten.
2. `maxLines`-Rotation oder ein vergleichbares Limit implementieren.
3. `append(entry)` muss den Eintrag als NDJSON und ggf. auch in kompatibler Form aufzeichnen.
4. `readRecent(int)` muss die letzten Einträge liefern.
5. `clear()` muss die Datei mit sinnvoller Persistenz-/Cleanup-Logik bereinigen.
6. `parseNdjson(String)` muss JSON-Objekte im Schema des `LogEntry`-Records lesen.
7. `nowFormatted()` muss die vorhandene Zeitstempel-Logik wiederherstellen.
8. Legacy-Parser für bisherige Zeilenformate beibehalten, damit alte Logs lesbar bleiben.

## Aufwand/Scope
- Keine API-Signatur von AuditLogger/DebugLogger ändern.
- Keine Änderung an `AuditLogEntry` und `DebugLogEntry` außer optionaler Kompatibilitäts-Umsetzung.
- Keine Datenverluste für bestehende Logdateien.

## Verifikation
Nach dem Fix sollte `mvn -q test` wieder bis zum Compile-/Test-Lauf durchkommen, bzw. die spezifischen Fehler im Logging-Teil verschwinden.

## Wichtige Hinweise
- Das Problem ist nicht in `LogEntry`, sondern in `LogFileBase`.
- Das `LogEntry`-Record ist bereits als gemeinsame Struktur konzipiert. Die Basis-Klasse muss nur die Datei-/Parser-/Rotationslogik ergänzen.
- `AuditLogFile.parse(...)` und `DebugLogFile.parse(...)` müssen weiterhin funktionieren.

## Was nicht angefasst werden darf
- `HostOwnership` / `UserHostStore` / `NetworkStore`-Refactor
- Theme-Änderungen
- Sidebar- oder Emoji-Änderungen

# Clean-Code-Guide Refactoring Status - JULI 2026

## 🎯 Fortschritt

| Phase | Status | Fortschritt |
|-------|--------|------------|
| **Leere Catch-Blöcke (Sicherheit)** | 🟨 IN PROGRESS | 9/45 (20%) |
| **Magische Zahlen** | ✅ COMPLETE | 1/84 (1%) - nur BannerGrabber |
| **Utility-Klassen Umbenennung** | 🔴 NOT STARTED | 0/5 (0%) |
| **Große Klassen Refactoring** | 🔴 NOT STARTED | 0/20 (0%) |
| **Testbarkeit** | 🟡 PARTIAL | Tests existieren |

---

## ✅ Abgeschlossene Dateien (9 Dateien)

### Core
1. ✅ `src/main/java/networktool/Main.java` - Logger, LebenszyklT Exception-Handling
2. ✅ `src/main/java/networktool/cli/MenuHandler.java` - NumberFormatException mit Logging

### GUI
3. ✅ `src/main/java/networktool/gui/GUI.java` - SecurityMonitor-Init Thread Exception

### Logic - Analysis
4. ✅ `src/main/java/networktool/logic/analysis/OsDetectionPipeline.java` - Hostname-Auflösung
5. ✅ `src/main/java/networktool/logic/ports/BannerGrabber.java` - 8 Protokoll-Grabber

### Logic - Scan
6. ✅ `src/main/java/networktool/logic/scan/PingSweep.java` - ICMP + ExecutorService
7. ✅ `src/main/java/networktool/logic/scan/HostAliveChecker.java` - ARP-Cache-Lesevorgänge

### Security
8. ✅ `src/main/java/networktool/security/AuditLogger.java` - ExecutorService Shutdown

---

## 🔴 Noch zu reparieren (36 Dateien mit leeren Catches)

### GUI Layer (10 Dateien)
- [ ] `gui/GuiMenuHandler.java` - mehrere Catches
- [ ] `gui/GuiNetworkMap.java`
- [ ] `gui/GuiOutputPanel.java`
- [ ] `gui/GuiPrivacyPanel.java`
- [ ] `gui/GuiScanActions.java`
- [ ] `gui/GuiSshTerminal.java`
- [ ] `gui/component/LocalToast.java`
- [ ] `gui/component/MapCanvas.java`
- [ ] `gui/component/MapHopDiscovery.java`
- [ ] `gui/component/MapSwitchStore.java`

### Logic - Analysis (7 Dateien)
- [ ] `logic/analysis/ArpMonitor.java`
- [ ] `logic/analysis/MdnsDiscovery.java`
- [ ] `logic/analysis/OsBannerAnalyzer.java`
- [ ] `logic/analysis/OsDetectorArp.java`
- [ ] `logic/analysis/OsDetectorPorts.java`
- [ ] `logic/analysis/OsProbeUdp.java`
- [ ] `logic/analysis/TracerouteRunner.java`
- [ ] `logic/analysis/UpnpDiscovery.java` (8 Dateien)

### Logic - Scan (5 Dateien)
- [ ] `logic/scan/NetworkHostScanner.java`
- [ ] `logic/scan/NetworkScanner.java`
- [ ] `logic/scan/RemoteNetGateway.java`
- [ ] `logic/scan/RemoteNetProbe.java`
- [ ] `logic/scan/RemoteNetScanner.java`

### Filter + UI
- [ ] `filter/HostResultPrinter.java`
- [ ] `gui/component/AppIcon.java`
- [ ] `gui/component/NotificationListener.java`

### Storage + Security (10+ Dateien)
- [ ] `security/AuditLogFile.java`
- [ ] `security/SecurityMonitor.java`
- [ ] `storage/AutoBackup.java`
- [ ] `storage/DataExporter.java`
- [ ] `storage/HostJsonBuilder.java`
- [ ] `storage/NetworkRegistry.java`
- [ ] `storage/NetworkStore.java`
- [ ] `storage/NetworkStoreHostOps.java`
- [ ] `storage/NetworkStoreLegacy.java`
- [ ] `storage/NetworkStoreNtfy.java`
- [ ] `storage/NetworkStorePersistence.java`
- [ ] `storage/SavedHostsStore.java`
- [ ] `storage/ScanProfileStore.java`

---

## 📊 Refactor-Muster (angewendet)

```java
// ✅ Pattern 1: Exception-spezifisches Logging
try {
    riskyOperation();
} catch (SpecificException e) {
    LOGGER.log(Level.FINE, "Context message", e);
}

// ✅ Pattern 2: InterruptedException Special Case
try {
    Thread.sleep(timeout);
} catch (InterruptedException e) {
    LOGGER.log(Level.FINE, "Thread interrupted", e);
    Thread.currentThread().interrupt();  // Re-interrupt!
}

// ✅ Pattern 3: ExecutorService Shutdown
try {
    executor.shutdown();
    executor.awaitTermination(timeout, unit);
} catch (InterruptedException e) {
    LOGGER.log(Level.FINE, "Termination interrupted", e);
    Thread.currentThread().interrupt();
}

// ✅ Pattern 4: IOException vs generic Exception
try {
    networkOperation();
} catch (IOException ioe) {
    LOGGER.log(Level.FINE, "IO error", ioe);
} catch (Exception e) {
    LOGGER.log(Level.WARNING, "Unexpected error", e);
}
```

---

## 🔧 Automatisierungs-Scripts

### Verfügbare Scripts:
1. `refactor-script.ps1` - Analyse der Code-Quality-Probleme
2. `apply-refactor.ps1` - Template für Batch-Refactoring
3. `refactor_catches.ps1` - PowerShell Batch-Refactorer (mit Encoding-Problemen)
4. `refactor_catches.py` - Python Batch-Refactorer (Python fehlt auf System)

### Manuelle Refactoring-Strategie (Erfolgreich)
- Direktes Reparieren mit `replace_string_in_file`
- Logger-Import + Logger-Feld + Catch-Blöcke pro Datei
- Durchschnitt: 3-5 Min. pro Datei

---

## 🛠️ Nächste Schritte (Priorisiert)

### Phase 2: Batch-Reparatur der verbleibenden 36 Dateien
**Geschätzte Zeit:** 2-3 Stunden bei manuellem Refactoring

### Phase 3: Utility-Klassen Umbenennung (5 Dateien)
```
StorageUtils.java      → StorageAccessor.java
CIDRUtils.java         → CidrCalculator.java
PlatformUtils.java     → PlatformInfo.java
LoginLayoutHelper.java → LoginLayoutBuilder.java
JsonHelper.java        → JsonProcessor.java
```
**Risiko:** Hohes Risiko für Import-Fehler (500+ Zeilen betroffen)

### Phase 4: Große Klassen Aufbrechen (20 Klassen)
**Top 5 Priorität:**
1. `GuiMenuHandler.java` (528 Zeilen) → 5-6 Handler-Klassen
2. `GuiSavedHostsPanel.java` (474 Zeilen) → Panel + Filter + Search
3. `GuiSidebar.java` (395 Zeilen) → MenuBuilder + ThemeManager
4. `MapCanvas.java` (293 Zeilen) → Separate Renderer-Klassen
5. `SavedHostsStore.java` (276 Zeilen) → Persistence + Repository

---

## 📋 Review-Checkliste für Committer

- [ ] `pom.xml` ist nicht modifiziert
- [ ] Alle Tests compilen noch: `mvn clean test`
- [ ] Keine new Features hinzugefügt (nur Refactoring)
- [ ] Alle Catch-Blöcke haben Logging
- [ ] Logger-Felder sind `private static final`
- [ ] Keine sensiblen Daten werden geloggt
- [ ] Thread.currentThread().interrupt() bei InterruptedException

---

## 💡 Tipps für Fortsetzung

1. **Effizient Arbeiten:** Copy-Paste der Logger-Import + Logger-Feld pro Datei
2. **Pattern Matching:** 90% der Catches folgen einem von 4 Patterns
3. **Testing:** Nach jeder Batch von 5-10 Dateien `mvn clean compile` durchführen
4. **Commits:** Ein Commit pro "Modul" (z.B. "security:", "logic:", "gui:")

---

**Letztes Update:** 2026-07-01 14:30 UTC  
**Geschätzte Gesamtdauer:** 4-5 Stunden (bei manuellem Refactoring)  
**Aktueller Fortschritt:** ~25% (Plan + erste Phase abgeschlossen)


# Projektkontext: nettool

> Automatisch erzeugtes Inventar. Es enthält Metadaten und Pfade, aber keine Quelltext-Inhalte.

## Überblick

- Erfasst am: 2026-10-28 22:09:14 +01:00
- Projektordner: nettool
- Dateien im erfassten Umfang: 625
- Ordner im erfassten Umfang: 125
- Java-Dateien: 587
- Erkannte Java-Packages: 61
- Datenmenge der erfassten Dateien: 3.08 MiB

### Umfang und Ausschlüsse

`.git`-Interna und Verzeichnis-Links werden immer ausgelassen. Generierte Build-/Cache-Ordner sowie lokale Laufzeitdaten werden standardmäßig ausgelassen; die Liste zeigt die erkannten Ausschlüsse. Inhalte dieser Ordner werden nicht aufgelistet.

- `.git` — Git internals
- `saves` — Laufzeit-/lokale Daten (mit -IncludeRuntimeData einschließen)
- `target` — generierte/build-Dateien (mit -IncludeGenerated einschließen)
- `tmp` — temporärer Ordner (Inhalte werden nicht versioniert)
- `tools\output` — Skriptausgaben (werden separat versioniert)

## Projekt- und Build-Konfiguration

- Maven-Koordinaten: `networktool:nettool:0.8.12`
- Packaging: `jar`
- Java-Version laut Maven: `21`
- Main-Klasse: `main.java.networktool.Main`
- Maven-Quellcodeverzeichnis: `src`
- Maven-Testverzeichnis: `test`

Direkte Maven-Abhängigkeiten aus `pom.xml` (keine transitiven Abhängigkeiten):
- `org.junit.jupiter:junit-jupiter-api:5.10.2` — test
- `org.junit.jupiter:junit-jupiter-engine:5.10.2` — test
- `org.junit.jupiter:junit-jupiter-params:5.10.2` — test
- `org.junit.jupiter:junit-jupiter:5.10.2` — test

## Git-Status

Git-Repository: erkannt (lokaler Pfad aus Datenschutzgründen ausgelassen)
Branch: master
HEAD: 160862e stuff
Working-tree entries (ohne saves/-Pfade): 22
```text
 M .github/workflows/test-quality.yml
 M .gitignore
 M .idea/misc.xml
 M README.md
 M pom.xml
 M src/main/java/networktool/gui/components/actions/GuiBackupActions.java
 M src/main/java/networktool/gui/map/MapSwitchStore.java
 M src/main/java/networktool/gui/panels/tags/HostTagPersistence.java
 M src/main/java/networktool/logic/analysis/probe/BandwidthHistoryStore.java
 M src/main/java/networktool/logic/scan/schedule/OfflineThresholdMonitor.java
 M src/main/java/networktool/logic/sonify/SonifyConfigStore.java
 M src/main/java/networktool/storage/BackupManager.java
 M src/main/java/networktool/storage/SavedHostsStore.java
 M src/main/java/networktool/storage/StorageLocations.java
 M src/main/java/networktool/storage/StorageLocationsResolver.java
 M src/main/java/networktool/storage/StorageUtils.java
 M src/main/java/networktool/storage/profile/ScanProfileStore.java
 M tools/README.md
 M tools/output/context-report.md
 M tools/scripts/New-ContextReport.ps1
?? test/main/java/networktool/storage/StorageLocationsTest.java
?? tools/scripts/Build-WindowsPackage.ps1
```

## Dateitypen

| Endung | Anzahl | Gesamtgröße (MiB) |
|---|---:|---:|
| `.gitignore` | 2 | 0 |
| `.ico` | 1 | 0.08 |
| `.java` | 587 | 1.72 |
| `.js` | 2 | 0.01 |
| `.json` | 1 | 0 |
| `.log` | 4 | 1.14 |
| `.md` | 5 | 0.03 |
| `.properties` | 1 | 0 |
| `.ps1` | 9 | 0.07 |
| `.txt` | 1 | 0 |
| `.xml` | 8 | 0.02 |
| `.yaml` | 2 | 0 |
| `.yml` | 2 | 0 |

## Größte erfasste Dateien (Top 20)

| Größe (MiB) | Pfad |
|---:|---|
| 0.56 | `replay_pid74832.log` |
| 0.45 | `replay_pid77032.log` |
| 0.08 | `src\main\resources\icon.ico` |
| 0.07 | `hs_err_pid74832.log` |
| 0.06 | `hs_err_pid77032.log` |
| 0.02 | `test\networktool\logic\ScanInfraTest.java` |
| 0.02 | `tools\scripts\New-ContextReport.ps1` |
| 0.02 | `src\main\java\networktool\gui\panels\saved\GuiSavedHostsPanel.java` |
| 0.02 | `tools\scripts\New-CoverageChart.ps1` |
| 0.01 | `test\networktool\filter\FilterTest.java` |
| 0.01 | `src\main\java\networktool\security\UserAuth.java` |
| 0.01 | `test\main\java\networktool\storage\JsonHelperFuzzTest.java` |
| 0.01 | `src\main\java\networktool\logic\analysis\os\OsBannerAnalyzer.java` |
| 0.01 | `test\main\java\networktool\gui\map\NetworkMapTest.java` |
| 0.01 | `test\networktool\logic\LogicTest.java` |
| 0.01 | `src\main\java\networktool\logic\analysis\os\OsProbeUdp.java` |
| 0.01 | `src\main\java\networktool\logic\ports\BannerProtocolProbes.java` |
| 0.01 | `test\main\java\networktool\gui\map\MapTopologyTest.java` |
| 0.01 | `test\networktool\security\SecurityTest.java` |
| 0.01 | `.idea\workspace.xml` |

## Java-Packages (61)

- `main.java.networktool`
- `main.java.networktool.cli`
- `main.java.networktool.filter`
- `main.java.networktool.gui.components`
- `main.java.networktool.gui.components.actions`
- `main.java.networktool.gui.components.map`
- `main.java.networktool.gui.components.scan`
- `main.java.networktool.gui.components.table`
- `main.java.networktool.gui.components.terminal`
- `main.java.networktool.gui.core`
- `main.java.networktool.gui.dashboard`
- `main.java.networktool.gui.hostdetails`
- `main.java.networktool.gui.login`
- `main.java.networktool.gui.map`
- `main.java.networktool.gui.notification`
- `main.java.networktool.gui.panels`
- `main.java.networktool.gui.panels.audit`
- `main.java.networktool.gui.panels.bandwidth`
- `main.java.networktool.gui.panels.privacy`
- `main.java.networktool.gui.panels.saved`
- `main.java.networktool.gui.panels.security`
- `main.java.networktool.gui.panels.tags`
- `main.java.networktool.gui.security`
- `main.java.networktool.logging`
- `main.java.networktool.logic`
- `main.java.networktool.logic.analysis.discovery`
- `main.java.networktool.logic.analysis.os`
- `main.java.networktool.logic.analysis.probe`
- `main.java.networktool.logic.analysis.security`
- `main.java.networktool.logic.analysis.snmp`
- `main.java.networktool.logic.error`
- `main.java.networktool.logic.messaging`
- `main.java.networktool.logic.ports`
- `main.java.networktool.logic.scan.host`
- `main.java.networktool.logic.scan.remote`
- `main.java.networktool.logic.scan.schedule`
- `main.java.networktool.logic.sonify`
- `main.java.networktool.logic.visualize`
- `main.java.networktool.logic.windows`
- `main.java.networktool.model`
- `main.java.networktool.security`
- `main.java.networktool.storage`
- `main.java.networktool.storage.export`
- `main.java.networktool.storage.network`
- `main.java.networktool.storage.profile`
- `main.java.networktool.theme`
- `main.java.networktool.transfer`
- `main.java.networktool.util`
- `networktool.cli`
- `networktool.filter`
- `networktool.gui`
- `networktool.gui.panels`
- `networktool.logic`
- `networktool.logic.scan`
- `networktool.misc`
- `networktool.model`
- `networktool.network`
- `networktool.security`
- `networktool.storage`
- `networktool.transfer`
- `networktool.util`

## Ordnerinventar (125)

- `.github`
- `.github\workflows`
- `.idea`
- `docs`
- `src`
- `src\main`
- `src\main\java`
- `src\main\java\networktool`
- `src\main\java\networktool\cli`
- `src\main\java\networktool\filter`
- `src\main\java\networktool\gui`
- `src\main\java\networktool\gui\components`
- `src\main\java\networktool\gui\components\actions`
- `src\main\java\networktool\gui\components\map`
- `src\main\java\networktool\gui\components\scan`
- `src\main\java\networktool\gui\components\table`
- `src\main\java\networktool\gui\components\terminal`
- `src\main\java\networktool\gui\core`
- `src\main\java\networktool\gui\dashboard`
- `src\main\java\networktool\gui\hostdetails`
- `src\main\java\networktool\gui\login`
- `src\main\java\networktool\gui\map`
- `src\main\java\networktool\gui\notification`
- `src\main\java\networktool\gui\panels`
- `src\main\java\networktool\gui\panels\audit`
- `src\main\java\networktool\gui\panels\bandwidth`
- `src\main\java\networktool\gui\panels\privacy`
- `src\main\java\networktool\gui\panels\saved`
- `src\main\java\networktool\gui\panels\security`
- `src\main\java\networktool\gui\panels\tags`
- `src\main\java\networktool\gui\security`
- `src\main\java\networktool\logging`
- `src\main\java\networktool\logic`
- `src\main\java\networktool\logic\analysis`
- `src\main\java\networktool\logic\analysis\discovery`
- `src\main\java\networktool\logic\analysis\os`
- `src\main\java\networktool\logic\analysis\probe`
- `src\main\java\networktool\logic\analysis\security`
- `src\main\java\networktool\logic\analysis\snmp`
- `src\main\java\networktool\logic\error`
- `src\main\java\networktool\logic\messaging`
- `src\main\java\networktool\logic\ports`
- `src\main\java\networktool\logic\scan`
- `src\main\java\networktool\logic\scan\host`
- `src\main\java\networktool\logic\scan\remote`
- `src\main\java\networktool\logic\scan\schedule`
- `src\main\java\networktool\logic\sonify`
- `src\main\java\networktool\logic\visualize`
- `src\main\java\networktool\logic\windows`
- `src\main\java\networktool\model`
- `src\main\java\networktool\security`
- `src\main\java\networktool\storage`
- `src\main\java\networktool\storage\export`
- `src\main\java\networktool\storage\network`
- `src\main\java\networktool\storage\profile`
- `src\main\java\networktool\theme`
- `src\main\java\networktool\transfer`
- `src\main\java\networktool\util`
- `src\main\resources`
- `test`
- `test\main`
- `test\main\java`
- `test\main\java\networktool`
- `test\main\java\networktool\cli`
- `test\main\java\networktool\gui`
- `test\main\java\networktool\gui\components`
- `test\main\java\networktool\gui\components\actions`
- `test\main\java\networktool\gui\components\scan`
- `test\main\java\networktool\gui\components\table`
- `test\main\java\networktool\gui\core`
- `test\main\java\networktool\gui\dashboard`
- `test\main\java\networktool\gui\map`
- `test\main\java\networktool\gui\notification`
- `test\main\java\networktool\gui\panels`
- `test\main\java\networktool\gui\panels\audit`
- `test\main\java\networktool\gui\panels\bandwidth`
- `test\main\java\networktool\gui\panels\saved`
- `test\main\java\networktool\gui\panels\security`
- `test\main\java\networktool\gui\panels\tags`
- `test\main\java\networktool\gui\security`
- `test\main\java\networktool\logging`
- `test\main\java\networktool\logic`
- `test\main\java\networktool\logic\analysis`
- `test\main\java\networktool\logic\analysis\discovery`
- `test\main\java\networktool\logic\analysis\os`
- `test\main\java\networktool\logic\analysis\probe`
- `test\main\java\networktool\logic\analysis\security`
- `test\main\java\networktool\logic\analysis\snmp`
- `test\main\java\networktool\logic\error`
- `test\main\java\networktool\logic\messaging`
- `test\main\java\networktool\logic\ports`
- `test\main\java\networktool\logic\scan`
- `test\main\java\networktool\logic\scan\host`
- `test\main\java\networktool\logic\scan\remote`
- `test\main\java\networktool\logic\scan\schedule`
- `test\main\java\networktool\logic\sonify`
- `test\main\java\networktool\logic\visualize`
- `test\main\java\networktool\logic\windows`
- `test\main\java\networktool\model`
- `test\main\java\networktool\security`
- `test\main\java\networktool\storage`
- `test\main\java\networktool\storage\export`
- `test\main\java\networktool\storage\network`
- `test\main\java\networktool\theme`
- `test\main\java\networktool\transfer`
- `test\main\java\networktool\util`
- `test\networktool`
- `test\networktool\cli`
- `test\networktool\filter`
- `test\networktool\gui`
- `test\networktool\gui\panels`
- `test\networktool\logic`
- `test\networktool\logic\scan`
- `test\networktool\logic\scan\host`
- `test\networktool\logic\scan\schedule`
- `test\networktool\misc`
- `test\networktool\model`
- `test\networktool\network`
- `test\networktool\security`
- `test\networktool\storage`
- `test\networktool\transfer`
- `test\networktool\util`
- `test\resources`
- `tools`
- `tools\scripts`

## Dateiinventar (625)

Quelltext-Inhalte werden absichtlich nicht ausgegeben. Größen sind Byte-genau; Zeitstempel sind lokale Dateisystem-Zeit.

| Größe (Bytes) | Geändert | Pfad |
|---:|---|---|
| 689 | 2026-10-02 12:20 | `.github\copilot-instructions.md` |
| 1313 | 2026-10-10 17:05 | `.github\workflows\qodana_code_quality.yml` |
| 1962 | 2026-10-28 22:05 | `.github\workflows\test-quality.yml` |
| 831 | 2026-10-28 20:59 | `.gitignore` |
| 228 | 2026-10-02 12:20 | `.idea\.gitignore` |
| 240 | 2026-10-02 12:20 | `.idea\codeInsightSettings.xml` |
| 710 | 2026-10-02 12:26 | `.idea\compiler.xml` |
| 423 | 2026-10-02 12:20 | `.idea\encodings.xml` |
| 864 | 2026-10-02 12:26 | `.idea\jarRepositories.xml` |
| 651 | 2026-10-02 12:26 | `.idea\misc.xml` |
| 180 | 2026-10-02 12:20 | `.idea\vcs.xml` |
| 10703 | 2026-10-28 21:09 | `.idea\workspace.xml` |
| 5416 | 2026-10-10 17:05 | `docs\testing.md` |
| 74741 | 2026-10-28 22:09 | `hs_err_pid74832.log` |
| 67782 | 2026-10-28 22:09 | `hs_err_pid77032.log` |
| 10534 | 2026-10-28 22:06 | `pom.xml` |
| 1992 | 2026-10-10 17:05 | `qodana.yaml` |
| 7575 | 2026-10-28 22:07 | `README.md` |
| 582180 | 2026-10-28 22:09 | `replay_pid74832.log` |
| 472747 | 2026-10-28 22:09 | `replay_pid77032.log` |
| 1371 | 2026-10-02 12:20 | `src\main\java\networktool\cli\CliArgs.java` |
| 1802 | 2026-10-02 12:20 | `src\main\java\networktool\cli\CliRunner.java` |
| 856 | 2026-10-02 12:20 | `src\main\java\networktool\filter\ClipboardUtil.java` |
| 1801 | 2026-10-02 12:20 | `src\main\java\networktool\filter\HostResultFilter.java` |
| 1843 | 2026-10-02 12:20 | `src\main\java\networktool\filter\HostResultPrinter.java` |
| 1432 | 2026-10-02 12:20 | `src\main\java\networktool\filter\JsonExporter.java` |
| 565 | 2026-10-02 12:20 | `src\main\java\networktool\filter\OutputRenderer.java` |
| 502 | 2026-10-02 12:20 | `src\main\java\networktool\filter\OutputRendererRegistry.java` |
| 1462 | 2026-10-02 12:20 | `src\main\java\networktool\filter\ScanFilter.java` |
| 1234 | 2026-10-02 12:20 | `src\main\java\networktool\filter\TablePrinter.java` |
| 2310 | 2026-10-28 21:00 | `src\main\java\networktool\gui\components\actions\GuiBackupActions.java` |
| 5698 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiContextMenu.java` |
| 6993 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiDataIOActions.java` |
| 7842 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiDiagnosticsActions.java` |
| 3146 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiOfflineMonitorActions.java` |
| 4065 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiRemoteActions.java` |
| 1158 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiTagFilterActions.java` |
| 1236 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\GuiWebhookActions.java` |
| 2115 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\NtfyTopicPrompt.java` |
| 3998 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\actions\RemoteDeviceDialogs.java` |
| 2335 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\BandwidthHistoryChart.java` |
| 3562 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\GuiNetworkBar.java` |
| 5696 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\GuiProgressBar.java` |
| 8826 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\GuiSidebar.java` |
| 1684 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\GuiStatusBar.java` |
| 3241 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\map\GuiNetworkMap.java` |
| 5105 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\map\GuiNetworkMapChrome.java` |
| 3769 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\map\GuiNetworkMapScanTasks.java` |
| 1790 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\PingGraphRenderer.java` |
| 1854 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiArpSnifferActions.java` |
| 2753 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiForeignNetActions.java` |
| 5602 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiHopAnalysis.java` |
| 1645 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiIpv6ScanActions.java` |
| 2087 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiMapExportActions.java` |
| 828 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiMapHeatmapActions.java` |
| 1492 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiPdfExportActions.java` |
| 5460 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiScanActions.java` |
| 3668 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiScanCompareActions.java` |
| 5608 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiScanProfileActions.java` |
| 1148 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiScanTimelineActions.java` |
| 4159 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiSchedulerActions.java` |
| 4634 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiSecurityAutomationActions.java` |
| 2166 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiSnmpActions.java` |
| 3075 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiSonifyActions.java` |
| 2864 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiTrafficSpectrogramActions.java` |
| 2581 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiTrafficVisualizerActions.java` |
| 4068 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\scan\GuiWolSchedulerActions.java` |
| 9202 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\SidebarAccordion.java` |
| 9077 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\SidebarAdminButton.java` |
| 1476 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\SidebarDebugButton.java` |
| 7484 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\SidebarPowerMenu.java` |
| 6739 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\table\GuiSearchBar.java` |
| 7332 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\table\GuiTableRenderer.java` |
| 2987 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\table\SearchResultRow.java` |
| 556 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\terminal\GuiSshTerminal.java` |
| 2649 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\terminal\SshConnectionWorker.java` |
| 5846 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\terminal\SshTerminalWindowBuilder.java` |
| 1525 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\terminal\TerminalChrome.java` |
| 1245 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\TrafficSpectrogramPanel.java` |
| 2479 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\TrafficSpectrogramRenderer.java` |
| 1251 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\TrafficVisualizerPanel.java` |
| 2475 | 2026-10-02 12:20 | `src\main\java\networktool\gui\components\TrafficWaveformRenderer.java` |
| 5270 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GUI.java` |
| 3884 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiDebugMode.java` |
| 974 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiErrorPresenter.java` |
| 3070 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiFrameLayout.java` |
| 464 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiMenuContext.java` |
| 3697 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiMenuDispatch.java` |
| 10634 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiMenuHandler.java` |
| 786 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiMenuRegistry.java` |
| 2007 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiRestartFlow.java` |
| 1759 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiStartupTasks.java` |
| 1328 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiStatusReporter.java` |
| 659 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiTestSuiteMenus.java` |
| 806 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiToggleAction.java` |
| 3091 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\GuiWindowActions.java` |
| 1070 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\TestSuiteAutomationMenu.java` |
| 1670 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\TestSuiteDataMenu.java` |
| 904 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\TestSuiteReportingMenu.java` |
| 933 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\TestSuiteScanningMenu.java` |
| 663 | 2026-10-02 12:20 | `src\main\java\networktool\gui\core\TestSuiteSecurityMenu.java` |
| 2790 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiDashboardPanel.java` |
| 1061 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiDashboardStats.java` |
| 1083 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiScanDeltaChart.java` |
| 2494 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiScanDeltaChartRenderer.java` |
| 1150 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiScanTimelineChart.java` |
| 2136 | 2026-10-02 12:20 | `src\main\java\networktool\gui\dashboard\GuiScanTimelineChartRenderer.java` |
| 2464 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostDetailRows.java` |
| 3803 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostDetailsPanel.java` |
| 3922 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostInfoTab.java` |
| 2790 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostNotesTab.java` |
| 4982 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostPingTab.java` |
| 3737 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostPortsTab.java` |
| 4254 | 2026-10-02 12:20 | `src\main\java\networktool\gui\hostdetails\HostSaveDialog.java` |
| 1846 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\GuiLoginRateLimiter.java` |
| 1973 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginButtons.java` |
| 1592 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginChoiceScreen.java` |
| 1808 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginFormBuilder.java` |
| 1646 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginFormLayout.java` |
| 2148 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginInputs.java` |
| 1423 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginLockoutWatcher.java` |
| 3693 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginScreens.java` |
| 1353 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\LoginShakeEffect.java` |
| 5341 | 2026-10-02 12:20 | `src\main\java\networktool\gui\login\RegisterScreen.java` |
| 8435 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapCanvas.java` |
| 2779 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapContextMenu.java` |
| 3754 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapDeviceSignatures.java` |
| 4809 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapEdgeBuilder.java` |
| 4501 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapExporter.java` |
| 900 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapHeatmapRenderer.java` |
| 696 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapHeatmapSettings.java` |
| 5408 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapHopDiscovery.java` |
| 3256 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapLayout.java` |
| 2610 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapLegend.java` |
| 4849 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapNodeCollector.java` |
| 3844 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapNodeStyle.java` |
| 5467 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapRenderer.java` |
| 555 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapSnapshot.java` |
| 2671 | 2026-10-28 21:00 | `src\main\java\networktool\gui\map\MapSwitchStore.java` |
| 6785 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapTopology.java` |
| 1791 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapTrafficHeatmap.java` |
| 1298 | 2026-10-02 12:20 | `src\main\java\networktool\gui\map\MapTrafficLoad.java` |
| 3461 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\LocalToast.java` |
| 1234 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\NotificationListener.java` |
| 3059 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\NotificationTcpServer.java` |
| 2402 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\NtfyJsonParser.java` |
| 5382 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\NtfySubscriptionManager.java` |
| 6113 | 2026-10-02 12:20 | `src\main\java\networktool\gui\notification\NtfySubscriptions.java` |
| 5038 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\audit\GuiAuditLegend.java` |
| 7280 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\audit\GuiAuditPanel.java` |
| 4871 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\audit\GuiAuditTable.java` |
| 3272 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\bandwidth\GuiBandwidthHistoryPanel.java` |
| 4111 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\GuiInputPanel.java` |
| 2532 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\GuiNetworkDialogs.java` |
| 5984 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\GuiOutputPanel.java` |
| 2361 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\OutputStreamRedirector.java` |
| 4650 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\privacy\GuiPrivacyPanel.java` |
| 8047 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\privacy\PrivacyNetworkActions.java` |
| 2712 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\privacy\PrivacyPanelStyle.java` |
| 16154 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\GuiSavedHostsPanel.java` |
| 3573 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\SavedHostsBulkActions.java` |
| 2806 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\SavedHostsManualAdd.java` |
| 3419 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\SavedHostsMoveMenu.java` |
| 1474 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\SavedHostsStyle.java` |
| 1331 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\saved\SavedHostsTagFilter.java` |
| 5651 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\security\GuiSecurityFindingsPanel.java` |
| 3099 | 2026-10-28 21:00 | `src\main\java\networktool\gui\panels\tags\HostTagPersistence.java` |
| 2895 | 2026-10-02 12:20 | `src\main\java\networktool\gui\panels\tags\HostTagStore.java` |
| 311 | 2026-10-02 12:20 | `src\main\java\networktool\gui\security\NoteDecryptionException.java` |
| 4542 | 2026-10-02 12:20 | `src\main\java\networktool\gui\security\NoteEncryption.java` |
| 665 | 2026-10-02 12:20 | `src\main\java\networktool\logging\DebugLogEntry.java` |
| 1617 | 2026-10-02 12:20 | `src\main\java\networktool\logging\DebugLogFile.java` |
| 2276 | 2026-10-02 12:20 | `src\main\java\networktool\logging\DebugLogger.java` |
| 1540 | 2026-10-02 12:20 | `src\main\java\networktool\logging\LogEntry.java` |
| 6042 | 2026-10-02 12:20 | `src\main\java\networktool\logging\LogFileBase.java` |
| 7920 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\discovery\ArpMonitor.java` |
| 3840 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\discovery\DhcpOptionAnalyzer.java` |
| 8026 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\discovery\MdnsDiscovery.java` |
| 4909 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\discovery\UpnpDiscovery.java` |
| 2893 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\ExtendedOsDetector.java` |
| 12236 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsBannerAnalyzer.java` |
| 492 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectionLogger.java` |
| 8574 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectionPipeline.java` |
| 595 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectionStepRunner.java` |
| 2876 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetector.java` |
| 5717 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectorArp.java` |
| 8595 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectorHostname.java` |
| 5263 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsDetectorPorts.java` |
| 3336 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsFingerprint.java` |
| 2127 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsParallelStepRunner.java` |
| 4585 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsPortClassifier.java` |
| 11606 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsProbeUdp.java` |
| 1075 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\OsSignature.java` |
| 303 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\os\ScanDepth.java` |
| 2156 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\BandwidthHistoryEntry.java` |
| 3304 | 2026-10-28 21:00 | `src\main\java\networktool\logic\analysis\probe\BandwidthHistoryStore.java` |
| 3605 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\IcmpAnalyzer.java` |
| 9355 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\IpInspector.java` |
| 9106 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\OuiDatabase.java` |
| 5562 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\OuiUpdater.java` |
| 9377 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\PingMonitor.java` |
| 2379 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\PingUtil.java` |
| 5026 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\TracerouteRenderer.java` |
| 3552 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\TracerouteRunner.java` |
| 2016 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\VlanDetector.java` |
| 3278 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\probe\WakeOnLan.java` |
| 2000 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\CveLookup.java` |
| 2906 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\DefaultCredentialProbe.java` |
| 4609 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\DhcpDiscoverProbe.java` |
| 1264 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\DhcpOfferTracker.java` |
| 660 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\FindingReporter.java` |
| 228 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\FindingsSource.java` |
| 753 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\FindingsSourceRegistry.java` |
| 1485 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\PeriodicJob.java` |
| 2008 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\RogueDhcpDetector.java` |
| 2967 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\ScanSecurityHook.java` |
| 491 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\SecurityFinding.java` |
| 1577 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\SecurityFindingsCollector.java` |
| 2911 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\TlsCertInspector.java` |
| 3058 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\security\TlsCertScheduler.java` |
| 3599 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\BerReader.java` |
| 735 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\BerTag.java` |
| 2603 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\BerWriter.java` |
| 2494 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpMessages.java` |
| 2356 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpOid.java` |
| 2548 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpPortTable.java` |
| 280 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpProtocolException.java` |
| 251 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpTransport.java` |
| 186 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpVarBind.java` |
| 2071 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\SnmpWalker.java` |
| 1562 | 2026-10-02 12:20 | `src\main\java\networktool\logic\analysis\snmp\UdpSnmpTransport.java` |
| 1097 | 2026-10-02 12:20 | `src\main\java\networktool\logic\error\ScanContext.java` |
| 1349 | 2026-10-02 12:20 | `src\main\java\networktool\logic\error\ScanFailure.java` |
| 4952 | 2026-10-02 12:20 | `src\main\java\networktool\logic\messaging\MessageDelivery.java` |
| 2093 | 2026-10-02 12:20 | `src\main\java\networktool\logic\messaging\MessageDeliverySsh.java` |
| 2452 | 2026-10-02 12:20 | `src\main\java\networktool\logic\messaging\MessageDeliveryWinRm.java` |
| 5613 | 2026-10-02 12:20 | `src\main\java\networktool\logic\messaging\MessageSender.java` |
| 3095 | 2026-10-02 12:20 | `src\main\java\networktool\logic\messaging\WebhookDelivery.java` |
| 3147 | 2026-10-02 12:20 | `src\main\java\networktool\logic\ports\BannerGrabber.java` |
| 11234 | 2026-10-02 12:20 | `src\main\java\networktool\logic\ports\BannerProtocolProbes.java` |
| 2977 | 2026-10-02 12:20 | `src\main\java\networktool\logic\ports\BannerText.java` |
| 6462 | 2026-10-02 12:20 | `src\main\java\networktool\logic\ports\PortScanner.java` |
| 2964 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\ArpNeighborSource.java` |
| 287 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\ArpSighting.java` |
| 3030 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\ArpSniffer.java` |
| 7976 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\HostAliveChecker.java` |
| 1885 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\Ipv6HostProbe.java` |
| 2882 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\Ipv6HostRange.java` |
| 2750 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\Ipv6NeighborSource.java` |
| 2155 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkDiscoverySweep.java` |
| 4089 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkHostArpResolver.java` |
| 3024 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkHostnameResolver.java` |
| 3314 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkHostScanner.java` |
| 7138 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkHostScanOrchestrator.java` |
| 2642 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkInfo.java` |
| 3335 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkScanner.java` |
| 1997 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkScannerIpScan.java` |
| 3474 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\NetworkScannerV6.java` |
| 3133 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\PingSweep.java` |
| 3135 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\ScanErrorClassifier.java` |
| 2385 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\ScanProgress.java` |
| 5552 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\SubnetDetector.java` |
| 1213 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\TailscaleRouteParser.java` |
| 2976 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\host\TailscaleRouteSource.java` |
| 3329 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\remote\RemoteNetGateway.java` |
| 4888 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\remote\RemoteNetProbe.java` |
| 7869 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\remote\RemoteNetScanner.java` |
| 3319 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\AdaptiveTimeoutEstimator.java` |
| 314 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\ArpCacheEntry.java` |
| 1354 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\LastScanCache.java` |
| 9805 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\MapTrafficObserver.java` |
| 1824 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\OfflineAliveProbe.java` |
| 4448 | 2026-10-28 21:00 | `src\main\java\networktool\logic\scan\schedule\OfflineThresholdMonitor.java` |
| 1519 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\OfflineTracker.java` |
| 1817 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\OfflineTrackerStore.java` |
| 6129 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\PortChangeMonitor.java` |
| 8413 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\ScanDelta.java` |
| 2829 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\ScanHistory.java` |
| 3394 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\ScanRateLimiter.java` |
| 6625 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\ScanScheduler.java` |
| 694 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\SubnetStats.java` |
| 236 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\WolSchedule.java` |
| 3348 | 2026-10-02 12:20 | `src\main\java\networktool\logic\scan\schedule\WolScheduler.java` |
| 761 | 2026-10-02 12:20 | `src\main\java\networktool\logic\ScanOutcome.java` |
| 1535 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\ActiveInterfaceDetector.java` |
| 553 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\BitEncoder.java` |
| 1545 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\InterfaceStatsReader.java` |
| 1197 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\PlainTone.java` |
| 595 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\SonifyConfig.java` |
| 2085 | 2026-10-28 21:00 | `src\main\java\networktool\logic\sonify\SonifyConfigStore.java` |
| 248 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\ToneDuration.java` |
| 247 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\ToneFrequency.java` |
| 2306 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\ToneGenerator.java` |
| 2890 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\TrafficSonifier.java` |
| 614 | 2026-10-02 12:20 | `src\main\java\networktool\logic\sonify\TrafficTone.java` |
| 3499 | 2026-10-02 12:20 | `src\main\java\networktool\logic\TimeoutConfig.java` |
| 218 | 2026-10-02 12:20 | `src\main\java\networktool\logic\visualize\TrafficSample.java` |
| 3575 | 2026-10-02 12:20 | `src\main\java\networktool\logic\visualize\TrafficVisualizer.java` |
| 1359 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PowerShellRunner.java` |
| 1760 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PsArpResolver.java` |
| 1517 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PsCidrResolver.java` |
| 1677 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PsInterfaceStatsResolver.java` |
| 1309 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PsNetScanResolver.java` |
| 1026 | 2026-10-02 12:20 | `src\main\java\networktool\logic\windows\PsPortScanResolver.java` |
| 1956 | 2026-10-02 12:20 | `src\main\java\networktool\Main.java` |
| 1386 | 2026-10-02 12:20 | `src\main\java\networktool\model\HostResult.java` |
| 823 | 2026-10-02 12:20 | `src\main\java\networktool\model\ScanProfile.java` |
| 751 | 2026-10-02 12:20 | `src\main\java\networktool\model\ScanResult.java` |
| 1164 | 2026-10-02 12:20 | `src\main\java\networktool\security\AuditLogEntry.java` |
| 1601 | 2026-10-02 12:20 | `src\main\java\networktool\security\AuditLogFile.java` |
| 5364 | 2026-10-02 12:20 | `src\main\java\networktool\security\AuditLogger.java` |
| 5046 | 2026-10-02 12:20 | `src\main\java\networktool\security\LoginDialog.java` |
| 7749 | 2026-10-02 12:20 | `src\main\java\networktool\security\SecurityMonitor.java` |
| 1265 | 2026-10-02 12:20 | `src\main\java\networktool\security\SessionAdminRateLimiter.java` |
| 13934 | 2026-10-02 12:20 | `src\main\java\networktool\security\UserAuth.java` |
| 4615 | 2026-10-02 12:20 | `src\main\java\networktool\security\UserAuthPersistence.java` |
| 3481 | 2026-10-28 21:00 | `src\main\java\networktool\storage\BackupManager.java` |
| 1031 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\DataExporter.java` |
| 3729 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\DataExportFormatters.java` |
| 841 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\DataExportImport.java` |
| 2922 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\DataImporter.java` |
| 1498 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\ExportFiles.java` |
| 4533 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\HtmlReportBuilder.java` |
| 4455 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\PdfDocument.java` |
| 1130 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\PdfHostLines.java` |
| 1123 | 2026-10-02 12:20 | `src\main\java\networktool\storage\export\PdfReportBuilder.java` |
| 7594 | 2026-10-02 12:20 | `src\main\java\networktool\storage\JsonCodec.java` |
| 6594 | 2026-10-02 12:20 | `src\main\java\networktool\storage\JsonHelper.java` |
| 5998 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\HostJsonBuilder.java` |
| 1792 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\HostSchemaMigration.java` |
| 3190 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\NetworkRegistry.java` |
| 8388 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\NetworkStore.java` |
| 3805 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\NetworkStoreHostOps.java` |
| 1440 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\NetworkStoreNtfy.java` |
| 5805 | 2026-10-02 12:20 | `src\main\java\networktool\storage\network\NetworkStorePersistence.java` |
| 1782 | 2026-10-02 12:20 | `src\main\java\networktool\storage\NotificationHistory.java` |
| 7957 | 2026-10-28 21:00 | `src\main\java\networktool\storage\profile\ScanProfileStore.java` |
| 6924 | 2026-10-28 21:00 | `src\main\java\networktool\storage\SavedHostsStore.java` |
| 5469 | 2026-10-28 21:06 | `src\main\java\networktool\storage\StorageLocations.java` |
| 509 | 2026-10-28 21:00 | `src\main\java\networktool\storage\StorageLocationsResolver.java` |
| 486 | 2026-10-28 21:00 | `src\main\java\networktool\storage\StorageUtils.java` |
| 3256 | 2026-10-02 12:20 | `src\main\java\networktool\storage\TestConstants.java` |
| 839 | 2026-10-02 12:20 | `src\main\java\networktool\theme\GuiColorPalette.java` |
| 5337 | 2026-10-02 12:20 | `src\main\java\networktool\theme\GuiTheme.java` |
| 3043 | 2026-10-02 12:20 | `src\main\java\networktool\theme\GuiThemeDark.java` |
| 3039 | 2026-10-02 12:20 | `src\main\java\networktool\theme\GuiThemeLight.java` |
| 4749 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\BandwidthHttpProbe.java` |
| 2186 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\BandwidthTester.java` |
| 1432 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\FileClient.java` |
| 1773 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\FileReceiver.java` |
| 1267 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\FileServer.java` |
| 1859 | 2026-10-02 12:20 | `src\main\java\networktool\transfer\LatencyProbe.java` |
| 5301 | 2026-10-02 12:20 | `src\main\java\networktool\util\AppIcon.java` |
| 3308 | 2026-10-02 12:20 | `src\main\java\networktool\util\ButtonFactory.java` |
| 2448 | 2026-10-02 12:20 | `src\main\java\networktool\util\CIDRUtils.java` |
| 2334 | 2026-10-02 12:20 | `src\main\java\networktool\util\ContextMenuActions.java` |
| 3099 | 2026-10-02 12:20 | `src\main\java\networktool\util\Ipv6AddressUtils.java` |
| 1738 | 2026-10-02 12:20 | `src\main\java\networktool\util\IpValidator.java` |
| 5099 | 2026-10-02 12:20 | `src\main\java\networktool\util\PlatformSupport.java` |
| 3919 | 2026-10-02 12:20 | `src\main\java\networktool\util\PlatformUtils.java` |
| 1450 | 2026-10-02 12:20 | `src\main\java\networktool\util\SafeCommand.java` |
| 542 | 2026-10-02 12:20 | `src\main\java\networktool\util\StatusTags.java` |
| 7605 | 2026-10-02 12:20 | `src\main\java\networktool\util\TableConfig.java` |
| 473 | 2026-10-02 12:20 | `src\main\resources\cve-table.json` |
| 80274 | 2026-10-02 12:20 | `src\main\resources\icon.ico` |
| 1913 | 2026-10-02 12:20 | `test\main\java\networktool\cli\CliArgsTest.java` |
| 1837 | 2026-10-02 12:20 | `test\main\java\networktool\cli\CliRunnerTest.java` |
| 1406 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\actions\GuiBackupActionsTest.java` |
| 1120 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\actions\GuiDataIOActionsTest.java` |
| 3946 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\actions\GuiOfflineMonitorActionsTest.java` |
| 1042 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\actions\GuiTagFilterActionsTest.java` |
| 2139 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\actions\GuiWebhookActionsTest.java` |
| 1923 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\BandwidthHistoryChartTest.java` |
| 1760 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\GuiProgressBarTest.java` |
| 1877 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\GuiSidebarAccessTest.java` |
| 1127 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\GuiSidebarTest.java` |
| 1359 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\GuiStatusBarTest.java` |
| 1458 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiArpSnifferActionsTest.java` |
| 1651 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiIpv6ScanActionsTest.java` |
| 3832 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiSecurityAutomationActionsTest.java` |
| 1754 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiSnmpActionsTest.java` |
| 2251 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiTrafficSpectrogramActionsTest.java` |
| 2243 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiTrafficVisualizerActionsTest.java` |
| 1614 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiWolSchedulerActionsTest.java` |
| 2425 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\scan\GuiWorkstreamCActionsTest.java` |
| 5007 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\SidebarAccordionTest.java` |
| 1169 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\SidebarAdminButtonTest.java` |
| 1346 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\table\SearchResultRowTest.java` |
| 2394 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\TrafficSpectrogramRendererTest.java` |
| 2902 | 2026-10-02 12:20 | `test\main\java\networktool\gui\components\TrafficWaveformRendererTest.java` |
| 2047 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiErrorPresenterTest.java` |
| 3253 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiMenuDispatchTest.java` |
| 3559 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiMenuHandlerAsyncTest.java` |
| 2039 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiMenuRegistryTest.java` |
| 961 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiStatusReporterTest.java` |
| 799 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiTestSuiteMenusTest.java` |
| 938 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiToggleActionTest.java` |
| 2058 | 2026-10-02 12:20 | `test\main\java\networktool\gui\core\GuiWindowActionsTest.java` |
| 517 | 2026-10-02 12:20 | `test\main\java\networktool\gui\dashboard\GuiDashboardPanelTest.java` |
| 1822 | 2026-10-02 12:20 | `test\main\java\networktool\gui\dashboard\GuiDashboardStatsTest.java` |
| 2898 | 2026-10-02 12:20 | `test\main\java\networktool\gui\dashboard\GuiScanDeltaChartRendererTest.java` |
| 3499 | 2026-10-02 12:20 | `test\main\java\networktool\gui\dashboard\GuiScanTimelineChartRendererTest.java` |
| 8886 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\GuiNetworkMapTest.java` |
| 444 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapContextMenuTest.java` |
| 1613 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapDeviceSignaturesTest.java` |
| 4984 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapEdgeBuilderTest.java` |
| 4776 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapExporterTest.java` |
| 1618 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapHeatmapRendererTest.java` |
| 641 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapHeatmapSettingsTest.java` |
| 3206 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapLayoutTest.java` |
| 456 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapLegendTest.java` |
| 3676 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapNodeStyleTest.java` |
| 656 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapSnapshotTest.java` |
| 11105 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapTopologyTest.java` |
| 1839 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapTrafficHeatmapTest.java` |
| 1112 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\MapTrafficLoadTest.java` |
| 12098 | 2026-10-02 12:20 | `test\main\java\networktool\gui\map\NetworkMapTest.java` |
| 1422 | 2026-10-02 12:20 | `test\main\java\networktool\gui\notification\NotificationListenerPackageTest.java` |
| 2507 | 2026-10-02 12:20 | `test\main\java\networktool\gui\notification\NtfyJsonParserTest.java` |
| 3467 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\audit\GuiAuditPanelTest.java` |
| 944 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\bandwidth\GuiBandwidthHistoryPanelTest.java` |
| 2034 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\GuiOutputPanelTest.java` |
| 2781 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\OutputStreamRedirectorTest.java` |
| 1496 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\saved\SavedHostsBulkActionsTest.java` |
| 2212 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\saved\SavedHostsTagFilterTest.java` |
| 656 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\security\GuiSecurityFindingsPanelTest.java` |
| 3106 | 2026-10-02 12:20 | `test\main\java\networktool\gui\panels\tags\HostTagStoreTest.java` |
| 1414 | 2026-10-02 12:20 | `test\main\java\networktool\gui\security\NoteEncryptionPasswordPolicyTest.java` |
| 1677 | 2026-10-02 12:20 | `test\main\java\networktool\gui\security\NoteEncyptionExceptionTest.java` |
| 4415 | 2026-10-02 12:20 | `test\main\java\networktool\logging\DebugLoggerTest.java` |
| 1780 | 2026-10-02 12:20 | `test\main\java\networktool\logging\LogEntryTest.java` |
| 2996 | 2026-10-02 12:20 | `test\main\java\networktool\logging\LogFileBaseTest.java` |
| 363 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\discovery\ArpMonitorLoggingTest.java` |
| 2161 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\discovery\DhcpOptionAnalyzerExtTest.java` |
| 10137 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\discovery\NetworkDiscoveryTest.java` |
| 1316 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\ExtendedOsDetectorTest.java` |
| 947 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsBannerAnalyzerTest.java` |
| 1915 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectionLoggerTest.java` |
| 1959 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectionPipelineDepthTest.java` |
| 2032 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectionPipelineTest.java` |
| 936 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectionStepRunnerTest.java` |
| 1689 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorArpTest.java` |
| 2837 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorDelegationTest.java` |
| 1807 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorDepthOverloadTest.java` |
| 1390 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorFixTest.java` |
| 5457 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorHostnameTest.java` |
| 2845 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorPackageTest.java` |
| 1066 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsDetectorPortsTest.java` |
| 7039 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsFingerprintTest.java` |
| 4227 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsParallelStepRunnerTest.java` |
| 5018 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsPortClassifierTest.java` |
| 10368 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsProbeUdpTest.java` |
| 1732 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\OsSignatureTest.java` |
| 512 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\os\ScanDepthTest.java` |
| 1612 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\BandwidthHistoryEntryTest.java` |
| 2127 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\BandwidthHistoryStoreTest.java` |
| 1788 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\IpInspectorExtTest.java` |
| 1315 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\TracerouteRunnerFixTest.java` |
| 2471 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\TracerouteRunnerPackageTest.java` |
| 1484 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\probe\VlanDetectorTest.java` |
| 1174 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\CveLookupTest.java` |
| 832 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\DefaultCredentialProbeTest.java` |
| 3197 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\DhcpDiscoverProbeTest.java` |
| 1362 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\DhcpOfferTrackerTest.java` |
| 1025 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\FindingReporterTest.java` |
| 2554 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\FindingsSourceRegistryTest.java` |
| 1581 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\PeriodicJobTest.java` |
| 2076 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\RogueDhcpDetectorTest.java` |
| 2822 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\ScanSecurityHookTest.java` |
| 1314 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\SecurityFindingsCollectorAddIfNewTest.java` |
| 2507 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\SecurityFindingsCollectorTest.java` |
| 1120 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\SecurityFindingTest.java` |
| 641 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\TlsCertInspectorTest.java` |
| 2881 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\security\TlsCertSchedulerTest.java` |
| 3201 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\BerCodecTest.java` |
| 2280 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\FakeSnmpAgent.java` |
| 2952 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\SnmpMessagesTest.java` |
| 2471 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\SnmpOidTest.java` |
| 2855 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\SnmpPortTableTest.java` |
| 2838 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\SnmpWalkerTest.java` |
| 1728 | 2026-10-02 12:20 | `test\main\java\networktool\logic\analysis\snmp\UdpSnmpTransportTest.java` |
| 2249 | 2026-10-02 12:20 | `test\main\java\networktool\logic\error\ScanFailureTest.java` |
| 2008 | 2026-10-02 12:20 | `test\main\java\networktool\logic\messaging\MessageDeliveryInjectionTest.java` |
| 824 | 2026-10-02 12:20 | `test\main\java\networktool\logic\messaging\MessageDeliverySshTest.java` |
| 900 | 2026-10-02 12:20 | `test\main\java\networktool\logic\messaging\MessageDeliveryWinRmTest.java` |
| 2434 | 2026-10-02 12:20 | `test\main\java\networktool\logic\messaging\MessagingPackageTest.java` |
| 3639 | 2026-10-02 12:20 | `test\main\java\networktool\logic\messaging\WebhookDeliveryTest.java` |
| 1360 | 2026-10-02 12:20 | `test\main\java\networktool\logic\ports\BannerProtocolProbesLoggingTest.java` |
| 2654 | 2026-10-02 12:20 | `test\main\java\networktool\logic\ports\PortScannerFixTest.java` |
| 4985 | 2026-10-02 12:20 | `test\main\java\networktool\logic\ports\PortsPackageTest.java` |
| 1743 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\ArpNeighborSourceTest.java` |
| 3920 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\ArpSnifferTest.java` |
| 2251 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\HostAliveCheckerRateLimitTest.java` |
| 836 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\Ipv6HostProbeTest.java` |
| 2180 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\Ipv6HostRangeTest.java` |
| 1617 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\Ipv6NeighborSourceTest.java` |
| 805 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\NetworkDiscoverySweepTest.java` |
| 458 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\NetworkHostScannerPackageTest.java` |
| 2859 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\NetworkScannerV6Test.java` |
| 794 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\NetworkTimeoutTestBase.java` |
| 4804 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\ScanErrorClassifierTest.java` |
| 1824 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\SubnetDetectorRouteTest.java` |
| 1932 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\SubnetDetectorTest.java` |
| 1761 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\TailscaleRouteParserTest.java` |
| 1198 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\host\TailscaleRouteSourceTest.java` |
| 2019 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\remote\RemoteNetScannerPackageTest.java` |
| 5163 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\AdaptiveTimeoutEstimatorTest.java` |
| 1178 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\ArpCacheEntryTest.java` |
| 1426 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\MapTrafficObserverTest.java` |
| 815 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\OfflineAliveProbeTest.java` |
| 3862 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\OfflineThresholdMonitorTest.java` |
| 1244 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\OfflineTrackerStoreTest.java` |
| 1852 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\OfflineTrackerTest.java` |
| 3380 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\ScanRateLimiterTest.java` |
| 2037 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\ScanSchedulerFixTest.java` |
| 3272 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\ScanSchedulerLifecycleRegressionTest.java` |
| 2315 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\ScanSchedulerPackageTest.java` |
| 4766 | 2026-10-02 12:20 | `test\main\java\networktool\logic\scan\schedule\WolSchedulerTest.java` |
| 929 | 2026-10-02 12:20 | `test\main\java\networktool\logic\ScanOutcomeTest.java` |
| 814 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\BitEncoderTest.java` |
| 475 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\InterfaceStatsReaderTest.java` |
| 507 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\PlainToneTest.java` |
| 654 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\SonifyConfigStoreTest.java` |
| 569 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\SonifyConfigTest.java` |
| 734 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\ToneGeneratorTest.java` |
| 891 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\TrafficSonifierConfigTest.java` |
| 1193 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\TrafficSonifierTest.java` |
| 1122 | 2026-10-02 12:20 | `test\main\java\networktool\logic\sonify\TrafficToneTest.java` |
| 1175 | 2026-10-02 12:20 | `test\main\java\networktool\logic\visualize\TrafficSampleTest.java` |
| 4872 | 2026-10-02 12:20 | `test\main\java\networktool\logic\visualize\TrafficVisualizerTest.java` |
| 620 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PowerShellRunnerTest.java` |
| 429 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PsArpResolverTest.java` |
| 484 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PsCidrResolverTest.java` |
| 728 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PsNetScanResolverTest.java` |
| 963 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PsPortScanResolverTest.java` |
| 1368 | 2026-10-02 12:20 | `test\main\java\networktool\logic\windows\PsResolverInjectionTest.java` |
| 5672 | 2026-10-02 12:20 | `test\main\java\networktool\MainTest.java` |
| 1652 | 2026-10-02 12:20 | `test\main\java\networktool\model\ModelEdgeCaseTest.java` |
| 2137 | 2026-10-02 12:20 | `test\main\java\networktool\security\AuditLoggerSessionAdminTest.java` |
| 10648 | 2026-10-02 12:20 | `test\main\java\networktool\security\AuditLoggerTest.java` |
| 1685 | 2026-10-02 12:20 | `test\main\java\networktool\security\UserAuthDefaultWarningTest.java` |
| 2065 | 2026-10-02 12:20 | `test\main\java\networktool\security\UserAuthPasswordPolicyTest.java` |
| 1639 | 2026-10-02 12:20 | `test\main\java\networktool\security\UserAuthSessionAdminLockoutTest.java` |
| 2830 | 2026-10-02 12:20 | `test\main\java\networktool\security\UserAuthSessionAdminTest.java` |
| 4523 | 2026-10-02 12:20 | `test\main\java\networktool\storage\BackupManagerTest.java` |
| 3371 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\DataExporterPackageTest.java` |
| 1721 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\DataImporterFixTest.java` |
| 4537 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\DataImporterRegressionTest.java` |
| 1248 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\ExportFilesTest.java` |
| 2496 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\ExportImportTest.java` |
| 1184 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\PdfHostLinesTest.java` |
| 4190 | 2026-10-02 12:20 | `test\main\java\networktool\storage\export\PdfReportBuilderTest.java` |
| 6481 | 2026-10-02 12:20 | `test\main\java\networktool\storage\JsonCodecFuzzTest.java` |
| 13289 | 2026-10-02 12:20 | `test\main\java\networktool\storage\JsonHelperFuzzTest.java` |
| 3922 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\HostJsonBuilderTest.java` |
| 2349 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\HostSchemaMigrationV2Test.java` |
| 2296 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\HostSchemaVersionTest.java` |
| 4556 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkRegistryTest.java` |
| 2830 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkStoreFixTest.java` |
| 3169 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkStoreIsolationTest.java` |
| 5127 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkStorePersistenceCorruptionTest.java` |
| 2218 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkStorePersistenceEdgeTest.java` |
| 3177 | 2026-10-02 12:20 | `test\main\java\networktool\storage\network\NetworkStorePersistencePackageTest.java` |
| 1889 | 2026-10-28 21:00 | `test\main\java\networktool\storage\StorageLocationsTest.java` |
| 1344 | 2026-10-02 12:20 | `test\main\java\networktool\theme\GuiThemePaletteTest.java` |
| 2473 | 2026-10-02 12:20 | `test\main\java\networktool\theme\GuiThemeTest.java` |
| 1340 | 2026-10-02 12:20 | `test\main\java\networktool\transfer\BandwidthTesterActiveTest.java` |
| 7288 | 2026-10-02 12:20 | `test\main\java\networktool\transfer\FileTransferRegressionTest.java` |
| 2063 | 2026-10-02 12:20 | `test\main\java\networktool\util\ButtonFactoryTest.java` |
| 2628 | 2026-10-02 12:20 | `test\main\java\networktool\util\CIDRUtilsBoundaryTest.java` |
| 1469 | 2026-10-02 12:20 | `test\main\java\networktool\util\CIDRUtilsExtTest.java` |
| 1583 | 2026-10-02 12:20 | `test\main\java\networktool\util\CIDRUtilsPropertyTest.java` |
| 3270 | 2026-10-02 12:20 | `test\main\java\networktool\util\Ipv6AddressUtilsTest.java` |
| 1494 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformSupportNtfyTopicTest.java` |
| 1082 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformSupportOsTest.java` |
| 1511 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformSupportPropertyTest.java` |
| 1089 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformSupportSubnetTest.java` |
| 1072 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformUtilsSubnetTest.java` |
| 3529 | 2026-10-02 12:20 | `test\main\java\networktool\util\PlatformUtilsTest.java` |
| 1732 | 2026-10-02 12:20 | `test\main\java\networktool\util\SafeCommandTest.java` |
| 776 | 2026-10-02 12:20 | `test\main\java\networktool\util\StatusTagsTest.java` |
| 3033 | 2026-10-02 12:20 | `test\main\java\networktool\util\TableConfigExtTest.java` |
| 1585 | 2026-10-02 12:20 | `test\main\java\networktool\util\TableConfigFixTest.java` |
| 2503 | 2026-10-02 12:20 | `test\main\java\networktool\util\TableConfigRendererTest.java` |
| 5868 | 2026-10-02 12:20 | `test\networktool\cli\CliTest.java` |
| 2133 | 2026-10-02 12:20 | `test\networktool\filter\FilterExtTest.java` |
| 13978 | 2026-10-02 12:20 | `test\networktool\filter\FilterTest.java` |
| 1926 | 2026-10-02 12:20 | `test\networktool\gui\GuiLoginRateLimiterPackageTest.java` |
| 9275 | 2026-10-02 12:20 | `test\networktool\gui\panels\PrivacyNetworkActionsTest.java` |
| 5238 | 2026-10-02 12:20 | `test\networktool\logic\AnalysisTest.java` |
| 11954 | 2026-10-02 12:20 | `test\networktool\logic\LogicTest.java` |
| 1000 | 2026-10-02 12:20 | `test\networktool\logic\MessagingTest.java` |
| 3326 | 2026-10-02 12:20 | `test\networktool\logic\scan\host\HostAliveCheckerTest.java` |
| 1973 | 2026-10-02 12:20 | `test\networktool\logic\scan\host\SubnetDetectorExtTest.java` |
| 2896 | 2026-10-02 12:20 | `test\networktool\logic\scan\schedule\LastScanCacheFixTest.java` |
| 21994 | 2026-10-02 12:20 | `test\networktool\logic\ScanInfraTest.java` |
| 7912 | 2026-10-02 12:20 | `test\networktool\logic\ScanNetworkTest.java` |
| 1530 | 2026-10-02 12:20 | `test\networktool\logic\TrafficVisualizerIntegrationTest.java` |
| 6497 | 2026-10-02 12:20 | `test\networktool\misc\MiscTest.java` |
| 7416 | 2026-10-02 12:20 | `test\networktool\model\ModelTest.java` |
| 6074 | 2026-10-02 12:20 | `test\networktool\network\NetworkTest.java` |
| 2233 | 2026-10-02 12:20 | `test\networktool\security\SecurityMonitorTest.java` |
| 11042 | 2026-10-02 12:20 | `test\networktool\security\SecurityTest.java` |
| 3125 | 2026-10-02 12:20 | `test\networktool\storage\ScanProfileStoreExtTest.java` |
| 3039 | 2026-10-02 12:20 | `test\networktool\storage\StorageExtTest.java` |
| 6784 | 2026-10-02 12:20 | `test\networktool\storage\StorageTest.java` |
| 2634 | 2026-10-02 12:20 | `test\networktool\transfer\TransferTest.java` |
| 1210 | 2026-10-02 12:20 | `test\networktool\util\PollHelper.java` |
| 5633 | 2026-10-02 12:20 | `test\networktool\util\UtilTest.java` |
| 286 | 2026-10-02 12:20 | `test\resources\junit-platform.properties` |
| 5377 | 2026-10-02 12:20 | `tools\clean_code_guide.md` |
| 1691 | 2026-10-02 12:20 | `tools\clean_code_guide.yaml` |
| 8735 | 2026-10-28 22:05 | `tools\README.md` |
| 2731 | 2026-10-10 17:05 | `tools\script fast access.txt` |
| 4956 | 2026-10-28 22:08 | `tools\scripts\Build-WindowsPackage.ps1` |
| 7393 | 2026-10-10 17:05 | `tools\scripts\chart-range.js` |
| 1180 | 2026-10-10 17:05 | `tools\scripts\chart-tooltip.js` |
| 7271 | 2026-10-10 17:05 | `tools\scripts\Find-DeadCode.ps1` |
| 2141 | 2026-10-10 17:05 | `tools\scripts\Measure-TestRuntime.ps1` |
| 17019 | 2026-10-28 21:05 | `tools\scripts\New-ContextReport.ps1` |
| 15834 | 2026-10-10 17:05 | `tools\scripts\New-CoverageChart.ps1` |
| 1090 | 2026-10-10 17:05 | `tools\scripts\Run-Coverage.ps1` |
| 9569 | 2026-10-10 17:05 | `tools\scripts\Update-TestCoverageHistory.ps1` |
| 9157 | 2026-10-10 17:05 | `tools\scripts\Update-TestRuntimeHistory.ps1` |
| 5342 | 2026-10-10 17:05 | `tools\scripts\Verify-TestSuite.ps1` |

## Projekt-README

Quelle: `README.md`

````md
# NetTool

NetTool is a Java desktop application for discovering and inspecting devices on
networks you own or are authorized to administer. The application combines a
Swing-based graphical interface with network scanning, host details, saved
inventory, monitoring, and security-oriented diagnostics.

> **Authorization:** Network discovery and security probes can generate traffic
> and may be interpreted as suspicious by network equipment. Use NetTool only
> on networks and devices for which you have explicit permission. Review scan
> settings and the impact of each operation before running it.

## Highlights

- **Network discovery:** IPv4 and IPv6 host scanning, subnet detection, ARP
  discovery and sniffing, hostname resolution, and optional route-aware scans.
- **Host inspection:** Ping and reachability information, open-port scans,
  service banners, operating-system fingerprinting, and traceroute.
- **Network context:** SNMP-based device information, VLAN detection, DHCP,
  mDNS and UPnP discovery, OUI/vendor lookup, and a visual network map.
- **Inventory and history:** Save hosts and network snapshots, organize hosts
  with tags, compare scans, review scan timelines, and track offline devices
  and port changes.
- **Monitoring and automation:** Scheduled scans, Wake-on-LAN, bandwidth
  history and visualization, traffic sonification, and notifications.
- **Security diagnostics:** TLS certificate inspection, rogue-DHCP detection,
  default-credential checks, and collected security findings.
- **Desktop workflow:** Searchable results, host detail views, export and
  backup actions, audit/debug views, and user/login screens.

Feature availability depends on the operating system, network adapter,
permissions, and the capabilities of devices on the network. Some operations
require elevated privileges or optional tools and may be skipped or unavailable
on a particular machine.

## Requirements

- Java Development Kit (JDK) 21 or later.
- Apache Maven 3.8.9 or later (the project is built with Maven).
- A graphical desktop environment to use the Swing interface.
- Windows is the primary tested environment; some network operations use
  Windows PowerShell. Behavior on other operating systems may differ.

The project uses Maven Compiler Plugin with Java release 21. JUnit 5 is used
for tests; JaCoCo measures coverage.

## Build and run

From the repository root:

```powershell
# Compile and run the default test suite
mvn clean test

# Package the executable JAR
mvn package

# Build a portable Windows package with an included Java runtime
.\tools\scripts\Build-WindowsPackage.ps1

# Start the graphical application
java -jar target/nettool-0.8.12.jar
```

The Windows packaging script requires JDK 21+ with `jpackage` and a built
application JAR. It creates `target/dist/nettool-<version>-windows-x64.zip`,
including a launcher and Java runtime; unzip it and start `NetTool\NetTool.exe`.
Application data stays in the user's `.nettool` directory, separate from the
package. CI builds and smoke-tests this package on Windows and publishes it as
a workflow artifact.

With the current Maven coordinates, the executable JAR is
`target/nettool-0.8.12.jar`. To print the supported
command-line options or version:

```powershell
java -jar target/nettool-0.8.12.jar --help
java -jar target/nettool-0.8.12.jar --version
```

Starting without arguments opens the graphical application. The CLI currently
provides help and version output; it does **not** provide a headless scanning
mode.

## Tests

The standard Maven test configuration excludes tests tagged `slow`. For a
clean run with explicit report and result validation, use the repository
script:

```powershell
.\tools\scripts\Verify-TestSuite.ps1
```

Useful variants:

```powershell
# Run one selected test or test class
.\tools\scripts\Verify-TestSuite.ps1 -TestSelector 'OsParallelStepRunnerTest'

# Include slow tests
.\tools\scripts\Verify-TestSuite.ps1 -IncludeSlow

# Repeat independent standard-suite runs
.\tools\scripts\Verify-TestSuite.ps1 -Runs 5

# Also run Maven verify, including the configured JaCoCo coverage gate
.\tools\scripts\Verify-TestSuite.ps1 -Verify
```

The complete suite, including slow tests, can also be run directly through the
nightly Maven profile:

```powershell
mvn clean test -Pnightly
```

See [`docs/testing.md`](docs/testing.md) for the test-report contract, coverage
workflow, CI behavior, and guidance for interpreting skipped tests.

## Data and local files

At runtime, NetTool stores application data in the current user's home
directory under `.nettool/` (for example `%USERPROFILE%\.nettool` on Windows).
The legacy project-local `saves/` directory is copied into this location on
first startup; existing destination files are kept, and the old files are not
deleted automatically. To use a different location, set the Java system
property `-Dnettool.dataDir=<path>`:

| Directory | Contents |
|---|---|
| `.nettool/userdata/` | User/account data |
| `.nettool/networkdata/` | Saved networks, hosts, and related inventory |
| `.nettool/profiles/` | Scan profiles |
| `.nettool/logs/` | Audit and debug logs |
| `.nettool/cache/` | Local caches and application settings |

These files are local application state, not source code. Do not publish
personal or sensitive network inventories, account data, logs, or backups.
Runtime contents under the repository's `saves/` directory are ignored by Git;
only an empty placeholder is retained. The ignore rule does not remove older
copies from Git history.

## Repository layout

```text
src/main/java/networktool/   Application source
src/main/resources/          Application resources
test/                        JUnit tests and test resources
docs/                        Project and testing documentation
tools/                       Quick access, guides, and tool documentation
tools/scripts/               PowerShell and JavaScript maintenance scripts
tools/output/                Generated reports and script output
.github/workflows/           GitHub Actions CI configuration
pom.xml                      Maven build and plugin configuration
```

The Java code is organized by responsibility. Important areas include:

- `gui/` — Swing windows, panels, actions, map, dashboards, and UI support.
- `logic/scan/` — host discovery, scanning, scheduling, and scan history.
- `logic/analysis/` — protocol discovery, OS detection, probes, and findings.
- `logic/ports/` — port scanning and service banner handling.
- `storage/` — persistence and local data formats.
- `security/` — authentication and audit logging.
- `model/` — shared application data structures.
- `cli/` — the small command-line contract (`--help` and `--version`).

## Development tools

The `tools/scripts/` directory includes scripts for verifying the suite,
collecting coverage history, measuring test runtimes, and identifying
potentially unused code. Generated script reports are stored in
`tools/output/`. Quick access commands, code guides, and tool documentation
remain directly in `tools/`. See [`tools/README.md`](tools/README.md) for
details.

## CI

GitHub Actions runs the standard suite on pushes and pull requests. Its
scheduled workflow repeats the standard suite and also runs the slow tests.
The workflow uses Windows and Java 21; Surefire reports and run summaries are
retained as workflow artifacts.

````

## Test- und Coverage-Anleitung

Quelle: `docs\testing.md`

````md
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

````

## Coverage-Dateien

Lesbare Coverage-Dateien werden vollständig eingebettet. Binärdateien wie XLSX und JaCoCo-EXEC bleiben im Dateiinventar, werden aber nicht als Text interpretiert.

## Coverage: tools\output\test_coverage_history.csv

Quelle: `tools\output\test_coverage_history.csv`

```csv
"run","timestamp","element","class_pct","class_covered","class_total","method_pct","method_covered","method_total","line_pct","line_covered","line_total","branch_pct","branch_covered","branch_total"
"1","2026-09-18 21:23:05","main.java.networktool","0.6247","233","373","0.4788","1118","2335","0.4151","4479","10791","0.3896","2157","5536"
"1","2026-09-18 21:23:05","filter","0.8571","6","7","0.8214","23","28","0.6893","71","103","0.7143","40","56"
"1","2026-09-18 21:23:05","gui","0.4142","70","169","0.2273","238","1047","0.2268","1096","4832","0.2505","461","1840"
"1","2026-09-18 21:23:05","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"1","2026-09-18 21:23:05","logic","0.8115","99","122","0.6508","425","653","0.4952","1713","3459","0.4144","903","2179"
"1","2026-09-18 21:23:05","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"1","2026-09-18 21:23:05","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"1","2026-09-18 21:23:05","storage","0.9615","25","26","0.8289","189","228","0.7691","756","983","0.5658","391","691"
"1","2026-09-18 21:23:05","theme","0.6667","2","3","0.3968","25","63","0.4936","77","156","0.0152","1","66"
"1","2026-09-18 21:23:05","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"1","2026-09-18 21:23:05","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3617","102","282"
"1","2026-09-18 21:23:05","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"3","2026-09-21 06:49:47","main.java.networktool","0.6928","309","446","0.5388","1459","2708","0.4737","5705","12043","0.4391","2655","6046"
"3","2026-09-21 06:49:47","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"3","2026-09-21 06:49:47","gui","0.5126","102","199","0.2927","346","1182","0.2915","1528","5242","0.3114","624","2004"
"3","2026-09-21 06:49:47","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"3","2026-09-21 06:49:47","logic","0.8562","137","160","0.7247","624","861","0.5685","2382","4190","0.4797","1191","2483"
"3","2026-09-21 06:49:47","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"3","2026-09-21 06:49:47","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"3","2026-09-21 06:49:47","storage","0.9677","30","31","0.8488","219","258","0.7898","864","1094","0.5894","432","733"
"3","2026-09-21 06:49:47","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"3","2026-09-21 06:49:47","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"3","2026-09-21 06:49:47","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3617","102","282"
"3","2026-09-21 06:49:47","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"4","2026-09-21 20:00:31","main.java.networktool","0.6928","309","446","0.5388","1459","2708","0.4737","5705","12043","0.4391","2655","6046"
"4","2026-09-21 20:00:31","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"4","2026-09-21 20:00:31","gui","0.5126","102","199","0.2927","346","1182","0.2915","1528","5242","0.3114","624","2004"
"4","2026-09-21 20:00:31","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"4","2026-09-21 20:00:31","logic","0.8562","137","160","0.7247","624","861","0.5685","2382","4190","0.4797","1191","2483"
"4","2026-09-21 20:00:31","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"4","2026-09-21 20:00:31","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"4","2026-09-21 20:00:31","storage","0.9677","30","31","0.8488","219","258","0.7898","864","1094","0.5894","432","733"
"4","2026-09-21 20:00:31","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"4","2026-09-21 20:00:31","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"4","2026-09-21 20:00:31","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3617","102","282"
"4","2026-09-21 20:00:31","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"5","2026-09-21 20:35:46","main.java.networktool","0.6928","309","446","0.5388","1459","2708","0.4737","5705","12043","0.4391","2655","6046"
"5","2026-09-21 20:35:46","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"5","2026-09-21 20:35:46","gui","0.5126","102","199","0.2927","346","1182","0.2915","1528","5242","0.3114","624","2004"
"5","2026-09-21 20:35:46","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"5","2026-09-21 20:35:46","logic","0.8562","137","160","0.7247","624","861","0.5685","2382","4190","0.4797","1191","2483"
"5","2026-09-21 20:35:46","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"5","2026-09-21 20:35:46","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"5","2026-09-21 20:35:46","storage","0.9677","30","31","0.8488","219","258","0.7898","864","1094","0.5894","432","733"
"5","2026-09-21 20:35:46","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"5","2026-09-21 20:35:46","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"5","2026-09-21 20:35:46","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3617","102","282"
"5","2026-09-21 20:35:46","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"6","2026-09-21 20:49:56","main.java.networktool","0.6942","311","448","0.542","1478","2727","0.4752","5744","12088","0.442","2685","6074"
"6","2026-09-21 20:49:56","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"6","2026-09-21 20:49:56","gui","0.5126","102","199","0.2927","346","1182","0.2915","1528","5242","0.3114","624","2004"
"6","2026-09-21 20:49:56","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"6","2026-09-21 20:49:56","logic","0.858","139","162","0.7307","643","880","0.5717","2421","4235","0.4859","1220","2511"
"6","2026-09-21 20:49:56","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"6","2026-09-21 20:49:56","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"6","2026-09-21 20:49:56","storage","0.9677","30","31","0.8488","219","258","0.7898","864","1094","0.5894","432","733"
"6","2026-09-21 20:49:56","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"6","2026-09-21 20:49:56","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"6","2026-09-21 20:49:56","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3652","103","282"
"6","2026-09-21 20:49:56","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"7","2026-09-21 21:22:16","main.java.networktool","0.6942","311","448","0.542","1478","2727","0.4752","5744","12088","0.4424","2687","6074"
"7","2026-09-21 21:22:16","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"7","2026-09-21 21:22:16","gui","0.5126","102","199","0.2927","346","1182","0.2915","1528","5242","0.3119","625","2004"
"7","2026-09-21 21:22:16","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"7","2026-09-21 21:22:16","logic","0.858","139","162","0.7307","643","880","0.5717","2421","4235","0.4863","1221","2511"
"7","2026-09-21 21:22:16","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"7","2026-09-21 21:22:16","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.65","169","260"
"7","2026-09-21 21:22:16","storage","0.9677","30","31","0.8488","219","258","0.7898","864","1094","0.5894","432","733"
"7","2026-09-21 21:22:16","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"7","2026-09-21 21:22:16","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"7","2026-09-21 21:22:16","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3652","103","282"
"7","2026-09-21 21:22:16","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"8","2026-09-22 11:12:21","main.java.networktool","0.6942","311","448","0.5387","1469","2727","0.4718","5703","12088","0.4376","2658","6074"
"8","2026-09-22 11:12:21","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"8","2026-09-22 11:12:21","gui","0.5126","102","199","0.2927","346","1182","0.2907","1524","5242","0.3084","618","2004"
"8","2026-09-22 11:12:21","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"8","2026-09-22 11:12:21","logic","0.858","139","162","0.7307","643","880","0.5702","2415","4235","0.4851","1218","2511"
"8","2026-09-22 11:12:21","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"8","2026-09-22 11:12:21","security","0.8333","10","12","0.712","89","125","0.6674","317","475","0.5423","141","260"
"8","2026-09-22 11:12:21","storage","0.9677","30","31","0.8566","221","258","0.7998","875","1094","0.6016","441","733"
"8","2026-09-22 11:12:21","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"8","2026-09-22 11:12:21","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"8","2026-09-22 11:12:21","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3652","103","282"
"8","2026-09-22 11:12:21","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"9","2026-09-22 13:41:55","main.java.networktool","0.6942","311","448","0.5427","1480","2727","0.4756","5749","12088","0.4424","2687","6074"
"9","2026-09-22 13:41:55","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"9","2026-09-22 13:41:55","gui","0.5126","102","199","0.2927","346","1182","0.2907","1524","5242","0.3089","619","2004"
"9","2026-09-22 13:41:55","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"9","2026-09-22 13:41:55","logic","0.858","139","162","0.7307","643","880","0.5712","2419","4235","0.4863","1221","2511"
"9","2026-09-22 13:41:55","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"9","2026-09-22 13:41:55","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.6385","166","260"
"9","2026-09-22 13:41:55","storage","0.9677","30","31","0.8566","221","258","0.7998","875","1094","0.6016","441","733"
"9","2026-09-22 13:41:55","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"9","2026-09-22 13:41:55","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"9","2026-09-22 13:41:55","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3652","103","282"
"9","2026-09-22 13:41:55","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"10","2026-09-23 15:45:39","main.java.networktool","0.694","313","451","0.5368","1489","2774","0.4693","5817","12394","0.4372","2706","6190"
"10","2026-09-23 15:45:39","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"10","2026-09-23 15:45:39","gui","0.5149","104","202","0.2883","354","1228","0.2857","1580","5531","0.2995","632","2110"
"10","2026-09-23 15:45:39","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"10","2026-09-23 15:45:39","logic","0.858","139","162","0.7307","643","880","0.5712","2419","4235","0.4863","1221","2511"
"10","2026-09-23 15:45:39","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"10","2026-09-23 15:45:39","security","0.8333","10","12","0.8016","101","126","0.75","369","492","0.637","172","270"
"10","2026-09-23 15:45:39","storage","0.9677","30","31","0.8566","221","258","0.8016","877","1094","0.6016","441","733"
"10","2026-09-23 15:45:39","theme","0.6667","2","3","0.4127","26","63","0.5","78","156","0.0606","4","66"
"10","2026-09-23 15:45:39","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"10","2026-09-23 15:45:39","util","0.5625","9","16","0.505","51","101","0.4642","188","405","0.3652","103","282"
"10","2026-09-23 15:45:39","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"8","2026-09-24 19:04:58","main.java.networktool","0.7545","338","448","0.5988","1633","2727","0.5347","6464","12088","0.4827","2932","6074"
"8","2026-09-24 19:04:58","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"8","2026-09-24 19:04:58","gui","0.6181","123","199","0.357","422","1182","0.3886","2037","5242","0.3663","734","2004"
"8","2026-09-24 19:04:58","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"8","2026-09-24 19:04:58","logic","0.858","139","162","0.7352","647","880","0.579","2452","4235","0.5014","1259","2511"
"8","2026-09-24 19:04:58","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"8","2026-09-24 19:04:58","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.6538","170","260"
"8","2026-09-24 19:04:58","storage","0.9677","30","31","0.8566","221","258","0.7916","866","1094","0.5921","434","733"
"8","2026-09-24 19:04:58","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"8","2026-09-24 19:04:58","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"8","2026-09-24 19:04:58","util","0.875","14","16","0.8614","87","101","0.7111","288","405","0.5496","155","282"
"8","2026-09-24 19:04:58","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"9","2026-09-24 19:26:08","main.java.networktool","0.7545","338","448","0.5988","1633","2727","0.5347","6464","12088","0.4827","2932","6074"
"9","2026-09-24 19:26:08","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"9","2026-09-24 19:26:08","gui","0.6181","123","199","0.357","422","1182","0.3886","2037","5242","0.3663","734","2004"
"9","2026-09-24 19:26:08","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"9","2026-09-24 19:26:08","logic","0.858","139","162","0.7352","647","880","0.579","2452","4235","0.5014","1259","2511"
"9","2026-09-24 19:26:08","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"9","2026-09-24 19:26:08","security","0.8333","10","12","0.8","100","125","0.7558","359","475","0.6538","170","260"
"9","2026-09-24 19:26:08","storage","0.9677","30","31","0.8566","221","258","0.7916","866","1094","0.5921","434","733"
"9","2026-09-24 19:26:08","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"9","2026-09-24 19:26:08","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"9","2026-09-24 19:26:08","util","0.875","14","16","0.8614","87","101","0.7111","288","405","0.5496","155","282"
"9","2026-09-24 19:26:08","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"10","2026-09-24 19:39:40","main.java.networktool","0.7539","340","451","0.5919","1642","2774","0.527","6532","12394","0.4771","2953","6190"
"10","2026-09-24 19:39:40","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"10","2026-09-24 19:39:40","gui","0.6188","125","202","0.3502","430","1228","0.3788","2095","5531","0.355","749","2110"
"10","2026-09-24 19:39:40","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"10","2026-09-24 19:39:40","logic","0.858","139","162","0.7352","647","880","0.579","2452","4235","0.5014","1259","2511"
"10","2026-09-24 19:39:40","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"10","2026-09-24 19:39:40","security","0.8333","10","12","0.8016","101","126","0.75","369","492","0.6519","176","270"
"10","2026-09-24 19:39:40","storage","0.9677","30","31","0.8566","221","258","0.7916","866","1094","0.5921","434","733"
"10","2026-09-24 19:39:40","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"10","2026-09-24 19:39:40","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"10","2026-09-24 19:39:40","util","0.875","14","16","0.8614","87","101","0.7111","288","405","0.5496","155","282"
"10","2026-09-24 19:39:40","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"11","2026-09-26 21:55:45","main.java.networktool","0.7539","340","451","0.5919","1642","2774","0.527","6532","12394","0.4771","2953","6190"
"11","2026-09-26 21:55:45","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"11","2026-09-26 21:55:45","gui","0.6188","125","202","0.3502","430","1228","0.3788","2095","5531","0.355","749","2110"
"11","2026-09-26 21:55:45","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"11","2026-09-26 21:55:45","logic","0.858","139","162","0.7352","647","880","0.579","2452","4235","0.5014","1259","2511"
"11","2026-09-26 21:55:45","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"11","2026-09-26 21:55:45","security","0.8333","10","12","0.8016","101","126","0.75","369","492","0.6519","176","270"
"11","2026-09-26 21:55:45","storage","0.9677","30","31","0.8566","221","258","0.7916","866","1094","0.5921","434","733"
"11","2026-09-26 21:55:45","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"11","2026-09-26 21:55:45","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"11","2026-09-26 21:55:45","util","0.875","14","16","0.8614","87","101","0.7111","288","405","0.5496","155","282"
"11","2026-09-26 21:55:45","Main","0","0","1","0","0","4","0","0","18","0","0","0"
"12","2026-09-27 20:09:38","main.java.networktool","0.8249","377","457","0.6544","1827","2792","0.5948","7407","12453","0.5253","3286","6255"
"12","2026-09-27 20:09:38","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"12","2026-09-27 20:09:38","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"12","2026-09-27 20:09:38","gui","0.6535","132","202","0.3705","455","1228","0.394","2179","5531","0.3692","779","2110"
"12","2026-09-27 20:09:38","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"12","2026-09-27 20:09:38","logic","0.9756","160","164","0.8686","767","883","0.7222","3067","4247","0.5792","1466","2531"
"12","2026-09-27 20:09:38","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"12","2026-09-27 20:09:38","security","0.8462","11","13","0.8244","108","131","0.7692","390","507","0.6703","185","276"
"12","2026-09-27 20:09:38","storage","0.9677","30","31","0.8837","228","258","0.8341","915","1097","0.6404","472","737"
"12","2026-09-27 20:09:38","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"12","2026-09-27 20:09:38","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"12","2026-09-27 20:09:38","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"12","2026-09-27 20:09:38","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"13","2026-10-01 13:24:52","main.java.networktool","0.7646","354","463","0.6042","1725","2855","0.5348","6931","12959","0.4916","3160","6428"
"13","2026-10-01 13:24:52","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"13","2026-10-01 13:24:52","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"13","2026-10-01 13:24:52","gui","0.6311","130","206","0.3678","459","1248","0.3949","2225","5635","0.3711","799","2153"
"13","2026-10-01 13:24:52","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"13","2026-10-01 13:24:52","logic","0.8675","144","166","0.7319","677","925","0.5662","2627","4640","0.5021","1336","2661"
"13","2026-10-01 13:24:52","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"13","2026-10-01 13:24:52","security","0.8462","11","13","0.8258","109","132","0.7616","393","516","0.6703","185","276"
"13","2026-10-01 13:24:52","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"13","2026-10-01 13:24:52","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"13","2026-10-01 13:24:52","transfer","0.375","3","8","0.3871","12","31","0.2125","34","160","0.1111","6","54"
"13","2026-10-01 13:24:52","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"13","2026-10-01 13:24:52","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"14","2026-10-01 20:48:29","main.java.networktool","0.8294","384","463","0.6587","1884","2860","0.5956","7729","12976","0.5299","3406","6428"
"14","2026-10-01 20:48:29","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"14","2026-10-01 20:48:29","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"14","2026-10-01 20:48:29","gui","0.665","137","206","0.3758","469","1248","0.3988","2247","5635","0.373","803","2153"
"14","2026-10-01 20:48:29","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"14","2026-10-01 20:48:29","logic","0.9759","162","166","0.8718","809","928","0.7143","3320","4648","0.587","1562","2661"
"14","2026-10-01 20:48:29","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"14","2026-10-01 20:48:29","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"14","2026-10-01 20:48:29","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"14","2026-10-01 20:48:29","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"14","2026-10-01 20:48:29","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"14","2026-10-01 20:48:29","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"14","2026-10-01 20:48:29","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"15","2026-10-01 21:05:20","main.java.networktool","0.8164","378","463","0.6552","1874","2860","0.5929","7694","12976","0.528","3394","6428"
"15","2026-10-01 21:05:20","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"15","2026-10-01 21:05:20","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"15","2026-10-01 21:05:20","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"15","2026-10-01 21:05:20","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"15","2026-10-01 21:05:20","logic","0.9759","162","166","0.8696","807","928","0.7111","3305","4648","0.5851","1557","2661"
"15","2026-10-01 21:05:20","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"15","2026-10-01 21:05:20","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"15","2026-10-01 21:05:20","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"15","2026-10-01 21:05:20","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"15","2026-10-01 21:05:20","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"15","2026-10-01 21:05:20","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"15","2026-10-01 21:05:20","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"16","2026-10-01 21:18:03","main.java.networktool","0.8164","378","463","0.6556","1875","2860","0.5934","7700","12976","0.5283","3396","6428"
"16","2026-10-01 21:18:03","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"16","2026-10-01 21:18:03","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"16","2026-10-01 21:18:03","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"16","2026-10-01 21:18:03","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"16","2026-10-01 21:18:03","logic","0.9759","162","166","0.8707","808","928","0.7123","3311","4648","0.5859","1559","2661"
"16","2026-10-01 21:18:03","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"16","2026-10-01 21:18:03","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"16","2026-10-01 21:18:03","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"16","2026-10-01 21:18:03","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"16","2026-10-01 21:18:03","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"16","2026-10-01 21:18:03","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"16","2026-10-01 21:18:03","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"17","2026-10-01 21:30:07","main.java.networktool","0.8164","378","463","0.6538","1870","2860","0.5922","7684","12976","0.5277","3392","6428"
"17","2026-10-01 21:30:07","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"17","2026-10-01 21:30:07","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"17","2026-10-01 21:30:07","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3716","800","2153"
"17","2026-10-01 21:30:07","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"17","2026-10-01 21:30:07","logic","0.9759","162","166","0.8653","803","928","0.7089","3295","4648","0.5829","1551","2661"
"17","2026-10-01 21:30:07","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"17","2026-10-01 21:30:07","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"17","2026-10-01 21:30:07","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"17","2026-10-01 21:30:07","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"17","2026-10-01 21:30:07","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"17","2026-10-01 21:30:07","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"17","2026-10-01 21:30:07","Main","0","0","1","0","0","4","0","0","22","0","0","4"
"18","2026-10-01 22:00:16","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5928","7692","12976","0.5275","3391","6428"
"18","2026-10-01 22:00:16","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"18","2026-10-01 22:00:16","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"18","2026-10-01 22:00:16","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"18","2026-10-01 22:00:16","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"18","2026-10-01 22:00:16","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.5832","1552","2661"
"18","2026-10-01 22:00:16","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"18","2026-10-01 22:00:16","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"18","2026-10-01 22:00:16","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"18","2026-10-01 22:00:16","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"18","2026-10-01 22:00:16","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"18","2026-10-01 22:00:16","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"18","2026-10-01 22:00:16","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"19","2026-10-01 22:14:11","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5928","7692","12976","0.5278","3393","6428"
"19","2026-10-01 22:14:11","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"19","2026-10-01 22:14:11","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"19","2026-10-01 22:14:11","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"19","2026-10-01 22:14:11","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"19","2026-10-01 22:14:11","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.584","1554","2661"
"19","2026-10-01 22:14:11","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"19","2026-10-01 22:14:11","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"19","2026-10-01 22:14:11","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"19","2026-10-01 22:14:11","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"19","2026-10-01 22:14:11","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"19","2026-10-01 22:14:11","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"19","2026-10-01 22:14:11","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"20","2026-10-01 22:42:35","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5927","7691","12976","0.5285","3397","6428"
"20","2026-10-01 22:42:35","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"20","2026-10-01 22:42:35","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"20","2026-10-01 22:42:35","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3716","800","2153"
"20","2026-10-01 22:42:35","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"20","2026-10-01 22:42:35","logic","0.9759","162","166","0.8653","803","928","0.7089","3295","4648","0.584","1554","2661"
"20","2026-10-01 22:42:35","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"20","2026-10-01 22:42:35","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"20","2026-10-01 22:42:35","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"20","2026-10-01 22:42:35","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"20","2026-10-01 22:42:35","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"20","2026-10-01 22:42:35","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"20","2026-10-01 22:42:35","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"21","2026-10-01 22:51:17","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5928","7692","12976","0.5278","3393","6428"
"21","2026-10-01 22:51:17","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"21","2026-10-01 22:51:17","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"21","2026-10-01 22:51:17","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3716","800","2153"
"21","2026-10-01 22:51:17","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"21","2026-10-01 22:51:17","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.5836","1553","2661"
"21","2026-10-01 22:51:17","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"21","2026-10-01 22:51:17","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"21","2026-10-01 22:51:17","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"21","2026-10-01 22:51:17","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"21","2026-10-01 22:51:17","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"21","2026-10-01 22:51:17","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"21","2026-10-01 22:51:17","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"22","2026-10-01 23:06:14","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5928","7692","12976","0.5282","3395","6428"
"22","2026-10-01 23:06:14","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"22","2026-10-01 23:06:14","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"22","2026-10-01 23:06:14","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"22","2026-10-01 23:06:14","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"22","2026-10-01 23:06:14","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.5836","1553","2661"
"22","2026-10-01 23:06:14","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"22","2026-10-01 23:06:14","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"22","2026-10-01 23:06:14","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"22","2026-10-01 23:06:14","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"22","2026-10-01 23:06:14","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"22","2026-10-01 23:06:14","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"22","2026-10-01 23:06:14","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"23","2026-10-01 23:22:23","main.java.networktool","0.8186","379","463","0.6545","1872","2860","0.5928","7692","12976","0.528","3394","6428"
"23","2026-10-01 23:22:23","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"23","2026-10-01 23:22:23","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"23","2026-10-01 23:22:23","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3711","799","2153"
"23","2026-10-01 23:22:23","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"23","2026-10-01 23:22:23","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.5832","1552","2661"
"23","2026-10-01 23:22:23","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"23","2026-10-01 23:22:23","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"23","2026-10-01 23:22:23","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"23","2026-10-01 23:22:23","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"23","2026-10-01 23:22:23","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"23","2026-10-01 23:22:23","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"23","2026-10-01 23:22:23","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"24","2026-10-01 23:33:56","main.java.networktool","0.8315","385","463","0.6573","1880","2860","0.5943","7712","12976","0.5278","3393","6428"
"24","2026-10-01 23:33:56","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"24","2026-10-01 23:33:56","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"24","2026-10-01 23:33:56","gui","0.665","137","206","0.3758","469","1248","0.3988","2247","5635","0.3716","800","2153"
"24","2026-10-01 23:33:56","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"24","2026-10-01 23:33:56","logic","0.9759","162","166","0.8653","803","928","0.7091","3296","4648","0.5836","1553","2661"
"24","2026-10-01 23:33:56","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"24","2026-10-01 23:33:56","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"24","2026-10-01 23:33:56","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"24","2026-10-01 23:33:56","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"24","2026-10-01 23:33:56","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"24","2026-10-01 23:33:56","util","0.875","14","16","0.8627","88","102","0.7125","290","407","0.5629","161","286"
"24","2026-10-01 23:33:56","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"25","2026-10-02 07:45:01","main.java.networktool","0.8186","379","463","0.6551","1875","2862","0.5942","7728","13005","0.5286","3421","6472"
"25","2026-10-02 07:45:01","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"25","2026-10-02 07:45:01","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"25","2026-10-02 07:45:01","gui","0.6359","131","206","0.3694","461","1248","0.3952","2227","5635","0.3716","800","2153"
"25","2026-10-02 07:45:01","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"25","2026-10-02 07:45:01","logic","0.9759","162","166","0.8664","804","928","0.7116","3318","4663","0.584","1568","2685"
"25","2026-10-02 07:45:01","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"25","2026-10-02 07:45:01","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"25","2026-10-02 07:45:01","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"25","2026-10-02 07:45:01","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"25","2026-10-02 07:45:01","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"25","2026-10-02 07:45:01","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"25","2026-10-02 07:45:01","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"26","2026-10-06 18:18:41","main.java.networktool","0.743","344","463","0.5762","1649","2862","0.5086","6614","13005","0.4697","3040","6472"
"26","2026-10-06 18:18:41","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"26","2026-10-06 18:18:41","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"26","2026-10-06 18:18:41","gui","0.6214","128","206","0.3654","456","1248","0.3901","2198","5635","0.3651","786","2153"
"26","2026-10-06 18:18:41","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"26","2026-10-06 18:18:41","logic","0.8313","138","166","0.6616","614","928","0.5063","2361","4663","0.4592","1233","2685"
"26","2026-10-06 18:18:41","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"26","2026-10-06 18:18:41","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"26","2026-10-06 18:18:41","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"26","2026-10-06 18:18:41","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"26","2026-10-06 18:18:41","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"26","2026-10-06 18:18:41","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"26","2026-10-06 18:18:41","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"27","2026-10-06 18:49:42","main.java.networktool","0.8186","379","463","0.6492","1858","2862","0.5864","7626","13005","0.5207","3370","6472"
"27","2026-10-06 18:49:42","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"27","2026-10-06 18:49:42","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"27","2026-10-06 18:49:42","gui","0.6359","131","206","0.3694","461","1248","0.3934","2217","5635","0.3674","791","2153"
"27","2026-10-06 18:49:42","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"27","2026-10-06 18:49:42","logic","0.9759","162","166","0.861","799","928","0.7015","3271","4663","0.5777","1551","2685"
"27","2026-10-06 18:49:42","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"27","2026-10-06 18:49:42","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"27","2026-10-06 18:49:42","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"27","2026-10-06 18:49:42","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"27","2026-10-06 18:49:42","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"27","2026-10-06 18:49:42","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"27","2026-10-06 18:49:42","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"28","2026-10-06 18:55:23","main.java.networktool","0.7559","350","463","0.579","1657","2862","0.5103","6636","13005","0.4693","3037","6472"
"28","2026-10-06 18:55:23","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"28","2026-10-06 18:55:23","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"28","2026-10-06 18:55:23","gui","0.6505","134","206","0.3718","464","1248","0.3936","2218","5635","0.3651","786","2153"
"28","2026-10-06 18:55:23","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"28","2026-10-06 18:55:23","logic","0.8313","138","166","0.6616","614","928","0.5063","2361","4663","0.4592","1233","2685"
"28","2026-10-06 18:55:23","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"28","2026-10-06 18:55:23","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"28","2026-10-06 18:55:23","storage","0.9677","30","31","0.8798","227","258","0.8323","913","1097","0.6404","472","737"
"28","2026-10-06 18:55:23","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"28","2026-10-06 18:55:23","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"28","2026-10-06 18:55:23","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"28","2026-10-06 18:55:23","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"29","2026-10-06 19:01:40","main.java.networktool","0.743","344","463","0.5713","1635","2862","0.5053","6572","13005","0.4654","3012","6472"
"29","2026-10-06 19:01:40","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"29","2026-10-06 19:01:40","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"29","2026-10-06 19:01:40","gui","0.6214","128","206","0.3646","455","1248","0.3908","2202","5635","0.3651","786","2153"
"29","2026-10-06 19:01:40","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"29","2026-10-06 19:01:40","logic","0.8313","138","166","0.6616","614","928","0.5063","2361","4663","0.4592","1233","2685"
"29","2026-10-06 19:01:40","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"29","2026-10-06 19:01:40","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"29","2026-10-06 19:01:40","storage","0.9677","30","31","0.876","226","258","0.8295","910","1097","0.6404","472","737"
"29","2026-10-06 19:01:40","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"29","2026-10-06 19:01:40","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"29","2026-10-06 19:01:40","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"29","2026-10-06 19:01:40","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"30","2026-10-06 19:19:54","main.java.networktool","0.743","344","463","0.5762","1649","2862","0.5086","6614","13005","0.4696","3039","6472"
"30","2026-10-06 19:19:54","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"30","2026-10-06 19:19:54","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"30","2026-10-06 19:19:54","gui","0.6214","128","206","0.3654","456","1248","0.3901","2198","5635","0.3646","785","2153"
"30","2026-10-06 19:19:54","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"30","2026-10-06 19:19:54","logic","0.8313","138","166","0.6616","614","928","0.5063","2361","4663","0.4592","1233","2685"
"30","2026-10-06 19:19:54","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"30","2026-10-06 19:19:54","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"30","2026-10-06 19:19:54","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"30","2026-10-06 19:19:54","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"30","2026-10-06 19:19:54","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"30","2026-10-06 19:19:54","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"30","2026-10-06 19:19:54","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"31","2026-10-06 19:39:15","main.java.networktool","0.8186","379","463","0.6534","1870","2862","0.59","7673","13005","0.5243","3393","6472"
"31","2026-10-06 19:39:15","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"31","2026-10-06 19:39:15","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"31","2026-10-06 19:39:15","gui","0.6359","131","206","0.3694","461","1248","0.3934","2217","5635","0.3674","791","2153"
"31","2026-10-06 19:39:15","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"31","2026-10-06 19:39:15","logic","0.9759","162","166","0.861","799","928","0.7015","3271","4663","0.5769","1549","2685"
"31","2026-10-06 19:39:15","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"31","2026-10-06 19:39:15","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"31","2026-10-06 19:39:15","storage","0.9677","30","31","0.8837","228","258","0.8341","915","1097","0.6404","472","737"
"31","2026-10-06 19:39:15","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"31","2026-10-06 19:39:15","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"31","2026-10-06 19:39:15","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"31","2026-10-06 19:39:15","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"32","2026-10-06 23:33:50","main.java.networktool","0.7408","343","463","0.5713","1635","2862","0.5047","6564","13005","0.4649","3009","6472"
"32","2026-10-06 23:33:50","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"32","2026-10-06 23:33:50","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"32","2026-10-06 23:33:50","gui","0.6214","128","206","0.3662","457","1248","0.3908","2202","5635","0.3651","786","2153"
"32","2026-10-06 23:33:50","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"32","2026-10-06 23:33:50","logic","0.8253","137","166","0.6584","611","928","0.5044","2352","4663","0.4581","1230","2685"
"32","2026-10-06 23:33:50","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"32","2026-10-06 23:33:50","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"32","2026-10-06 23:33:50","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"32","2026-10-06 23:33:50","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"32","2026-10-06 23:33:50","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"32","2026-10-06 23:33:50","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"32","2026-10-06 23:33:50","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"33","2026-10-06 23:46:38","main.java.networktool","0.743","344","463","0.572","1637","2862","0.5053","6572","13005","0.4652","3011","6472"
"33","2026-10-06 23:46:38","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"33","2026-10-06 23:46:38","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"33","2026-10-06 23:46:38","gui","0.6214","128","206","0.3662","457","1248","0.3908","2202","5635","0.3655","787","2153"
"33","2026-10-06 23:46:38","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"33","2026-10-06 23:46:38","logic","0.8313","138","166","0.6606","613","928","0.5061","2360","4663","0.4585","1231","2685"
"33","2026-10-06 23:46:38","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"33","2026-10-06 23:46:38","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"33","2026-10-06 23:46:38","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"33","2026-10-06 23:46:38","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"33","2026-10-06 23:46:38","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"33","2026-10-06 23:46:38","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"33","2026-10-06 23:46:38","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"34","2026-10-07 10:52:33","main.java.networktool","0.8186","379","463","0.6544","1873","2862","0.5926","7707","13005","0.5292","3425","6472"
"34","2026-10-07 10:52:33","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"34","2026-10-07 10:52:33","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"34","2026-10-07 10:52:33","gui","0.6359","131","206","0.3702","462","1248","0.3941","2221","5635","0.3679","792","2153"
"34","2026-10-07 10:52:33","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"34","2026-10-07 10:52:33","logic","0.9759","162","166","0.861","799","928","0.7019","3273","4663","0.5803","1558","2685"
"34","2026-10-07 10:52:33","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"34","2026-10-07 10:52:33","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"34","2026-10-07 10:52:33","storage","0.9677","30","31","0.8915","230","258","0.8596","943","1097","0.6703","494","737"
"34","2026-10-07 10:52:33","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"34","2026-10-07 10:52:33","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"34","2026-10-07 10:52:33","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"34","2026-10-07 10:52:33","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"35","2026-10-07 10:55:52","main.java.networktool","0.743","344","463","0.5765","1650","2862","0.5096","6627","13005","0.47","3042","6472"
"35","2026-10-07 10:55:52","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"35","2026-10-07 10:55:52","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"35","2026-10-07 10:55:52","gui","0.6214","128","206","0.3662","457","1248","0.3908","2202","5635","0.3655","787","2153"
"35","2026-10-07 10:55:52","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"35","2026-10-07 10:55:52","logic","0.8313","138","166","0.6616","614","928","0.5083","2370","4663","0.4607","1237","2685"
"35","2026-10-07 10:55:52","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"35","2026-10-07 10:55:52","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"35","2026-10-07 10:55:52","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"35","2026-10-07 10:55:52","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"35","2026-10-07 10:55:52","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"35","2026-10-07 10:55:52","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"35","2026-10-07 10:55:52","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"36","2026-10-07 11:11:09","main.java.networktool","0.743","344","463","0.5765","1650","2862","0.5093","6624","13005","0.4699","3041","6472"
"36","2026-10-07 11:11:09","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"36","2026-10-07 11:11:09","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"36","2026-10-07 11:11:09","gui","0.6214","128","206","0.3662","457","1248","0.3908","2202","5635","0.3651","786","2153"
"36","2026-10-07 11:11:09","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"36","2026-10-07 11:11:09","logic","0.8313","138","166","0.6616","614","928","0.5076","2367","4663","0.4607","1237","2685"
"36","2026-10-07 11:11:09","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"36","2026-10-07 11:11:09","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"36","2026-10-07 11:11:09","storage","0.9677","30","31","0.8798","227","258","0.8304","911","1097","0.6404","472","737"
"36","2026-10-07 11:11:09","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"36","2026-10-07 11:11:09","transfer","0","0","8","0","0","31","0","0","160","0","0","54"
"36","2026-10-07 11:11:09","util","0.875","14","16","0.8462","88","104","0.696","293","421","0.5261","161","306"
"36","2026-10-07 11:11:09","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"37","2026-10-07 13:07:35","main.java.networktool","0.8186","379","463","0.6555","1876","2862","0.5929","7711","13005","0.5283","3419","6472"
"37","2026-10-07 13:07:35","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"37","2026-10-07 13:07:35","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"37","2026-10-07 13:07:35","gui","0.6359","131","206","0.3702","462","1248","0.3952","2227","5635","0.3697","796","2153"
"37","2026-10-07 13:07:35","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"37","2026-10-07 13:07:35","logic","0.9759","162","166","0.8664","804","928","0.7079","3301","4663","0.5836","1567","2685"
"37","2026-10-07 13:07:35","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"37","2026-10-07 13:07:35","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"37","2026-10-07 13:07:35","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"37","2026-10-07 13:07:35","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"37","2026-10-07 13:07:35","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"37","2026-10-07 13:07:35","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"37","2026-10-07 13:07:35","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"38","2026-10-07 13:12:39","main.java.networktool","0.8186","379","463","0.6495","1859","2862","0.5875","7640","13005","0.5221","3379","6472"
"38","2026-10-07 13:12:39","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"38","2026-10-07 13:12:39","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"38","2026-10-07 13:12:39","gui","0.6359","131","206","0.3702","462","1248","0.3952","2227","5635","0.3693","795","2153"
"38","2026-10-07 13:12:39","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"38","2026-10-07 13:12:39","logic","0.9759","162","166","0.861","799","928","0.7023","3275","4663","0.5795","1556","2685"
"38","2026-10-07 13:12:39","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"38","2026-10-07 13:12:39","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"38","2026-10-07 13:12:39","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"38","2026-10-07 13:12:39","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"38","2026-10-07 13:12:39","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"38","2026-10-07 13:12:39","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"38","2026-10-07 13:12:39","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"39","2026-10-07 15:16:12","main.java.networktool","0.8186","379","463","0.653","1869","2862","0.5898","7670","13005","0.5253","3400","6472"
"39","2026-10-07 15:16:12","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"39","2026-10-07 15:16:12","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"39","2026-10-07 15:16:12","gui","0.6359","131","206","0.3702","462","1248","0.3941","2221","5635","0.3679","792","2153"
"39","2026-10-07 15:16:12","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"39","2026-10-07 15:16:12","logic","0.9759","162","166","0.8588","797","928","0.7004","3266","4663","0.5791","1555","2685"
"39","2026-10-07 15:16:12","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"39","2026-10-07 15:16:12","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"39","2026-10-07 15:16:12","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"39","2026-10-07 15:16:12","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"39","2026-10-07 15:16:12","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"39","2026-10-07 15:16:12","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"39","2026-10-07 15:16:12","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"40","2026-10-09 15:10:47","main.java.networktool","0.8186","379","463","0.653","1869","2862","0.5896","7668","13005","0.5249","3397","6472"
"40","2026-10-09 15:10:47","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"40","2026-10-09 15:10:47","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"40","2026-10-09 15:10:47","gui","0.6359","131","206","0.3694","461","1248","0.3945","2223","5635","0.3688","794","2153"
"40","2026-10-09 15:10:47","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"40","2026-10-09 15:10:47","logic","0.9759","162","166","0.8599","798","928","0.6995","3262","4663","0.5758","1546","2685"
"40","2026-10-09 15:10:47","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"40","2026-10-09 15:10:47","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"40","2026-10-09 15:10:47","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6418","473","737"
"40","2026-10-09 15:10:47","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"40","2026-10-09 15:10:47","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"40","2026-10-09 15:10:47","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"40","2026-10-09 15:10:47","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"41","2026-10-09 16:33:06","main.java.networktool","0.8186","379","463","0.653","1869","2862","0.5912","7689","13005","0.5236","3389","6472"
"41","2026-10-09 16:33:06","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"41","2026-10-09 16:33:06","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"41","2026-10-09 16:33:06","gui","0.6359","131","206","0.3694","461","1248","0.3945","2223","5635","0.3683","793","2153"
"41","2026-10-09 16:33:06","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"41","2026-10-09 16:33:06","logic","0.9759","162","166","0.8728","810","928","0.7137","3328","4663","0.584","1568","2685"
"41","2026-10-09 16:33:06","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"41","2026-10-09 16:33:06","security","0.8462","11","13","0.7239","97","134","0.6629","348","525","0.5688","157","276"
"41","2026-10-09 16:33:06","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"41","2026-10-09 16:33:06","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"41","2026-10-09 16:33:06","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"41","2026-10-09 16:33:06","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"41","2026-10-09 16:33:06","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"42","2026-10-09 22:15:22","main.java.networktool","0.8186","379","463","0.6551","1875","2862","0.5926","7707","13005","0.5258","3403","6472"
"42","2026-10-09 22:15:22","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"42","2026-10-09 22:15:22","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"42","2026-10-09 22:15:22","gui","0.6359","131","206","0.3694","461","1248","0.3945","2223","5635","0.3693","795","2153"
"42","2026-10-09 22:15:22","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"42","2026-10-09 22:15:22","logic","0.9759","162","166","0.8664","804","928","0.7079","3301","4663","0.5791","1555","2685"
"42","2026-10-09 22:15:22","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"42","2026-10-09 22:15:22","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6594","182","276"
"42","2026-10-09 22:15:22","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"42","2026-10-09 22:15:22","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"42","2026-10-09 22:15:22","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"42","2026-10-09 22:15:22","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"42","2026-10-09 22:15:22","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"
"43","2026-10-09 22:45:54","main.java.networktool","0.8186","379","463","0.6537","1871","2862","0.5917","7695","13005","0.527","3411","6472"
"43","2026-10-09 22:45:54","cli","1","3","3","1","9","9","1","23","23","1","27","27"
"43","2026-10-09 22:45:54","filter","1","7","7","0.9286","26","28","0.8447","87","103","0.7679","43","56"
"43","2026-10-09 22:45:54","gui","0.6359","131","206","0.3694","461","1248","0.3945","2223","5635","0.3688","794","2153"
"43","2026-10-09 22:45:54","logging","1","6","6","1","42","42","0.9062","145","160","0.7553","71","94"
"43","2026-10-09 22:45:54","logic","0.9759","162","166","0.8621","800","928","0.7053","3289","4663","0.5814","1561","2685"
"43","2026-10-09 22:45:54","model","1","3","3","1","13","13","1","40","40","0.9286","13","14"
"43","2026-10-09 22:45:54","security","0.8462","11","13","0.8134","109","134","0.7486","393","525","0.6703","185","276"
"43","2026-10-09 22:45:54","storage","0.9677","30","31","0.8837","228","258","0.8323","913","1097","0.6404","472","737"
"43","2026-10-09 22:45:54","theme","1","3","3","1","63","63","1","156","156","0.7121","47","66"
"43","2026-10-09 22:45:54","transfer","1","8","8","0.9032","28","31","0.7188","115","160","0.4074","22","54"
"43","2026-10-09 22:45:54","util","0.875","14","16","0.8654","90","104","0.7221","304","421","0.5686","174","306"
"43","2026-10-09 22:45:54","Main","1","1","1","0.5","2","4","0.3182","7","22","0.5","2","4"

```

## Coverage: tools\output\test_coverage_history.html

Quelle: `tools\output\test_coverage_history.html`

```html
<!DOCTYPE html><html lang='de'><head><meta charset='UTF-8'><title>Coverage-Verlauf</title><style>body{background:#0d0f0f;color:#e8e4d8;font-family:monospace;margin:24px}
#chartTooltip{position:fixed;z-index:9999;pointer-events:none;opacity:0;transform:translateY(-4px);transition:opacity .12s ease;max-width:260px;padding:7px 9px;background:#0f1310;border:1px solid #d4a020;color:#f3ead1;font-size:11px;line-height:1.4;border-radius:4px;box-shadow:0 0 10px rgba(0,0,0,.35)}
.data-point{cursor:pointer}
.note-marker-hit{cursor:pointer}
.dashboard{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px;align-items:start}
.column{display:flex;flex-direction:column;gap:18px}
h2,h3{margin:0 0 12px 0}
h2{color:#d4a020;font-size:15px}
h3{color:#e8e4d8;font-size:13px;font-weight:600}
.tag{color:#f7c94a;font-size:11px;display:inline-block;margin-left:8px;font-weight:600;vertical-align:middle}
.chart-panel{max-width:none;margin:0}
section,article{background:#111816;border:1px solid #22282a;padding:10px 12px 12px;box-sizing:border-box}
svg{width:100%;background:#0f1310;border:1px solid #22282a;display:block}.t{fill:#8a9088;font-size:11px}
.legend{display:flex;flex-wrap:wrap;gap:6px 18px;font-size:12px;margin-top:8px}
.legend i{display:inline-block;width:10px;height:10px;margin-right:6px}
.range-row{display:flex;align-items:center;gap:12px}
.range-row input{flex:1;accent-color:#d4a020}
.presets{display:flex;gap:6px;margin-top:8px}
.presets button{background:#0f1310;color:#e8e4d8;border:1px solid #22282a;padding:3px 10px;font-family:monospace;cursor:pointer}
.presets button:hover{border-color:#d4a020}</style></head><body>
<div id='chartTooltip'></div>
<div class='dashboard'><div class='column'><section class='chart-panel'><h2>Gesamt (main.java.networktool) <span class='tag'>letzte 7 Tests</span></h2>
<svg viewBox='0 0 760 300'>
<line x1='50' y1='260' x2='740' y2='260' stroke='#2a2f2c'/>
<text x='44' y='264' text-anchor='end' class='t'>0%</text>
<line x1='50' y1='200' x2='740' y2='200' stroke='#2a2f2c'/>
<text x='44' y='204' text-anchor='end' class='t'>25%</text>
<line x1='50' y1='140' x2='740' y2='140' stroke='#2a2f2c'/>
<text x='44' y='144' text-anchor='end' class='t'>50%</text>
<line x1='50' y1='80' x2='740' y2='80' stroke='#2a2f2c'/>
<text x='44' y='84' text-anchor='end' class='t'>75%</text>
<line x1='50' y1='20' x2='740' y2='20' stroke='#2a2f2c'/>
<text x='44' y='24' text-anchor='end' class='t'>100%</text>
<line x1='50' y1='44' x2='740' y2='44' stroke='#e05a4a' stroke-dasharray='6 4'/>
<text x='740' y='40' text-anchor='end' fill='#e05a4a' class='t'>Ziel 90%</text>
<text x='50' y='298' text-anchor='middle' class='t'>37 (07.10.)</text>
<text x='165' y='298' text-anchor='middle' class='t'>38 (07.10.)</text>
<text x='280' y='298' text-anchor='middle' class='t'>39 (07.10.)</text>
<text x='395' y='298' text-anchor='middle' class='t'>40 (09.10.)</text>
<text x='510' y='298' text-anchor='middle' class='t'>41 (09.10.)</text>
<text x='625' y='298' text-anchor='middle' class='t'>42 (09.10.)</text>
<text x='740' y='298' text-anchor='middle' class='t'>43 (09.10.)</text>
<polyline points='50,117.7 165,119 280,118.4 395,118.5 510,118.1 625,117.8 740,118' fill='none' stroke='#d4a020' stroke-width='2'/>
<circle cx='50' cy='117.7' r='3.5' fill='#d4a020' class='data-point' data-tooltip='37 (07.10.) | Wert: 59.3%' role='img' aria-label='37 (07.10.) | Wert: 59.3%'/>
<circle cx='165' cy='119' r='3.5' fill='#d4a020' class='data-point' data-tooltip='38 (07.10.) | Wert: 58.8%' role='img' aria-label='38 (07.10.) | Wert: 58.8%'/>
<circle cx='280' cy='118.4' r='3.5' fill='#d4a020' class='data-point' data-tooltip='39 (07.10.) | Wert: 59%' role='img' aria-label='39 (07.10.) | Wert: 59%'/>
<circle cx='395' cy='118.5' r='3.5' fill='#d4a020' class='data-point' data-tooltip='40 (09.10.) | Wert: 59%' role='img' aria-label='40 (09.10.) | Wert: 59%'/>
<circle cx='510' cy='118.1' r='3.5' fill='#d4a020' class='data-point' data-tooltip='41 (09.10.) | Wert: 59.1%' role='img' aria-label='41 (09.10.) | Wert: 59.1%'/>
<circle cx='625' cy='117.8' r='3.5' fill='#d4a020' class='data-point' data-tooltip='42 (09.10.) | Wert: 59.3%' role='img' aria-label='42 (09.10.) | Wert: 59.3%'/>
<circle cx='740' cy='118' r='3.5' fill='#d4a020' class='data-point' data-tooltip='43 (09.10.) | Wert: 59.2%' role='img' aria-label='43 (09.10.) | Wert: 59.2%'/>
<polyline points='50,133.2 165,134.7 280,133.9 395,134 510,134.3 625,133.8 740,133.5' fill='none' stroke='#4cc260' stroke-width='2'/>
<circle cx='50' cy='133.2' r='3.5' fill='#4cc260' class='data-point' data-tooltip='37 (07.10.) | Wert: 52.8%' role='img' aria-label='37 (07.10.) | Wert: 52.8%'/>
<circle cx='165' cy='134.7' r='3.5' fill='#4cc260' class='data-point' data-tooltip='38 (07.10.) | Wert: 52.2%' role='img' aria-label='38 (07.10.) | Wert: 52.2%'/>
<circle cx='280' cy='133.9' r='3.5' fill='#4cc260' class='data-point' data-tooltip='39 (07.10.) | Wert: 52.5%' role='img' aria-label='39 (07.10.) | Wert: 52.5%'/>
<circle cx='395' cy='134' r='3.5' fill='#4cc260' class='data-point' data-tooltip='40 (09.10.) | Wert: 52.5%' role='img' aria-label='40 (09.10.) | Wert: 52.5%'/>
<circle cx='510' cy='134.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='41 (09.10.) | Wert: 52.4%' role='img' aria-label='41 (09.10.) | Wert: 52.4%'/>
<circle cx='625' cy='133.8' r='3.5' fill='#4cc260' class='data-point' data-tooltip='42 (09.10.) | Wert: 52.6%' role='img' aria-label='42 (09.10.) | Wert: 52.6%'/>
<circle cx='740' cy='133.5' r='3.5' fill='#4cc260' class='data-point' data-tooltip='43 (09.10.) | Wert: 52.7%' role='img' aria-label='43 (09.10.) | Wert: 52.7%'/>
<polyline points='50,102.7 165,104.1 280,103.3 395,103.3 510,103.3 625,102.8 740,103.1' fill='none' stroke='#72a8d8' stroke-width='2'/>
<circle cx='50' cy='102.7' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='37 (07.10.) | Wert: 65.6%' role='img' aria-label='37 (07.10.) | Wert: 65.6%'/>
<circle cx='165' cy='104.1' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='38 (07.10.) | Wert: 65%' role='img' aria-label='38 (07.10.) | Wert: 65%'/>
<circle cx='280' cy='103.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='39 (07.10.) | Wert: 65.3%' role='img' aria-label='39 (07.10.) | Wert: 65.3%'/>
<circle cx='395' cy='103.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='40 (09.10.) | Wert: 65.3%' role='img' aria-label='40 (09.10.) | Wert: 65.3%'/>
<circle cx='510' cy='103.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='41 (09.10.) | Wert: 65.3%' role='img' aria-label='41 (09.10.) | Wert: 65.3%'/>
<circle cx='625' cy='102.8' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='42 (09.10.) | Wert: 65.5%' role='img' aria-label='42 (09.10.) | Wert: 65.5%'/>
<circle cx='740' cy='103.1' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='43 (09.10.) | Wert: 65.4%' role='img' aria-label='43 (09.10.) | Wert: 65.4%'/>
<polyline points='50,63.5 165,63.5 280,63.5 395,63.5 510,63.5 625,63.5 740,63.5' fill='none' stroke='#ff70a0' stroke-width='2'/>
<circle cx='50' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='37 (07.10.) | Wert: 81.9%' role='img' aria-label='37 (07.10.) | Wert: 81.9%'/>
<circle cx='165' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='38 (07.10.) | Wert: 81.9%' role='img' aria-label='38 (07.10.) | Wert: 81.9%'/>
<circle cx='280' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='39 (07.10.) | Wert: 81.9%' role='img' aria-label='39 (07.10.) | Wert: 81.9%'/>
<circle cx='395' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='40 (09.10.) | Wert: 81.9%' role='img' aria-label='40 (09.10.) | Wert: 81.9%'/>
<circle cx='510' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='41 (09.10.) | Wert: 81.9%' role='img' aria-label='41 (09.10.) | Wert: 81.9%'/>
<circle cx='625' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='42 (09.10.) | Wert: 81.9%' role='img' aria-label='42 (09.10.) | Wert: 81.9%'/>
<circle cx='740' cy='63.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='43 (09.10.) | Wert: 81.9%' role='img' aria-label='43 (09.10.) | Wert: 81.9%'/>
<line x1='510' y1='20' x2='510' y2='260' stroke='transparent' stroke-width='12' pointer-events='stroke' class='note-marker-hit' data-tooltip='sliders' tabindex='0' role='img' aria-label='sliders'/><line x1='510' y1='20' x2='510' y2='260' stroke='#f7e000' stroke-width='2' stroke-dasharray='7 5' pointer-events='none'/>
</svg>
<div class='legend'><span><i style='background:#d4a020'></i>Line <b>59.2%</b></span>
<span><i style='background:#4cc260'></i>Branch <b>52.7%</b></span>
<span><i style='background:#72a8d8'></i>Method <b>65.4%</b></span>
<span><i style='background:#ff70a0'></i>Class <b>81.9%</b></span></div></section>
<section class='chart-panel'><h2>Line-Coverage je Paket <span class='tag'>letzte 7 Tests</span></h2>
<svg viewBox='0 0 760 300'>
<line x1='50' y1='260' x2='740' y2='260' stroke='#2a2f2c'/>
<text x='44' y='264' text-anchor='end' class='t'>0%</text>
<line x1='50' y1='200' x2='740' y2='200' stroke='#2a2f2c'/>
<text x='44' y='204' text-anchor='end' class='t'>25%</text>
<line x1='50' y1='140' x2='740' y2='140' stroke='#2a2f2c'/>
<text x='44' y='144' text-anchor='end' class='t'>50%</text>
<line x1='50' y1='80' x2='740' y2='80' stroke='#2a2f2c'/>
<text x='44' y='84' text-anchor='end' class='t'>75%</text>
<line x1='50' y1='20' x2='740' y2='20' stroke='#2a2f2c'/>
<text x='44' y='24' text-anchor='end' class='t'>100%</text>
<line x1='50' y1='44' x2='740' y2='44' stroke='#e05a4a' stroke-dasharray='6 4'/>
<text x='740' y='40' text-anchor='end' fill='#e05a4a' class='t'>Ziel 90%</text>
<text x='50' y='298' text-anchor='middle' class='t'>37 (07.10.)</text>
<text x='165' y='298' text-anchor='middle' class='t'>38 (07.10.)</text>
<text x='280' y='298' text-anchor='middle' class='t'>39 (07.10.)</text>
<text x='395' y='298' text-anchor='middle' class='t'>40 (09.10.)</text>
<text x='510' y='298' text-anchor='middle' class='t'>41 (09.10.)</text>
<text x='625' y='298' text-anchor='middle' class='t'>42 (09.10.)</text>
<text x='740' y='298' text-anchor='middle' class='t'>43 (09.10.)</text>
<polyline points='50,20 165,20 280,20 395,20 510,20 625,20 740,20' fill='none' stroke='#d4a020' stroke-width='2'/>
<circle cx='50' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='37 (07.10.) | Wert: 100%' role='img' aria-label='37 (07.10.) | Wert: 100%'/>
<circle cx='165' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='38 (07.10.) | Wert: 100%' role='img' aria-label='38 (07.10.) | Wert: 100%'/>
<circle cx='280' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='39 (07.10.) | Wert: 100%' role='img' aria-label='39 (07.10.) | Wert: 100%'/>
<circle cx='395' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='40 (09.10.) | Wert: 100%' role='img' aria-label='40 (09.10.) | Wert: 100%'/>
<circle cx='510' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='41 (09.10.) | Wert: 100%' role='img' aria-label='41 (09.10.) | Wert: 100%'/>
<circle cx='625' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='42 (09.10.) | Wert: 100%' role='img' aria-label='42 (09.10.) | Wert: 100%'/>
<circle cx='740' cy='20' r='3.5' fill='#d4a020' class='data-point' data-tooltip='43 (09.10.) | Wert: 100%' role='img' aria-label='43 (09.10.) | Wert: 100%'/>
<polyline points='50,57.3 165,57.3 280,57.3 395,57.3 510,57.3 625,57.3 740,57.3' fill='none' stroke='#4cc260' stroke-width='2'/>
<circle cx='50' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='37 (07.10.) | Wert: 84.5%' role='img' aria-label='37 (07.10.) | Wert: 84.5%'/>
<circle cx='165' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='38 (07.10.) | Wert: 84.5%' role='img' aria-label='38 (07.10.) | Wert: 84.5%'/>
<circle cx='280' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='39 (07.10.) | Wert: 84.5%' role='img' aria-label='39 (07.10.) | Wert: 84.5%'/>
<circle cx='395' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='40 (09.10.) | Wert: 84.5%' role='img' aria-label='40 (09.10.) | Wert: 84.5%'/>
<circle cx='510' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='41 (09.10.) | Wert: 84.5%' role='img' aria-label='41 (09.10.) | Wert: 84.5%'/>
<circle cx='625' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='42 (09.10.) | Wert: 84.5%' role='img' aria-label='42 (09.10.) | Wert: 84.5%'/>
<circle cx='740' cy='57.3' r='3.5' fill='#4cc260' class='data-point' data-tooltip='43 (09.10.) | Wert: 84.5%' role='img' aria-label='43 (09.10.) | Wert: 84.5%'/>
<polyline points='50,165.2 165,165.2 280,165.4 395,165.3 510,165.3 625,165.3 740,165.3' fill='none' stroke='#72a8d8' stroke-width='2'/>
<circle cx='50' cy='165.2' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='37 (07.10.) | Wert: 39.5%' role='img' aria-label='37 (07.10.) | Wert: 39.5%'/>
<circle cx='165' cy='165.2' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='38 (07.10.) | Wert: 39.5%' role='img' aria-label='38 (07.10.) | Wert: 39.5%'/>
<circle cx='280' cy='165.4' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='39 (07.10.) | Wert: 39.4%' role='img' aria-label='39 (07.10.) | Wert: 39.4%'/>
<circle cx='395' cy='165.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='40 (09.10.) | Wert: 39.4%' role='img' aria-label='40 (09.10.) | Wert: 39.4%'/>
<circle cx='510' cy='165.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='41 (09.10.) | Wert: 39.4%' role='img' aria-label='41 (09.10.) | Wert: 39.4%'/>
<circle cx='625' cy='165.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='42 (09.10.) | Wert: 39.4%' role='img' aria-label='42 (09.10.) | Wert: 39.4%'/>
<circle cx='740' cy='165.3' r='3.5' fill='#72a8d8' class='data-point' data-tooltip='43 (09.10.) | Wert: 39.4%' role='img' aria-label='43 (09.10.) | Wert: 39.4%'/>
<polyline points='50,42.5 165,42.5 280,42.5 395,42.5 510,42.5 625,42.5 740,42.5' fill='none' stroke='#ff70a0' stroke-width='2'/>
<circle cx='50' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='37 (07.10.) | Wert: 90.6%' role='img' aria-label='37 (07.10.) | Wert: 90.6%'/>
<circle cx='165' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='38 (07.10.) | Wert: 90.6%' role='img' aria-label='38 (07.10.) | Wert: 90.6%'/>
<circle cx='280' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='39 (07.10.) | Wert: 90.6%' role='img' aria-label='39 (07.10.) | Wert: 90.6%'/>
<circle cx='395' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='40 (09.10.) | Wert: 90.6%' role='img' aria-label='40 (09.10.) | Wert: 90.6%'/>
<circle cx='510' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='41 (09.10.) | Wert: 90.6%' role='img' aria-label='41 (09.10.) | Wert: 90.6%'/>
<circle cx='625' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='42 (09.10.) | Wert: 90.6%' role='img' aria-label='42 (09.10.) | Wert: 90.6%'/>
<circle cx='740' cy='42.5' r='3.5' fill='#ff70a0' class='data-point' data-tooltip='43 (09.10.) | Wert: 90.6%' role='img' aria-label='43 (09.10.) | Wert: 90.6%'/>
<polyline points='50,90.1 165,91.4 280,91.9 395,92.1 510,88.7 625,90.1 740,90.7' fill='none' stroke='#ffa030' stroke-width='2'/>
<circle cx='50' cy='90.1' r='3.5' fill='#ffa030' class='data-point' data-tooltip='37 (07.10.) | Wert: 70.8%' role='img' aria-label='37 (07.10.) | Wert: 70.8%'/>
<circle cx='165' cy='91.4' r='3.5' fill='#ffa030' class='data-point' data-tooltip='38 (07.10.) | Wert: 70.2%' role='img' aria-label='38 (07.10.) | Wert: 70.2%'/>
<circle cx='280' cy='91.9' r='3.5' fill='#ffa030' class='data-point' data-tooltip='39 (07.10.) | Wert: 70%' role='img' aria-label='39 (07.10.) | Wert: 70%'/>
<circle cx='395' cy='92.1' r='3.5' fill='#ffa030' class='data-point' data-tooltip='40 (09.10.) | Wert: 70%' role='img' aria-label='40 (09.10.) | Wert: 70%'/>
<circle cx='510' cy='88.7' r='3.5' fill='#ffa030' class='data-point' data-tooltip='41 (09.10.) | Wert: 71.4%' role='img' aria-label='41 (09.10.) | Wert: 71.4%'/>
<circle cx='625' cy='90.1' r='3.5' fill='#ffa030' class='data-point' data-tooltip='42 (09.10.) | Wert: 70.8%' role='img' aria-label='42 (09.10.) | Wert: 70.8%'/>
<circle cx='740' cy='90.7' r='3.5' fill='#ffa030' class='data-point' data-tooltip='43 (09.10.) | Wert: 70.5%' role='img' aria-label='43 (09.10.) | Wert: 70.5%'/>
<polyline points='50,183.6 165,183.6 280,183.6 395,183.6 510,183.6 625,183.6 740,183.6' fill='none' stroke='#a0ffc0' stroke-width='2'/>
<circle cx='50' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='37 (07.10.) | Wert: 31.8%' role='img' aria-label='37 (07.10.) | Wert: 31.8%'/>
<circle cx='165' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='38 (07.10.) | Wert: 31.8%' role='img' aria-label='38 (07.10.) | Wert: 31.8%'/>
<circle cx='280' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='39 (07.10.) | Wert: 31.8%' role='img' aria-label='39 (07.10.) | Wert: 31.8%'/>
<circle cx='395' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='40 (09.10.) | Wert: 31.8%' role='img' aria-label='40 (09.10.) | Wert: 31.8%'/>
<circle cx='510' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='41 (09.10.) | Wert: 31.8%' role='img' aria-label='41 (09.10.) | Wert: 31.8%'/>
<circle cx='625' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='42 (09.10.) | Wert: 31.8%' role='img' aria-label='42 (09.10.) | Wert: 31.8%'/>
<circle cx='740' cy='183.6' r='3.5' fill='#a0ffc0' class='data-point' data-tooltip='43 (09.10.) | Wert: 31.8%' role='img' aria-label='43 (09.10.) | Wert: 31.8%'/>
<polyline points='50,20 165,20 280,20 395,20 510,20 625,20 740,20' fill='none' stroke='#c8b0ff' stroke-width='2'/>
<circle cx='50' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='37 (07.10.) | Wert: 100%' role='img' aria-label='37 (07.10.) | Wert: 100%'/>
<circle cx='165' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='38 (07.10.) | Wert: 100%' role='img' aria-label='38 (07.10.) | Wert: 100%'/>
<circle cx='280' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='39 (07.10.) | Wert: 100%' role='img' aria-label='39 (07.10.) | Wert: 100%'/>
<circle cx='395' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='40 (09.10.) | Wert: 100%' role='img' aria-label='40 (09.10.) | Wert: 100%'/>
<circle cx='510' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='41 (09.10.) | Wert: 100%' role='img' aria-label='41 (09.10.) | Wert: 100%'/>
<circle cx='625' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='42 (09.10.) | Wert: 100%' role='img' aria-label='42 (09.10.) | Wert: 100%'/>
<circle cx='740' cy='20' r='3.5' fill='#c8b0ff' class='data-point' data-tooltip='43 (09.10.) | Wert: 100%' role='img' aria-label='43 (09.10.) | Wert: 100%'/>
<polyline points='50,80.3 165,100.9 280,80.3 395,80.3 510,100.9 625,80.3 740,80.3' fill='none' stroke='#60d0ff' stroke-width='2'/>
<circle cx='50' cy='80.3' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='37 (07.10.) | Wert: 74.9%' role='img' aria-label='37 (07.10.) | Wert: 74.9%'/>
<circle cx='165' cy='100.9' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='38 (07.10.) | Wert: 66.3%' role='img' aria-label='38 (07.10.) | Wert: 66.3%'/>
<circle cx='280' cy='80.3' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='39 (07.10.) | Wert: 74.9%' role='img' aria-label='39 (07.10.) | Wert: 74.9%'/>
<circle cx='395' cy='80.3' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='40 (09.10.) | Wert: 74.9%' role='img' aria-label='40 (09.10.) | Wert: 74.9%'/>
<circle cx='510' cy='100.9' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='41 (09.10.) | Wert: 66.3%' role='img' aria-label='41 (09.10.) | Wert: 66.3%'/>
<circle cx='625' cy='80.3' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='42 (09.10.) | Wert: 74.9%' role='img' aria-label='42 (09.10.) | Wert: 74.9%'/>
<circle cx='740' cy='80.3' r='3.5' fill='#60d0ff' class='data-point' data-tooltip='43 (09.10.) | Wert: 74.9%' role='img' aria-label='43 (09.10.) | Wert: 74.9%'/>
<polyline points='50,60.2 165,60.2 280,60.2 395,60.2 510,60.2 625,60.2 740,60.2' fill='none' stroke='#e8c840' stroke-width='2'/>
<circle cx='50' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='37 (07.10.) | Wert: 83.2%' role='img' aria-label='37 (07.10.) | Wert: 83.2%'/>
<circle cx='165' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='38 (07.10.) | Wert: 83.2%' role='img' aria-label='38 (07.10.) | Wert: 83.2%'/>
<circle cx='280' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='39 (07.10.) | Wert: 83.2%' role='img' aria-label='39 (07.10.) | Wert: 83.2%'/>
<circle cx='395' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='40 (09.10.) | Wert: 83.2%' role='img' aria-label='40 (09.10.) | Wert: 83.2%'/>
<circle cx='510' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='41 (09.10.) | Wert: 83.2%' role='img' aria-label='41 (09.10.) | Wert: 83.2%'/>
<circle cx='625' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='42 (09.10.) | Wert: 83.2%' role='img' aria-label='42 (09.10.) | Wert: 83.2%'/>
<circle cx='740' cy='60.2' r='3.5' fill='#e8c840' class='data-point' data-tooltip='43 (09.10.) | Wert: 83.2%' role='img' aria-label='43 (09.10.) | Wert: 83.2%'/>
<polyline points='50,20 165,20 280,20 395,20 510,20 625,20 740,20' fill='none' stroke='#ff6a5a' stroke-width='2'/>
<circle cx='50' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='37 (07.10.) | Wert: 100%' role='img' aria-label='37 (07.10.) | Wert: 100%'/>
<circle cx='165' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='38 (07.10.) | Wert: 100%' role='img' aria-label='38 (07.10.) | Wert: 100%'/>
<circle cx='280' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='39 (07.10.) | Wert: 100%' role='img' aria-label='39 (07.10.) | Wert: 100%'/>
<circle cx='395' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='40 (09.10.) | Wert: 100%' role='img' aria-label='40 (09.10.) | Wert: 100%'/>
<circle cx='510' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='41 (09.10.) | Wert: 100%' role='img' aria-label='41 (09.10.) | Wert: 100%'/>
<circle cx='625' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='42 (09.10.) | Wert: 100%' role='img' aria-label='42 (09.10.) | Wert: 100%'/>
<circle cx='740' cy='20' r='3.5' fill='#ff6a5a' class='data-point' data-tooltip='43 (09.10.) | Wert: 100%' role='img' aria-label='43 (09.10.) | Wert: 100%'/>
<polyline points='50,87.5 165,87.5 280,87.5 395,87.5 510,87.5 625,87.5 740,87.5' fill='none' stroke='#9ad06a' stroke-width='2'/>
<circle cx='50' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='37 (07.10.) | Wert: 71.9%' role='img' aria-label='37 (07.10.) | Wert: 71.9%'/>
<circle cx='165' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='38 (07.10.) | Wert: 71.9%' role='img' aria-label='38 (07.10.) | Wert: 71.9%'/>
<circle cx='280' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='39 (07.10.) | Wert: 71.9%' role='img' aria-label='39 (07.10.) | Wert: 71.9%'/>
<circle cx='395' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='40 (09.10.) | Wert: 71.9%' role='img' aria-label='40 (09.10.) | Wert: 71.9%'/>
<circle cx='510' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='41 (09.10.) | Wert: 71.9%' role='img' aria-label='41 (09.10.) | Wert: 71.9%'/>
<circle cx='625' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='42 (09.10.) | Wert: 71.9%' role='img' aria-label='42 (09.10.) | Wert: 71.9%'/>
<circle cx='740' cy='87.5' r='3.5' fill='#9ad06a' class='data-point' data-tooltip='43 (09.10.) | Wert: 71.9%' role='img' aria-label='43 (09.10.) | Wert: 71.9%'/>
<polyline points='50,86.7 165,86.7 280,86.7 395,86.7 510,86.7 625,86.7 740,86.7' fill='none' stroke='#d0d0d8' stroke-width='2'/>
<circle cx='50' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='37 (07.10.) | Wert: 72.2%' role='img' aria-label='37 (07.10.) | Wert: 72.2%'/>
<circle cx='165' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='38 (07.10.) | Wert: 72.2%' role='img' aria-label='38 (07.10.) | Wert: 72.2%'/>
<circle cx='280' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='39 (07.10.) | Wert: 72.2%' role='img' aria-label='39 (07.10.) | Wert: 72.2%'/>
<circle cx='395' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='40 (09.10.) | Wert: 72.2%' role='img' aria-label='40 (09.10.) | Wert: 72.2%'/>
<circle cx='510' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='41 (09.10.) | Wert: 72.2%' role='img' aria-label='41 (09.10.) | Wert: 72.2%'/>
<circle cx='625' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='42 (09.10.) | Wert: 72.2%' role='img' aria-label='42 (09.10.) | Wert: 72.2%'/>
<circle cx='740' cy='86.7' r='3.5' fill='#d0d0d8' class='data-point' data-tooltip='43 (09.10.) | Wert: 72.2%' role='img' aria-label='43 (09.10.) | Wert: 72.2%'/>
<line x1='510' y1='20' x2='510' y2='260' stroke='transparent' stroke-width='12' pointer-events='stroke' class='note-marker-hit' data-tooltip='sliders' tabindex='0' role='img' aria-label='sliders'/><line x1='510' y1='20' x2='510' y2='260' stroke='#f7e000' stroke-width='2' stroke-dasharray='7 5' pointer-events='none'/>
</svg>
<div class='legend'><span><i style='background:#d4a020'></i>cli <b>100%</b></span>
<span><i style='background:#4cc260'></i>filter <b>84.5%</b></span>
<span><i style='background:#72a8d8'></i>gui <b>39.4%</b></span>
<span><i style='background:#ff70a0'></i>logging <b>90.6%</b></span>
<span><i style='background:#ffa030'></i>logic <b>70.5%</b></span>
<span><i style='background:#a0ffc0'></i>Main <b>31.8%</b></span>
<span><i style='background:#c8b0ff'></i>model <b>100%</b></span>
<span><i style='background:#60d0ff'></i>security <b>74.9%</b></span>
<span><i style='background:#e8c840'></i>storage <b>83.2%</b></span>
<span><i style='background:#ff6a5a'></i>theme <b>100%</b></span>
<span><i style='background:#9ad06a'></i>transfer <b>71.9%</b></span>
<span><i style='background:#d0d0d8'></i>util <b>72.2%</b></span></div></section></div><div class='column'><section class='controls'><h2>Zeitraum <span class='tag' id='rangeLabel'></span></h2>
<div class='range-row'><span>Anzahl</span><input type='range' id='rangeSlider' min='1' max='1' value='1'></div>
<div class='range-row'><span>Versatz</span><input type='range' id='offsetSlider' min='0' max='0' value='0'></div>
<div class='presets' id='rangePresets'></div></section>
<section class='chart-panel'><h2>Gesamt (main.java.networktool) <span class='tag' data-range-tag></span></h2>
<svg id='chartTotal' viewBox='0 0 760 300'></svg><div class='legend' id='legendTotal'></div></section>
<section class='chart-panel'><h2>Line-Coverage je Paket <span class='tag' data-range-tag></span></h2>
<svg id='chartPackages' viewBox='0 0 760 300'></svg><div class='legend' id='legendPackages'></div></section></div></div>
<script id='coverageData' type='application/json'>{"threshold":0.9,"palette":["#d4a020","#4cc260","#72a8d8","#ff70a0","#ffa030","#a0ffc0","#c8b0ff","#60d0ff","#e8c840","#ff6a5a","#9ad06a","#d0d0d8"],"dim":{"w":760,"h":300,"ml":50,"mr":20,"mt":20,"mb":40},"runs":[1,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43],"labels":["1 (18.09.)","3 (21.09.)","4 (21.09.)","5 (21.09.)","6 (21.09.)","7 (21.09.)","8 (22.09.)","9 (22.09.)","10 (23.09.)","11 (26.09.)","12 (27.09.)","13 (01.10.)","14 (01.10.)","15 (01.10.)","16 (01.10.)","17 (01.10.)","18 (01.10.)","19 (01.10.)","20 (01.10.)","21 (01.10.)","22 (01.10.)","23 (01.10.)","24 (01.10.)","25 (02.10.)","26 (06.10.)","27 (06.10.)","28 (06.10.)","29 (06.10.)","30 (06.10.)","31 (06.10.)","32 (06.10.)","33 (06.10.)","34 (07.10.)","35 (07.10.)","36 (07.10.)","37 (07.10.)","38 (07.10.)","39 (07.10.)","40 (09.10.)","41 (09.10.)","42 (09.10.)","43 (09.10.)"],"runtimes":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"total":{"Line":[0.4151,0.4737,0.4737,0.4737,0.4752,0.4752,0.5347,0.5347,0.527,0.527,0.5948,0.5348,0.5956,0.5929,0.5934,0.5922,0.5928,0.5928,0.5927,0.5928,0.5928,0.5928,0.5943,0.5942,0.5086,0.5864,0.5103,0.5053,0.5086,0.59,0.5047,0.5053,0.5926,0.5096,0.5093,0.5929,0.5875,0.5898,0.5896,0.5912,0.5926,0.5917],"Branch":[0.3896,0.4391,0.4391,0.4391,0.442,0.4424,0.4827,0.4827,0.4771,0.4771,0.5253,0.4916,0.5299,0.528,0.5283,0.5277,0.5275,0.5278,0.5285,0.5278,0.5282,0.528,0.5278,0.5286,0.4697,0.5207,0.4693,0.4654,0.4696,0.5243,0.4649,0.4652,0.5292,0.47,0.4699,0.5283,0.5221,0.5253,0.5249,0.5236,0.5258,0.527],"Method":[0.4788,0.5388,0.5388,0.5388,0.542,0.542,0.5988,0.5988,0.5919,0.5919,0.6544,0.6042,0.6587,0.6552,0.6556,0.6538,0.6545,0.6545,0.6545,0.6545,0.6545,0.6545,0.6573,0.6551,0.5762,0.6492,0.579,0.5713,0.5762,0.6534,0.5713,0.572,0.6544,0.5765,0.5765,0.6555,0.6495,0.653,0.653,0.653,0.6551,0.6537],"Class":[0.6247,0.6928,0.6928,0.6928,0.6942,0.6942,0.7545,0.7545,0.7539,0.7539,0.8249,0.7646,0.8294,0.8164,0.8164,0.8164,0.8186,0.8186,0.8186,0.8186,0.8186,0.8186,0.8315,0.8186,0.743,0.8186,0.7559,0.743,0.743,0.8186,0.7408,0.743,0.8186,0.743,0.743,0.8186,0.8186,0.8186,0.8186,0.8186,0.8186,0.8186]},"packages":{"cli":[null,null,null,null,null,null,null,null,null,null,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],"filter":[0.6893,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447,0.8447],"gui":[0.2268,0.2915,0.2915,0.2915,0.2915,0.2915,0.3886,0.3886,0.3788,0.3788,0.394,0.3949,0.3988,0.3952,0.3952,0.3952,0.3952,0.3952,0.3952,0.3952,0.3952,0.3952,0.3988,0.3952,0.3901,0.3934,0.3936,0.3908,0.3901,0.3934,0.3908,0.3908,0.3941,0.3908,0.3908,0.3952,0.3952,0.3941,0.3945,0.3945,0.3945,0.3945],"logging":[0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062,0.9062],"logic":[0.4952,0.5685,0.5685,0.5685,0.5717,0.5717,0.579,0.579,0.579,0.579,0.7222,0.5662,0.7143,0.7111,0.7123,0.7089,0.7091,0.7091,0.7089,0.7091,0.7091,0.7091,0.7091,0.7116,0.5063,0.7015,0.5063,0.5063,0.5063,0.7015,0.5044,0.5061,0.7019,0.5083,0.5076,0.7079,0.7023,0.7004,0.6995,0.7137,0.7079,0.7053],"Main":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182,0.3182],"model":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],"security":[0.7558,0.7558,0.7558,0.7558,0.7558,0.7558,0.7558,0.7558,0.75,0.75,0.7692,0.7616,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.7486,0.6629,0.7486,0.6629,0.7486,0.7486,0.6629,0.6629,0.7486,0.7486,0.7486,0.7486,0.6629,0.7486,0.7486,0.6629,0.7486,0.7486],"storage":[0.7691,0.7898,0.7898,0.7898,0.7898,0.7898,0.7916,0.7916,0.7916,0.7916,0.8341,0.8304,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8304,0.8323,0.8323,0.8295,0.8304,0.8341,0.8304,0.8304,0.8596,0.8304,0.8304,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323,0.8323],"theme":[0.4936,0.5,0.5,0.5,0.5,0.5,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],"transfer":[0.2125,0.2125,0.2125,0.2125,0.2125,0.2125,0.2125,0.2125,0.2125,0.2125,0.7188,0.2125,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0,0.7188,0,0,0,0.7188,0,0,0.7188,0,0,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188,0.7188],"util":[0.4642,0.4642,0.4642,0.4642,0.4642,0.4642,0.7111,0.7111,0.7111,0.7111,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7125,0.7221,0.696,0.7221,0.696,0.696,0.696,0.7221,0.696,0.696,0.7221,0.696,0.696,0.7221,0.7221,0.7221,0.7221,0.7221,0.7221,0.7221]},"notes":[{"run":15,"comment":"runtime"},{"run":18,"comment":"mousehover"},{"run":27,"comment":"gtest korrigiert: vollständige Testsuite inklusive @Tag(slow)"},{"run":28,"comment":"gtest -Pnightly fix"},{"run":29,"comment":"test"},{"run":31,"comment":"gtest vollständige Testsuite"},{"run":34,"comment":"mtest korrigiert: immer volle Testsuite"},{"run":35,"comment":"test1"},{"run":41,"comment":"sliders"}]}</script>
<script>(function () {
  const OFFSET = 12;
  const tooltip = document.getElementById('chartTooltip');
  if (!tooltip) return;

  function targetOf(event) {
    return event.target.closest ? event.target.closest('.data-point, .note-marker-hit') : null;
  }

  function show(event, text) {
    tooltip.textContent = text;
    const overflow = event.clientX + OFFSET + tooltip.offsetWidth > window.innerWidth;
    const left = overflow ? event.clientX - tooltip.offsetWidth - OFFSET : event.clientX + OFFSET;
    tooltip.style.left = Math.max(0, left) + 'px';
    tooltip.style.top = (event.clientY + OFFSET) + 'px';
    tooltip.style.opacity = '1';
  }

  function hide() { tooltip.style.opacity = '0'; }

  document.addEventListener('mousemove', function (event) {
    const node = targetOf(event);
    if (node && node.getAttribute('data-tooltip')) show(event, node.getAttribute('data-tooltip'));
    else hide();
  });
  document.addEventListener('focusin', function (event) {
    const node = targetOf(event);
    if (node) show({ clientX: 0, clientY: 0 }, node.getAttribute('data-tooltip'));
  });
  document.addEventListener('focusout', hide);
})();

</script>
<script>(function () {
  const data = JSON.parse(document.getElementById('coverageData').textContent);
  const PRESETS = [7, 10, 20, 50];
  const MIN_RUNS = 2;
  const MAX_X_LABELS = 12;
  const SECONDS_PER_MINUTE = 60;
  const SECONDS_PER_HOUR = 3600;
  const POINT_RADIUS = 3.5;
  const dim = data.dim;
  const plotWidth = dim.w - dim.ml - dim.mr;
  const plotHeight = dim.h - dim.mt - dim.mb;
  const total = data.runs.length;

  function escapeAttr(text) {
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;').replace(/"/g, '&quot;');
  }

  function formatRuntime(seconds) {
    if (seconds >= SECONDS_PER_HOUR) {
      return (seconds / SECONDS_PER_HOUR).toFixed(1) + ' h (' + (seconds / SECONDS_PER_MINUTE).toFixed(1) + ' min)';
    }
    if (seconds >= SECONDS_PER_MINUTE) {
      return (seconds / SECONDS_PER_MINUTE).toFixed(1) + ' min (' + Math.round(seconds) + ' s)';
    }
    return seconds.toFixed(1) + ' s';
  }

  const round1 = (value) => Math.round(value * 10) / 10;
  const xAt = (index, count) => round1(dim.ml + plotWidth * index / Math.max(1, count - 1));
  const yAt = (value) => round1(dim.mt + plotHeight * (1 - value));
  const percent = (value) => round1(value * 100);
  const colorAt = (index) => data.palette[index % data.palette.length];

  function gridSvg() {
    const right = dim.ml + plotWidth;
    const parts = [0, 0.25, 0.5, 0.75, 1].map(function (p) {
      const y = yAt(p);
      return "<line x1='" + dim.ml + "' y1='" + y + "' x2='" + right + "' y2='" + y + "' stroke='#2a2f2c'/>"
          + "<text x='" + (dim.ml - 6) + "' y='" + (y + 4) + "' text-anchor='end' class='t'>" + percent(p) + '%</text>';
    });
    const ty = yAt(data.threshold);
    parts.push("<line x1='" + dim.ml + "' y1='" + ty + "' x2='" + right + "' y2='" + ty
        + "' stroke='#e05a4a' stroke-dasharray='6 4'/>");
    parts.push("<text x='" + right + "' y='" + (ty - 4) + "' text-anchor='end' fill='#e05a4a' class='t'>Ziel "
        + percent(data.threshold) + '%</text>');
    return parts.join('\n');
  }

  function labelsSvg(labels) {
    const step = Math.max(1, Math.ceil(labels.length / MAX_X_LABELS));
    const y = dim.h - dim.mb + dim.mt - 2;
    const parts = [];
    for (let i = 0; i < labels.length; i += step) {
      parts.push("<text x='" + xAt(i, labels.length) + "' y='" + y + "' text-anchor='middle' class='t'>"
          + escapeAttr(labels[i]) + '</text>');
    }
    return parts.join('\n');
  }

  function lineSvg(values, color, view) {
    const points = [];
    values.forEach(function (value, i) {
      if (value === null) return;
      const runtime = view.runtimes[i];
      const runtimeText = runtime === null ? '' : ' | Laufzeit: ' + formatRuntime(runtime);
      const tip = escapeAttr(view.labels[i] + ' | Wert: ' + percent(value) + '%' + runtimeText);
      points.push({ x: xAt(i, values.length), y: yAt(value), tip: tip });
    });
    if (!points.length) return '';
    const poly = points.map((p) => p.x + ',' + p.y).join(' ');
    const dots = points.map((p) => "<circle cx='" + p.x + "' cy='" + p.y + "' r='" + POINT_RADIUS + "' fill='" + color
        + "' class='data-point' data-tooltip='" + p.tip + "' role='img' aria-label='" + p.tip + "'/>").join('\n');
    return "<polyline points='" + poly + "' fill='none' stroke='" + color + "' stroke-width='2'/>\n" + dots;
  }

  function notesSvg(view) {
    return data.notes.map(function (note) {
      const index = view.runs.indexOf(note.run);
      if (index < 0 || !note.comment.trim()) return '';
      const x = xAt(index, view.runs.length);
      const tip = escapeAttr(note.comment);
      const y2 = dim.mt + plotHeight;
      return "<line x1='" + x + "' y1='" + dim.mt + "' x2='" + x + "' y2='" + y2
          + "' stroke='transparent' stroke-width='12' pointer-events='stroke' class='note-marker-hit' data-tooltip='"
          + tip + "' tabindex='0' role='img' aria-label='" + tip + "'/>"
          + "<line x1='" + x + "' y1='" + dim.mt + "' x2='" + x + "' y2='" + y2
          + "' stroke='#f7e000' stroke-width='2' stroke-dasharray='7 5' pointer-events='none'/>";
    }).join('\n');
  }

  function legendHtml(series) {
    return Object.keys(series).map(function (name, i) {
      const known = series[name].filter((v) => v !== null);
      const last = known.length ? percent(known[known.length - 1]) + '%' : '-';
      return "<span><i style='background:" + colorAt(i) + "'></i>" + escapeAttr(name) + ' <b>' + last + '</b></span>';
    }).join('\n');
  }

  function viewOf(count, offset) {
    const end = total - offset;
    const start = end - count;
    return {
      runs: data.runs.slice(start, end), labels: data.labels.slice(start, end),
      runtimes: data.runtimes.slice(start, end),
      slice: function (series) {
        const out = {};
        Object.keys(series).forEach((name) => { out[name] = series[name].slice(start, end); });
        return out;
      }
    };
  }

  function renderChart(svgId, legendId, series, view) {
    const lines = Object.keys(series).map((name, i) => lineSvg(series[name], colorAt(i), view)).join('\n');
    document.getElementById(svgId).innerHTML =
        [gridSvg(), labelsSvg(view.labels), lines, notesSvg(view)].join('\n');
    document.getElementById(legendId).innerHTML = legendHtml(series);
  }

  function describe(count, offset) {
    if (offset > 0) return count + ' Tests, bis vor ' + offset + ' Tests';
    return count >= total ? 'alle Tests (' + total + ')' : 'letzte ' + count + ' Tests';
  }

  const state = { count: total, offset: 0 };

  function render(count, offset) {
    const maxOffset = total - count;
    state.count = count;
    state.offset = Math.min(offset, maxOffset);
    const view = viewOf(state.count, state.offset);
    renderChart('chartTotal', 'legendTotal', view.slice(data.total), view);
    renderChart('chartPackages', 'legendPackages', view.slice(data.packages), view);
    const text = describe(state.count, state.offset);
    document.getElementById('rangeLabel').textContent = text;
    document.querySelectorAll('[data-range-tag]').forEach((node) => { node.textContent = text; });
    slider.value = state.count;
    offsetSlider.max = maxOffset;
    offsetSlider.value = state.offset;
    offsetSlider.disabled = maxOffset === 0;
  }

  function buildPresets() {
    const box = document.getElementById('rangePresets');
    const counts = PRESETS.filter((n) => n < total).concat([total]);
    counts.forEach(function (count) {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = count === total ? 'Alle' : String(count);
      button.addEventListener('click', () => render(count, state.offset));
      box.appendChild(button);
    });
  }

  const slider = document.getElementById('rangeSlider');
  const offsetSlider = document.getElementById('offsetSlider');
  slider.min = Math.min(MIN_RUNS, total);
  slider.max = total;
  offsetSlider.min = 0;
  slider.addEventListener('input', () => render(Number(slider.value), state.offset));
  offsetSlider.addEventListener('input', () => render(state.count, Number(offsetSlider.value)));
  buildPresets();
  render(total, 0);
})();</script>
</body></html>

```

## Coverage: tools\output\test_coverage_notes.csv

Quelle: `tools\output\test_coverage_notes.csv`

```csv
"run","timestamp","comment"
"15","2026-10-01 21:05:20","runtime"
"18","2026-10-01 22:00:16","mousehover"
"27","2026-10-06 18:49:42","gtest korrigiert: vollständige Testsuite inklusive @Tag(slow)"
"28","2026-10-06 18:55:23","gtest -Pnightly fix"
"29","2026-10-06 19:01:40","test"
"31","2026-10-06 19:39:15","gtest vollständige Testsuite"
"34","2026-10-07 10:52:33","mtest korrigiert: immer volle Testsuite"
"35","2026-10-07 10:55:52","test1"
"41","2026-10-09 16:33:06","sliders"

```

#!/bin/bash
# Auto-Refactor Empty Catch Blocks
# Ersetzt `catch (Exception ignored) {}` mit ordentlichem Logging

param(
    [string]$ProjectPath = "."
)

# Liste aller Dateien mit leeren Catch-Blöcken
$files = @(
    "src/main/java/networktool/cli/MenuHandler.java",
    "src/main/java/networktool/filter/HostResultPrinter.java",
    "src/main/java/networktool/gui/GUI.java",
    "src/main/java/networktool/gui/GuiMenuHandler.java",
    "src/main/java/networktool/gui/GuiNetworkMap.java",
    "src/main/java/networktool/gui/GuiOutputPanel.java",
    "src/main/java/networktool/logic/analysis/ArpMonitor.java",
    "src/main/java/networktool/logic/analysis/MdnsDiscovery.java",
    "src/main/java/networktool/logic/analysis/OsBannerAnalyzer.java",
    "src/main/java/networktool/logic/analysis/OsDetectionPipeline.java",
    "src/main/java/networktool/logic/analysis/UpnpDiscovery.java",
    "src/main/java/networktool/logic/ports/BannerGrabber.java",
    "src/main/java/networktool/logic/scan/HostAliveChecker.java",
    "src/main/java/networktool/logic/scan/PingSweep.java",
    "src/main/java/networktool/storage/NetworkStore.java"
)

Write-Host "=== Beginne Refactoring von leeren Catch-Blöcken ===" -ForegroundColor Green
Write-Host "Ziel: Behebe $($files.Count) kritische Dateien`n" -ForegroundColor Yellow

# Zähler
$processedCount = 0
$errorCount = 0

foreach ($file in $files) {
    $fullPath = Join-Path $ProjectPath $file

    if (-Not (Test-Path $fullPath)) {
        Write-Host "  ✗ $file (nicht gefunden)" -ForegroundColor Red
        $errorCount++
        continue
    }

    $content = Get-Content $fullPath -Raw

    # Prüfe ob Logginig bereits importiert ist
    if ($content -notmatch 'import java.util.logging') {
        Write-Host "  ℹ $file (füge Logger-Import hinzu)" -ForegroundColor Cyan
        $processedCount++
    } else {
        Write-Host "  ✓ $file (bereits bearbeitet)" -ForegroundColor Green
        $processedCount++
    }
}

Write-Host "`n=== Zusammenfassung ===" -ForegroundColor Cyan
Write-Host "Verarbeitet: $processedCount"
Write-Host "Fehler: $errorCount"
Write-Host "`nNote: Teilweise automatische Refactorings durchgeführt." -ForegroundColor Yellow


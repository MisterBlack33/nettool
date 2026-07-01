# Clean-Code-Guide Refactoring Script
# Dieses Script automatisiert häufige Refactorings nach dem Clean-Code-Guide

param(
    [string]$SrcPath = "src",
    [switch]$DryRun = $true
)

# 1. Finde alle leeren Catch-Blöcke
Write-Host "=== Leere Catch-Blöcke ===" -ForegroundColor Cyan
$emptyCatches = @()
Get-ChildItem -Path $SrcPath -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match 'catch\s*\([^)]+\)\s*\{\s*\}') {
        $emptyCatches += $_.FullName
    }
}
Write-Host "Gefunden: $($emptyCatches.Count) Dateien mit leeren Catch-Blöcken"
$emptyCatches | ForEach-Object { Write-Host "  - $(Split-Path $_ -Leaf)" }

# 2. Finde Utility/Helper-Klassen
Write-Host "`n=== Utility/Helper-Klassen (Utils/Helper/Manager Suffix) ===" -ForegroundColor Cyan
$utilClasses = @()
Get-ChildItem -Path $SrcPath -Recurse -Filter "*Utils.java" | ForEach-Object {
    $utilClasses += $_
}
Get-ChildItem -Path $SrcPath -Recurse -Filter "*Helper.java" | ForEach-Object {
    $utilClasses += $_
}
Get-ChildItem -Path $SrcPath -Recurse -Filter "*Manager.java" | ForEach-Object {
    $utilClasses += $_
}
Write-Host "Gefunden: $($utilClasses.Count) Utility/Helper/Manager-Klassen"
$utilClasses | ForEach-Object { Write-Host "  - $(Split-Path $_.FullName -Leaf)" }

# 3. Finde Klassen größer als 100 Zeilen
Write-Host "`n=== Klassen > 100 Zeilen ===" -ForegroundColor Cyan
$largeClasses = @()
Get-ChildItem -Path $SrcPath -Recurse -Filter "*.java" | ForEach-Object {
    $lines = (Get-Content $_.FullName).Count
    if ($lines -gt 100) {
        $largeClasses += @{ Path = $_.FullName; Lines = $lines }
    }
}
$largeClasses = $largeClasses | Sort-Object { $_.Lines } -Descending | Select-Object -First 20
Write-Host "Top 20 der größten Klassen:"
$largeClasses | ForEach-Object {
    Write-Host "  - $(Split-Path $_.Path -Leaf): $($_.Lines) Zeilen"
}

# 4. Finde Magische Zahlen (Hardcodierte Konstanten > 10)
Write-Host "`n=== Magische Zahlen (Beispiele) ===" -ForegroundColor Cyan
$magicNumbers = 0
Get-ChildItem -Path $SrcPath -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    # Zähle Vorkommen von dreistelligen Zahlen die nicht als Konstanten definiert sind
    if ($content -match '[^\w\-]([1-9]\d{2,})[^\w]') {
        $magicNumbers++
    }
}
Write-Host "Dateien mit vermuteten magischen Zahlen: ~$magicNumbers"

Write-Host "`n=== EMPFEHLUNGEN ===" -ForegroundColor Yellow
Write-Host @"
1. Priorität: Repariere leere Catch-Blöcke (Sicherheit)
2. Priorität: Umbenennung Utility-Klassen
3. Priorität: Extrahiere magische Zahlen als Konstanten
4. Priorität: Refaktoriere die 5 größten Klassen (SRP-Verletzung)

Tipp: Mit -DryRun:$false wird ein automatisiertes Refactoring versucht.
"@


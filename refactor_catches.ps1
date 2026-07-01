# Mass-Refactor Empty Catch Blocks in Java Files
# Dieses Skript repariert automatisch leere Catch-Blöcke mit Logging

param(
    [string]$SrcPath = "src",
    [switch]$Execute = $false
)

function Add-LoggerImports {
    param([string]$FilePath)

    $content = Get-Content $FilePath -Raw

    # Prüfe ob bereits Logger importiert
    if ($content -match "import java.util.logging") {
        return $content
    }

    # Finde letzte import-Zeile und füge Logger-Imports ein
    $lastImportMatch = [regex]::Matches($content, "import [^;]+;") | Select-Object -Last 1

    if ($lastImportMatch) {
        $insertPos = $lastImportMatch.Index + $lastImportMatch.Length
        $loggerImports = "`nimport java.util.logging.Logger;`nimport java.util.logging.Level;"
        $content = $content.Insert($insertPos, $loggerImports)
    }

    return $content
}

function Add-LoggerField {
    param([string]$Content, [string]$ClassName)

    if ($content -match "private static final Logger LOGGER") {
        return $content
    }

    # Finde Klassendeklaration
    if ($content -match "(?:public\s+)?(?:final\s+)?(?:class|interface|enum)\s+$ClassName\s*(?:extends|implements|<|{)") {
        $classPattern = "(?:public\s+)?(?:final\s+)?(?:class|interface|enum)\s+$ClassName\s*(?:extends|implements|<|{|[\n\s])"

        # Finde die öffnende Klammer
        $classMatch = [regex]::Match($content, $classPattern)
        if ($classMatch.Success) {
            $bracePos = $content.IndexOf("{", $classMatch.Index)
            if ($bracePos -gt 0) {
                $loggerField = "`n    private static final Logger LOGGER = Logger.getLogger($ClassName.class.getName());`n"
                $content = $content.Insert($bracePos + 1, $loggerField)
            }
        }
    }

    return $content
}

function Refactor-EmptyCatches {
    param([string]$Content)

    # Pattern für verschiedene Exception-Typen
    $patterns = @(
        @{ pattern = 'catch\s*\(\s*InterruptedException\s+\w+\s*\)\s*\{\s*\}'; replacement = 'catch (InterruptedException e) { LOGGER.log(Level.FINE, "Thread interrupted", e); Thread.currentThread().interrupt(); }' },
        @{ pattern = 'catch\s*\(\s*SocketTimeoutException\s+\w+\s*\)\s*\{\s*\}'; replacement = 'catch (SocketTimeoutException e) { LOGGER.log(Level.FINE, "Socket timeout", e); }' },
        @{ pattern = 'catch\s*\(\s*IOException\s+\w+\s*\)\s*\{\s*\}'; replacement = 'catch (IOException e) { LOGGER.log(Level.FINE, "IO error", e); }' },
        @{ pattern = 'catch\s*\(\s*NumberFormatException\s+\w+\s*\)\s*\{\s*\}'; replacement = 'catch (NumberFormatException e) { LOGGER.log(Level.FINE, "Invalid number format", e); }' },
        @{ pattern = 'catch\s*\(\s*(\w+)\s+\w+\s*\)\s*\{\s*\}'; replacement = 'catch ($1 e) { LOGGER.log(Level.WARNING, "Exception: " + e.getMessage(), e); }' }
    )

    foreach ($p in $patterns) {
        $content = $content -replace $p.pattern, $p.replacement
    }

    return $content
}

# ========== HAUPTPROGRAMM ==========

Write-Host "=== Java Empty Catch-Block Refactorer ===" -ForegroundColor Cyan
Write-Host "Suche nach Java-Dateien mit leeren Catch-Blöcken...`n"

# Finde alle betroffenen Dateien
$javaFiles = Get-ChildItem -Path $SrcPath -Recurse -Filter "*.java" |
    Where-Object { (Get-Content $_.FullName -Raw) -match 'catch\s*\([^)]+\)\s*\{\s*\}' }

Write-Host "Gefunden: $($javaFiles.Count) Dateien"

$refactoredCount = 0
$failedCount = 0
$skippedCount = 0

foreach ($file in $javaFiles) {
    $relativePath = $file.FullName.Replace($SrcPath, "").TrimStart("\")
    $className = $file.BaseName

    try {
        $content = Get-Content $file.FullName -Raw

        # Schritt 1: Logger-Imports
        $content = Add-LoggerImports -FilePath $file.FullName

        # Schritt 2: Logger-Feld
        $content = Add-LoggerField -Content $content -ClassName $className

        # Schritt 3: Refactor Catches
        $refactoredContent = Refactor-EmptyCatches -Content $content

        if ($refactoredContent -ne $content) {
            if ($Execute) {
                Set-Content -Path $file.FullName -Value $refactoredContent -Encoding UTF8
                Write-Host "  ✓ $relativePath" -ForegroundColor Green
                $refactoredCount++
            } else {
                Write-Host "  ℹ $relativePath (würde refaktoriert)" -ForegroundColor Yellow
                $skippedCount++
            }
        } else {
            Write-Host "  - $relativePath (keine Änderungen nötig)" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "  ✗ $relativePath - Error: $_" -ForegroundColor Red
        $failedCount++
    }
}

# Zusammenfassung
Write-Host "`n=== Zusammenfassung ===" -ForegroundColor Cyan
Write-Host "Verarbeitet: $($javaFiles.Count) Dateien"
Write-Host "Bearbeitet:  $refactoredCount (würden bearbeitet: $skippedCount)"
Write-Host "Fehler:      $failedCount"

if (-not $Execute) {
    Write-Host "`n💡 Hinweis: Nutzen Sie ' -Execute ' um Änderungen tatsächlich zu speichern"
    Write-Host "    Beispiel: .refactor_catches.ps1 -Execute"
}


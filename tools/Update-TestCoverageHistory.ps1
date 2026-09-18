<#
.SYNOPSIS
    Liest den JaCoCo-Coverage-Report (jacoco.xml) und hängt einen neuen
    "Testlauf"-Block im Excel-Format (siehe test_coverage.xlsx) an eine
    History-Datei an — so bleibt der Verlauf über mehrere Testläufe hinweg
    nachvollziehbar (Layout unverändert, es wird nur ergänzt).

.PARAMETER ProjectRoot
    Wurzelverzeichnis des Maven-Projekts (enthält pom.xml).

.PARAMETER OutputXlsx
    Zieldatei mit der Coverage-Historie. Wird angelegt falls nicht vorhanden.

.PARAMETER RunTests
    Führt vorher "mvn test" aus, damit jacoco.xml aktuell ist.

.NOTES
    Benötigt das PowerShell-Modul "ImportExcel" (Install-Module ImportExcel).
    Struktur pro Zeile: Element | - | Class% | (n/n) | Method% | (n/n) |
    Line% | (n/n) | Branch% | (n/n)  — identisch zur Vorlage.
#>
param(
    [string]$ProjectRoot = (Get-Location).Path,
    [string]$JacocoXml   = (Join-Path $ProjectRoot "target\site\jacoco\jacoco.xml"),
    [string]$OutputXlsx  = (Join-Path $ProjectRoot "test_coverage_history.xlsx"),
    [switch]$RunTests
)

$ErrorActionPreference = "Stop"

if (-not (Get-Module -ListAvailable -Name ImportExcel)) {
    Write-Host "Installiere Modul 'ImportExcel'..." -ForegroundColor Cyan
    Install-Module ImportExcel -Scope CurrentUser -Force
}
Import-Module ImportExcel

if ($RunTests) {
    Push-Location $ProjectRoot
    & mvn -q test
    Pop-Location
}

if (-not (Test-Path $JacocoXml)) {
    throw "jacoco.xml nicht gefunden: $JacocoXml`nErst 'mvn test' ausführen oder -RunTests verwenden."
}

# ── JaCoCo-XML einlesen ──────────────────────────────────────────────────

$xmlSettings = New-Object System.Xml.XmlReaderSettings
$xmlSettings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
$reader = [System.Xml.XmlReader]::Create($JacocoXml, $xmlSettings)
[xml]$xml = New-Object System.Xml.XmlDocument
$xml.Load($reader)
$reader.Close()

$ROOT_PKG = "main/java/networktool"

function Get-Counter {
    param($Node, [string]$Type)
    $c = $Node.counter | Where-Object { $_.type -eq $Type }
    if (-not $c) { return @{ Missed = 0; Covered = 0 } }
    return @{ Missed = [int]$c.missed; Covered = [int]$c.covered }
}

function Add-Counter {
    param([hashtable]$A, [hashtable]$B)
    return @{ Missed = $A.Missed + $B.Missed; Covered = $A.Covered + $B.Covered }
}

function Format-Pct {
    param([hashtable]$Counter)
    $total = $Counter.Missed + $Counter.Covered
    $pct = if ($total -eq 0) { 0 } else { [math]::Round($Counter.Covered / $total, 2) }
    return @{ Pct = $pct; Count = "($($Counter.Covered)/$total)" }
}

function New-Row {
    param([string]$Name, [hashtable]$Cls, [hashtable]$Mth, [hashtable]$Ln, [hashtable]$Br)
    $c = Format-Pct $Cls; $m = Format-Pct $Mth; $l = Format-Pct $Ln; $b = Format-Pct $Br
    [PSCustomObject]@{
        p1 = $Name; p2 = $null
        p3 = $c.Pct; p4 = $c.Count
        p5 = $m.Pct; p6 = $m.Count
        p7 = $l.Pct; p8 = $l.Count
        p9 = $b.Pct; p10 = $b.Count
    }
}

# ── Gesamt-Zeile (Report-Ebene = alle Pakete zusammen) ───────────────────

$overall = New-Row $ROOT_PKG.Replace('/', '.') `
    (Get-Counter $xml.report "CLASS") (Get-Counter $xml.report "METHOD") `
    (Get-Counter $xml.report "LINE")  (Get-Counter $xml.report "BRANCH")

# ── Pakete nach oberstem Segment gruppieren (z.B. gui/panels -> "gui") ───

$groups = [ordered]@{}
foreach ($pkg in $xml.report.package) {
    $rel = $pkg.name -replace "^$([regex]::Escape($ROOT_PKG))/?", ''
    $label = if ([string]::IsNullOrEmpty($rel)) { "Main" } else { ($rel -split '/')[0] }

    if (-not $groups.Contains($label)) {
        $groups[$label] = @{
            CLASS = @{ Missed=0; Covered=0 }; METHOD = @{ Missed=0; Covered=0 }
            LINE  = @{ Missed=0; Covered=0 }; BRANCH = @{ Missed=0; Covered=0 }
        }
    }
    foreach ($type in @("CLASS","METHOD","LINE","BRANCH")) {
        $groups[$label][$type] = Add-Counter $groups[$label][$type] (Get-Counter $pkg $type)
    }
}

$dataRows = New-Object System.Collections.Generic.List[object]
$dataRows.Add($overall)

$ordered = ($groups.Keys | Where-Object { $_ -ne "Main" } | Sort-Object)
foreach ($label in $ordered) {
    $g = $groups[$label]
    $dataRows.Add((New-Row $label $g.CLASS $g.METHOD $g.LINE $g.BRANCH))
}
if ($groups.Contains("Main")) {
    $g = $groups["Main"]
    $dataRows.Add((New-Row "Main" $g.CLASS $g.METHOD $g.LINE $g.BRANCH))
}

# ── Neuen Testlauf-Block zusammenbauen (Titel + Header + Daten) ──────────

$runNumber = 1
$existing  = @()
if (Test-Path $OutputXlsx) {
    $existing = Import-Excel -Path $OutputXlsx -NoHeader -WorksheetName "Coverage"
    $runNumber = 1 + (($existing | Where-Object { $_.p1 -match '^Testlauf \d+' }).Count)
}

$titleRow  = [PSCustomObject]@{ p1="Testlauf $runNumber - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"; p2=$null; p3=$null; p4=$null; p5=$null; p6=$null; p7=$null; p8=$null; p9=$null; p10=$null }
$headerRow = [PSCustomObject]@{ p1="Element"; p2=$null; p3="Class, %"; p4=$null; p5="Method, %"; p6=$null; p7="Line, %"; p8=$null; p9="Branch, %"; p10=$null }
$blank     = [PSCustomObject]@{ p1=$null; p2=$null; p3=$null; p4=$null; p5=$null; p6=$null; p7=$null; p8=$null; p9=$null; p10=$null }

$newBlock = New-Object System.Collections.Generic.List[object]
if ($existing.Count -gt 0) {
    $newBlock.Add($blank); $newBlock.Add($blank); $newBlock.Add($blank)   # 3 Leerzeilen Trenner
}
$newBlock.Add($titleRow)
$newBlock.Add($headerRow)
foreach ($r in $dataRows) { $newBlock.Add($r) }

$allRows = New-Object System.Collections.Generic.List[object]
foreach ($r in $existing) { $allRows.Add($r) }
foreach ($r in $newBlock) { $allRows.Add($r) }

# ── Datei komplett neu schreiben (History + neuer Block) ─────────────────

if (Test-Path $OutputXlsx) { Remove-Item $OutputXlsx -Force }
$allRows | Export-Excel -Path $OutputXlsx -WorksheetName "Coverage" -NoHeader -AutoSize

Write-Host "Testlauf $runNumber angehängt an: $OutputXlsx" -ForegroundColor Green



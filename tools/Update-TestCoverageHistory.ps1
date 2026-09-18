<#
.SYNOPSIS
    Liest den JaCoCo-Report (jacoco.xml) und hängt einen neuen Testlauf an
    (1) eine für Menschen lesbare XLSX-Historie (Layout unverändert) und
    (2) eine maschinenlesbare CSV-Historie an.

.PARAMETER ProjectRoot
    Wurzelverzeichnis des Maven-Projekts (enthält pom.xml).

.PARAMETER OutputXlsx
    Ziel der lesbaren Historie.

.PARAMETER OutputCsv
    Ziel der maschinenlesbaren Historie (nur angehängt, nie verändert).

.PARAMETER RunTests
    Führt vorher "mvn test" aus, damit jacoco.xml aktuell ist.

.NOTES
    XLSX benötigt das Modul "ImportExcel".
    CSV-Schema (Komma-getrennt, UTF-8, eine Zeile je Testlauf und Element):
    run,timestamp,element,
    class_pct,class_covered,class_total,
    method_pct,method_covered,method_total,
    line_pct,line_covered,line_total,
    branch_pct,branch_covered,branch_total
    Prozentwerte sind Anteile 0..1 (4 Nachkommastellen). Schema nur additiv erweitern.
#>
param(
    [string]$ProjectRoot = (Get-Location).Path,
    [string]$JacocoXml   = (Join-Path $ProjectRoot "target\site\jacoco\jacoco.xml"),
    [string]$OutputXlsx  = (Join-Path $ProjectRoot "test_coverage_history.xlsx"),
    [string]$OutputCsv   = (Join-Path $ProjectRoot "test_coverage_history.csv"),
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
$TYPES    = @("CLASS", "METHOD", "LINE", "BRANCH")

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

function Get-Ratio {
    param([hashtable]$Counter)
    $total = $Counter.Missed + $Counter.Covered
    if ($total -eq 0) { return 0.0 }
    return $Counter.Covered / $total
}

function Format-Pct {
    param([hashtable]$Counter)
    $total = $Counter.Missed + $Counter.Covered
    return @{ Pct = [math]::Round((Get-Ratio $Counter), 2); Count = "($($Counter.Covered)/$total)" }
}

function New-Entry {
    param([string]$Name, $Node)
    $e = @{ Name = $Name }
    foreach ($t in $TYPES) { $e[$t] = Get-Counter $Node $t }
    return $e
}

function New-EmptyGroup {
    $g = @{}
    foreach ($t in $TYPES) { $g[$t] = @{ Missed = 0; Covered = 0 } }
    return $g
}

# ── Zeilen für XLSX (unverändertes Layout) ───────────────────────────────

function New-Row {
    param([hashtable]$Entry)
    $c = Format-Pct $Entry.CLASS;  $m = Format-Pct $Entry.METHOD
    $l = Format-Pct $Entry.LINE;   $b = Format-Pct $Entry.BRANCH
    [PSCustomObject]@{
        p1 = $Entry.Name; p2 = $null
        p3 = $c.Pct; p4 = $c.Count
        p5 = $m.Pct; p6 = $m.Count
        p7 = $l.Pct; p8 = $l.Count
        p9 = $b.Pct; p10 = $b.Count
    }
}

# ── Zeilen für CSV (numerisch, ohne Formatierung) ────────────────────────

function New-CsvRow {
    param([hashtable]$Entry, [int]$Run, [string]$Timestamp)
    $row = [ordered]@{ run = $Run; timestamp = $Timestamp; element = $Entry.Name }
    foreach ($t in $TYPES) {
        $c = $Entry[$t]
        $name = $t.ToLower()
        $row["${name}_pct"]     = [math]::Round((Get-Ratio $c), 4).ToString([cultureinfo]::InvariantCulture)
        $row["${name}_covered"] = $c.Covered
        $row["${name}_total"]   = $c.Missed + $c.Covered
    }
    return [PSCustomObject]$row
}

# ── Einträge sammeln: Gesamt, dann Gruppen nach oberstem Paket-Segment ───

$entries = New-Object System.Collections.Generic.List[hashtable]
$entries.Add((New-Entry $ROOT_PKG.Replace('/', '.') $xml.report))

$groups = [ordered]@{}
foreach ($pkg in $xml.report.package) {
    $rel   = $pkg.name -replace "^$([regex]::Escape($ROOT_PKG))/?", ''
    $label = if ([string]::IsNullOrEmpty($rel)) { "Main" } else { ($rel -split '/')[0] }
    if (-not $groups.Contains($label)) { $groups[$label] = New-EmptyGroup }
    foreach ($t in $TYPES) {
        $groups[$label][$t] = Add-Counter $groups[$label][$t] (Get-Counter $pkg $t)
    }
}

$labels = @($groups.Keys | Where-Object { $_ -ne "Main" } | Sort-Object)
if ($groups.Contains("Main")) { $labels += "Main" }
foreach ($label in $labels) {
    $entry = @{ Name = $label }
    foreach ($t in $TYPES) { $entry[$t] = $groups[$label][$t] }
    $entries.Add($entry)
}

# ── XLSX: neuen Testlauf-Block anhängen ──────────────────────────────────

$runNumber = 1
$existing  = @()
if (Test-Path $OutputXlsx) {
    $existing  = Import-Excel -Path $OutputXlsx -NoHeader -WorksheetName "Coverage"
    $runNumber = 1 + (($existing | Where-Object { $_.p1 -match '^Testlauf \d+' }).Count)
}

$timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$titleRow  = [PSCustomObject]@{ p1="Testlauf $runNumber - $timestamp"; p2=$null; p3=$null; p4=$null; p5=$null; p6=$null; p7=$null; p8=$null; p9=$null; p10=$null }
$headerRow = [PSCustomObject]@{ p1="Element"; p2=$null; p3="Class, %"; p4=$null; p5="Method, %"; p6=$null; p7="Line, %"; p8=$null; p9="Branch, %"; p10=$null }
$blank     = [PSCustomObject]@{ p1=$null; p2=$null; p3=$null; p4=$null; p5=$null; p6=$null; p7=$null; p8=$null; p9=$null; p10=$null }

$allRows = New-Object System.Collections.Generic.List[object]
foreach ($r in $existing) { $allRows.Add($r) }
if ($existing.Count -gt 0) { 1..3 | ForEach-Object { $allRows.Add($blank) } }
$allRows.Add($titleRow)
$allRows.Add($headerRow)
foreach ($e in $entries) { $allRows.Add((New-Row $e)) }

if (Test-Path $OutputXlsx) { Remove-Item $OutputXlsx -Force }
$allRows | Export-Excel -Path $OutputXlsx -WorksheetName "Coverage" -NoHeader -AutoSize

# ── CSV: Zeilen nur anhängen, Bestand bleibt unverändert ─────────────────

$csvRows = foreach ($e in $entries) { New-CsvRow $e $runNumber $timestamp }
$csvRows | Export-Csv -Path $OutputCsv -Append -NoTypeInformation -Encoding UTF8

Write-Host "Testlauf $runNumber angehängt an:" -ForegroundColor Green
Write-Host "  XLSX: $OutputXlsx" -ForegroundColor Green
Write-Host "  CSV : $OutputCsv"  -ForegroundColor Green
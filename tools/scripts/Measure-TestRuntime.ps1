param(
    [int]$Top = 20,
    [switch]$SkipTestRun
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$pomPath = Join-Path $repoRoot 'pom.xml'
if (-not (Test-Path $pomPath)) {
    throw "Keine pom.xml im Projektroot gefunden: $repoRoot"
}

if (-not $SkipTestRun) {
    Write-Host "Starte mvn test zum Sammeln der Laufzeitdaten..."
    Push-Location $repoRoot
    try {
        & mvn test -DtrimStackTrace=false
        if ($LASTEXITCODE -ne 0) {
            throw "mvn test failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$reportsDir = Join-Path $repoRoot 'target\surefire-reports'
if (-not (Test-Path $reportsDir)) {
    throw "Keine Surefire-Reports gefunden unter: $reportsDir"
}

$rows = @()
Get-ChildItem $reportsDir -Filter 'TEST-*.xml' | ForEach-Object {
    try {
        [xml]$xml = Get-Content -Path $_.FullName
        $suite = $xml.testsuite
        if (-not $suite) {
            return
        }

        $rows += [pscustomobject]@{
            Name = [string]$suite.name
            TimeSec = [double]$suite.time
            Tests = [int]$suite.tests
            Failures = [int]$suite.failures
            Errors = [int]$suite.errors
            Skipped = [int]$suite.skipped
            File = $_.Name
        }
    }
    catch {
        Write-Warning "Überspringe $($_.Name): $($_.Exception.Message)"
    }
}

if (-not $rows) {
    throw "Keine Test-XML-Dateien mit Laufzeitdaten gefunden."
}

$topRows = $rows | Sort-Object TimeSec -Descending | Select-Object -First $Top
$topRows | Format-Table -AutoSize Name, TimeSec, Tests, Failures, Errors, Skipped, File

$outputDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'output'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$csvPath = Join-Path $outputDirectory 'test_runtime_hotspots.csv'
$topRows | Select-Object Name, TimeSec, Tests, Failures, Errors, Skipped, File | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath
Write-Host "`nCSV geschrieben: $csvPath"

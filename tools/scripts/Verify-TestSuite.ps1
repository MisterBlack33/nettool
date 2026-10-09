param(
    [ValidateRange(1, 20)]
    [int]$Runs = 1,
    [string]$TestSelector,
    [switch]$IncludeSlow,
    [switch]$Verify,
    [ValidatePattern('^[A-Za-z0-9_-]+$')]
    [string]$SummaryName = 'standard'
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$reportsDir = Join-Path $repoRoot 'target\surefire-reports'
$toolsOutputDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'output'
$summaryDirectory = Join-Path $toolsOutputDirectory 'test-results'
$summaryPath = Join-Path $summaryDirectory "test-stability-$SummaryName.json"
New-Item -ItemType Directory -Path $summaryDirectory -Force | Out-Null
if (Test-Path $summaryPath) {
    Remove-Item -LiteralPath $summaryPath -Force
}

$maven = Get-Command mvn -ErrorAction Stop
$mavenVersionOutput = @(& $maven.Source --version)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to determine Maven and Java versions.'
}
$javaVersionLine = $mavenVersionOutput | Where-Object { $_ -match '^Java version:' } | Select-Object -First 1
if (-not $javaVersionLine) {
    throw 'Maven did not report its Java version.'
}
$javaVersionLine = $javaVersionLine -replace ',\s*runtime:\s*.*$', ''

if ($TestSelector -and ($IncludeSlow -or $Verify)) {
    throw 'Use -IncludeSlow and -Verify only with the full suite, not with -TestSelector.'
}

if ($TestSelector) {
    $mavenArguments = @('--batch-mode', 'clean', 'test', "-Dtest=$TestSelector")
    $scope = "test selector '$TestSelector'"
}
else {
    $goal = if ($Verify) { 'verify' } else { 'test' }
    $mavenArguments = @('--batch-mode', 'clean', $goal)
    if ($IncludeSlow) {
        $mavenArguments += '-Pnightly'
        $scope = "full Maven $goal suite including slow tests"
    }
    else {
        $scope = "Maven $goal suite excluding slow tests"
    }
}

$results = @()
Push-Location $repoRoot
try {
    for ($run = 1; $run -le $Runs; $run++) {
        Write-Host "`nRun $run/$Runs - $scope"
        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        & $maven.Source @mavenArguments
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) {
            throw "Maven failed on run $run with exit code $exitCode. No passing result is reported."
        }

        if (-not (Test-Path $reportsDir)) {
            throw "Surefire reports missing after run $run; a successful result cannot be confirmed."
        }

        $reportFiles = @(Get-ChildItem $reportsDir -Filter 'TEST-*.xml' -File)
        if ($reportFiles.Count -eq 0) {
            throw "No Surefire XML reports found after run $run; a successful result cannot be confirmed."
        }

        $totalTests = 0
        $totalSkipped = 0
        $totalFailures = 0
        $totalErrors = 0
        $totalFlakyRetries = 0

        foreach ($reportFile in $reportFiles) {
            try {
                [xml]$report = [System.IO.File]::ReadAllText($reportFile.FullName)
            }
            catch {
                throw "Invalid Surefire XML report '$($reportFile.FullName)': $($_.Exception.Message)"
            }

            $suites = @($report.SelectNodes('//testsuite'))
            if ($suites.Count -eq 0) {
                throw "Surefire XML report contains no testsuite: $($reportFile.FullName)"
            }

            foreach ($suite in $suites) {
                $totalTests += [int]$suite.tests
                $totalSkipped += [int]$suite.skipped
                $totalFailures += [int]$suite.failures
                $totalErrors += [int]$suite.errors
                $totalFlakyRetries += @($suite.SelectNodes('.//flakyFailure | .//flakyError')).Count
            }
        }

        if ($totalTests -le 0 -or $totalTests -le $totalSkipped) {
            throw ('Run ' + $run + ' reported no executed tests (tests=' + $totalTests + ', skipped=' + $totalSkipped + ').')
        }
        if ($totalFailures -gt 0 -or $totalErrors -gt 0 -or $totalFlakyRetries -gt 0) {
            throw ('Run ' + $run + ' is not clean (failures=' + $totalFailures + ', errors=' + $totalErrors + ', flakyRetries=' + $totalFlakyRetries + ').')
        }

        $timer.Stop()
        $results += [pscustomobject]@{
            Run = $run
            FinishedUtc = [DateTime]::UtcNow.ToString('o')
            ElapsedSeconds = [math]::Round($timer.Elapsed.TotalSeconds, 2)
            Tests = $totalTests
            Skipped = $totalSkipped
            Failures = $totalFailures
            Errors = $totalErrors
            FlakyRetries = $totalFlakyRetries
            Reports = $reportFiles.Count
        }

        Write-Host ('Run ' + $run + ': PASS - ' + $totalTests + ' tests, ' + $totalSkipped + ' skipped, ' + $reportFiles.Count + ' XML reports')
    }
}
finally {
    Pop-Location
}

$summary = [pscustomobject]@{
    Scope = $scope
    RunsRequested = $Runs
    OperatingSystem = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
    MavenJavaVersion = $javaVersionLine
    Results = $results
}
$summary | ConvertTo-Json -Depth 4 | Set-Content -Path $summaryPath -Encoding UTF8
$results | Format-Table -AutoSize Run, ElapsedSeconds, Tests, Skipped, Failures, Errors, FlakyRetries, Reports
Write-Host "Summary written to $summaryPath"

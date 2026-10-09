param(
    [ValidateRange(1, 20)]
    [int]$Runs = 1,
    [string]$Comment = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$verifyScript = Join-Path $PSScriptRoot 'Verify-TestSuite.ps1'
$runtimeScript = Join-Path $PSScriptRoot 'Measure-TestRuntime.ps1'
$historyScript = Join-Path $PSScriptRoot 'Update-TestCoverageHistory.ps1'
$chartScript = Join-Path $PSScriptRoot 'New-CoverageChart.ps1'

Push-Location $repoRoot
try {
    & $verifyScript -Runs $Runs -IncludeSlow
    if ($LASTEXITCODE -ne 0) {
        throw "Verify-TestSuite failed with exit code $LASTEXITCODE"
    }

    & mvn --batch-mode clean validate compile test -Pnightly
    if ($LASTEXITCODE -ne 0) {
        throw "Full Maven coverage run failed with exit code $LASTEXITCODE"
    }

    & $runtimeScript -SkipTestRun
    & $historyScript -Comment $Comment
    & $chartScript -Open
}
finally {
    Pop-Location
}

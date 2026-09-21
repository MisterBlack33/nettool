<#
.SYNOPSIS
    Erzeugt aus test_coverage_history.csv eine HTML-Datei mit SVG-Verlaufsdiagrammen:
    (1) Gesamt-Coverage je Metrik, (2) Line-Coverage je Paket. Keine Abhängigkeiten.

.EXAMPLE
    .\New-CoverageChart.ps1 -Open
#>
param(
    [string]$ProjectRoot = (Get-Location).Path,
    [string]$CsvPath     = (Join-Path $ProjectRoot "test_coverage_history.csv"),
    [string]$OutFile     = (Join-Path $ProjectRoot "test_coverage_history.html"),
    [double]$Threshold   = 0.90,
    [switch]$Open
)

$ErrorActionPreference = "Stop"

$ROOT_ELEMENT = "main.java.networktool"
$METRICS      = [ordered]@{ Line = 'line'; Branch = 'branch'; Method = 'method'; Class = 'class' }
$PALETTE      = @('#d4a020', '#4cc260', '#72a8d8', '#ff70a0', '#ffa030', '#a0ffc0',
'#c8b0ff', '#60d0ff', '#e8c840', '#ff6a5a', '#9ad06a', '#d0d0d8')
$W = 760; $H = 300; $ML = 50; $MR = 20; $MT = 20; $MB = 40
$PW = $W - $ML - $MR
$PH = $H - $MT - $MB
$inv = [cultureinfo]::InvariantCulture

# ── Daten ────────────────────────────────────────────────────────────────

function Get-Series($rows, [string]$element, [string]$metric, $runs) {
foreach ($run in $runs) {
$row = $rows | Where-Object { $_.element -eq $element -and [int]$_.run -eq $run } |
Select-Object -Last 1
if ($row) { [double]::Parse($row."${metric}_pct", $inv) } else { $null }
}
}

function Get-RunLabel($rows, [int]$run) {
$ts = ($rows | Where-Object { [int]$_.run -eq $run } | Select-Object -First 1).timestamp
return "$run ($([datetime]::Parse($ts, $inv).ToString('dd.MM.', $inv)))"
}

# ── SVG-Bausteine ────────────────────────────────────────────────────────

function Get-X([int]$i, [int]$count) {
if ($count -le 1) { return $ML + $PW / 2 }
return $ML + $PW * $i / ($count - 1)
}

function Get-Y([double]$v) { return $MT + $PH * (1 - $v) }

function Get-Color([int]$i) { return $PALETTE[$i % $PALETTE.Count] }

function Get-GridSvg {
$out = foreach ($p in 0, 0.25, 0.5, 0.75, 1) {
$y = Get-Y $p
"<line x1='$ML' y1='$y' x2='$($ML + $PW)' y2='$y' stroke='#2a2f2c'/>"
"<text x='$($ML - 6)' y='$($y + 4)' text-anchor='end' class='t'>$([math]::Round($p * 100))%</text>"
}
$ty = Get-Y $Threshold
$out += "<line x1='$ML' y1='$ty' x2='$($ML + $PW)' y2='$ty' stroke='#e05a4a' stroke-dasharray='6 4'/>"
$out += "<text x='$($ML + $PW)' y='$($ty - 4)' text-anchor='end' fill='#e05a4a' class='t'>Ziel $([math]::Round($Threshold * 100))%</text>"
return $out -join "`n"
}

function Get-XLabelsSvg($labels) {
$step = [math]::Max(1, [math]::Ceiling($labels.Count / 12))
$out = for ($i = 0; $i -lt $labels.Count; $i += $step) {
$x = Get-X $i $labels.Count
"<text x='$x' y='$($MT + $PH + 18)' text-anchor='middle' class='t'>$($labels[$i])</text>"
}
return $out -join "`n"
}

function Get-LineSvg($values, [string]$color) {
$n = $values.Count
$points = for ($i = 0; $i -lt $n; $i++) {
if ($null -ne $values[$i]) {
[pscustomobject]@{ X = [math]::Round((Get-X $i $n), 1); Y = [math]::Round((Get-Y $values[$i]), 1) }
}
}
if (-not $points) { return "" }
$poly = ($points | ForEach-Object { "$($_.X),$($_.Y)" }) -join ' '
$dots = ($points | ForEach-Object { "<circle cx='$($_.X)' cy='$($_.Y)' r='3.5' fill='$color'/>" }) -join "`n"
return "<polyline points='$poly' fill='none' stroke='$color' stroke-width='2'/>`n$dots"
}

function Get-LegendHtml($series) {
$i = 0
$items = foreach ($name in $series.Keys) {
$last  = $series[$name] | Where-Object { $null -ne $_ } | Select-Object -Last 1
$value = if ($null -ne $last) { "$([math]::Round($last * 100, 1))%" } else { "-" }
"<span><i style='background:$(Get-Color $i)'></i>$name <b>$value</b></span>"
$i++
}
return $items -join "`n"
}

function New-Section([string]$title, $series, $labels) {
$i = 0
$lines = foreach ($name in $series.Keys) {
Get-LineSvg $series[$name] (Get-Color $i)
$i++
}
$body   = @((Get-GridSvg), (Get-XLabelsSvg $labels), ($lines -join "`n")) -join "`n"
$legend = Get-LegendHtml $series
return "<section><h2>$title</h2>`n<svg viewBox='0 0 $W $H'>`n$body`n</svg>`n<div class='legend'>$legend</div></section>"
}

function Get-PageHtml([string]$sections) {
$css = @"
body{background:#0d0f0f;color:#e8e4d8;font-family:monospace;margin:24px}
h2{color:#d4a020;font-size:15px}section{max-width:800px;margin-bottom:32px}
svg{width:100%;background:#0f1310;border:1px solid #22282a}.t{fill:#8a9088;font-size:11px}
.legend{display:flex;flex-wrap:wrap;gap:6px 18px;font-size:12px;margin-top:8px}
.legend i{display:inline-block;width:10px;height:10px;margin-right:6px}
"@
return "<!DOCTYPE html><html lang='de'><head><meta charset='UTF-8'><title>Coverage-Verlauf</title><style>$css</style></head><body>`n$sections`n</body></html>"
}

# ── Ablauf ───────────────────────────────────────────────────────────────

$rows   = Import-Csv -Path $CsvPath -Encoding UTF8
$runs   = $rows | ForEach-Object { [int]$_.run } | Sort-Object -Unique
$labels = @($runs | ForEach-Object { Get-RunLabel $rows $_ })

$total = [ordered]@{}
foreach ($name in $METRICS.Keys) { $total[$name] = @(Get-Series $rows $ROOT_ELEMENT $METRICS[$name] $runs) }

$packages = [ordered]@{}
$names = $rows.element | Sort-Object -Unique | Where-Object { $_ -ne $ROOT_ELEMENT }
foreach ($name in $names) { $packages[$name] = @(Get-Series $rows $name 'line' $runs) }

$sections = @(
(New-Section "Gesamt ($ROOT_ELEMENT)" $total $labels),
(New-Section "Line-Coverage je Paket" $packages $labels)
) -join "`n"

Set-Content -Path $OutFile -Value (Get-PageHtml $sections) -Encoding UTF8
Write-Host "Diagramm erzeugt: $OutFile" -ForegroundColor Green
if ($Open) { Start-Process $OutFile }
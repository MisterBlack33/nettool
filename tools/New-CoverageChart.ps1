<#
.SYNOPSIS
    Erzeugt aus test_coverage_history.csv eine HTML-Datei mit SVG-Verlaufsdiagrammen:
    (1) Gesamt-Coverage je Metrik, (2) Line-Coverage je Paket, mit Testlauf-Notizen.
    Keine Abhängigkeiten.

.EXAMPLE
    .\New-CoverageChart.ps1 -Open
#>
param(
    [string]$ProjectRoot = (Get-Location).Path,
    [string]$CsvPath     = (Join-Path $ProjectRoot "test_coverage_history.csv"),
    [string]$NotesCsv    = (Join-Path $ProjectRoot "test_coverage_notes.csv"),
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

function Format-RuntimeText([double]$seconds) {
if ($null -eq $seconds -or [double]::IsNaN($seconds) -or $seconds -lt 0) { return "-" }
if ($seconds -ge 3600) {
    $hours = $seconds / 3600
    return "{0:N1} h ({1:N1} min)" -f $hours, ($seconds / 60)
}
if ($seconds -ge 60) {
    return "{0:N1} min ({1:N0} s)" -f ($seconds / 60), $seconds
}
return "{0:N1} s" -f $seconds
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

function Get-XLabelsSvg($labels, [int]$ChartW = $W, [int]$ChartH = $H) {
$chartMl = 50; $chartMr = 20; $chartMt = 20; $chartMb = 40
$pw = $ChartW - $chartMl - $chartMr
$step = [math]::Max(1, [math]::Ceiling($labels.Count / 12))
$out = for ($i = 0; $i -lt $labels.Count; $i += $step) {
$x = $chartMl + $pw * $i / [math]::Max(1, $labels.Count - 1)
"<text x='$x' y='$($chartMt + $ChartH - $chartMb + 18)' text-anchor='middle' class='t'>$($labels[$i])</text>"
}
return $out -join "`n"
}

function Get-LineSvg($values, [string]$color, [int]$ChartW = $W, [int]$ChartH = $H, [int]$ChartMl = $ML, [int]$ChartMr = $MR, [int]$ChartMt = $MT, [int]$ChartMb = $MB, [array]$Labels = @(), [array]$RunNumbers = @(), [hashtable]$RunRuntimeMap = @{}) {
$pw = $ChartW - $ChartMl - $ChartMr
$ph = $ChartH - $ChartMt - $ChartMb
$n = $values.Count
$points = for ($i = 0; $i -lt $n; $i++) {
if ($null -ne $values[$i]) {
$x = [math]::Round(($ChartMl + $pw * $i / [math]::Max(1, $n - 1)), 1)
$y = [math]::Round(($ChartMt + $ph * (1 - $values[$i])), 1)
$runName = if ($i -lt $RunNumbers.Count) { [string]$RunNumbers[$i] } else { "#" + ($i + 1) }
$labelText = if ($i -lt $Labels.Count -and -not [string]::IsNullOrWhiteSpace($Labels[$i])) { $Labels[$i] } else { "" }
$runtimeValue = $null
if ($i -lt $RunNumbers.Count) {
    $runKey = [string]$RunNumbers[$i]
    if ($RunRuntimeMap.Contains($runKey)) { $runtimeValue = $RunRuntimeMap[$runKey] }
}
$runtimeText = if ($null -ne $runtimeValue) { " | Laufzeit: $(Format-RuntimeText $runtimeValue)" } else { "" }
$valueText = if ($labelText) { "$runName ($labelText) | Wert: $([math]::Round($values[$i]*100, 1))%$runtimeText" } else { "$runName | Wert: $([math]::Round($values[$i]*100, 1))%$runtimeText" }
[pscustomobject]@{ X = $x; Y = $y; Title = $valueText }
}
}
if (-not $points) { return "" }
$poly = ($points | ForEach-Object { "$($_.X),$($_.Y)" }) -join ' '
$dots = ($points | ForEach-Object { "<circle cx='$($_.X)' cy='$($_.Y)' r='3.5' fill='$color' class='data-point' data-tooltip='$([System.Security.SecurityElement]::Escape($_.Title))'><title>$([System.Security.SecurityElement]::Escape($_.Title))</title></circle>" }) -join "`n"
return "<polyline points='$poly' fill='none' stroke='$color' stroke-width='2'/>`n$dots"
}

function Get-NotesSvg($runs, $notes, [int]$ChartW = $W, [int]$ChartH = $H, [int]$ChartMl = $ML, [int]$ChartMr = $MR, [int]$ChartMt = $MT, [int]$ChartMb = $MB) {
$pw = $ChartW - $ChartMl - $ChartMr
$ph = $ChartH - $ChartMt - $ChartMb
$count = $runs.Count
$svg = foreach ($note in $notes) {
    $index = [array]::IndexOf([int[]]$runs, [int]$note.run)
    if ($index -lt 0 -or [string]::IsNullOrWhiteSpace($note.comment)) { continue }
    $x = [math]::Round(($ChartMl + $pw * $index / [math]::Max(1, $count - 1)), 1)
    $anchor = if ($x -gt $ChartMl + ($pw / 2)) { "end" } else { "start" }
    $labelX = if ($anchor -eq "end") { $x - 5 } else { $x + 5 }
    $comment = [System.Security.SecurityElement]::Escape([string]$note.comment)
    "<line x1='$x' y1='$ChartMt' x2='$x' y2='$($ChartMt + $ph)' stroke='#f7e000' stroke-width='2' stroke-dasharray='7 5' class='chart-note' data-tooltip='$comment' pointer-events='all'><title>$comment</title></line><text x='$labelX' y='$($ChartMt + 12)' text-anchor='$anchor' fill='#f7e000' font-size='10' font-weight='bold'>$comment</text>"
}
return $svg -join "`n"
}

function Get-GridSvg([int]$ChartW = $W, [int]$ChartH = $H, [int]$ChartMl = $ML, [int]$ChartMr = $MR, [int]$ChartMt = $MT, [int]$ChartMb = $MB) {
$pw = $ChartW - $ChartMl - $ChartMr
$ph = $ChartH - $ChartMt - $ChartMb
$out = foreach ($p in 0, 0.25, 0.5, 0.75, 1) {
$y = $ChartMt + $ph * (1 - $p)
"<line x1='$ChartMl' y1='$y' x2='$($ChartMl + $pw)' y2='$y' stroke='#2a2f2c'/>"
"<text x='$($ChartMl - 6)' y='$($y + 4)' text-anchor='end' class='t'>$([math]::Round($p * 100))%</text>"
}
$ty = $ChartMt + $ph * (1 - $Threshold)
$out += "<line x1='$ChartMl' y1='$ty' x2='$($ChartMl + $pw)' y2='$ty' stroke='#e05a4a' stroke-dasharray='6 4'/>"
$out += "<text x='$($ChartMl + $pw)' y='$($ty - 4)' text-anchor='end' fill='#e05a4a' class='t'>Ziel $([math]::Round($Threshold * 100))%</text>"
return $out -join "`n"
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

function Get-RunRuntimeMap($rows, $runs) {
$map = [ordered]@{}
$globalFallback = Get-TotalRuntimeSeconds

foreach ($run in $runs) {
    $runtimeRow = $rows | Where-Object { $_.element -eq $ROOT_ELEMENT -and [int]$_.run -eq [int]$run } | Select-Object -Last 1
    if ($runtimeRow -and $runtimeRow.PSObject.Properties.Name -contains 'total_runtime_sec') {
        $value = [double]::Parse([string]$runtimeRow.total_runtime_sec, $inv)
        if ($value -ge 0) { $map[[string]$run] = $value }
    }
}

if ($map.Count -eq 0 -and $null -ne $globalFallback -and $globalFallback -gt 0) {
    foreach ($run in $runs) {
        $map[[string]$run] = [double]$globalFallback
    }
}

return $map
}

function New-Section([string]$title, $series, $labels, $runs, $notes, [string]$labelSuffix = "") {
$i = 0
$runtimeMap = Get-RunRuntimeMap $rows $runs
$lines = foreach ($name in $series.Keys) {
Get-LineSvg $series[$name] (Get-Color $i) -Labels $labels -RunNumbers $runs -RunRuntimeMap $runtimeMap
$i++
}
$body   = @((Get-GridSvg), (Get-XLabelsSvg $labels), ($lines -join "`n"), (Get-NotesSvg $runs $notes)) -join "`n"
$legend = Get-LegendHtml $series
$tag = if ($labelSuffix) { "<span class='tag'>$labelSuffix</span>" } else { "" }
return "<section class='chart-panel'><h2>$title $tag</h2>`n<svg viewBox='0 0 $W $H'>`n$body`n</svg>`n<div class='legend'>$legend</div></section>"
}

function Get-PageHtml([string]$left, [string]$right) {
$css = @"
body{background:#0d0f0f;color:#e8e4d8;font-family:monospace;margin:24px}
#chartTooltip{position:fixed;z-index:9999;pointer-events:none;opacity:0;transform:translateY(-4px);transition:opacity .12s ease;max-width:260px;padding:7px 9px;background:#0f1310;border:1px solid #d4a020;color:#f3ead1;font-size:11px;line-height:1.4;border-radius:4px;box-shadow:0 0 10px rgba(0,0,0,.35)}
.data-point,.chart-note{cursor:pointer}
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
"@
$script = @"
<script>
(function() {
  function bindTooltip() {
    const tooltip = document.getElementById('chartTooltip');
    if (!tooltip) return;
    const show = (event, text) => {
      if (!text) return;
      tooltip.textContent = text;
      tooltip.style.opacity = '1';
      tooltip.style.left = (event.clientX + 12) + 'px';
      tooltip.style.top = (event.clientY + 12) + 'px';
    };
    const hide = () => { tooltip.style.opacity = '0'; };
    document.querySelectorAll('.data-point, .chart-note').forEach(function(node) {
      node.addEventListener('mousemove', function(event) {
        show(event, node.getAttribute('data-tooltip'));
      });
      node.addEventListener('mouseleave', hide);
      node.addEventListener('mouseenter', function(event) {
        show(event, node.getAttribute('data-tooltip'));
      });
      node.addEventListener('focus', function(event) {
        show(event, node.getAttribute('data-tooltip'));
      });
      node.addEventListener('blur', hide);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bindTooltip);
  } else {
    bindTooltip();
  }
})();
</script>
"@
return "<!DOCTYPE html><html lang='de'><head><meta charset='UTF-8'><title>Coverage-Verlauf</title><style>$css</style></head><body>`n<div id='chartTooltip'></div>`n<div class='dashboard'><div class='column'>$left</div><div class='column'>$right</div></div>`n$script`n</body></html>"
}

function Get-TotalRuntimeSeconds {
    $reportsDir = Join-Path $ProjectRoot 'target\surefire-reports'
    if (Test-Path $reportsDir) {
        $total = 0.0
        Get-ChildItem $reportsDir -Filter 'TEST-*.xml' | ForEach-Object {
            try {
                [xml]$xml = Get-Content -Path $_.FullName -Raw
                $suite = $xml.testsuite
                if ($suite -and $null -ne $suite.time) {
                    $total += [double]$suite.time
                }
            }
            catch {
            }
        }
        if ($total -gt 0) { return $total }
    }

    $hotspotsCsv = Join-Path $ProjectRoot 'target\test_runtime_hotspots.csv'
    if (Test-Path $hotspotsCsv) {
        try {
            $rows = Import-Csv -Path $hotspotsCsv -Encoding UTF8
            if ($rows) {
                $sum = ($rows | ForEach-Object { [double]$_.TimeSec } | Measure-Object -Sum).Sum
                if ($null -ne $sum -and $sum -gt 0) { return [double]$sum }
            }
        }
        catch {
        }
    }

    return $null
}

# ── Ablauf ───────────────────────────────────────────────────────────────

$rows = Import-Csv -Path $CsvPath -Encoding UTF8
$allRuns = $rows | ForEach-Object { [int]$_.run } | Sort-Object -Unique
$recentRuns = if ($allRuns.Count -gt 7) { $allRuns | Select-Object -Last 7 } else { $allRuns }
$notes = if (Test-Path $NotesCsv) { @(Import-Csv -Path $NotesCsv -Encoding UTF8) } else { @() }

function Build-SectionSet($runs) {
$labels = @($runs | ForEach-Object { Get-RunLabel $rows $_ })
$total = [ordered]@{}
foreach ($name in $METRICS.Keys) { $total[$name] = @(Get-Series $rows $ROOT_ELEMENT $METRICS[$name] $runs) }

$packages = [ordered]@{}
$names = $rows.element | Sort-Object -Unique | Where-Object { $_ -ne $ROOT_ELEMENT }
foreach ($name in $names) { $packages[$name] = @(Get-Series $rows $name 'line' $runs) }

return [pscustomobject]@{
    Labels = $labels
    Total = $total
    Packages = $packages
    Runs = @($runs)
}
}

$recent = Build-SectionSet $recentRuns
$full = Build-SectionSet $allRuns

$mainLeft = @(
(New-Section "Gesamt ($ROOT_ELEMENT)" $recent.Total $recent.Labels $recent.Runs $notes "letzte 7 Tests"),
(New-Section "Line-Coverage je Paket" $recent.Packages $recent.Labels $recent.Runs $notes "letzte 7 Tests")
) -join "`n"

$mainRight = @(
(New-Section "Gesamt ($ROOT_ELEMENT)" $full.Total $full.Labels $full.Runs $notes "alle Tests"),
(New-Section "Line-Coverage je Paket" $full.Packages $full.Labels $full.Runs $notes "alle Tests")
) -join "`n"

$totalRuntimeSeconds = Get-TotalRuntimeSeconds
$runtimeSummary = if ($null -ne $totalRuntimeSeconds) {
    $display = if ($totalRuntimeSeconds -ge 60) {
        "{0:N2} min ({1:N0} s)" -f ($totalRuntimeSeconds / 60), $totalRuntimeSeconds
    }
    else {
        "{0:N2} s" -f $totalRuntimeSeconds
    }
    "<div class='summary'><div class='summary-label'>Gesamtlaufzeit</div><div class='summary-value'>$display</div></div>"
}
else {
    "<div class='summary'><div class='summary-label'>Gesamtlaufzeit</div><div class='summary-value'>-</div></div>"
}

Set-Content -Path $OutFile -Value (Get-PageHtml $mainLeft $mainRight) -Encoding UTF8
Write-Host "Diagramm erzeugt: $OutFile" -ForegroundColor Green
if ($Open) { Start-Process $OutFile }


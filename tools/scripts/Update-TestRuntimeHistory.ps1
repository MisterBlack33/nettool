<#
.SYNOPSIS
    Hängt Surefire-Testklassenlaufzeiten an die CSV-Historie an und erzeugt
    daraus ein eigenständiges HTML-Diagramm.

.EXAMPLE
    .\tools\scripts\Update-TestRuntimeHistory.ps1
    .\tools\scripts\Update-TestRuntimeHistory.ps1 -SkipTestRun
#>
param(
    [string]$ProjectRoot = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
    [string]$ReportsPath = (Join-Path $ProjectRoot 'target\surefire-reports'),
    [string]$OutputCsv = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) 'output') 'test_runtime_history.csv'),
    [string]$OutFile = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) 'output') 'test_runtime_history.html'),
    [ValidateRange(1, 20)]
    [int]$Top = 10,
    [switch]$SkipTestRun,
    [switch]$IncludeSlow,
    [switch]$Open
)

$ErrorActionPreference = 'Stop'
$invariant = [cultureinfo]::InvariantCulture
$expectedColumns = @('Run', 'Timestamp', 'Name', 'TimeSec', 'Tests', 'Failures', 'Errors', 'Skipped', 'File')

if (-not $SkipTestRun) {
    $arguments = @('--batch-mode', 'clean', 'test')
    if ($IncludeSlow) {
        $arguments += '-Pnightly'
    }

    Push-Location $ProjectRoot
    try {
        & mvn @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Maven failed with exit code $LASTEXITCODE; runtime history was not updated."
        }
    }
    finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $ReportsPath -PathType Container)) {
    throw "Surefire reports not found: $ReportsPath"
}

$reports = @(Get-ChildItem -LiteralPath $ReportsPath -Filter 'TEST-*.xml' -File)
if ($reports.Count -eq 0) {
    throw "No Surefire XML reports found in: $ReportsPath"
}

$currentRows = foreach ($reportFile in $reports) {
    [xml]$xml = [System.IO.File]::ReadAllText($reportFile.FullName)
    $suites = @($xml.SelectNodes('/testsuite | /testsuites/testsuite'))
    foreach ($suite in $suites) {
        [pscustomobject]@{
            Name = [string]$suite.name
            TimeSec = [double]::Parse([string]$suite.time, $invariant)
            Tests = [int]$suite.tests
            Failures = [int]$suite.failures
            Errors = [int]$suite.errors
            Skipped = [int]$suite.skipped
            File = $reportFile.Name
        }
    }
}

$currentRows = @($currentRows | Where-Object { -not [string]::IsNullOrWhiteSpace($_.Name) })
if ($currentRows.Count -eq 0) {
    throw "Surefire XML reports contained no test suites."
}

$outputDirectory = Split-Path -Parent $OutputCsv
if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}
$htmlDirectory = Split-Path -Parent $OutFile
if ($htmlDirectory -and -not (Test-Path -LiteralPath $htmlDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $htmlDirectory -Force | Out-Null
}

$history = @()
$runNumber = 1
if (Test-Path -LiteralPath $OutputCsv -PathType Leaf) {
    $history = @(Import-Csv -LiteralPath $OutputCsv)
    if ($history.Count -gt 0) {
        $actualColumns = @($history[0].PSObject.Properties.Name)
        if (($expectedColumns -join ',') -ne ($actualColumns -join ',')) {
            throw "Unexpected runtime history CSV schema in $OutputCsv"
        }
        $runNumber = 1 + [int](($history | Measure-Object -Property Run -Maximum).Maximum)
    }
}

$timestamp = [DateTime]::UtcNow.ToString('o', $invariant)
$newRows = @($currentRows | ForEach-Object {
    [pscustomobject][ordered]@{
        Run = $runNumber
        Timestamp = $timestamp
        Name = $_.Name
        TimeSec = $_.TimeSec.ToString('R', $invariant)
        Tests = $_.Tests
        Failures = $_.Failures
        Errors = $_.Errors
        Skipped = $_.Skipped
        File = $_.File
    }
})
$newRows | Export-Csv -LiteralPath $OutputCsv -Append -NoTypeInformation -Encoding UTF8
$history += $newRows

$runs = @($history | Group-Object -Property Run | Sort-Object { [int]$_.Name } | ForEach-Object {
    $runRows = @($_.Group)
    [ordered]@{
        number = [int]$_.Name
        timestamp = [string]$runRows[0].Timestamp
        rows = @($runRows | ForEach-Object {
            [ordered]@{
                name = [string]$_.Name
                timeSec = [double]::Parse([string]$_.TimeSec, $invariant)
            }
        })
    }
})

$data = [ordered]@{
    runs = $runs
    top = $Top
}
$dataJson = ($data | ConvertTo-Json -Depth 8 -Compress).Replace('<', '\u003c')

$html = @"
<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Testlaufzeit-Verlauf</title>
<style>
body{background:#0d0f0f;color:#e8e4d8;font:14px monospace;margin:24px}
h1{color:#d4a020;font-size:20px}p{color:#a9afa7;line-height:1.5}
svg{width:100%;height:auto;background:#0f1310;border:1px solid #22282a}
.grid{stroke:#2a2f2c}.axis{fill:#8a9088;font-size:12px}
.legend{display:flex;flex-wrap:wrap;gap:8px 18px;margin-top:12px}
.legend span{white-space:nowrap}.swatch{display:inline-block;width:10px;height:10px;margin-right:6px}
</style>
</head>
<body>
<h1>Testlaufzeit-Verlauf</h1>
<p>Verlauf der bis zu $Top langsamsten Testklassen des neuesten Laufs.
Die Laufzeiten stammen aus den Surefire-Berichten und sind pro Testklasse angegeben.
Nur Klassen, die im neuesten Lauf enthalten sind, werden im Diagramm gezeigt.</p>
<svg id="chart" viewBox="0 0 1000 520" role="img" aria-label="Laufzeiten der langsamsten Testklassen je Testlauf"></svg>
<div id="legend" class="legend"></div>
<script id="runtimeData" type="application/json">$dataJson</script>
<script>
(function(){
  const data=JSON.parse(document.getElementById('runtimeData').textContent);
  const svg=document.getElementById('chart');
  const legend=document.getElementById('legend');
  const ns='http://www.w3.org/2000/svg';
  const colors=['#d4a020','#4cc260','#72a8d8','#ff70a0','#ffa030','#a0ffc0','#c8b0ff','#60d0ff','#e8c840','#ff6a5a'];
  const width=1000,height=520,left=72,right=24,top=28,bottom=66;
  const plotW=width-left-right,plotH=height-top-bottom;
  const latest=data.runs[data.runs.length-1];
  const ranked=latest.rows.slice().sort((a,b)=>b.timeSec-a.timeSec).slice(0,data.top);
  const names=ranked.map(row=>row.name);
  const maximum=Math.max(1,...data.runs.flatMap(run=>run.rows.filter(row=>names.includes(row.name)).map(row=>row.timeSec)));
  const step=Math.max(1,Math.ceil(maximum/5));
  const yMax=step*5;
  const xAt=i=>left+plotW*i/Math.max(1,data.runs.length-1);
  const yAt=value=>top+plotH*(1-value/yMax);
  function el(tag,attrs,text){
    const node=document.createElementNS(ns,tag);
    Object.keys(attrs||{}).forEach(key=>node.setAttribute(key,attrs[key]));
    if(text!==undefined) node.textContent=text;
    return node;
  }
  for(let tick=0;tick<=5;tick++){
    const value=step*tick,y=yAt(value);
    svg.appendChild(el('line',{x1:left,y1:y,x2:width-right,y2:y,class:'grid'}));
    svg.appendChild(el('text',{x:left-10,y:y+4,'text-anchor':'end',class:'axis'},value.toFixed(value<10?1:0)+' s'));
  }
  const labelStep=Math.max(1,Math.ceil(data.runs.length/12));
  data.runs.forEach((run,i)=>{
    if(i%labelStep!==0 && i!==data.runs.length-1)return;
    const date=new Date(run.timestamp);
    const label='Run '+run.number+' ('+date.toLocaleDateString('de-DE')+')';
    svg.appendChild(el('text',{x:xAt(i),y:height-22,'text-anchor':'middle',class:'axis'},label));
  });
  names.forEach((name,seriesIndex)=>{
    const color=colors[seriesIndex%colors.length];
    let segment=[];
    function drawSegment(){
      if(segment.length>1) svg.appendChild(el('polyline',{points:segment.map(point=>point.x+','+point.y).join(' '),fill:'none',stroke:color,'stroke-width':2}));
      segment.forEach(point=>{
        const circle=el('circle',{cx:point.x,cy:point.y,r:4,fill:color});
        circle.appendChild(el('title',{},point.title));
        svg.appendChild(circle);
      });
      segment=[];
    }
    data.runs.forEach((run,i)=>{
      const row=run.rows.find(item=>item.name===name);
      if(!row){drawSegment();return;}
      const date=new Date(run.timestamp).toLocaleString('de-DE');
      segment.push({x:xAt(i),y:yAt(row.timeSec),title:name+' | Run '+run.number+' ('+date+') | '+row.timeSec.toFixed(3)+' s'});
    });
    drawSegment();
    const item=document.createElement('span');
    const swatch=document.createElement('i');
    swatch.className='swatch';
    swatch.style.backgroundColor=color;
    item.appendChild(swatch);
    item.appendChild(document.createTextNode(name+' — '+ranked[seriesIndex].timeSec.toFixed(2)+' s'));
    legend.appendChild(item);
  });
})();
</script>
</body>
</html>
"@

Set-Content -LiteralPath $OutFile -Value $html -Encoding UTF8
Write-Host "Runtime history run $runNumber appended ($($newRows.Count) test classes)."
Write-Host "CSV:  $OutputCsv"
Write-Host "HTML: $OutFile"
if ($Open) {
    Start-Process -FilePath $OutFile
}

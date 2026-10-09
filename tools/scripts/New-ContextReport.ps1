<#
Creates a compact Markdown inventory of the repository for sharing with an AI assistant.
Source-code contents are omitted; setup documentation and readable coverage data are embedded.
#>

[CmdletBinding()]
param(
    [string]$RootPath = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
    [string]$OutputPath,
    [switch]$IncludeGenerated,
    [switch]$IncludeRuntimeData,
    [ValidateRange(1, 100)]
    [int]$TopLargeFiles = 20
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $RootPath -PathType Container)) {
    throw "Projektordner nicht gefunden: $RootPath"
}

$root = (Resolve-Path -LiteralPath $RootPath).Path.TrimEnd('\', '/')
if (-not $OutputPath) {
    $OutputPath = Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) 'output') 'context-report.md'
}
$outputFullPath = [System.IO.Path]::GetFullPath($OutputPath)
$scriptOutputDirectory = [System.IO.Path]::GetFullPath(
    (Join-Path (Split-Path -Parent $PSScriptRoot) 'output')
).TrimEnd('\', '/')

$generatedDirectoryNames = @(
    'target', 'out', 'build', 'dist', 'node_modules', '.gradle',
    '.next', '.nuxt', '.pytest_cache', '__pycache__', 'coverage'
)
$runtimeDirectoryNames = @('saves', 'tmp', '.test-results')
$files = New-Object 'System.Collections.Generic.List[object]'
$directories = New-Object 'System.Collections.Generic.List[string]'
$excludedDirectories = New-Object 'System.Collections.Generic.List[object]'
$pendingDirectories = New-Object 'System.Collections.Generic.Stack[string]'
$pendingDirectories.Push($root)

while ($pendingDirectories.Count -gt 0) {
    $currentDirectory = $pendingDirectories.Pop()
    foreach ($item in Get-ChildItem -LiteralPath $currentDirectory -Force) {
        $relativePath = $item.FullName.Substring($root.Length).TrimStart('\', '/')

        if ($item.PSIsContainer) {
            $reason = $null
            if ([string]::Equals(
                    [System.IO.Path]::GetFullPath($item.FullName).TrimEnd('\', '/'),
                    $scriptOutputDirectory,
                    [System.StringComparison]::OrdinalIgnoreCase
                )) {
                $reason = 'Skriptausgaben'
            }
            elseif ($item.Name -eq '.git') {
                $reason = 'Git internals'
            }
            elseif (-not $IncludeGenerated -and $generatedDirectoryNames -contains $item.Name) {
                $reason = 'generierte/build-Dateien (mit -IncludeGenerated einschließen)'
            }
            elseif (-not $IncludeRuntimeData -and $runtimeDirectoryNames -contains $item.Name) {
                $reason = 'Laufzeit-/lokale Daten (mit -IncludeRuntimeData einschließen)'
            }
            elseif (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
                $reason = 'Verzeichnis-Link (wird aus Sicherheitsgründen nicht verfolgt)'
            }

            if ($reason) {
                $excludedDirectories.Add([pscustomobject]@{
                    Path = $relativePath
                    Reason = $reason
                })
                continue
            }

            $directories.Add($relativePath)
            $pendingDirectories.Push($item.FullName)
            continue
        }

        if ([string]::Equals(
                [System.IO.Path]::GetFullPath($item.FullName),
                $outputFullPath,
                [System.StringComparison]::OrdinalIgnoreCase
            )) {
            continue
        }

        $extension = $item.Extension.ToLowerInvariant()
        if (-not $extension) {
            $extension = '(ohne Endung)'
        }
        $files.Add([pscustomobject]@{
            Path = $relativePath
            Bytes = [long]$item.Length
            Extension = $extension
            Modified = $item.LastWriteTime
        })
    }
}

$fileRows = @($files | Sort-Object Path)
$directoryRows = @($directories | Sort-Object)
$javaFiles = @($fileRows | Where-Object { $_.Extension -eq '.java' })
$packages = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::Ordinal)

foreach ($javaFile in $javaFiles) {
    $fullPath = Join-Path $root $javaFile.Path
    foreach ($match in Select-String -LiteralPath $fullPath -Pattern '^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)\s*;' -AllMatches) {
        foreach ($packageMatch in $match.Matches) {
            [void]$packages.Add($packageMatch.Groups[1].Value)
        }
    }
}

$pomPath = Join-Path $root 'pom.xml'
$pomInfo = $null
if (Test-Path -LiteralPath $pomPath -PathType Leaf) {
    [xml]$pom = [System.IO.File]::ReadAllText($pomPath)
    $project = $pom.DocumentElement
    $properties = @{}
    $propertyNode = $project.SelectSingleNode("*[local-name()='properties']")
    if ($propertyNode) {
        foreach ($property in $propertyNode.ChildNodes) {
            if ($property.NodeType -eq [System.Xml.XmlNodeType]::Element) {
                $properties[$property.LocalName] = $property.InnerText.Trim()
            }
        }
    }

    $getPomValue = {
        param([System.Xml.XmlNode]$Node, [string]$Name)
        $valueNode = $Node.SelectSingleNode("*[local-name()='$Name']")
        if ($valueNode) { return $valueNode.InnerText.Trim() }
        return ''
    }

    $dependencies = @()
    $dependencyNodes = $project.SelectNodes("*[local-name()='dependencies']/*[local-name()='dependency']")
    foreach ($dependency in $dependencyNodes) {
        $groupId = & $getPomValue $dependency 'groupId'
        $artifactId = & $getPomValue $dependency 'artifactId'
        $version = & $getPomValue $dependency 'version'
        $scope = & $getPomValue $dependency 'scope'
        if ($version -match '^\$\{([^}]+)\}$') {
            $propertyName = $Matches[1]
            if ($properties.ContainsKey($propertyName)) {
                $version = $properties[$propertyName]
            }
        }
        if (-not $scope) { $scope = 'compile (default)' }
        $dependencies += [pscustomobject]@{
            Coordinate = "$groupId`:$artifactId`:$version"
            Scope = $scope
        }
    }

    $pomInfo = [pscustomobject]@{
        GroupId = & $getPomValue $project 'groupId'
        ArtifactId = & $getPomValue $project 'artifactId'
        Version = & $getPomValue $project 'version'
        Packaging = & $getPomValue $project 'packaging'
        JavaRelease = $properties['maven.compiler.release']
        JavaSource = $properties['maven.compiler.source']
        JavaTarget = $properties['maven.compiler.target']
        MainClass = ''
        SourceDirectory = (& $getPomValue ($project.SelectSingleNode("*[local-name()='build']")) 'sourceDirectory')
        TestSourceDirectory = (& $getPomValue ($project.SelectSingleNode("*[local-name()='build']")) 'testSourceDirectory')
        Dependencies = $dependencies
    }

    $mainClassNode = $project.SelectSingleNode(
        "//*[local-name()='artifactId' and text()='maven-jar-plugin']/following-sibling::*[local-name()='configuration']//*[local-name()='mainClass']"
    )
    if ($mainClassNode) {
        $pomInfo.MainClass = $mainClassNode.InnerText.Trim()
    }
}

$gitLines = @()
$gitCommand = Get-Command git -ErrorAction SilentlyContinue
if ($gitCommand) {
    Push-Location $root
    try {
        $gitRoot = @(& $gitCommand.Source rev-parse --show-toplevel 2>$null)
        if ($LASTEXITCODE -eq 0 -and $gitRoot.Count -gt 0) {
            $branch = @(& $gitCommand.Source branch --show-current 2>$null)
            $commit = @(& $gitCommand.Source log -1 --format='%h %s' 2>$null)
            $status = @(& $gitCommand.Source status --short --untracked-files=normal 2>$null)
            $gitLines += 'Git-Repository: erkannt (lokaler Pfad aus Datenschutzgründen ausgelassen)'
            if ($branch.Count -gt 0) { $gitLines += "Branch: $($branch[0])" }
            if ($commit.Count -gt 0) { $gitLines += "HEAD: $($commit[0])" }
            $gitLines += "Working-tree entries: $($status.Count)"
            if ($status.Count -gt 0) {
                $gitLines += '```text'
                $gitLines += $status
                $gitLines += '```'
            }
        }
        else {
            $gitLines += 'Kein Git-Repository erkannt.'
        }
    }
    finally {
        Pop-Location
    }
}
else {
    $gitLines += 'Git ist auf diesem System nicht verfügbar.'
}

$lines = New-Object 'System.Collections.Generic.List[string]'
$lines.Add("# Projektkontext: $(Split-Path -Leaf $root)")
$lines.Add('')
$lines.Add("> Automatisch erzeugtes Inventar. Es enthält Metadaten und Pfade, aber keine Quelltext-Inhalte.")
$lines.Add('')
$lines.Add('## Überblick')
$lines.Add('')
$lines.Add("- Erfasst am: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')")
$lines.Add("- Projektordner: $(Split-Path -Leaf $root)")
$lines.Add("- Dateien im erfassten Umfang: $($fileRows.Count)")
$lines.Add("- Ordner im erfassten Umfang: $($directoryRows.Count)")
$lines.Add("- Java-Dateien: $($javaFiles.Count)")
$lines.Add("- Erkannte Java-Packages: $($packages.Count)")
$lines.Add("- Datenmenge der erfassten Dateien: $([math]::Round((($fileRows | Measure-Object -Property Bytes -Sum).Sum / 1MB), 2)) MiB")
$lines.Add('')
$lines.Add('### Umfang und Ausschlüsse')
$lines.Add('')
$lines.Add('`.git`-Interna und Verzeichnis-Links werden immer ausgelassen. Generierte Build-/Cache-Ordner sowie lokale Laufzeitdaten werden standardmäßig ausgelassen; die Liste zeigt die erkannten Ausschlüsse. Inhalte dieser Ordner werden nicht aufgelistet.')
if ($excludedDirectories.Count -gt 0) {
    $lines.Add('')
    foreach ($excluded in ($excludedDirectories | Sort-Object Path)) {
        $lines.Add("- ``$($excluded.Path)`` — $($excluded.Reason)")
    }
}
else {
    $lines.Add('')
    $lines.Add('- Keine Verzeichnisse ausgeschlossen.')
}
$lines.Add('')
$lines.Add('## Projekt- und Build-Konfiguration')
$lines.Add('')
if ($pomInfo) {
    $lines.Add("- Maven-Koordinaten: ``$($pomInfo.GroupId):$($pomInfo.ArtifactId):$($pomInfo.Version)``")
    $lines.Add("- Packaging: ``$($pomInfo.Packaging)``")
    $javaVersion = if ($pomInfo.JavaRelease) { $pomInfo.JavaRelease } elseif ($pomInfo.JavaTarget) { $pomInfo.JavaTarget } else { $pomInfo.JavaSource }
    if ($javaVersion) { $lines.Add("- Java-Version laut Maven: ``$javaVersion``") }
    if ($pomInfo.MainClass) { $lines.Add("- Main-Klasse: ``$($pomInfo.MainClass)``") }
    if ($pomInfo.SourceDirectory) { $lines.Add("- Maven-Quellcodeverzeichnis: ``$($pomInfo.SourceDirectory)``") }
    if ($pomInfo.TestSourceDirectory) { $lines.Add("- Maven-Testverzeichnis: ``$($pomInfo.TestSourceDirectory)``") }
    $lines.Add('')
    $lines.Add('Direkte Maven-Abhängigkeiten aus `pom.xml` (keine transitiven Abhängigkeiten):')
    if ($pomInfo.Dependencies.Count -gt 0) {
        foreach ($dependency in ($pomInfo.Dependencies | Sort-Object Coordinate)) {
            $lines.Add("- ``$($dependency.Coordinate)`` — $($dependency.Scope)")
        }
    }
    else {
        $lines.Add('- Keine direkten Maven-Abhängigkeiten deklariert.')
    }
}
else {
    $lines.Add('- Keine `pom.xml` gefunden; Maven-Metadaten nicht verfügbar.')
}

$lines.Add('')
$lines.Add('## Git-Status')
$lines.Add('')
$lines.AddRange([string[]]$gitLines)
$lines.Add('')
$lines.Add('## Dateitypen')
$lines.Add('')
$lines.Add('| Endung | Anzahl | Gesamtgröße (MiB) |')
$lines.Add('|---|---:|---:|')
foreach ($group in ($fileRows | Group-Object Extension | Sort-Object Name)) {
    $size = ($group.Group | Measure-Object -Property Bytes -Sum).Sum
    $lines.Add("| ``$($group.Name)`` | $($group.Count) | $([math]::Round(($size / 1MB), 2)) |")
}

$lines.Add('')
$lines.Add("## Größte erfasste Dateien (Top $([math]::Min($TopLargeFiles, $fileRows.Count)))")
$lines.Add('')
$lines.Add('| Größe (MiB) | Pfad |')
$lines.Add('|---:|---|')
foreach ($file in ($fileRows | Sort-Object Bytes -Descending | Select-Object -First $TopLargeFiles)) {
    $escapedPath = $file.Path.Replace('|', '\|')
    $lines.Add("| $([math]::Round(($file.Bytes / 1MB), 2)) | ``$escapedPath`` |")
}

$lines.Add('')
$lines.Add("## Java-Packages ($($packages.Count))")
$lines.Add('')
if ($packages.Count -gt 0) {
    foreach ($package in ($packages | Sort-Object)) {
        $lines.Add("- ``$package``")
    }
}
else {
    $lines.Add('- Keine Java-Package-Deklarationen gefunden.')
}

$lines.Add('')
$lines.Add("## Ordnerinventar ($($directoryRows.Count))")
$lines.Add('')
if ($directoryRows.Count -gt 0) {
    foreach ($directory in $directoryRows) {
        $lines.Add("- ``$directory``")
    }
}
else {
    $lines.Add('- Keine Unterordner vorhanden.')
}

$lines.Add('')
$lines.Add("## Dateiinventar ($($fileRows.Count))")
$lines.Add('')
$lines.Add('Quelltext-Inhalte werden absichtlich nicht ausgegeben. Größen sind Byte-genau; Zeitstempel sind lokale Dateisystem-Zeit.')
$lines.Add('')
$lines.Add('| Größe (Bytes) | Geändert | Pfad |')
$lines.Add('|---:|---|---|')
foreach ($file in $fileRows) {
    $escapedPath = $file.Path.Replace('|', '\|')
    $lines.Add("| $($file.Bytes) | $($file.Modified.ToString('yyyy-MM-dd HH:mm')) | ``$escapedPath`` |")
}

$appendEmbeddedFile = {
    param([string]$Title, [string]$RelativePath)

    $fullPath = Join-Path $root $RelativePath
    $content = [System.IO.File]::ReadAllText($fullPath)
    $longestBacktickRun = 0
    foreach ($match in [regex]::Matches($content, '`+')) {
        $longestBacktickRun = [math]::Max($longestBacktickRun, $match.Length)
    }
    $fence = '`' * [math]::Max(3, $longestBacktickRun + 1)
    $extension = [System.IO.Path]::GetExtension($RelativePath).TrimStart('.').ToLowerInvariant()
    $lines.Add('')
    $lines.Add("## $Title")
    $lines.Add('')
    $lines.Add("Quelle: ``$RelativePath``")
    $lines.Add('')
    $lines.Add("$fence$extension")
    foreach ($contentLine in [regex]::Split($content, "\r\n|\n|\r")) {
        $lines.Add($contentLine)
    }
    $lines.Add($fence)
}

$readmePath = Join-Path $root 'README.md'
if (Test-Path -LiteralPath $readmePath -PathType Leaf) {
    & $appendEmbeddedFile 'Projekt-README' 'README.md'
}

$testingGuidePath = Join-Path $root 'docs\testing.md'
if (Test-Path -LiteralPath $testingGuidePath -PathType Leaf) {
    & $appendEmbeddedFile 'Test- und Coverage-Anleitung' 'docs\testing.md'
}

$coverageFiles = @(
    Get-ChildItem -LiteralPath $root -File |
        Where-Object {
            $_.Name -match '(?i)(coverage|jacoco)' -and
            $_.Extension.ToLowerInvariant() -in @('.csv', '.html', '.xml', '.md', '.txt')
        } |
        Sort-Object FullName
)
$coverageOutputDirectory = Join-Path $root 'tools\output'
if (Test-Path -LiteralPath $coverageOutputDirectory -PathType Container) {
    $coverageFiles += Get-ChildItem -LiteralPath $coverageOutputDirectory -File |
        Where-Object {
            $_.Name -match '(?i)(coverage|jacoco)' -and
            $_.Extension.ToLowerInvariant() -in @('.csv', '.html', '.xml', '.md', '.txt')
        } |
        Sort-Object FullName
}
$jacocoCsvPath = Join-Path $root 'target\site\jacoco\jacoco.csv'
if (Test-Path -LiteralPath $jacocoCsvPath -PathType Leaf) {
    $coverageFiles += Get-Item -LiteralPath $jacocoCsvPath
}

$lines.Add('')
$lines.Add('## Coverage-Dateien')
$lines.Add('')
$lines.Add('Lesbare Coverage-Dateien werden vollständig eingebettet. Binärdateien wie XLSX und JaCoCo-EXEC bleiben im Dateiinventar, werden aber nicht als Text interpretiert.')
if ($coverageFiles.Count -gt 0) {
    foreach ($coverageFile in $coverageFiles) {
        $relativeCoveragePath = $coverageFile.FullName.Substring($root.Length).TrimStart('\', '/')
        & $appendEmbeddedFile "Coverage: $relativeCoveragePath" $relativeCoveragePath
    }
}
else {
    $lines.Add('')
    $lines.Add('- Keine lesbaren Coverage-Dateien gefunden.')
}

$outputDirectory = Split-Path -Parent $outputFullPath
if (-not (Test-Path -LiteralPath $outputDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}
[System.IO.File]::WriteAllText(
    $outputFullPath,
    ($lines -join [Environment]::NewLine) + [Environment]::NewLine,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Kontextbericht geschrieben: $outputFullPath"
Write-Host ("Erfasst: {0} Dateien, {1} Ordner, {2} Java-Packages" -f $fileRows.Count, $directoryRows.Count, $packages.Count)

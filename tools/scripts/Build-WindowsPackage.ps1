param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
[xml]$pom = [System.IO.File]::ReadAllText((Join-Path $repoRoot 'pom.xml'))
$artifactIdNode = $pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='artifactId']")
$versionNode = $pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='version']")
if (-not $artifactIdNode -or $artifactIdNode.InnerText -notmatch '^[A-Za-z0-9._-]+$' -or
    -not $versionNode -or $versionNode.InnerText -notmatch '^\d+(\.\d+){1,3}([.-][A-Za-z0-9.-]+)?$') {
    throw 'Could not determine a valid application artifact and version from pom.xml.'
}
$artifactId = $artifactIdNode.InnerText.Trim()
$version = $versionNode.InnerText.Trim()

$jdkBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { $null }
$jpackage = if ($jdkBin) { Join-Path $jdkBin 'jpackage.exe' } else { $null }
if (-not $jpackage -or -not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    $command = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $command) { $command = Get-Command jpackage -ErrorAction SilentlyContinue }
    if ($command) { $jpackage = $command.Source }
}
if (-not $jpackage -or -not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    throw 'JDK 21 or later with jpackage is required. Set JAVA_HOME to the JDK installation.'
}
$jdkVersion = (& $jpackage '--version' | Select-Object -First 1).Trim()
if ($LASTEXITCODE -ne 0 -or $jdkVersion -notmatch '^(?<major>\d+)(\.|$)' -or
    [int]$Matches.major -lt 21) {
    throw "JDK 21 or later is required for packaging. Detected jpackage version '$jdkVersion'."
}

$jar = Join-Path $repoRoot "target\$artifactId-$version.jar"
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
    throw "Application JAR not found: $jar. Run 'mvn package' first."
}

$icon = Join-Path $repoRoot 'src\main\resources\icon.ico'
if (-not (Test-Path -LiteralPath $icon -PathType Leaf)) {
    throw "Application icon not found: $icon"
}

$dist = Join-Path $repoRoot 'target\dist'
$image = Join-Path $dist 'NetTool'
$archive = Join-Path $dist "nettool-$version-windows-x64.zip"
$input = Join-Path $dist 'input'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
if (Test-Path -LiteralPath $image) {
    Remove-Item -LiteralPath $image -Recurse -Force
}
if (Test-Path -LiteralPath $archive) {
    Remove-Item -LiteralPath $archive -Force
}
if (Test-Path -LiteralPath $input) {
    Remove-Item -LiteralPath $input -Recurse -Force
}
New-Item -ItemType Directory -Path $input | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $input 'nettool.jar')

$arguments = @(
    '--type', 'app-image',
    '--dest', $dist,
    '--name', 'NetTool',
    '--app-version', $version,
    '--vendor', 'NetTool',
    '--description', 'Network discovery and diagnostics for authorized networks',
    '--input', $input,
    '--main-jar', 'nettool.jar',
    '--main-class', 'main.java.networktool.Main',
    '--icon', $icon
)
try {
    & $jpackage @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }

    $launcher = Join-Path $image 'NetTool.exe'
    $runtimeRelease = Join-Path $image 'runtime\release'
    $runtimeLibrary = Join-Path $image 'runtime\bin\server\jvm.dll'
    if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
        throw "Packaged launcher is missing: $launcher"
    }
    if (-not (Test-Path -LiteralPath $runtimeRelease -PathType Leaf) -or
        -not (Test-Path -LiteralPath $runtimeLibrary -PathType Leaf)) {
        throw 'Bundled Java runtime is incomplete.'
    }

    $packageReadme = @"
NetTool $version - portable Windows package

Start the application by opening NetTool.exe.
The package includes its own Java runtime; a separate JDK is not required.

Application data is stored separately in %USERPROFILE%\.nettool.
Removing this package folder does not remove that application data.

Use network discovery and security diagnostics only on networks and devices
for which you have explicit permission.
"@
    [System.IO.File]::WriteAllText(
        (Join-Path $image 'README.txt'),
        $packageReadme,
        [System.Text.UTF8Encoding]::new($false)
    )

    & $launcher '--version'
    if ($LASTEXITCODE -ne 0) {
        throw "Packaged launcher smoke test failed with exit code $LASTEXITCODE."
    }

    Compress-Archive -LiteralPath $image -DestinationPath $archive -CompressionLevel Optimal
    if (-not (Test-Path -LiteralPath $archive -PathType Leaf) -or
        (Get-Item -LiteralPath $archive).Length -eq 0) {
        throw "Portable package was not created: $archive"
    }
}
finally {
    if (Test-Path -LiteralPath $input) {
        Remove-Item -LiteralPath $input -Recurse -Force
    }
}

Write-Host "Portable Windows package created: $archive"

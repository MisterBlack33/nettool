function Get-MavenExecutable {
    $mavenCommand = Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    $candidates = @()
    foreach ($variableName in @('MAVEN_HOME', 'M2_HOME')) {
        $mavenHome = [Environment]::GetEnvironmentVariable($variableName)
        if ($mavenHome) {
            $candidates += Join-Path $mavenHome 'bin\mvn.cmd'
        }
    }

    $installRoots = @()
    if ($env:ProgramFiles) {
        $installRoots += Join-Path $env:ProgramFiles 'JetBrains'
    }
    if ($env:LOCALAPPDATA) {
        $installRoots += Join-Path $env:LOCALAPPDATA 'Programs\JetBrains'
    }

    foreach ($installRoot in $installRoots) {
        if (Test-Path -LiteralPath $installRoot -PathType Container) {
            $installations = Get-ChildItem -LiteralPath $installRoot -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -like 'IntelliJ IDEA*' }
            foreach ($installation in $installations) {
                $candidates += Join-Path $installation.FullName 'plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
            }
        }
    }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw "Maven was not found. Add Maven 3.8.9 or later to PATH, set MAVEN_HOME, or install IntelliJ IDEA's bundled Maven."
}

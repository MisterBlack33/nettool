<#
Full-Package-Split.ps1
Führt den kompletten Subpackage-Split für alle betroffenen Pakete aus:
  storage        -> storage.{network,export,backup,profile}
  logic.analysis -> logic.analysis.{os,discovery,probe}
  logic.scan     -> logic.scan.{host,remote,schedule}
  gui.components -> gui.components.{map,table,scan,actions,terminal}
  gui.panels     -> gui.panels.{saved,audit,privacy}

Vorher: git add -A; git commit -m "pre-split snapshot"; git checkout -b package-split

Nutzung:  .\Full-Package-Split.ps1
#>

$ErrorActionPreference = "Stop"
$root = Get-Location

# ══════════════════════════════════════════════════════════════════════
#  KONFIGURATION: Ein Eintrag pro zu splittendem Package
# ══════════════════════════════════════════════════════════════════════

$splits = @(
    @{
        OldPkg  = "main.java.networktool.storage"
        SrcRoot = "src\main\java\networktool\storage"
        Map     = @{
            "network" = @("NetworkStore","NetworkStoreLegacy","NetworkStoreNtfy",
            "NetworkStorePersistence","NetworkStoreHostOps","NetworkRegistry",
            "HostJsonBuilder","HostSchemaMigration","HostOwnership",
            "HostOwnershipPersistence","UserHostStore")
            "export"  = @("DataExporter","DataExportFormatters","DataExportImport",
            "DataImporter","HtmlReportBuilder")
            "backup"  = @("AutoBackup","BackupCrypto","DataExportBackup","CacheCrypto",
            "NetworkStoreTestBase")
            "profile" = @("ScanProfileStore")
        }
        TestRoots = @("test\main\java\networktool\storage")
        TestFileMap = @{
            "ExportImportTest.java"                  = "export"
            "DataExporterPackageTest.java"            = "export"
            "DataImporterFixTest.java"                = "export"
            "NetworkStoreFixTest.java"                = "network"
            "NetworkStoreIsolationTest.java"          = "network"
            "NetworkStorePersistencePackageTest.java" = "network"
            "NetworkRegistryTest.java"                = "network"
            "HostJsonBuilderTest.java"                = "network"
            "HostSchemaVersionTest.java"               = "network"
            "HostOwnershipTest.java"                  = "network"
            "UserHostStoreTest.java"                  = "network"
            "BackupCryptoTest.java"                   = "backup"
            "AutoBackupPackageTest.java"               = "backup"
            "CacheCryptoTest.java"                     = "backup"
            "NetworkStoreTestBase.java"                = "backup"
        }
        # package-private Methoden, die public werden müssen (Cross-Subpackage-Zugriff)
        VisibilityFixFiles = @("JsonHelper.java")
    },
    @{
        OldPkg  = "main.java.networktool.logic.analysis"
        SrcRoot = "src\main\java\networktool\logic\analysis"
        Map     = @{
            "os"        = @("OsDetector","OsDetectionPipeline","OsDetectionLogger","OsDetectionStepRunner",
            "OsDetectorHostname","OsDetectorArp","OsDetectorPorts","OsFingerprint",
            "OsSignature","OsBannerAnalyzer","OsProbeUdp","OsParallelStepRunner",
            "ExtendedOsDetector","ScanDepth")
            "discovery" = @("MdnsDiscovery","UpnpDiscovery","DhcpOptionAnalyzer","ArpMonitor")
            "probe"     = @("IcmpAnalyzer","PingMonitor","PingUtil","IpInspector",
            "TracerouteRunner","TracerouteRenderer","WakeOnLan","OuiDatabase","OuiUpdater")
        }
        TestRoots = @("test\main\java\networktool\logic\analysis")
        TestFileMap = @{
            "OsDetectorPackageTest.java"          = "os"
            "OsDetectorPortsTest.java"            = "os"
            "OsDetectorHostnameTest.java"         = "os"
            "OsDetectorFixTest.java"              = "os"
            "OsDetectorDelegationTest.java"       = "os"
            "OsDetectorDepthOverloadTest.java"    = "os"
            "OsSignatureTest.java"                = "os"
            "OsFingerprintTest.java"              = "os"
            "OsBannerAnalyzerTest.java"           = "os"
            "OsProbeUdpTest.java"                 = "os"
            "OsParallelStepRunnerTest.java"       = "os"
            "OsDetectionLoggerTest.java"          = "os"
            "OsDetectionStepRunnerTest.java"      = "os"
            "OsDetectionPipelineTest.java"        = "os"
            "OsDetectionPipelineDepthTest.java"   = "os"
            "ScanDepthTest.java"                  = "os"
            "ExtendedOsDetectorTest.java"         = "os"
            "NetworkDiscoveryTest.java"           = "discovery"
            "ArpMonitorLoggingTest.java"          = "discovery"
            "IpInspectorExtTest.java"             = "probe"
            "TracerouteRunnerFixTest.java"        = "probe"
            "TracerouteRunnerPackageTest.java"    = "probe"
        }
        VisibilityFixFiles = @()
    },
    @{
        OldPkg  = "main.java.networktool.logic.scan"
        SrcRoot = "src\main\java\networktool\logic\scan"
        Map     = @{
            "host"     = @("NetworkHostScanner","NetworkHostArpResolver","NetworkHostnameResolver",
            "NetworkScanner","NetworkInfo","NetworkDiscoverySweep","HostAliveChecker",
            "PingSweep","SubnetDetector","ScanProgress")
            "remote"   = @("RemoteNetScanner","RemoteNetProbe","RemoteNetGateway")
            "schedule" = @("ScanScheduler","ScanHistory","ScanDelta","PortChangeMonitor",
            "MapTrafficObserver","AdaptiveTimeoutEstimator","SubnetStats",
            "ArpCacheEntry","ArpCachePersistence","ScanRateLimiter","LastScanCache")
        }
        TestRoots = @("test\main\java\networktool\logic\scan", "test\networktool\logic\scan")
        TestFileMap = @{
            "NetworkHostScannerPackageTest.java" = "host"
            "SubnetDetectorTest.java"            = "host"
            "NetworkDiscoverySweepTest.java"     = "host"
            "NetworkTimeoutTestBase.java"        = "host"
            "HostAliveCheckerTest.java"          = "host"
            "HostAliveCheckerRateLimitTest.java" = "host"
            "SubnetDetectorExtTest.java"         = "host"
            "RemoteNetScannerPackageTest.java"   = "remote"
            "ScanSchedulerPackageTest.java"      = "schedule"
            "ScanSchedulerFixTest.java"          = "schedule"
            "MapTrafficObserverTest.java"        = "schedule"
            "AdaptiveTimeoutEstimatorTest.java"  = "schedule"
            "ArpCacheEntryTest.java"             = "schedule"
            "ArpCachePersistenceTest.java"       = "schedule"
            "ScanRateLimiterTest.java"           = "schedule"
            "LastScanCacheFixTest.java"          = "schedule"
        }
        VisibilityFixFiles = @()
    },
    @{
        OldPkg  = "main.java.networktool.gui.components"
        SrcRoot = "src\main\java\networktool\gui\components"
        Map     = @{
            "map"      = @("GuiNetworkMap","GuiNetworkMapChrome","GuiNetworkMapScanTasks")
            "table"    = @("GuiTableRenderer","SearchResultRow","GuiSearchBar")
            "scan"     = @("GuiScanActions","GuiScanCompareActions","GuiScanProfileActions",
            "GuiForeignNetActions","GuiHopAnalysis","GuiSonifyActions","GuiSchedulerActions")
            "actions"  = @("GuiDataIOActions","GuiDiagnosticsActions","GuiRemoteActions",
            "ContextMenuActions","GuiContextMenu","NtfyTopicPrompt")
            "terminal" = @("GuiSshTerminal","SshConnectionWorker","TerminalChrome")
        }
        TestRoots = @("test\main\java\networktool\gui\components")
        TestFileMap = @{
            "SearchResultRowTest.java" = "table"
        }
        VisibilityFixFiles = @()
    },
    @{
        OldPkg  = "main.java.networktool.gui.panels"
        SrcRoot = "src\main\java\networktool\gui\panels"
        Map     = @{
            "saved"   = @("GuiSavedHostsPanel","SavedHostsBulkActions","SavedHostsManualAdd",
            "SavedHostsMoveMenu","SavedHostsStyle")
            "audit"   = @("GuiAuditPanel","GuiAuditTable","GuiAuditLegend")
            "privacy" = @("GuiPrivacyPanel","PrivacyNetworkActions","PrivacyPanelStyle")
        }
        TestRoots = @("test\main\java\networktool\gui\panels")
        TestFileMap = @{
            "SavedHostsBulkActionsTest.java" = "saved"
            "GuiAuditPanelTest.java"         = "audit"
        }
        VisibilityFixFiles = @()
    }
)

# ══════════════════════════════════════════════════════════════════════
#  Ausführung: für jeden Split dieselbe Pipeline
# ══════════════════════════════════════════════════════════════════════

foreach ($split in $splits) {
    $oldPkg  = $split.OldPkg
    $srcRoot = $split.SrcRoot
    $map     = $split.Map

    Write-Host "`n=== Splitte $oldPkg ===" -ForegroundColor Cyan

    if (-not (Test-Path $srcRoot)) {
        Write-Host "  Übersprungen (Pfad nicht gefunden: $srcRoot)" -ForegroundColor Yellow
        continue
    }

    # 1) Zielordner anlegen + Produktivklassen verschieben
    foreach ($sub in $map.Keys) {
        $dir = Join-Path $srcRoot $sub
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        foreach ($cls in $map[$sub]) {
            $from = Join-Path $srcRoot "$cls.java"
            $to   = Join-Path $dir "$cls.java"
            if (Test-Path $from) {
                git mv $from $to 2>$null
                if (-not $?) { Move-Item $from $to -Force }
                (Get-Content $to -Raw) `
                    -replace "package $([regex]::Escape($oldPkg));", "package $oldPkg.$sub;" `
                    | Set-Content $to -NoNewline
            }
        }
    }

    # 2) Testdateien gemäß expliziter Zuordnung verschieben
    foreach ($testRoot in $split.TestRoots) {
        if (-not (Test-Path $testRoot)) { continue }
        foreach ($fileName in $split.TestFileMap.Keys) {
            $sub  = $split.TestFileMap[$fileName]
            $from = Join-Path $testRoot $fileName
            if (-not (Test-Path $from)) { continue }
            $destDir = Join-Path $testRoot $sub
            New-Item -ItemType Directory -Force -Path $destDir | Out-Null
            $to = Join-Path $destDir $fileName
            git mv $from $to 2>$null
            if (-not $?) { Move-Item $from $to -Force }
            (Get-Content $to -Raw) `
                -replace "package $([regex]::Escape($oldPkg));", "package $oldPkg.$sub;" `
                | Set-Content $to -NoNewline
        }
    }

    # 3) Bestehende Imports/FQN-Referenzen repo-weit umschreiben
    $allFiles = Get-ChildItem -Recurse -Include *.java -Path "src","test"
    $classToSub = @{}
    foreach ($sub in $map.Keys) { foreach ($cls in $map[$sub]) { $classToSub[$cls] = $sub } }

    foreach ($file in $allFiles) {
        $text = Get-Content $file.FullName -Raw
        $orig = $text
        foreach ($cls in $classToSub.Keys) {
            $sub = $classToSub[$cls]
            $text = $text -replace "(import\s+$([regex]::Escape($oldPkg)))\.$cls;", "`$1.$sub.$cls;"
            $text = $text -replace "(?<!\.)$([regex]::Escape($oldPkg))\.$cls\b", "$oldPkg.$sub.$cls"
        }
        if ($text -ne $orig) { Set-Content $file.FullName $text -NoNewline }
    }

    # 4) Fehlende Cross-Subpackage-Imports automatisch ergänzen
    $srcRootFull = (Resolve-Path $srcRoot).Path
    function Get-CurrentSub($filePath, $rootFull) {
        $rel = $filePath.Substring($rootFull.Length).TrimStart('\')
        $parts = $rel -split '\\'
        if ($parts.Length -gt 1) { return $parts[0] }
        return "root"
    }

    $allFiles = Get-ChildItem -Recurse -Include *.java -Path "src","test"
    foreach ($file in $allFiles) {
        $text = Get-Content $file.FullName -Raw
        $orig = $text
        $ownClassName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)

        $isUnderSrcRoot = $file.FullName.StartsWith($srcRootFull)
        $currentSub = if ($isUnderSrcRoot) { Get-CurrentSub $file.FullName $srcRootFull } else { $null }

        $importsToAdd = New-Object System.Collections.Generic.List[string]

        foreach ($cls in $classToSub.Keys) {
            if ($cls -eq $ownClassName) { continue }
            if (-not ($text -match "\b$cls\b")) { continue }

            $targetSub = $classToSub[$cls]
            if ($isUnderSrcRoot -and $currentSub -eq $targetSub) { continue }

            $fqn = "$oldPkg.$targetSub.$cls"
            if ($text -match [regex]::Escape("import $fqn;")) { continue }

            $importsToAdd.Add("import $fqn;")
        }

        if ($importsToAdd.Count -gt 0) {
            $block = ($importsToAdd | Sort-Object -Unique) -join "`n"
            $text = $text -replace "(package [^\n]+;\n)", "`$1`n$block`n"
            Set-Content $file.FullName $text -NoNewline
        }
    }

    # 5) Sichtbarkeits-Fix: package-private static Methoden -> public
    #    (nötig, wenn eine im Root verbliebene Klasse von mehreren neuen
    #     Subpackages aus verwendet wird)
    foreach ($fname in $split.VisibilityFixFiles) {
        $fpath = Join-Path $srcRoot $fname
        if (Test-Path $fpath) {
            (Get-Content $fpath -Raw) `
                -replace "(?m)^(\s{4})static ", "`$1public static " `
                | Set-Content $fpath -NoNewline
            Write-Host "  Sichtbarkeit angehoben: $fname" -ForegroundColor DarkYellow
        }
    }

    Write-Host "  Fertig: $oldPkg" -ForegroundColor Green
}

# ══════════════════════════════════════════════════════════════════════
#  Abschluss
# ══════════════════════════════════════════════════════════════════════

Write-Host "`n=== Alle Splits durchgeführt ===" -ForegroundColor Cyan
Write-Host "Nächste Schritte:" -ForegroundColor White
Write-Host "  1) mvn -q compile        2>&1 | Tee-Object compile-errors.txt"
Write-Host "  2) mvn -q test-compile   2>&1 | Tee-Object test-compile-errors.txt"
Write-Host "  3) Verbleibende Fehler manuell prüfen (typischerweise package-private"
Write-Host "     Cross-Package-Zugriffe, die Schritt 5 nicht automatisch erkennen konnte)."
Write-Host "  4) mvn -q test           2>&1 | Tee-Object test-errors.txt"
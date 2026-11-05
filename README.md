# NetTool

NetTool is a Java desktop application for discovering and inspecting devices on
networks you own or are authorized to administer. The application combines a
Swing-based graphical interface with network scanning, host details, saved
inventory, monitoring, and security-oriented diagnostics.

> **Authorization:** Network discovery and security probes can generate traffic
> and may be interpreted as suspicious by network equipment. Use NetTool only
> on networks and devices for which you have explicit permission. Review scan
> settings and the impact of each operation before running it.

## Highlights

- **Network discovery:** IPv4 and IPv6 host scanning, subnet detection, ARP
  discovery and sniffing, hostname resolution, and optional route-aware scans.
- **Host inspection:** Ping and reachability information, open-port scans,
  service banners, operating-system fingerprinting, and traceroute.
- **Network context:** SNMP-based device information, VLAN detection, DHCP,
  mDNS and UPnP discovery, OUI/vendor lookup, and a visual network map.
- **Inventory and history:** Save hosts and network snapshots, organize hosts
  with tags, compare scans, review scan timelines, and track offline devices
  and port changes.
- **Monitoring and automation:** Scheduled scans, Wake-on-LAN, bandwidth
  history and visualization, traffic sonification, and notifications.
- **Security diagnostics:** TLS certificate inspection, rogue-DHCP detection,
  default-credential checks, and collected security findings.
- **Desktop workflow:** Searchable results, host detail views, export and
  backup actions, audit/debug views, and user/login screens.

Feature availability depends on the operating system, network adapter,
permissions, and the capabilities of devices on the network. Some operations
require elevated privileges or optional tools and may be skipped or unavailable
on a particular machine.

## Requirements

- Java Development Kit (JDK) 21 or later.
- Apache Maven 3.8.9 or later (the project is built with Maven).
- A graphical desktop environment to use the Swing interface.
- Windows is the supported and fully tested desktop platform. CI also compiles
  the application and runs a targeted platform-independent smoke-test set on
  Linux and macOS; network operations on those systems are not yet guaranteed.
  Some features depend on Windows PowerShell and Windows networking APIs.

The project uses Maven Compiler Plugin with Java release 21. JUnit 5 is used
for tests; JaCoCo measures coverage.

## Build and run

From the repository root:

```powershell
# Compile and run the default test suite
mvn clean test

# Package the executable JAR
mvn package

# Build a portable Windows package with an included Java runtime
.\tools\scripts\Build-WindowsPackage.ps1

# Start the graphical application
java -jar target/nettool-0.8.12.jar
```

The Windows packaging script requires JDK 21+ with `jpackage` and a built
application JAR. It creates `target/dist/nettool-<version>-windows-x64.zip`,
including a launcher and Java runtime; unzip it and start `NetTool\NetTool.exe`.
Application data stays in the user's `.nettool` directory, separate from the
package. CI builds and smoke-tests this package on Windows and publishes it as
a workflow artifact.

With the current Maven coordinates, the executable JAR is
`target/nettool-0.8.12.jar`. To print the supported
command-line options or version:

```powershell
java -jar target/nettool-0.8.12.jar --help
java -jar target/nettool-0.8.12.jar --version
java -jar target/nettool-0.8.12.jar --profiles
java -jar target/nettool-0.8.12.jar --profiles-json
```

Starting without arguments opens the graphical application. `--profiles` lists
saved scan profiles, and `--profiles-json` emits their names and CIDRs as JSON.
Both are read-only local operations and send no network traffic. Active scans
are intentionally not available in headless CLI mode.

Security findings are kept in `.nettool/networkdata/security-findings.json`
across application restarts. The panel supports open/resolved states and
explicit deletion of the local history. Findings include their detection
source, local observation time, and an evidence-confidence label. Confidence describes how directly the evidence
supports a finding, not its severity: certificate expiry and a successful FTP
anonymous login are marked confirmed, while a local banner-to-CVE match is
low-confidence because a matching banner alone does not prove a vulnerability.
HTTP 200 responses are medium-confidence evidence, not proof by themselves.
Findings can be exported as a semicolon-delimited CSV from the Security-Findings
panel. The export includes evidence metadata and state and is written only to
the user-selected location. CIDR scans validate IPv4 ranges and refuse ranges larger than 65,534 hosts;
scan profiles show the targets and estimated size
before execution. The latest ten completed scans are stored locally for
comparison after restarting the application; interrupted or incomplete scans
are not added to that history.

## Tests

The standard Maven test configuration excludes tests tagged `slow`. For a
clean run with explicit report and result validation, use the repository
script:

```powershell
.\tools\scripts\Verify-TestSuite.ps1
```

Useful variants:

```powershell
# Run one selected test or test class
.\tools\scripts\Verify-TestSuite.ps1 -TestSelector 'OsParallelStepRunnerTest'

# Include slow tests
.\tools\scripts\Verify-TestSuite.ps1 -IncludeSlow

# Repeat independent standard-suite runs
.\tools\scripts\Verify-TestSuite.ps1 -Runs 5

# Also run Maven verify, including the configured JaCoCo coverage gate
.\tools\scripts\Verify-TestSuite.ps1 -Verify
```

The complete suite, including slow tests, can also be run directly through the
nightly Maven profile:

```powershell
mvn clean test -Pnightly
```

See [`docs/testing.md`](docs/testing.md) for the test-report contract, coverage
workflow, CI behavior, and guidance for interpreting skipped tests.

## Data and local files

At runtime, NetTool stores application data in the current user's home
directory under `.nettool/` (for example `%USERPROFILE%\.nettool` on Windows).
The legacy project-local `saves/` directory is copied into this location on
first startup; existing destination files are kept, and the old files are not
deleted automatically. To use a different location, set the Java system
property `-Dnettool.dataDir=<path>`:

| Directory | Contents |
|---|---|
| `.nettool/userdata/` | User/account data |
| `.nettool/networkdata/` | Saved networks, hosts, scan history, security findings, and related inventory |
| `.nettool/profiles/` | Scan profiles |
| `.nettool/logs/` | Audit and debug logs |
| `.nettool/cache/` | Local caches and application settings |

These files are local application state, not source code. Do not publish
personal or sensitive network inventories, account data, logs, or backups.
Runtime contents under the repository's `saves/` directory are ignored by Git;
only an empty placeholder is retained. The ignore rule does not remove older
copies from Git history.

## Repository layout

```text
src/main/java/networktool/   Application source
src/main/resources/          Application resources
test/                        JUnit tests and test resources
docs/                        Project and testing documentation
tools/                       Quick access, guides, and tool documentation
tools/scripts/               PowerShell and JavaScript maintenance scripts
tools/output/                Generated reports and script output
.github/workflows/           GitHub Actions CI configuration
pom.xml                      Maven build and plugin configuration
```

The Java code is organized by responsibility. Important areas include:

- `gui/` — Swing windows, panels, actions, map, dashboards, and UI support.
- `logic/scan/` — host discovery, scanning, scheduling, and scan history.
- `logic/analysis/` — protocol discovery, OS detection, probes, and findings.
- `logic/ports/` — port scanning and service banner handling.
- `storage/` — persistence and local data formats.
- `security/` — authentication and audit logging.
- `model/` — shared application data structures.
- `cli/` — command-line options for help/version and read-only scan-profile
  listing (`--help`, `--version`, `--profiles`, and `--profiles-json`).

## Development tools

The `tools/scripts/` directory includes scripts for verifying the suite,
collecting coverage history, measuring test runtimes, and identifying
potentially unused code. Generated script reports are stored in
`tools/output/`. Quick access commands, code guides, and tool documentation
remain directly in `tools/`. See [`tools/README.md`](tools/README.md) for
details.

## CI

GitHub Actions runs the standard suite on pushes and pull requests. Its
scheduled workflow repeats the standard suite and also runs the slow tests.
The workflow uses Windows and Java 21; Surefire reports and run summaries are
retained as workflow artifacts.

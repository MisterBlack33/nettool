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
- Windows is the primary tested environment; some network operations use
  Windows PowerShell. Behavior on other operating systems may differ.

The project uses Maven Compiler Plugin with Java release 21. JUnit 5 is used
for tests; JaCoCo measures coverage.

## Build and run

From the repository root:

```powershell
# Compile and run the default test suite
mvn clean test

# Package the executable JAR
mvn package

# Start the graphical application
java -jar target/nettool-0.8.12.jar
```

With the current Maven coordinates, the executable JAR is
`target/nettool-0.8.12.jar`. To print the supported
command-line options or version:

```powershell
java -jar target/nettool-0.8.12.jar --help
java -jar target/nettool-0.8.12.jar --version
```

Starting without arguments opens the graphical application. The CLI currently
provides help and version output; it does **not** provide a headless scanning
mode.

## Tests

The standard Maven test configuration excludes tests tagged `slow`. For a
clean run with explicit report and result validation, use the repository
script:

```powershell
.\tools\Verify-TestSuite.ps1
```

Useful variants:

```powershell
# Run one selected test or test class
.\tools\Verify-TestSuite.ps1 -TestSelector 'OsParallelStepRunnerTest'

# Include slow tests
.\tools\Verify-TestSuite.ps1 -IncludeSlow

# Repeat independent standard-suite runs
.\tools\Verify-TestSuite.ps1 -Runs 5

# Also run Maven verify, including the configured JaCoCo coverage gate
.\tools\Verify-TestSuite.ps1 -Verify
```

The complete suite, including slow tests, can also be run directly through the
nightly Maven profile:

```powershell
mvn clean test -Pnightly
```

See [`docs/testing.md`](docs/testing.md) for the test-report contract, coverage
workflow, CI behavior, and guidance for interpreting skipped tests.

## Data and local files

At runtime, NetTool stores application data under `saves/` in the project
directory:

| Directory | Contents |
|---|---|
| `saves/userdata/` | User/account data |
| `saves/networkdata/` | Saved networks, hosts, and related inventory |
| `saves/profiles/` | Scan profiles |
| `saves/logs/` | Audit and debug logs |
| `saves/cache/` | Local caches and application settings |

These files are local application state, not source code. Do not publish
personal or sensitive network inventories, account data, logs, or backups.
The `saves/` directory is ignored by Git.

## Repository layout

```text
src/main/java/networktool/   Application source
src/main/resources/          Application resources
test/                        JUnit tests and test resources
docs/                        Project and testing documentation
tools/                       PowerShell test and maintenance scripts
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
- `cli/` — the small command-line contract (`--help` and `--version`).

## Development tools

The `tools/` directory includes PowerShell scripts for verifying the suite,
collecting coverage history, measuring test runtimes, and identifying
potentially unused code. Start these scripts from the repository root unless
their own help or documentation says otherwise. See [`tools/README.md`](tools/README.md)
for script details and the Claude project-inventory report.

## CI

GitHub Actions runs the standard suite on pushes and pull requests. Its
scheduled workflow repeats the standard suite and also runs the slow tests.
The workflow uses Windows and Java 21; Surefire reports and run summaries are
retained as workflow artifacts.

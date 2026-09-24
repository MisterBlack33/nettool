# Paralleler Entwicklungsplan für die nächsten Erweiterungen

## Ziel

Die nächsten Arbeiten sollen gleichzeitig in vier Workstreams umgesetzt werden
können. Jeder Workstream hat deshalb einen eigenen fachlichen Schwerpunkt,
einen möglichst exklusiven Dateibereich und klar definierte Übergaben. Ziel ist
nicht, möglichst viele Features zu produzieren, sondern Sicherheit,
Reproduzierbarkeit, Testbarkeit und Nutzbarkeit dauerhaft zu verbessern.

Die vorgeschlagenen Erweiterungen bleiben sinnvoll. Die aktuelle Reihenfolge
ist:

1. Sicherheitsrisiken und kritische Fehlpfade absichern
2. Risikobasierte Regressionstests ergänzen
3. Bedienbarkeit und Wartbarkeit verbessern
4. Dokumentation, CLI und reproduzierbare Abläufe vervollständigen

## Gemeinsame Regeln

1. Jeder Workstream arbeitet auf einem eigenen Branch:

   ```text
   improvement/ws-a-security
   improvement/ws-b-regression-tests
   improvement/ws-c-errors-maintenance
   improvement/ws-d-cli-documentation
   ```

2. Ein Workstream bearbeitet keine Dateien aus dem exklusiven Bereich eines
   anderen Workstreams.
3. Gemeinsame Dateien wie `pom.xml`, zentrale Test-Hilfen und `README.md`
   werden nur nach vorheriger Abstimmung geändert.
4. Bestehende Tests und Datenformate werden nicht gelöscht oder abgeschwächt.
5. Externe Hosts, echte Credentials, sichtbare GUI-Fenster und
   Administratorrechte gehören nicht in den Standard-Testlauf.
6. Jede Verhaltensänderung erhält einen fokussierten Test. Produktionscode wird
   nicht nur zur Erhöhung der Coverage geändert.
7. Keine breiten `catch`-Blöcke, stillen Fallbacks oder globalen
   JaCoCo-Ausnahmen als Ersatz für eine fachliche Lösung.
8. Jeder Workstream führt seine eigenen Tests aus und dokumentiert verbleibende
   Risiken für die Integrationsphase.

## Dateibesitz und Konfliktvermeidung

| Workstream | Exklusiver Produktionsbereich | Exklusiver Test-/Dokumentationsbereich |
|---|---|---|
| A Sicherheit | `src/main/java/networktool/security`, sicherheitsrelevante Teile von `util`, `storage/export`, `logic/messaging` | neue Security-Regressionstests in den entsprechenden Testpaketen |
| B Regressionstests | keine Produktionsänderungen ohne Fehlernachweis | Scan-, Netzwerk-, Storage- und Transfer-Tests |
| C Fehler und Wartbarkeit | `logic/scan`, `logic/analysis`, GUI-Status-/Fehlerpfade; jeweils paket- oder klassenweise | zugehörige Tests in den vorhandenen Fachpaketen |
| D CLI und Dokumentation | `Main`, CLI-Paket und Automatisierung; nur abgestimmte Build-Dateien | `README.md`, Entwicklerdokumentation und CLI-Tests |

Die Pfadangaben sind Zuständigkeitsbereiche, keine Erlaubnis für großflächige
Refactorings. Falls eine Klasse mehrere Themen berührt, wird sie vor Beginn
einem einzigen Workstream zugeordnet.

---

## Workstream A: Sicherheits- und Robustheitsaudit

**Ziel:** Hochsichere Sicherheitsbefunde identifizieren, priorisieren und die
drei wichtigsten Risiken mit Regressionstests beheben.

**Verantwortung:**

- Authentifizierung, Sessions, Lockout und Audit-Logging
- `SafeCommand` und Prozessargumente
- JSON-Importe, Exportpfade und Pfad-Traversal
- Webhook-, Ntfy-, SSH- und WinRM-Ziele
- gespeicherte Credentials, Tokens, private Notizen und sensible Logs

**Vorgehen:**

1. Datenflüsse und Eingangsgrenzen der betroffenen Komponenten inventarisieren.
2. Nur Findings mit konkretem Datenfluss, betroffener Stelle und hoher
   Ausnutzbarkeit melden.
3. Findings nach Schweregrad und Beweissicherheit priorisieren.
4. Die drei kritischsten Findings einzeln beheben.
5. Für jede Behebung einen deterministischen Regressionstest ergänzen.
6. Vorhandene Speicherformate und Nutzerabläufe kompatibel halten.

**Nicht im Scope:** allgemeine Refactorings, GUI-Layout, neue CLI-Optionen und
Coverage-Arbeiten außerhalb der sicherheitsrelevanten Regressionstests.

**Abnahmekriterien:**

- Kein hochsicheres, unbehandeltes Finding ohne dokumentierte Begründung.
- Keine Passwörter, Tokens oder privaten Inhalte in neuen Logs und Ausgaben.
- Importziele und externe URLs werden nur nach fachlich begründeter Validierung
  verarbeitet.
- Die Security-Regressionstests laufen ohne Netzwerkzugriff und ohne echte
  Credentials.
- Änderungen sind auf nachgewiesene Risiken begrenzt.

**Arbeitsauftrag:**

> Prüfe ausschließlich die festgelegten Sicherheitsbereiche auf ausnutzbare
> Schwachstellen. Melde nur Findings mit konkretem Datenfluss und hoher
> Sicherheit. Behebe anschließend maximal die drei wichtigsten Findings,
> jeweils mit einem reproduzierbaren JUnit-Regressionstest. Ändere keine
> unbeteiligten Komponenten.

---

## Workstream B: Risikobasierte Regressionstests

**Ziel:** Kritische bisher ungetestete Verhaltensfälle abdecken, ohne
Produktionscode vorsorglich umzubauen.

**Testbereiche:**

- IPv4, IPv6, CIDR, Subnetzgrenzen und ungültige Adressen
- Timeouts, leere oder unvollständige Antworten und fehlerhafte Pakete
- Scan-Abbruch, Retry, parallele Ausführung und Scheduler-Statusübergänge
- Import/Export alter, unbekannter und beschädigter Datenformate
- Dateiübertragung, Teilübertragung, Berechtigungsfehler und Ressourcenabbau
- deterministische Netzwerk-, Prozess- und Zeit-Test-Doubles

**Vorgehen:**

1. Baseline und JaCoCo-Bericht auswerten.
2. Die fünf kritischsten ungetesteten Verhaltensfälle je abgegrenztem Paket
   auswählen.
3. Happy Path, Fehlerpfad und relevante Grenzfälle implementieren.
4. Race- und Timeout-Tests ohne feste Sleeps oder reale Systeme schreiben.
5. Einen Produktionsfehler nur dann beheben, wenn ein Test ihn nachweist.
6. Coverage-Deltas und verbleibende untestbare Pfade dokumentieren.

**Nicht im Scope:** Security-Produktionscode aus Workstream A, GUI-Refactorings,
neue CLI-Funktionen und Änderungen am globalen Coverage-Gate.

**Abnahmekriterien:**

- Tests sind unabhängig, deterministisch und mindestens fünfmal wiederholbar.
- Kein Test benötigt einen erreichbaren Host, Administratorrechte oder sichtbare
  Fenster.
- Fehler werden über Rückgabewerte, Exceptions oder Statusübergänge geprüft,
  nicht nur über „kein Absturz“.
- Der betroffene Paketlauf und anschließend `mvn clean verify` sind
  nachvollziehbar dokumentiert.
- Keine Dummy-Tests und keine Coverage-Ausnahmen nur zur Quotenerfüllung.

**Arbeitsauftrag:**

> Analysiere ein festgelegtes Scan-, Netzwerk-, Storage- oder Transferpaket.
> Identifiziere die fünf wichtigsten ungetesteten Verhaltensfälle und
> implementiere fokussierte JUnit-5-Tests mit Test-Doubles. Ändere
> Produktionscode ausschließlich bei einem konkret reproduzierten Fehler.

---

## Workstream C: Fehlerbehandlung und technische Wartbarkeit

**Ziel:** Fehler für Nutzer und Entwickler unterscheidbar machen und kleine,
risikobegrenzte Wartbarkeitsverbesserungen umsetzen.

**Verantwortung:**

- Timeout, Host-offline, DNS-Fehler, Verbindungsabbruch und
  Berechtigungsfehler unterscheiden
- GUI-Statusmeldungen und Fehlermeldungen vereinheitlichen
- Logs mit Host, Port, Scan-ID und Vorgangskontext ergänzen, ohne Geheimnisse
  zu protokollieren
- still verschluckte Fehler in den festgelegten Scan- und Analysepaketen
  sichtbar machen
- doppelte GUI-Aktionen und Statuslogik in einzelnen Klassen zusammenführen
- Nullwert- und Nebenläufigkeitsrisiken an Scan- und Scheduler-Grenzen
  beseitigen

**Vorgehen:**

1. Einen konkreten Fehlerpfad oder eine einzelne Klasse auswählen.
2. Bestehendes Verhalten mit Tests aus Workstream B oder vorhandenen Tests
   festhalten.
3. Nur die betroffene Klasse bzw. das betroffene Paket ändern.
4. Fehlerkontext über bestehende Domänenfehler, Statusobjekte oder
   Logging-Muster transportieren.
5. Nach jedem Refactoring Tests und relevante Coverage prüfen.

**Nicht im Scope:** Sicherheitsaudit und Security-Fixes aus A, neue CLI-Verträge
aus D sowie großflächige Paketverschiebungen.

**Abnahmekriterien:**

- Timeout, Offline-Zustand und Berechtigungsfehler sind im betroffenen Ablauf
  unterscheidbar.
- Fehler führen nicht mehr zu einer irreführenden Erfolgsmeldung.
- Keine sensiblen Daten erscheinen in Logs oder GUI-Meldungen.
- Keine breiten Catch-Blöcke oder stillen Erfolgs-Fallbacks werden eingeführt.
- Jede Refactoring-Änderung ist durch bestehende oder neue Tests abgesichert.

**Arbeitsauftrag:**

> Bearbeite genau ein festgelegtes Paket oder eine Klasse. Verbessere einen
> konkreten Fehlerpfad oder eine nachgewiesene Wartbarkeitslast. Bewahre das
> beobachtbare Verhalten, außer die Änderung behebt einen dokumentierten
> Fehler, und ergänze die dafür nötigen fokussierten Tests.

---

## Workstream D: CLI, Dokumentation und reproduzierbare Abläufe

**Ziel:** Nutzung, Automatisierung und Projekthand-off verbessern, ohne
unfertige Optionen oder nicht implementierte Verhaltensversprechen zu
dokumentieren.

**Verantwortung:**

- produktiven CLI-Einstiegspunkt und seinen bestehenden Umfang inventarisieren
- reproduzierbare Scan-Profile vorbereiten
- JSON-Ausgabe und stabile Exit-Codes nur auf Basis eines klaren Vertrags
  ergänzen
- Optionen wie `--timeout`, `--interface`, `--output` und `--quiet` erst nach
  geklärtem Umfang implementieren
- README, Installation, Berechtigungen und Troubleshooting aktualisieren
- Scan-Tiefen, Datenschutz, Sicherheitsmodell und Teststrategie dokumentieren
- Standardlauf `mvn clean verify` und relevante Reports beschreiben

**Empfohlene CLI-Reihenfolge:**

1. produktiven Einstiegspunkt und unterstützte Befehle dokumentieren
2. CLI-Parsing und ungültige Eingaben testen
3. reproduzierbare Scan-Profile festlegen
4. maschinenlesbare JSON-Ausgabe definieren
5. Exit-Codes für Erfolg, Eingabefehler und Scanfehler festlegen
6. Komfortoptionen erst danach ergänzen

**Nicht im Scope:** Änderungen an Scanlogik und Sicherheitsimplementierungen,
außer eine ausdrücklich abgestimmte CLI-Schnittstelle benötigt eine kleine
Adapteränderung.

**Abnahmekriterien:**

- Dokumentation beschreibt ausschließlich vorhandenes oder im selben Change
  vollständig implementiertes Verhalten.
- CLI-Tests decken gültige Eingaben, fehlende Argumente, ungültige Werte,
  Fehlerausgaben und Exit-Codes ab.
- JSON-Ausgabe ist deterministisch und für Skripte geeignet.
- Bestehende GUI- und Bibliotheksaufrufe bleiben kompatibel.
- Dokumentationsänderungen enthalten keine Zugangsdaten oder internen Secrets.

**Arbeitsauftrag:**

> Inventarisiere zuerst den produktiven CLI-Einstiegspunkt. Ergänze nur einen
> kleinen, klar dokumentierten CLI-Vertrag mit Tests für Eingaben, Ausgaben,
> Fehler und Exit-Codes. Aktualisiere anschließend README und
> Sicherheits-/Entwicklerdokumentation entsprechend der tatsächlichen
> Implementierung.

---

## Integrationsphase

Die Workstreams können parallel starten. Die Integration erfolgt erst, wenn
alle vier ihre eigenen Abnahmekriterien erfüllt haben.

### Übergaben

- A übergibt eine priorisierte Finding-Liste, behobene Findings und
  Security-Regressionstests.
- B übergibt Testfälle, Coverage-Deltas und bekannte Testlücken.
- C übergibt geänderte Fehlerverträge, Status-/Logänderungen und betroffene
  Testpakete.
- D übergibt CLI-Vertrag, Dokumentationsänderungen und CLI-Testprotokoll.

### Integrationsregeln

1. Gemeinsame Konflikte werden nicht durch stilles Überschreiben gelöst.
2. Bei Änderungen an `pom.xml`, zentralen Test-Hilfen oder `README.md` wird
   jeweils nur ein Integrationsbeitrag übernommen.
3. Security-Fixes aus A werden vor der finalen Coverage- und CLI-Bewertung
   integriert.
4. Änderungen an Fehlerverträgen aus C werden in CLI- und Dokumentationstexten
   aus D abgeglichen.
5. Regressionstests aus B und A müssen gemeinsam im Standardlauf bestehen.

### Verbindliche Abschlussprüfung

```powershell
mvn clean verify
```

Zusätzlich werden geprüft:

- JaCoCo-Bericht und Coverage-Gate
- Security-Regressionstests ohne externe Systeme
- wiederholbarer CLI-Testlauf mit stabilen Ausgaben und Exit-Codes
- Dokumentation gegen das tatsächlich implementierte Verhalten
- keine sensiblen Daten in Logs, Testausgaben oder Beispieldateien

## Kurzfazit

Die vier Workstreams teilen die ursprünglichen Themen so auf, dass Sicherheit,
Regressionstests, Fehlerbehandlung/Wartbarkeit sowie CLI/Dokumentation
gleichzeitig bearbeitet werden können. Die wichtigste technische Regel ist die
klare Dateiverantwortung; gemeinsam genutzte Verträge und Dateien werden erst
in der Integrationsphase zusammengeführt.

# AI-Task 2: Workstream B – Multi-User-System reduzieren

## Ziel
Die Nebenlogik um Hostownership und User-Host-Zuordnung reduzieren, ohne den Login-/Rollenmechanismus und die Sicherheitslogik zu beschädigen.

## Relevante Dateien
- `src/main/java/networktool/storage/network/HostOwnership.java`
- `src/main/java/networktool/storage/network/HostOwnershipPersistence.java`
- `src/main/java/networktool/storage/network/UserHostStore.java`
- `src/main/java/networktool/storage/network/NetworkStore.java`
- `src/test/...` soweit Tests für Ownership / UserHostStore betroffen sind

## Wichtig
Dies ist ein eigenständiger Workstream. Der Logging-Fehler muss nicht hier gelöst werden. Die AI sollte nur die Host-Ownership-Reduktion dokumentieren und die Interaktion mit anderen Workstreams sauber halten.

## Erwartete Änderung
1. `HostOwnership`, `HostOwnershipPersistence` und `UserHostStore` entfernen oder auf `NetworkStore` umstellen.
2. Rollen-Gating beibehalten, insbesondere `isAdmin()` für kritische Aktionen.
3. `hostOwnership.json` beim nächsten Start ignorieren bzw. einmalig bereinigen, aber kein neues Ownership-System erzeugen.
4. Tests anpassen oder entfernen, falls sie von der alten Ownership-Schicht abhängen.

## Nicht anfassen
- `UserAuth` und Passwort-Hashing
- `AuditLogger` / `DebugLogger`
- `GuiTheme` / Farbschema
- Sidebar- oder Icon-/Emoji-Änderungen

## Schnittstelle
Falls `GuiSavedHostsPanel` oder GUI-Dialoge trotzdem `UserHostStore` referenzieren, sollte diese Datei vorab mit Workstream D abgeklärt werden. Dies ist aber laut Roadmap derzeit keine direkte Kollision.

## Verifikation
- Nach dem Refactor sollte die Rollen-Logik für kritische Aktionen weiterhin bestehen.
- Die gespeicherten Hostdaten im vorhandenen Netzwerk-Store bleiben unberührt.

## Grundsatz
Der Workstream B zielt auf eine Reduktion der Komplexität: Hosts sind weiterhin Teil von Netzwerken, aber nicht zusätzlich an einzelne Nutzer gebunden, sofern die bestehende Architektur das modulartig noch erlaubt.

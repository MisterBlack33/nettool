# AI-Task 4: Workstream D – Sidebar-Struktur & Icon-/Emoji-Entfernung

## Ziel
Sidebar, Output-Panel und UI-Labels frei von Emojis/Icons machen; Test-Suite visuell abtrennen, ohne Funktionalität zu ändern.

## Relevante Dateien
- `src/main/java/networktool/gui/components/GuiSidebar.java`
- `src/main/java/networktool/gui/components/SidebarAccordion.java`
- `src/main/java/networktool/gui/panels/GuiOutputPanel.java`
- `src/main/java/networktool/gui/panels/OutputStreamRedirector.java`
- Status-/Kontextmenü-Dateien mit Emoji-Nutzung, z. B.:
  - `GuiContextMenu.java`
  - `HostDetailRows.java`
  - `GuiDiagnosticsActions.java`
  - `GuiSonifyActions.java`
  - `ContextMenuActions.java`
  - `PrivacyPanelStyle.java`
  - `GuiAuditPanel.java`
  - `NotificationTcpServer.java`
  - `NtfySubscriptionManager.java`
  - `SavedHostsManualAdd.java`

## Erwartete Änderung
1. In `GuiSidebar`/`SidebarAccordion` keine Icon-Strings im Inhalts- oder Header-Rendering mehr nutzen.
2. Emoji-Präfixe in Ausgabefenstern durch textuelle Status-Tags ersetzen:
   - `[OK]`
   - `[FEHLER]`
   - `[WARN]`
   - `[INFO]`
3. Button-/Kontextmenü-Labels ohne Emoji, nur mit Text.
4. Test-Suite visuell abgrenzen: eigener Bereich, eigene Farben, klare Beschriftung wie `TEST-SUITE (nur Entwicklung)`.

## Technik
- Eine kleine, zentrale Helper-Klasse wie `StatusTags` ist erwünscht, damit nicht jede Klasse eigenen String-Code erfindet.
- Das UI-Layout darf nur in der Darstellung verändert werden; Funktionalität der Menü-Handler bleibt unverändert.

## Nicht anfassen
- `GuiMenuHandler` / `GuiMenuDispatch`-Logik
- ASCII-Rahmen und bestehende Layout-Logik, sofern sie nicht direkt die Emoji-/Icon-Darstellung betrifft
- Daten-/Logik-Persistence außerhalb der Anzeige

## Schnittstelle zu Workstream C
- Die Farben für die Test-Suite-Sektion sollen aus `GuiTheme` bezogen werden.
- Wenn C noch nicht gemerged ist, dürfen vorübergehend lokale Platzhalter-Konstanten verwendet werden, aber nach dem Merge muss auf die zentrale `GuiTheme`-Quelle zurückgestellt werden.

## Verifikation
- Die UI-Ausgaben sollten keine Emoji-/Icon-Präfixe mehr enthalten.
- Die Test-Suite sollte klar von Produktivfunktionen getrennt sein.
- Der Code soll nur Darstellung und Markierung betreffen, nicht das Verhalten der Funktionen.

## Grundsatz
Dieser Workstream ist rein auf Sichtbarkeit, Trennung und textbasierte Statusdarstellung fokussiert. Er darf keine Logik- oder Datenrefactor betreffen.

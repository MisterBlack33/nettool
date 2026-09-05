# AI-Task 3: Workstream C – Farbschema in `GuiTheme`

## Ziel
`GuiTheme` soll das zentrale Farbschema für Dark/Light-Mode konsolidieren und konsistente Farbklassen statt Einzelwerten bereitstellen.

## Relevante Dateien
- `src/main/java/networktool/theme/GuiTheme.java`
- optional: `src/main/java/networktool/theme/GuiThemeDark.java`
- optional: `src/main/java/networktool/theme/GuiThemeLight.java`
- Testdateien: `GuiThemePackageTest.java`, `GuiThemeFixTest.java`

## Erwartete Änderung
1. Bestehende Farbkonstanten durch strukturierte Farbpalette ersetzen:
   - Background / Surface / Border / Text-Primary / Text-Dim / Accent / Warn / Success
2. Dark- und Light-Mode-Paletten definieren.
3. OS-/Kategorie-Farben (`WIN_COL`, `LIN_COL`, `APL_COL`, `AND_COL`, `NET_COL`, `PRN_COL`, `IOT_COL`, `RPI_COL`) modusabhängig anpassen.
4. `applyToStatics()` bleibt der zentrale Umschaltpunkt.

## Wichtig
Die Änderung betrifft nur `GuiTheme` selbst und passende Tests. Andere GUI-Klassen dürfen im Rahmen dieses Workstreams nicht händisch umgefärbt werden, da das mit Workstream D und eventuell anderen Klassen überschneidet.

## Nicht anfassen
- `toggleTheme()` / die Theme-Umschalt-Logik
- andere GUI-Klassen mit hardcoded `new Color(0x...)`
- Sidebar-/Emoji/Statusanzeige

## Schnittstellen
- Workstream D benötigt die Test-Suite-Farben ggf. für die Sidebar-Abgrenzung.
- `GuiTheme` soll die zentralen Konstanten liefern; D darf nur referenzieren, nicht `GuiTheme.java` gleichzeitig mit eigenen Änderungen überschreiben.

## Verifikation
- Nach dem Fix sollten die Farbkonstanten konsistent und mit klaren Semantiken definiert sein.
- Die Theme-Umschalt-Tests müssen weiterhin funktionieren.

## Grundsatz
Diese AI soll lediglich das Designsystem im Theme-Paket konsolidieren und keine Layout-/UX-Umstellung außerhalb dieses Pakets vornehmen.

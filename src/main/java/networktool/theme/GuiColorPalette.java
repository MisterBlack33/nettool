package main.java.networktool.theme;

import java.awt.Color;

/**
 * Vertrag einer vollständigen Theme-Palette (Dark oder Light).
 * Kategorien: Background/Surface/Border/Text, Zeilenfarben, OS-/Gerätefarben.
 * Implementiert von {@link GuiThemeDark} und {@link GuiThemeLight}.
 */
interface GuiColorPalette {

    // Background / Surface
    Color bg();
    Color panelBg();
    Color sidebarBg();
    Color btnBg();
    Color btnHov();

    // Border
    Color border();
    Color borderLt();

    // Text
    Color fg();
    Color fgDim();

    // Zeilen (Tabellen)
    Color rowEven();
    Color rowOdd();
    Color rowSel();

    // OS-/Gerätefarben
    Color winCol();
    Color linCol();
    Color aplCol();
    Color iosCol();
    Color andCol();
    Color netCol();
    Color prnCol();
    Color iotCol();
    Color rpiCol();
}
package main.java.networktool.theme;

import java.awt.Color;

/** Hellmodus-Palette: kontrastreich, OS-Farben für hellen Grund abgedunkelt. */
final class GuiThemeLight implements GuiColorPalette {

    static final GuiThemeLight INSTANCE = new GuiThemeLight();

    private static final Color BACKGROUND   = new Color(0xF4, 0xF2, 0xEE);
    private static final Color SURFACE      = new Color(0xE8, 0xE6, 0xE0);
    private static final Color SIDEBAR      = new Color(0xEE, 0xEC, 0xE6);
    private static final Color BUTTON       = new Color(0xDC, 0xDA, 0xD4);
    private static final Color BUTTON_HOVER = new Color(0xCE, 0xCC, 0xC4);
    private static final Color BORDER       = new Color(0xC0, 0xBE, 0xB4);
    private static final Color BORDER_LIGHT = new Color(0xA8, 0xA4, 0x98);
    private static final Color TEXT_PRIMARY = new Color(0x1C, 0x1E, 0x1A);
    private static final Color TEXT_DIM     = new Color(0x52, 0x54, 0x4E);
    private static final Color ROW_EVEN     = new Color(0xF4, 0xF2, 0xEE);
    private static final Color ROW_ODD      = new Color(0xE4, 0xE2, 0xDA);
    private static final Color ROW_SELECTED = new Color(0x90, 0xBC, 0xF0);

    private static final Color WINDOWS   = new Color(0x18, 0x60, 0xB8);
    private static final Color LINUX     = new Color(0x18, 0x80, 0x28);
    private static final Color APPLE     = new Color(0x50, 0x50, 0x60);
    private static final Color IOS       = new Color(0x30, 0x58, 0xA0);
    private static final Color ANDROID   = new Color(0x18, 0x78, 0x30);
    private static final Color NETWORK   = new Color(0xC8, 0x6A, 0x10);
    private static final Color PRINTER   = new Color(0x90, 0x70, 0x10);
    private static final Color IOT       = new Color(0x8A, 0x78, 0x10);
    private static final Color RASPBERRY = new Color(0xB8, 0x30, 0x60);

    private GuiThemeLight() {}

    @Override public Color bg()        { return BACKGROUND; }
    @Override public Color panelBg()   { return SURFACE; }
    @Override public Color sidebarBg() { return SIDEBAR; }
    @Override public Color btnBg()     { return BUTTON; }
    @Override public Color btnHov()    { return BUTTON_HOVER; }
    @Override public Color border()    { return BORDER; }
    @Override public Color borderLt()  { return BORDER_LIGHT; }
    @Override public Color fg()        { return TEXT_PRIMARY; }
    @Override public Color fgDim()     { return TEXT_DIM; }
    @Override public Color rowEven()   { return ROW_EVEN; }
    @Override public Color rowOdd()    { return ROW_ODD; }
    @Override public Color rowSel()    { return ROW_SELECTED; }

    @Override public Color winCol() { return WINDOWS; }
    @Override public Color linCol() { return LINUX; }
    @Override public Color aplCol() { return APPLE; }
    @Override public Color iosCol() { return IOS; }
    @Override public Color andCol() { return ANDROID; }
    @Override public Color netCol() { return NETWORK; }
    @Override public Color prnCol() { return PRINTER; }
    @Override public Color iotCol() { return IOT; }
    @Override public Color rpiCol() { return RASPBERRY; }
}
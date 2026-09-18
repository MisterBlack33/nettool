package main.java.networktool.theme;

import java.awt.Color;

/** Hellmodus-Palette: klassisch helles UI. */
final class GuiThemeLight implements GuiColorPalette {

    static final GuiThemeLight INSTANCE = new GuiThemeLight();

    private static final Color BACKGROUND   = new Color(0xF3, 0xF1, 0xEC);
    private static final Color SURFACE      = new Color(0xF8, 0xF6, 0xF2);
    private static final Color SIDEBAR      = new Color(0xE8, 0xE4, 0xDC);
    private static final Color BUTTON       = new Color(0xE4, 0xE1, 0xD8);
    private static final Color BUTTON_HOVER = new Color(0xD2, 0xCE, 0xC8);
    private static final Color BORDER       = new Color(0xC8, 0xC2, 0xB8);
    private static final Color BORDER_LIGHT = new Color(0xD8, 0xD2, 0xC8);
    private static final Color TEXT_PRIMARY = new Color(0x1A, 0x1C, 0x1A);
    private static final Color TEXT_DIM     = new Color(0x70, 0x72, 0x6E);
    private static final Color ROW_EVEN     = new Color(0xF6, 0xF4, 0xF0);
    private static final Color ROW_ODD      = new Color(0xEE, 0xEA, 0xE4);
    private static final Color ROW_SELECTED = new Color(0xD6, 0xD0, 0xC2);

    private static final Color WINDOWS   = new Color(0x4D, 0x82, 0xC6);
    private static final Color LINUX     = new Color(0x3A, 0xA0, 0x5E);
    private static final Color APPLE     = new Color(0x8D, 0x8F, 0x9D);
    private static final Color IOS       = new Color(0x52, 0x7B, 0xC7);
    private static final Color ANDROID   = new Color(0x3A, 0x9C, 0x5D);
    private static final Color NETWORK   = new Color(0xE8, 0x9D, 0x2A);
    private static final Color PRINTER   = new Color(0xD4, 0x8A, 0x00);
    private static final Color IOT       = new Color(0xA5, 0x8D, 0x00);
    private static final Color RASPBERRY = new Color(0xB5, 0x3C, 0x78);

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

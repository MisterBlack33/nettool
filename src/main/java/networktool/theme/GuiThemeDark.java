package main.java.networktool.theme;

import java.awt.Color;

/** Dunkelmodus-Palette: kontrastarm, augenschonend. */
final class GuiThemeDark implements GuiColorPalette {

    static final GuiThemeDark INSTANCE = new GuiThemeDark();

    private static final Color BACKGROUND   = new Color(0x08, 0x0A, 0x09);
    private static final Color SURFACE      = new Color(0x0F, 0x12, 0x10);
    private static final Color SIDEBAR      = new Color(0x0B, 0x0E, 0x0C);
    private static final Color BUTTON       = new Color(0x18, 0x1C, 0x1A);
    private static final Color BUTTON_HOVER = new Color(0x24, 0x2C, 0x26);
    private static final Color BORDER       = new Color(0x22, 0x28, 0x24);
    private static final Color BORDER_LIGHT = new Color(0x32, 0x3C, 0x34);
    private static final Color TEXT_PRIMARY = new Color(0xEC, 0xE8, 0xDC);
    private static final Color TEXT_DIM     = new Color(0x62, 0x68, 0x62);
    private static final Color ROW_EVEN     = new Color(0x08, 0x0A, 0x09);
    private static final Color ROW_ODD      = new Color(0x0E, 0x12, 0x10);
    private static final Color ROW_SELECTED = new Color(0x2A, 0x32, 0x20);

    private static final Color WINDOWS   = new Color(0x60, 0xA8, 0xF0);
    private static final Color LINUX     = new Color(0x7E, 0xE8, 0x7E);
    private static final Color APPLE     = new Color(0xD0, 0xD0, 0xD8);
    private static final Color IOS       = new Color(0xA8, 0xC8, 0xF0);
    private static final Color ANDROID   = new Color(0x78, 0xD8, 0x78);
    private static final Color NETWORK   = new Color(0xFF, 0xA0, 0x30);
    private static final Color PRINTER   = new Color(0xE8, 0xC8, 0x40);
    private static final Color IOT       = new Color(0xF0, 0xE0, 0x60);
    private static final Color RASPBERRY = new Color(0xFF, 0x70, 0xA0);

    private GuiThemeDark() {}

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
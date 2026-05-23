package main.java.networktool_v3.gui;

import java.awt.*;

/**
 * Zentrale Farb- und Font-Definitionen mit Dark/Light Theme Toggle.
 *
 * Alle mutable statischen Aliase sind volatile für Thread-Sichtbarkeit.
 * Nach toggleTheme() muss applyToStatics() aufgerufen werden.
 */
public final class GuiTheme {

    private GuiTheme() {}

    private static volatile boolean darkMode = true;

    public static boolean isDark()    { return darkMode; }
    public static String  themeName() { return darkMode ? "☀  Hell" : "🌙  Dunkel"; }

    public static boolean toggleTheme() { darkMode = !darkMode; return darkMode; }

    // ── Dynamische Accessoren ─────────────────────────────────────────────

    public static Color bg()         { return darkMode ? D_BG        : L_BG; }
    public static Color panelBg()    { return darkMode ? D_PANEL_BG  : L_PANEL_BG; }
    public static Color sidebarBg()  { return darkMode ? D_SIDEBAR_BG: L_SIDEBAR_BG; }
    public static Color btnBg()      { return darkMode ? D_BTN_BG    : L_BTN_BG; }
    public static Color btnHov()     { return darkMode ? D_BTN_HOV   : L_BTN_HOV; }
    public static Color border()     { return darkMode ? D_BORDER     : L_BORDER; }
    public static Color borderLt()   { return darkMode ? D_BORDER_LT  : L_BORDER_LT; }
    public static Color fg()         { return darkMode ? D_FG         : L_FG; }
    public static Color fgDim()      { return darkMode ? D_FG_DIM     : L_FG_DIM; }
    public static Color rowEven()    { return darkMode ? D_ROW_EVEN   : L_ROW_EVEN; }
    public static Color rowOdd()     { return darkMode ? D_ROW_ODD    : L_ROW_ODD; }

    // ── DARK THEME ────────────────────────────────────────────────────────

    private static final Color D_BG          = new Color(0x08, 0x0A, 0x09);
    private static final Color D_PANEL_BG    = new Color(0x0F, 0x12, 0x10);
    private static final Color D_SIDEBAR_BG  = new Color(0x0B, 0x0E, 0x0C);
    private static final Color D_BTN_BG      = new Color(0x18, 0x1C, 0x1A);
    private static final Color D_BTN_HOV     = new Color(0x24, 0x2C, 0x26);
    private static final Color D_BORDER      = new Color(0x22, 0x28, 0x24);
    private static final Color D_BORDER_LT   = new Color(0x32, 0x3C, 0x34);
    private static final Color D_FG          = new Color(0xEC, 0xE8, 0xDC);
    private static final Color D_FG_DIM      = new Color(0x62, 0x68, 0x62);
    private static final Color D_ROW_EVEN    = new Color(0x08, 0x0A, 0x09);
    private static final Color D_ROW_ODD     = new Color(0x0E, 0x12, 0x10);

    // ── LIGHT THEME ───────────────────────────────────────────────────────

    private static final Color L_BG          = new Color(0xF4, 0xF2, 0xEE);
    private static final Color L_PANEL_BG    = new Color(0xE8, 0xE6, 0xE0);
    private static final Color L_SIDEBAR_BG  = new Color(0xEE, 0xEC, 0xE6);
    private static final Color L_BTN_BG      = new Color(0xDC, 0xDA, 0xD4);
    private static final Color L_BTN_HOV     = new Color(0xCE, 0xCC, 0xC4);
    private static final Color L_BORDER      = new Color(0xC0, 0xBE, 0xB4);
    private static final Color L_BORDER_LT   = new Color(0xA8, 0xA4, 0x98);
    private static final Color L_FG          = new Color(0x1C, 0x1E, 0x1A);
    private static final Color L_FG_DIM      = new Color(0x52, 0x54, 0x4E);
    private static final Color L_ROW_EVEN    = new Color(0xF4, 0xF2, 0xEE);
    private static final Color L_ROW_ODD     = new Color(0xE4, 0xE2, 0xDA);

    // ── Unveränderliche Farben ────────────────────────────────────────────

    public static final Color ACCENT  = new Color(0xD4, 0xA0, 0x20);
    public static final Color ACCENT2 = new Color(0x4C, 0xC2, 0x60);
    public static final Color WARN    = new Color(0xFF, 0x45, 0x35);
    public static final Color INFO    = new Color(0x72, 0xA8, 0xD8);

    // OS-Farben
    public static final Color WIN_COL = new Color(0x60, 0xA8, 0xF0);
    public static final Color LIN_COL = new Color(0x7E, 0xE8, 0x7E);
    public static final Color APL_COL = new Color(0xD0, 0xD0, 0xD8);
    public static final Color IOS_COL = new Color(0xA8, 0xC8, 0xF0);
    public static final Color AND_COL = new Color(0x78, 0xD8, 0x78);
    public static final Color NET_COL = new Color(0xFF, 0xA0, 0x30);
    public static final Color PRN_COL = new Color(0xE8, 0xC8, 0x40);
    public static final Color IOT_COL = new Color(0xF0, 0xE0, 0x60);
    public static final Color RPI_COL = new Color(0xFF, 0x70, 0xA0);

    // ── Volatile statische Aliase (für Legacy-Code) ───────────────────────
    // Werden via applyToStatics() nach jedem Theme-Wechsel aktualisiert.

    public static volatile Color BG         = D_BG;
    public static volatile Color PANEL_BG   = D_PANEL_BG;
    public static volatile Color SIDEBAR_BG = D_SIDEBAR_BG;
    public static volatile Color BTN_BG     = D_BTN_BG;
    public static volatile Color BTN_HOV    = D_BTN_HOV;
    public static volatile Color BORDER     = D_BORDER;
    public static volatile Color BORDER_LT  = D_BORDER_LT;
    public static volatile Color FG         = D_FG;
    public static volatile Color FG_DIM     = D_FG_DIM;
    public static volatile Color ROW_EVEN   = D_ROW_EVEN;
    public static volatile Color ROW_ODD    = D_ROW_ODD;
    public static volatile Color ROW_SEL    = new Color(0x2A, 0x32, 0x20);

    // Fonts
    public static final Font MONO    = new Font("JetBrains Mono", Font.PLAIN, 13);
    public static final Font MONO_S  = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font MONO_XS = new Font("JetBrains Mono", Font.PLAIN, 11);
    public static final Font BTN_F   = new Font("JetBrains Mono", Font.BOLD,  12);
    public static final Font BTN_F_S = new Font("JetBrains Mono", Font.BOLD,  11);

    public static void applyToStatics() {
        BG        = bg();
        PANEL_BG  = panelBg();
        SIDEBAR_BG = sidebarBg();
        BTN_BG    = btnBg();
        BTN_HOV   = btnHov();
        BORDER    = border();
        BORDER_LT = borderLt();
        FG        = fg();
        FG_DIM    = fgDim();
        ROW_EVEN  = rowEven();
        ROW_ODD   = rowOdd();
        ROW_SEL   = darkMode ? new Color(0x2A, 0x32, 0x20) : new Color(0x90, 0xBC, 0xF0);
    }

    public static Color osColor(String os) {
        if (os == null || os.isEmpty()) return FG;
        String l = os.toLowerCase();
        if (l.startsWith("windows"))                                       return WIN_COL;
        if (l.contains("macos")||l.contains("apple"))                     return APL_COL;
        if (l.contains("ios")||l.contains("ipad"))                        return IOS_COL;
        if (l.contains("raspberry"))                                       return RPI_COL;
        if (l.contains("android")||l.contains("samsung")||l.contains("xiaomi")
                ||l.contains("huawei")||l.contains("pixel")||l.contains("nothing")
                ||l.contains("oneplus")||l.contains("oppo")||l.contains("realme")
                ||l.contains("motorola")||l.contains("sony"))                      return AND_COL;
        if (l.contains("mobil"))                                           return AND_COL;
        if (l.contains("linux")||l.contains("unix"))                      return LIN_COL;
        if (l.contains("router")||l.contains("switch")||l.contains("netzwerkgerät")) return NET_COL;
        if (l.contains("drucker")||l.contains("printer"))                 return PRN_COL;
        if (l.contains("iot")||l.contains("mqtt"))                        return IOT_COL;
        if (l.contains("unbekannt"))                                       return FG_DIM;
        return FG;
    }

    public static Color brighter(Color c, float factor) {
        return new Color(
                Math.min(255, (int)(c.getRed()   * factor)),
                Math.min(255, (int)(c.getGreen() * factor)),
                Math.min(255, (int)(c.getBlue()  * factor)));
    }
}
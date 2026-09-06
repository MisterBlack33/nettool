package main.java.networktool.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Zentrale Farb-/Font-Definitionen mit Dark/Light Theme Toggle.
 * Die Palettenwerte liegen in {@link GuiThemeDark} / {@link GuiThemeLight}
 * (Vertrag: {@link GuiColorPalette}); diese Klasse bleibt die stabile
 * öffentliche API, damit keine andere Klasse angepasst werden muss.
 *
 * Nach toggleTheme() muss applyToStatics() aufgerufen werden.
 */
public final class GuiTheme {

    private GuiTheme() {}

    private static volatile boolean darkMode = true;

    public static boolean isDark()    { return darkMode; }
    public static String  themeName() { return darkMode ? "☀  Hell" : "🌙  Dunkel"; }
    public static boolean toggleTheme() { darkMode = !darkMode; return darkMode; }

    private static GuiColorPalette palette() {
        return darkMode ? GuiThemeDark.INSTANCE : GuiThemeLight.INSTANCE;
    }

    // ────────────────────────────────────── Dynamische Accessoren ──────────────────────────────────────

    public static Color bg()         { return palette().bg(); }
    public static Color panelBg()    { return palette().panelBg(); }
    public static Color sidebarBg()  { return palette().sidebarBg(); }
    public static Color btnBg()      { return palette().btnBg(); }
    public static Color btnHov()     { return palette().btnHov(); }
    public static Color border()     { return palette().border(); }
    public static Color borderLt()   { return palette().borderLt(); }
    public static Color fg()         { return palette().fg(); }
    public static Color fgDim()      { return palette().fgDim(); }
    public static Color rowEven()    { return palette().rowEven(); }
    public static Color rowOdd()     { return palette().rowOdd(); }

    // ────────────────────────────────────── Unveränderliche Akzentfarben ──────────────────────────────────────

    public static final Color ACCENT  = new Color(0xD4, 0xA0, 0x20);
    public static final Color ACCENT2 = new Color(0x4C, 0xC2, 0x60);
    public static final Color WARN    = new Color(0xFF, 0x45, 0x35);
    public static final Color INFO    = new Color(0x72, 0xA8, 0xD8);

    // ────────────────────────────────────── OS-/Kategorie-Farben (modusabhängig) ──────────────────────────────────────

    public static volatile Color WIN_COL, LIN_COL, APL_COL, IOS_COL, AND_COL, NET_COL, PRN_COL, IOT_COL, RPI_COL;

    // ────────────────────────────────────── Volatile statische Aliase (für Legacy-Code) ──────────────────────────────────────

    public static volatile Color BG, PANEL_BG, SIDEBAR_BG, BTN_BG, BTN_HOV, BORDER, BORDER_LT,
            FG, FG_DIM, ROW_EVEN, ROW_ODD, ROW_SEL;

    // Fonts
    public static final Font MONO    = new Font("JetBrains Mono", Font.PLAIN, 13);
    public static final Font MONO_S  = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font MONO_XS = new Font("JetBrains Mono", Font.PLAIN, 11);
    public static final Font BTN_F   = new Font("JetBrains Mono", Font.BOLD,  12);
    public static final Font BTN_F_S = new Font("JetBrains Mono", Font.BOLD,  11);

    static { applyToStatics(); }

    public static void applyToStatics() {
        GuiColorPalette p = palette();

        BG         = p.bg();
        PANEL_BG   = p.panelBg();
        SIDEBAR_BG = p.sidebarBg();
        BTN_BG     = p.btnBg();
        BTN_HOV    = p.btnHov();
        BORDER     = p.border();
        BORDER_LT  = p.borderLt();
        FG         = p.fg();
        FG_DIM     = p.fgDim();
        ROW_EVEN   = p.rowEven();
        ROW_ODD    = p.rowOdd();
        ROW_SEL    = p.rowSel();

        WIN_COL = p.winCol(); LIN_COL = p.linCol(); APL_COL = p.aplCol();
        IOS_COL = p.iosCol(); AND_COL = p.andCol(); NET_COL = p.netCol();
        PRN_COL = p.prnCol(); IOT_COL = p.iotCol(); RPI_COL = p.rpiCol();
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
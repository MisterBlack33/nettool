package main.java.networktool.theme;

import java.awt.Color;
import java.awt.Font;

/** Zentrale Farb-/Font-Definitionen. Das aktive Theme kann noch umgeschaltet werden, aber die GUI-Wiring wurde entfernt. */
public final class GuiTheme {

    private GuiTheme() {}

    private static volatile GuiColorPalette palette = GuiThemeDark.INSTANCE;

    public static volatile Color BG;
    public static volatile Color PANEL_BG;
    public static volatile Color SIDEBAR_BG;
    public static volatile Color BTN_BG;
    public static volatile Color BTN_HOV;
    public static volatile Color BORDER;
    public static volatile Color BORDER_LT;
    public static volatile Color FG;
    public static volatile Color FG_DIM;
    public static volatile Color ROW_EVEN;
    public static volatile Color ROW_ODD;
    public static volatile Color ROW_SEL;

    public static volatile Color ACCENT  = new Color(0xD4, 0xA0, 0x20);
    public static volatile Color ACCENT2 = new Color(0x4C, 0xC2, 0x60);
    public static volatile Color WARN    = new Color(0xFF, 0x45, 0x35);
    public static volatile Color INFO    = new Color(0x72, 0xA8, 0xD8);

    public static volatile Color WIN_COL;
    public static volatile Color LIN_COL;
    public static volatile Color APL_COL;
    public static volatile Color IOS_COL;
    public static volatile Color AND_COL;
    public static volatile Color NET_COL;
    public static volatile Color PRN_COL;
    public static volatile Color IOT_COL;
    public static volatile Color RPI_COL;

    static {
        applyToStatics();
    }

    public static Color bg()         { return palette.bg(); }
    public static Color panelBg()    { return palette.panelBg(); }
    public static Color sidebarBg()  { return palette.sidebarBg(); }
    public static Color btnBg()      { return palette.btnBg(); }
    public static Color btnHov()     { return palette.btnHov(); }
    public static Color border()     { return palette.border(); }
    public static Color borderLt()   { return palette.borderLt(); }
    public static Color fg()         { return palette.fg(); }
    public static Color fgDim()      { return palette.fgDim(); }
    public static Color rowEven()    { return palette.rowEven(); }
    public static Color rowOdd()     { return palette.rowOdd(); }
    public static Color rowSel()     { return palette.rowSel(); }

    public static boolean isDark() {
        return palette == GuiThemeDark.INSTANCE;
    }

    public static void toggleTheme() {
        palette = (palette == GuiThemeDark.INSTANCE) ? GuiThemeLight.INSTANCE : GuiThemeDark.INSTANCE;
        applyToStatics();
    }

    public static void applyToStatics() {
        BG = palette.bg();
        PANEL_BG = palette.panelBg();
        SIDEBAR_BG = palette.sidebarBg();
        BTN_BG = palette.btnBg();
        BTN_HOV = palette.btnHov();
        BORDER = palette.border();
        BORDER_LT = palette.borderLt();
        FG = palette.fg();
        FG_DIM = palette.fgDim();
        ROW_EVEN = palette.rowEven();
        ROW_ODD = palette.rowOdd();
        ROW_SEL = palette.rowSel();

        WIN_COL = palette.winCol();
        LIN_COL = palette.linCol();
        APL_COL = palette.aplCol();
        IOS_COL = palette.iosCol();
        AND_COL = palette.andCol();
        NET_COL = palette.netCol();
        PRN_COL = palette.prnCol();
        IOT_COL = palette.iotCol();
        RPI_COL = palette.rpiCol();
    }

    public static String themeName() {
        return isDark() ? "Dunkel" : "Hell";
    }

    public static final Font MONO    = new Font("JetBrains Mono", Font.PLAIN, 13);
    public static final Font MONO_S  = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font MONO_XS = new Font("JetBrains Mono", Font.PLAIN, 11);
    public static final Font BTN_F   = new Font("JetBrains Mono", Font.BOLD,  12);
    public static final Font BTN_F_S = new Font("JetBrains Mono", Font.BOLD,  11);

    public static Color osColor(String os) {
        if (os == null || os.isEmpty()) return FG;
        String l = os.toLowerCase();
        if (l.startsWith("windows")) return WIN_COL;
        if (l.contains("macos") || l.contains("apple")) return APL_COL;
        if (l.contains("ios") || l.contains("ipad")) return IOS_COL;
        if (l.contains("raspberry")) return RPI_COL;
        if (l.contains("android") || l.contains("samsung") || l.contains("xiaomi")
                || l.contains("huawei") || l.contains("pixel") || l.contains("nothing")
                || l.contains("oneplus") || l.contains("oppo") || l.contains("realme")
                || l.contains("motorola") || l.contains("sony") || l.contains("mobil")) return AND_COL;
        if (l.contains("linux") || l.contains("unix")) return LIN_COL;
        if (l.contains("router") || l.contains("switch") || l.contains("netzwerkgerät")) return NET_COL;
        if (l.contains("drucker") || l.contains("printer")) return PRN_COL;
        if (l.contains("iot") || l.contains("mqtt")) return IOT_COL;
        if (l.contains("unbekannt")) return FG_DIM;
        return FG;
    }

    public static Color brighter(Color c, float factor) {
        return new Color(
                Math.min(255, (int) (c.getRed() * factor)),
                Math.min(255, (int) (c.getGreen() * factor)),
                Math.min(255, (int) (c.getBlue() * factor)));
    }
}
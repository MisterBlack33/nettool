package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static networktool.theme.GuiTheme.*;

/**
 * Statusleiste am unteren Fensterrand.
 * Rechts: "NetTool v3" in WeiÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â¸ statt "MenÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼punkt wÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤hlen".
 */
public class GuiStatusBar {

    private final JLabel statusLabel;

    public GuiStatusBar() {
        statusLabel = new JLabel("Bereit");
        statusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        statusLabel.setForeground(FG_DIM);
    }

    public JPanel buildPanel() {
        boolean dark = GuiTheme.isDark();
        Color barBg  = dark ? new Color(0x05, 0x07, 0x06) : new Color(0xD8, 0xD6, 0xCE);
        Color brandFg= dark ? new Color(0xCC, 0xC8, 0xBC) : new Color(0x20, 0x22, 0x1E);
        statusLabel.setForeground(dark ? FG_DIM : new Color(0x48, 0x4C, 0x46));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(barBg);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(4, 14, 4, 14)
        ));
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel brand = new JLabel("NetTool v3");
        brand.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        brand.setForeground(brandFg);
        bar.add(brand, BorderLayout.EAST);
        return bar;
    }

    public JLabel getLabel() { return statusLabel; }

    public void set(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(color);
        });
    }
}

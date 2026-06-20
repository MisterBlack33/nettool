package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.LoginFormBuilder.*;

/**
 * GridBag-Layout-Hilfsmethoden für Login- und Registrierungsformulare.
 */
final class LoginLayoutHelper {

    private LoginLayoutHelper() {}

    static GridBagConstraints defaultConstraints() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        return gc;
    }

    static void addFormRow(JPanel form, GridBagConstraints gc,
                           int row, String label, Component field) {
        gc.gridwidth = 1;
        gc.gridx     = 0;
        gc.gridy     = row;
        gc.weightx   = 0;
        gc.fill      = GridBagConstraints.NONE;
        gc.insets    = new Insets(row == 0 ? 0 : 14, 0, 0, 14);
        form.add(fieldLabel(label), gc);

        gc.gridx   = 1;
        gc.weightx = 1;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.insets  = new Insets(row == 0 ? 0 : 14, 0, 0, 0);
        form.add(field, gc);
    }

    static void addSpanRow(JPanel form, GridBagConstraints gc, int row, Component comp) {
        gc.gridx     = 0;
        gc.gridy     = row;
        gc.gridwidth = 2;
        gc.insets    = new Insets(8, 0, 0, 0);
        form.add(comp, gc);
        gc.gridwidth = 1;
    }

    static JPanel formPanel(Color bg) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(28, 40, 12, 40));
        return panel;
    }
}
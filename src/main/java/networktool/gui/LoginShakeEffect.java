package main.java.networktool.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Visueller "Shake"-Effekt für Fehlermeldungen im Login-Dialog.
 * Bewegt das Eltern-Fenster kurz horizontal hin und her.
 */
final class LoginShakeEffect {

    private static final int[] OFFSETS = {-7, 7, -5, 5, -3, 3, -1, 1, 0};
    private static final int   STEP_MS = 28;

    private LoginShakeEffect() {}

    static void shake(JLabel errorLabel, String message) {
        errorLabel.setText(message);
        Window window = SwingUtilities.getWindowAncestor(errorLabel);
        if (window == null) return;

        Point origin = window.getLocation();
        int[] index  = {0};

        Timer timer = new Timer(STEP_MS, null);
        timer.addActionListener(e -> {
            if (index[0] >= OFFSETS.length) {
                timer.stop();
                window.setLocation(origin);
                return;
            }
            window.setLocation(origin.x + OFFSETS[index[0]++], origin.y);
        });
        timer.start();
    }
}
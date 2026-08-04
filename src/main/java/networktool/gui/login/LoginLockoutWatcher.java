package networktool.gui.login;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import javax.swing.*;

import static networktool.theme.GuiTheme.WARN;

/**
 * Überwacht {@link GuiLoginRateLimiter} und sperrt den Login-Button
 * während einer aktiven Sperre, mit Sekunden-Countdown im Fehlerlabel.
 */
final class LoginLockoutWatcher {

    private static final int TICK_MS = 1000;

    private LoginLockoutWatcher() {}

    /** Prüft beim Öffnen des Bildschirms sofort, ob noch eine Sperre aktiv ist. */
    static void attach(JButton loginBtn, JLabel errLabel) {
        if (GuiLoginRateLimiter.isLocked()) {
            startCountdown(loginBtn, errLabel);
        }
    }

    /** Startet den Countdown nach einem fehlgeschlagenen Login-Versuch. */
    static void startCountdown(JButton loginBtn, JLabel errLabel) {
        loginBtn.setEnabled(false);
        updateLabel(errLabel);

        Timer timer = new Timer(TICK_MS, null);
        timer.addActionListener(e -> {
            if (!GuiLoginRateLimiter.isLocked()) {
                timer.stop();
                loginBtn.setEnabled(true);
                errLabel.setText(" ");
            } else {
                updateLabel(errLabel);
            }
        });
        timer.start();
    }

    private static void updateLabel(JLabel errLabel) {
        errLabel.setForeground(WARN);
        errLabel.setText("Gesperrt – noch " + GuiLoginRateLimiter.remainingSeconds() + "s");
    }
}
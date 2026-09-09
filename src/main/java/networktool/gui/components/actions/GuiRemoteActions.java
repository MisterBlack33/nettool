package main.java.networktool.gui.components.actions;

import main.java.networktool.filter.ClipboardUtil;
import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.security.AuditLogger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Öffnet Hosts im Browser oder simuliert Remote-Zugriff.
 * Dialog-Aufbau siehe {@link RemoteDeviceDialogs}.
 */
public final class GuiRemoteActions {

    private static final Logger LOG = Logger.getLogger(GuiRemoteActions.class.getName());

    private GuiRemoteActions() {}

    public static void openInBrowser(String ip, String osFromTable) {
        new Thread(() -> {
            AuditLogger.getInstance().log("BROWSER_OPEN", ip);
            String os = (osFromTable != null && !osFromTable.isBlank())
                    ? osFromTable : detectOsSafe(ip);
            openForHost(ip, os);
        }, "BrowserOpen-" + ip).start();
    }

    private static void openForHost(String ip, String os) {
        WebPort port = detectWebPort(ip);
        if (port != null) {
            browseUrl(port.toUrl(ip));
            return;
        }
        if (isMobileOs(os))       SwingUtilities.invokeLater(() -> RemoteDeviceDialogs.showPhone(ip, os));
        else if (!isServerOs(os)) SwingUtilities.invokeLater(() -> RemoteDeviceDialogs.showDesktop(ip, os));
        else                      browseUrl("http://" + ip);
    }

    /** Erreichbarer Web-Port eines Hosts (https vor http, Standardport vor Alt-Port). */
    private record WebPort(String proto, int port) {
        String toUrl(String ip) {
            return (port == 80 || port == 443) ? proto + "://" + ip : proto + "://" + ip + ":" + port;
        }
    }

    private static WebPort detectWebPort(String ip) {
        if (isPortOpen(ip, 443))  return new WebPort("https", 443);
        if (isPortOpen(ip, 8443)) return new WebPort("https", 8443);
        if (isPortOpen(ip, 80))   return new WebPort("http", 80);
        if (isPortOpen(ip, 8080)) return new WebPort("http", 8080);
        return null;
    }

    static boolean isPortOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), 600);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean isMobileOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return l.contains("android")||l.contains("ios")||l.contains("ipad")
                ||l.contains("samsung")||l.contains("xiaomi")||l.contains("huawei")
                ||l.contains("pixel")||l.contains("nothing")||l.contains("oneplus")
                ||l.contains("oppo")||l.contains("realme")||l.contains("motorola")
                ||l.contains("sony")||l.contains("mobil");
    }

    static boolean isServerOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return (l.contains("linux")&&l.contains("server"))
                ||l.contains("datenbankserver")||l.contains("mail-server")
                ||l.contains("dns-server")||l.contains("ftp-server");
    }

    private static String detectOsSafe(String ip) {
        try {
            return OsDetector.detect(ip);
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "OS-Erkennung für " + ip + " fehlgeschlagen", e);
            return "";
        }
    }

    static void browseUrl(String url) {
        try {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) d.browse(new URI(url));
            else ClipboardUtil.copy(url);
        } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
            LOG.log(Level.FINE, "Browser konnte nicht geöffnet werden: " + url, e);
            ClipboardUtil.copy(url);
        }
    }
}

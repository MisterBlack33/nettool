package main.java.networktool.logic.sonify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Liest Byte-Zähler eines Netzwerk-Interfaces aus dem Linux-sysfs.
 * Auf anderen Plattformen oder bei unbekannten Interfaces: {@code null}.
 */
public final class InterfaceStatsReader {

    private static final Logger LOG = Logger.getLogger(InterfaceStatsReader.class.getName());

    private InterfaceStatsReader() {}

    /** @return [rxBytes, txBytes] oder {@code null} wenn nicht lesbar. */
    public static long[] read(String iface) {
        try {
            Path rx = Path.of("/sys/class/net/" + iface + "/statistics/rx_bytes");
            Path tx = Path.of("/sys/class/net/" + iface + "/statistics/tx_bytes");
            if (!Files.exists(rx) || !Files.exists(tx)) return null;
            long r = Long.parseLong(Files.readString(rx).trim());
            long t = Long.parseLong(Files.readString(tx).trim());
            return new long[]{r, t};
        } catch (Exception e) {
            LOG.log(Level.FINE, "Interface-Statistik für \"" + iface + "\" nicht lesbar", e);
            return null;
        }
    }
}

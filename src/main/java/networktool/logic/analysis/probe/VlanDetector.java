package main.java.networktool.logic.analysis.probe;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort VLAN-Erkennung über Interface-Namenskonventionen
 * (z.B. "eth0.100", "vlan100"). Kein VLAN-Tag-Zugriff auf Layer-2-Ebene möglich
 * ohne native Bibliotheken — reine Introspektion von {@link NetworkInterface}.
 */
public final class VlanDetector {

    private static final Pattern DOT_SUFFIX = Pattern.compile(".*\\.(\\d{1,4})$");
    private static final Pattern VLAN_NAME  = Pattern.compile("^vlan\\.?(\\d{1,4})$", Pattern.CASE_INSENSITIVE);

    private VlanDetector() {}

    public static Optional<Integer> detect(String interfaceName) {
        if (interfaceName == null || interfaceName.isBlank()) return Optional.empty();
        Optional<Integer> byDot = match(DOT_SUFFIX, interfaceName);
        return byDot.isPresent() ? byDot : match(VLAN_NAME, interfaceName);
    }

    /** Erste erkannte VLAN-ID über alle Netzwerk-Interfaces des Systems, sonst leer. */
    public static Optional<Integer> detectForActiveInterfaces() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                Optional<Integer> vlan = detect(ni.getName());
                if (vlan.isPresent()) return vlan;
            }
        } catch (SocketException ignored) {
            // best effort – keine Interfaces lesbar
        }
        return Optional.empty();
    }

    private static Optional<Integer> match(Pattern pattern, String name) {
        Matcher m = pattern.matcher(name);
        if (!m.matches()) return Optional.empty();
        try {
            int vlan = Integer.parseInt(m.group(1));
            return (vlan >= 1 && vlan <= 4094) ? Optional.of(vlan) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

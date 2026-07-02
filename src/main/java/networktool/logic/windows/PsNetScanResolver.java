package main.java.networktool.logic.windows;

import java.util.*;
import java.util.regex.Pattern;

/** Host-Sweep via PowerShell (parallel Test-Connection). Windows-only, sonst leere Liste. */
public final class PsNetScanResolver {

    private static final int MAX_PARALLEL = 64;
    private static final Pattern IP = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    private PsNetScanResolver() {}

    /** Scannt IPs prefix.1 bis prefix.254 auf ICMP-Erreichbarkeit. */
    public static List<String> sweep(String subnetPrefix) {
        if (!PowerShellRunner.isAvailable() || subnetPrefix == null || subnetPrefix.isBlank())
            return List.of();

        List<String> alive = new ArrayList<>();
        for (String line : PowerShellRunner.run(buildScript(subnetPrefix), 15_000)) {
            String ip = line.trim();
            if (IP.matcher(ip).matches()) alive.add(ip);
        }
        return alive;
    }

    private static String buildScript(String prefix) {
        return "1..254 | ForEach-Object -ThrottleLimit " + MAX_PARALLEL + " -Parallel { "
                + "$ip = '" + prefix + ".' + $_; "
                + "if (Test-Connection -TargetName $ip -Count 1 -Quiet -TimeoutSeconds 1) { $ip } "
                + "}";
    }
}
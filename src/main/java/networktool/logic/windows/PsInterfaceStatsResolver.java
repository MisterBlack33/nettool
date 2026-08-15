package main.java.networktool.logic.windows;

import java.util.regex.*;

public final class PsInterfaceStatsResolver {

    private static final Pattern LINE = Pattern.compile(
            "^\"?([^\",]+)\"?,\"?(\\d+)\"?,\"?(\\d+)\"?");

    private PsInterfaceStatsResolver() {}

    /** @return [rxBytes, txBytes] oder null wenn Adapter unbekannt/PowerShell fehlt. */
    public static long[] read(String adapterName) {
        if (!PowerShellRunner.isAvailable() || adapterName == null) return null;
        String script =
                "Get-NetAdapterStatistics -Name '" + adapterName.replace("'", "''") + "' | " +
                        "Select-Object ReceivedBytes,SentBytes | ConvertTo-Csv -NoTypeInformation";
        for (String line : PowerShellRunner.run(script, 3000)) {
            Matcher m = Pattern.compile("^\"?(\\d+)\"?,\"?(\\d+)\"?").matcher(line.trim());
            if (m.find()) {
                try {
                    return new long[]{Long.parseLong(m.group(1)), Long.parseLong(m.group(2))};
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /** Listet alle aktiven Adapternamen (LAN + WLAN). */
    public static java.util.List<String> listActiveAdapters() {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (!PowerShellRunner.isAvailable()) return names;
        String script = "Get-NetAdapter | Where-Object Status -eq 'Up' | Select-Object -ExpandProperty Name";
        for (String line : PowerShellRunner.run(script, 3000)) {
            String n = line.trim();
            if (!n.isBlank()) names.add(n);
        }
        return names;
    }
}
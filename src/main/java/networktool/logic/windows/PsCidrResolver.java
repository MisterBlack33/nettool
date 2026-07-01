package main.java.networktool.logic.windows;

import main.java.networktool.util.CIDRUtils;

import java.util.*;
import java.util.regex.*;

/** CIDR-Erkennung via "Get-NetIPAddress" – erfasst auch VPN-/virtuelle Adapter zuverlässig. */
public final class PsCidrResolver {

    private static final Pattern LINE = Pattern.compile(
            "^\"?(\\d{1,3}(?:\\.\\d{1,3}){3})\"?,\"?(\\d{1,2})\"?");

    private PsCidrResolver() {}

    public static List<String> resolveCidrs() {
        List<String> result = new ArrayList<>();
        String script =
                "Get-NetIPAddress -AddressFamily IPv4 | " +
                        "Where-Object { $_.PrefixOrigin -ne 'WellKnown' -and $_.IPAddress -notlike '169.254.*' } | " +
                        "Select-Object IPAddress,PrefixLength | ConvertTo-Csv -NoTypeInformation";
        for (String line : PowerShellRunner.run(script)) {
            Matcher m = LINE.matcher(line.trim());
            if (!m.find()) continue;
            int prefix = Integer.parseInt(m.group(2));
            String ip  = m.group(1);
            if (prefix < 16 || prefix > 30 || ip.startsWith("127.")) continue;
            result.add(normalize(ip, prefix));
        }
        return result;
    }

    private static String normalize(String ip, int prefix) {
        int ipInt   = CIDRUtils.ipToInt(ip);
        int mask    = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        int network = ipInt & mask;
        return CIDRUtils.intToIp(network) + "/" + prefix;
    }
}
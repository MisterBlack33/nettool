package networktool.filter;

import networktool.model.ScanResult;
import networktool.storage.JsonHelper;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringJoiner;

public final class JsonExporter {

    private JsonExporter() {}

    public static void save(List<ScanResult> results, String filePath) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(toJson(results));
            System.out.println("Ergebnisse gespeichert: " + filePath);
        } catch (IOException e) {
            System.err.println("JsonExporter: " + e.getMessage());
        }
    }

    private static String toJson(List<ScanResult> results) {
        StringJoiner entries = new StringJoiner(",\n", "[\n", "\n]");
        for (ScanResult r : results) entries.add(toJsonObject(r));
        return entries.toString();
    }

    private static String toJsonObject(ScanResult r) {
        return "  {\n"
                + "    \"ip\": \""       + JsonHelper.esc(r.getIp())      + "\",\n"
                + "    \"hostname\": \"" + JsonHelper.esc(r.getHostname()) + "\",\n"
                + "    \"os\": \""       + JsonHelper.esc(r.getOsGuess())  + "\",\n"
                + "    \"ports\": "      + r.getOpenPorts().keySet()       + "\n"
                + "  }";
    }
}
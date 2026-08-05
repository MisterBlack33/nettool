package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Export gespeicherter Hosts als CSV / JSON / HTML.
 * Ausgelagert aus {@link DataExporter} (nur Split, keine Logik-Änderung).
 * Package-private — öffentliche Fassade bleibt DataExporter.
 */
final class DataExportFormatters {

    private DataExportFormatters() {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    static String now() { return LocalDateTime.now().format(FMT); }

    @FunctionalInterface
    interface HostConsumer { void accept(HostResult h, String category); }

    static void forEachHost(HostConsumer consumer) {
        NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .forEach(cat -> NetworkStore.getInstance().getAll(cat)
                        .forEach(h -> consumer.accept(h, cat)));
    }

    static String csv(String s) {
        if (s == null) return "";
        s = s.replace(";", ",").replace("\n", " ").replace("\r", "");
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    static Path exportCsv(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_export_" + now() + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("IP;Hostname;OS;Datum;Ports;Notiz;Kategorie");
        forEachHost((h, cat) -> lines.add(
                csv(h.ip) + ";" + csv(h.hostname) + ";" + csv(h.os) + ";"
                        + csv(h.savedAt) + ";" + csv(h.portsToString()) + ";"
                        + csv(h.notes) + ";" + csv(cat)));
        Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    static Path exportJson(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_export_" + now() + ".json");
        StringBuilder sb = new StringBuilder("[\n");
        boolean[] first = {true};
        forEachHost((h, cat) -> {
            if (!first[0]) sb.append(",\n");
            sb.append("  {")
                    .append("\"ip\":\"").append(esc(h.ip)).append("\",")
                    .append("\"hostname\":\"").append(esc(h.hostname)).append("\",")
                    .append("\"os\":\"").append(esc(h.os)).append("\",")
                    .append("\"savedAt\":\"").append(esc(h.savedAt)).append("\",")
                    .append("\"ports\":\"").append(esc(h.portsToString())).append("\",")
                    .append("\"notes\":\"").append(esc(h.notes)).append("\",")
                    .append("\"category\":\"").append(esc(cat)).append("\"")
                    .append("}");
            first[0] = false;
        });
        sb.append("\n]");
        Files.writeString(file, sb.toString());
        return file;
    }

    static Path exportHtml(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_report_" + now() + ".html");
        Files.writeString(file, HtmlReportBuilder.build());
        return file;
    }
}

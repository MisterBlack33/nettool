package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

/**
 * Export gespeicherter Hosts in CSV / JSON / HTML / ZIP.
 */
public final class DataExporter {

    private DataExporter() {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static Path exportCsv(Path outDir) throws IOException {
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

    public static Path exportJson(Path outDir) throws IOException {
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

    public static Path exportHtml(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_report_" + now() + ".html");
        Files.writeString(file, HtmlReportBuilder.build());
        return file;
    }

    // DataExporter.java — exportBackup fix: try-with-resources schützt gegen Hänger
    public static Path exportBackup(Path outDir) throws IOException {
        return exportBackup(outDir, NetworkStorePersistence.resolveTxtDir());
    }

    public static Path exportBackup(Path outDir, Path srcDir) throws IOException {
        Files.createDirectories(outDir);
        Path zipFile = outDir.resolve("nettool_backup_" + now() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(zipFile.toFile()))) {
            if (Files.isDirectory(srcDir)) {
                try (var stream = Files.walk(srcDir)) {
                    stream.filter(Files::isRegularFile).forEach(p -> {
                        try {
                            zos.putNextEntry(new ZipEntry(
                                    srcDir.relativize(p).toString().replace('\\', '/')));
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException ignored) {}
                    });
                }
            }
        }
        return zipFile;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

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
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String now() { return LocalDateTime.now().format(FMT); }
}
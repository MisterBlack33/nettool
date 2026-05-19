package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class DataExporter {

    private DataExporter() {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static Path exportCsv(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_export_" + now() + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("IP;Hostname;OS;Datum;Ports;Notiz;Kategorie");
        eachHost((cat, h) -> lines.add(
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
        eachHost((cat, h) -> {
            if (!first[0]) sb.append(",\n");
            sb.append("  {\n")
                    .append("    \"ip\": \"").append(esc(h.ip)).append("\",\n")
                    .append("    \"hostname\": \"").append(esc(h.hostname)).append("\",\n")
                    .append("    \"os\": \"").append(esc(h.os)).append("\",\n")
                    .append("    \"savedAt\": \"").append(esc(h.savedAt)).append("\",\n")
                    .append("    \"ports\": \"").append(esc(h.portsToString())).append("\",\n")
                    .append("    \"notes\": \"").append(esc(h.notes)).append("\",\n")
                    .append("    \"category\": \"").append(esc(cat)).append("\"\n")
                    .append("  }");
            first[0] = false;
        });
        sb.append("\n]");
        Files.writeString(file, sb.toString());
        return file;
    }

    public static Path exportHtml(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_report_" + now() + ".html");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n<title>NetTool v3 - Host-Report</title>\n<style>\n")
                .append("body{font-family:monospace;background:#0D0F0F;color:#E8E4D8;margin:0;padding:20px}\n")
                .append("h1{color:#D4A020;border-bottom:1px solid #282E2A;padding-bottom:8px}\n")
                .append("h2{color:#8CAA80;margin-top:30px}\n")
                .append("table{border-collapse:collapse;width:100%;margin-bottom:20px}\n")
                .append("th{background:#0A1608;color:#D4A020;padding:8px 12px;text-align:left;border-bottom:1px solid #D4A020}\n")
                .append("td{padding:6px 12px;border-bottom:1px solid #282E2A;font-size:12px}\n")
                .append("tr:nth-child(even){background:#0E1210} tr:nth-child(odd){background:#0D0F0D}\n")
                .append("tr:hover{background:#1A2018}\n")
                .append(".win{color:#60A8F0}.lin{color:#7EE87E}.mac{color:#D0D0D8}\n")
                .append(".and{color:#78D878}.net{color:#FFA030}.info{color:#A0C0A0;font-size:11px}\n")
                .append("</style>\n</head>\n<body>\n")
                .append("<h1>// NetTool v3  –  Host-Report</h1>\n")
                .append("<p class=\"info\">Erstellt: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                .append("</p>\n<p class=\"info\">Gesamt: <b>")
                .append(NetworkStore.getInstance().getAllHosts().size()).append("</b> Hosts</p>\n");

        NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .forEach(cat -> {
                    List<HostResult> hosts = NetworkStore.getInstance().getAll(cat);
                    html.append("<h2>").append(he(cat)).append("  (").append(hosts.size()).append(" Hosts)</h2>\n")
                            .append("<table><tr><th>IP</th><th>Hostname</th><th>OS/Gerät</th>")
                            .append("<th>Ports</th><th>Gespeichert</th><th>Notiz</th></tr>\n");
                    hosts.forEach(h -> html.append("<tr>")
                            .append("<td>").append(he(h.ip)).append("</td>")
                            .append("<td>").append(he(h.hostname)).append("</td>")
                            .append("<td class=\"").append(osClass(h.os)).append("\">").append(he(h.os)).append("</td>")
                            .append("<td>").append(he(h.portsToString())).append("</td>")
                            .append("<td>").append(he(h.savedAt)).append("</td>")
                            .append("<td>").append(he(h.notes)).append("</td></tr>\n"));
                    html.append("</table>\n");
                });
        html.append("</body></html>");
        Files.writeString(file, html.toString());
        return file;
    }

    public static Path exportBackup(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path zipFile = outDir.resolve("nettool_backup_" + now() + ".zip");
        Path srcDir  = NetworkStorePersistence.resolveTxtDir();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new FileOutputStream(zipFile.toFile()))) {
            Files.walk(srcDir).filter(Files::isRegularFile).forEach(p -> {
                try {
                    zos.putNextEntry(new java.util.zip.ZipEntry(
                            srcDir.relativize(p).toString().replace('\\', '/')));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException ignored) {}
            });
        }
        return zipFile;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @FunctionalInterface
    interface HostConsumer { void accept(String cat, HostResult h); }

    static void eachHost(HostConsumer c) {
        NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .forEach(cat -> NetworkStore.getInstance().getAll(cat).forEach(h -> c.accept(cat, h)));
    }

    private static String now() { return LocalDateTime.now().format(FMT); }

    static String csv(String s) {
        if (s == null) return "";
        s = s.replace(";", ",").replace("\n", " ").replace("\r", "");
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    static String he(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    static String osClass(String os) {
        if (os == null) return "";
        String l = os.toLowerCase();
        if (l.contains("windows")) return "win";
        if (l.contains("linux")||l.contains("unix")) return "lin";
        if (l.contains("mac")||l.contains("apple")) return "mac";
        if (l.contains("android")) return "and";
        if (l.contains("router")||l.contains("switch")) return "net";
        return "";
    }
}
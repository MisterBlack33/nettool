package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Export & Import für gespeicherte Hosts und Scan-Daten.
 *
 * Formate:
 *   CSV   – IP;Hostname;OS;Datum;Ports;Notiz;Kategorie
 *   JSON  – Array von Host-Objekten
 *   HTML  – Report mit farbigen Tabellen
 *   ZIP   – vollständiges Backup von savedHostsTags/
 */
public final class DataExportImport {

    private DataExportImport() {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // ── CSV Export ────────────────────────────────────────────────────────

    public static Path exportCsv(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_export_" + LocalDateTime.now().format(FMT) + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("IP;Hostname;OS;Datum;Ports;Notiz;Kategorie");
        NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .forEach(cat -> NetworkStore.getInstance().getAll(cat).forEach(h ->
                        lines.add(csv(h.ip) + ";" + csv(h.hostname) + ";" + csv(h.os) + ";"
                                + csv(h.savedAt) + ";" + csv(h.portsToString()) + ";"
                                + csv(h.notes) + ";" + csv(cat))));
        Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    // ── JSON Export ───────────────────────────────────────────────────────

    public static Path exportJson(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_export_" + LocalDateTime.now().format(FMT) + ".json");
        StringBuilder sb = new StringBuilder("[\n");
        List<String> cats = NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY)).collect(Collectors.toList());
        boolean first = true;
        for (String cat : cats) {
            for (HostResult h : NetworkStore.getInstance().getAll(cat)) {
                if (!first) sb.append(",\n");
                sb.append("  {\n")
                        .append("    \"ip\": \"").append(esc(h.ip)).append("\",\n")
                        .append("    \"hostname\": \"").append(esc(h.hostname)).append("\",\n")
                        .append("    \"os\": \"").append(esc(h.os)).append("\",\n")
                        .append("    \"savedAt\": \"").append(esc(h.savedAt)).append("\",\n")
                        .append("    \"ports\": \"").append(esc(h.portsToString())).append("\",\n")
                        .append("    \"notes\": \"").append(esc(h.notes)).append("\",\n")
                        .append("    \"category\": \"").append(esc(cat)).append("\"\n")
                        .append("  }");
                first = false;
            }
        }
        sb.append("\n]");
        Files.writeString(file, sb.toString());
        return file;
    }

    // ── HTML Report Export ────────────────────────────────────────────────

    public static Path exportHtml(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve("hosts_report_" + LocalDateTime.now().format(FMT) + ".html");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>NetTool v3 - Host-Report</title>\n<style>\n")
                .append("body{font-family:monospace;background:#0D0F0F;color:#E8E4D8;margin:0;padding:20px}\n")
                .append("h1{color:#D4A020;border-bottom:1px solid #282E2A;padding-bottom:8px}\n")
                .append("h2{color:#8CAA80;margin-top:30px}\n")
                .append("table{border-collapse:collapse;width:100%;margin-bottom:20px}\n")
                .append("th{background:#0A1608;color:#D4A020;padding:8px 12px;text-align:left;border-bottom:1px solid #D4A020}\n")
                .append("td{padding:6px 12px;border-bottom:1px solid #282E2A;font-size:12px}\n")
                .append("tr:nth-child(even){background:#0E1210} tr:nth-child(odd){background:#0D0F0D}\n")
                .append("tr:hover{background:#1A2018}\n")
                .append(".win{color:#60A8F0}.lin{color:#7EE87E}.mac{color:#D0D0D8}\n")
                .append(".and{color:#78D878}.net{color:#FFA030}.prn{color:#E8C840}\n")
                .append(".info{color:#A0C0A0;font-size:11px;margin-bottom:20px}\n")
                .append("</style>\n</head>\n<body>\n");
        html.append("<h1>// NetTool v3  –  Host-Report</h1>\n");
        html.append("<p class=\"info\">Erstellt: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append("</p>\n");

        int totalHosts = NetworkStore.getInstance().getAllHosts().size();
        html.append("<p class=\"info\">Gesamt: <b>").append(totalHosts).append("</b> Hosts</p>\n");

        NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .forEach(cat -> {
                    List<HostResult> hosts = NetworkStore.getInstance().getAll(cat);
                    html.append("<h2>").append(htmlEsc(cat)).append("  (")
                            .append(hosts.size()).append(" Hosts)</h2>\n");
                    html.append("<table><tr><th>IP</th><th>Hostname</th><th>OS/Gerät</th>")
                            .append("<th>Ports</th><th>Gespeichert</th><th>Notiz</th></tr>\n");
                    hosts.forEach(h -> {
                        String osClass = osClass(h.os);
                        html.append("<tr>")
                                .append("<td>").append(htmlEsc(h.ip)).append("</td>")
                                .append("<td>").append(htmlEsc(h.hostname)).append("</td>")
                                .append("<td class=\"").append(osClass).append("\">").append(htmlEsc(h.os)).append("</td>")
                                .append("<td>").append(htmlEsc(h.portsToString())).append("</td>")
                                .append("<td>").append(htmlEsc(h.savedAt)).append("</td>")
                                .append("<td>").append(htmlEsc(h.notes)).append("</td>")
                                .append("</tr>\n");
                    });
                    html.append("</table>\n");
                });
        html.append("</body></html>");
        Files.writeString(file, html.toString());
        return file;
    }

    // ── ZIP Backup ────────────────────────────────────────────────────────

    public static Path exportBackup(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path zipFile = outDir.resolve("nettool_backup_" + LocalDateTime.now().format(FMT) + ".zip");
        Path srcDir  = NetworkStorePersistence.resolveTxtDir();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new FileOutputStream(zipFile.toFile()))) {
            Files.walk(srcDir)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            String entryName = srcDir.relativize(p).toString().replace('\\', '/');
                            zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException ignored) {}
                    });
        }
        return zipFile;
    }

    // ── CSV Import ────────────────────────────────────────────────────────

    /** Importiert eine CSV-Datei (Format wie exportCsv). Gibt Anzahl importierter Hosts zurück. */
    public static int importCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        int count = 0;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("IP;")) continue;
            String[] p = line.split(";", 7);
            if (p.length < 1) continue;
            String ip   = p[0].trim();
            String hn   = p.length > 1 ? p[1].trim() : ip;
            String os   = p.length > 2 ? p[2].trim() : "";
            String date = p.length > 3 ? p[3].trim() : "";
            String notes= p.length > 5 ? p[5].trim() : "";
            String cat  = p.length > 6 ? p[6].trim() : "Import";
            if (ip.isBlank()) continue;
            HostResult h = new HostResult(ip, hn, os, date, null, notes);
            if (!NetworkStore.getInstance().getNetworkNames().contains(cat))
                NetworkStore.getInstance().createNetwork(cat, "");
            if (NetworkStore.getInstance().save(h, cat)) count++;
        }
        return count;
    }

    // ── JSON Import ───────────────────────────────────────────────────────

    /** Importiert eine JSON-Datei (Format wie exportJson). Gibt Anzahl importierter Hosts zurück. */
    public static int importJson(Path file) throws IOException {
        String content = Files.readString(file);
        int count = 0;
        // Einfacher JSON-Parser für unser Format (kein externer Parser)
        String[] objects = content.split("\\},\\s*\\{");
        for (String obj : objects) {
            String ip   = extractJsonStr(obj, "ip");
            String hn   = extractJsonStr(obj, "hostname");
            String os   = extractJsonStr(obj, "os");
            String date = extractJsonStr(obj, "savedAt");
            String notes= extractJsonStr(obj, "notes");
            String cat  = extractJsonStr(obj, "category");
            if (ip == null || ip.isBlank()) continue;
            if (cat == null || cat.isBlank()) cat = "Import";
            HostResult h = new HostResult(ip,
                    hn != null ? hn : ip,
                    os != null ? os : "",
                    date, null,
                    notes != null ? notes : "");
            if (!NetworkStore.getInstance().getNetworkNames().contains(cat))
                NetworkStore.getInstance().createNetwork(cat, "");
            if (NetworkStore.getInstance().save(h, cat)) count++;
        }
        return count;
    }

    // ── ZIP Restore ───────────────────────────────────────────────────────

    /** Stellt ein Backup-ZIP wieder her. Überschreibt vorhandene Daten. */
    public static int restoreBackup(Path zipFile) throws IOException {
        Path targetDir = NetworkStorePersistence.resolveTxtDir();
        int[] count = {0};
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new FileInputStream(zipFile.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = targetDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(targetDir)) continue; // Zip-Slip-Schutz
                Files.createDirectories(dest.getParent());
                Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                count[0]++;
            }
        }
        return count[0];
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

    private static String csv(String s) {
        if (s == null) return "";
        s = s.replace(";", ",").replace("\n", " ").replace("\r", "");
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String osClass(String os) {
        if (os == null) return "";
        String l = os.toLowerCase();
        if (l.contains("windows")) return "win";
        if (l.contains("linux") || l.contains("unix")) return "lin";
        if (l.contains("mac") || l.contains("apple")) return "mac";
        if (l.contains("android")) return "and";
        if (l.contains("router") || l.contains("switch")) return "net";
        if (l.contains("drucker")) return "prn";
        return "";
    }

    private static String extractJsonStr(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"'))
            if (json.charAt(start++) == '"') break;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) { sb.append(json.charAt(++i)); }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }
}
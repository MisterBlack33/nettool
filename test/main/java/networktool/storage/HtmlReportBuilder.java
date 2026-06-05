package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Erstellt den HTML-Report. Ausgelagert aus DataExporter. */
final class HtmlReportBuilder {

    private HtmlReportBuilder() {}

    static String build() {
        StringBuilder html = new StringBuilder();
        appendHead(html);
        appendBody(html);
        html.append("</body></html>");
        return html.toString();
    }

    private static void appendHead(StringBuilder html) {
        html.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n<title>NetTool v3 - Host-Report</title>\n<style>\n")
                .append("body{font-family:monospace;background:#0D0F0F;color:#E8E4D8;margin:0;padding:20px}\n")
                .append("h1{color:#D4A020}h2{color:#8CAA80;margin-top:30px}\n")
                .append("table{border-collapse:collapse;width:100%;margin-bottom:20px}\n")
                .append("th{background:#0A1608;color:#D4A020;padding:8px 12px;text-align:left;border-bottom:1px solid #D4A020}\n")
                .append("td{padding:6px 12px;border-bottom:1px solid #282E2A;font-size:12px}\n")
                .append("tr:nth-child(even){background:#0E1210}tr:nth-child(odd){background:#0D0F0D}\n")
                .append(".win{color:#60A8F0}.lin{color:#7EE87E}.mac{color:#D0D0D8}")
                .append(".and{color:#78D878}.net{color:#FFA030}.prn{color:#E8C840}\n")
                .append("</style>\n</head>\n<body>\n");
    }

    private static void appendBody(StringBuilder html) {
        html.append("<h1>// NetTool v3 – Host-Report</h1>\n");
        html.append("<p style=\"color:#A0C0A0;font-size:11px\">Erstellt: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                .append("</p>\n");
        html.append("<p style=\"color:#A0C0A0;font-size:11px\">Gesamt: <b>")
                .append(NetworkStore.getInstance().getAllHosts().size())
                .append("</b> Hosts</p>\n");

        appendTable(html);
    }

    private static void appendTable(StringBuilder html) {
        String headerRow = "<tr><th>IP</th><th>Hostname</th><th>OS</th>"
                + "<th>Ports</th><th>Gespeichert</th><th>Notiz</th></tr>\n";

        List<String> networks = NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(NetworkStore.ALL_CATEGORY))
                .toList();

        if (networks.isEmpty()) {
            // Leere Tabelle — garantiert <table> im Output
            html.append("<table>").append(headerRow).append("</table>\n");
            return;
        }

        for (String cat : networks) {
            List<HostResult> hosts = NetworkStore.getInstance().getAll(cat);
            html.append("<h2>").append(esc(cat)).append(" (").append(hosts.size()).append(")</h2>\n");
            html.append("<table>").append(headerRow);
            for (HostResult h : hosts) {
                html.append("<tr>")
                        .append("<td>").append(esc(h.ip)).append("</td>")
                        .append("<td>").append(esc(h.hostname)).append("</td>")
                        .append("<td class=\"").append(osClass(h.os)).append("\">").append(esc(h.os)).append("</td>")
                        .append("<td>").append(esc(h.portsToString())).append("</td>")
                        .append("<td>").append(esc(h.savedAt)).append("</td>")
                        .append("<td>").append(esc(h.notes)).append("</td>")
                        .append("</tr>\n");
            }
            html.append("</table>\n");
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String osClass(String os) {
        if (os == null) return "";
        String l = os.toLowerCase();
        if (l.contains("windows"))                   return "win";
        if (l.contains("linux") || l.contains("unix")) return "lin";
        if (l.contains("mac")   || l.contains("apple")) return "mac";
        if (l.contains("android"))                   return "and";
        if (l.contains("router") || l.contains("switch")) return "net";
        if (l.contains("drucker"))                   return "prn";
        return "";
    }
}